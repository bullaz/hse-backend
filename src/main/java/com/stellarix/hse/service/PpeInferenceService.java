package com.stellarix.hse.service;

import ai.onnxruntime.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.List;

@Slf4j
@Service
public class PpeInferenceService {

    @Value("${ppe.models.det}")
    private String detPath;

    @Value("${ppe.models.glasses}")
    private String glassesPath;

    @Value("${ppe.models.shoes}")
    private String shoesPath;

    private OrtEnvironment env;
    private OrtSession det;
    private OrtSession glasses;
    private OrtSession shoes;
    private boolean modelsLoaded = false;

    // Class index → PPE code (index 3 = person, skip)
    private static final String[] CLS_CODE = {
        "SAFETY_GLASSES", "GLOVES", "HELMET", null, "HIGH_VIS_VEST", "SAFETY_SHOES"
    };
    private static final float[] THR = { 0.03f, 0.04f, 0.04f, 0.05f, 0.03f, 0.04f };

    @PostConstruct
    public void init() {
        try {
            env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            det     = createSession(detPath,     opts);
            glasses = createSession(glassesPath, opts);
            shoes   = createSession(shoesPath,   opts);
            modelsLoaded = true;
            log.info("PPE ONNX models loaded: det={}, glasses={}, shoes={}", detPath, glassesPath, shoesPath);
        } catch (Exception e) {
            log.warn("PPE ONNX models could not be loaded (offline inference unavailable): {}", e.getMessage());
        }
    }

    private OrtSession createSession(String path, OrtSession.SessionOptions opts) throws Exception {
        File f = new File(path);
        if (f.exists()) return env.createSession(f.getAbsolutePath(), opts);
        // Fall back to classpath resource (e.g. src/main/resources/rtdetr-model/...)
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is != null) return env.createSession(is.readAllBytes(), opts);
        }
        throw new java.io.FileNotFoundException("ONNX model not found at path or classpath: " + path);
    }

    public boolean isReady() { return modelsLoaded; }

    // ── Public API ─────────────────────────────────────────────────────────────

    public record ItemResult(
        String ppeCode, String ppeLabel,
        boolean detected, float confidence,
        boolean partialWarning, boolean wrongType
    ) {}

    public record AnalysisResult(List<ItemResult> items, byte[] annotatedJpeg) {}

    public record RequiredItem(String code, String label) {}

    public AnalysisResult analyze(byte[] imageBytes, List<RequiredItem> required, boolean isFrench)
            throws Exception {
        if (!modelsLoaded) throw new IllegalStateException("PPE models not loaded");

        BufferedImage orig = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (orig == null) throw new IllegalArgumentException("Cannot decode image");

        BufferedImage img640 = resize(orig, 640, 640);
        List<Det> raw = runDetector(img640, orig.getWidth(), orig.getHeight());
        List<Det> filtered = filterByClass(raw);
        List<Det> inPerson = filterByPersonBox(filtered);
        List<Det> refined = refine(inPerson, orig, isFrench);

        List<ItemResult> results = toItemResults(required, refined);
        byte[] annotated = drawBoxes(copy(orig), refined, isFrench);

        return new AnalysisResult(results, annotated);
    }

    // ── RT-DETR ───────────────────────────────────────────────────────────────

    private List<Det> runDetector(BufferedImage img640, int origW, int origH) throws OrtException {
        float scaleW = (float) origW / 640f;
        float scaleH = (float) origH / 640f;

        FloatBuffer buf = FloatBuffer.allocate(3 * 640 * 640);
        for (int y = 0; y < 640; y++) {
            for (int x = 0; x < 640; x++) {
                int rgb = img640.getRGB(x, y);
                buf.put(((rgb >> 16) & 0xff) / 255f); // R
            }
        }
        for (int y = 0; y < 640; y++) {
            for (int x = 0; x < 640; x++) {
                int rgb = img640.getRGB(x, y);
                buf.put(((rgb >> 8) & 0xff) / 255f); // G
            }
        }
        for (int y = 0; y < 640; y++) {
            for (int x = 0; x < 640; x++) {
                int rgb = img640.getRGB(x, y);
                buf.put((rgb & 0xff) / 255f); // B
            }
        }
        buf.rewind();

        String inputName = det.getInputNames().iterator().next();
        OnnxTensor tensor = OnnxTensor.createTensor(env, buf, new long[]{1, 3, 640, 640});
        List<Det> dets = new ArrayList<>();
        try (OrtSession.Result result = det.run(Collections.singletonMap(inputName, tensor))) {
            // outputs[0] = logits [1, Q, 6], outputs[1] = boxes [1, Q, 4] cxcywh normalised
            float[][][] logitsAll = (float[][][]) ((OnnxTensor) result.get(0)).getValue();
            float[][][] boxesAll  = (float[][][]) ((OnnxTensor) result.get(1)).getValue();
            float[][] logitsQ = logitsAll[0];
            float[][] boxesQ  = boxesAll[0];

            for (int i = 0; i < logitsQ.length; i++) {
                float maxScore = -1f;
                int maxCls = 0;
                for (int c = 0; c < logitsQ[i].length; c++) {
                    float s = (float)(1.0 / (1.0 + Math.exp(-logitsQ[i][c])));
                    if (s > maxScore) { maxScore = s; maxCls = c; }
                }
                if (maxScore < THR[maxCls]) continue;

                float cx = boxesQ[i][0], cy = boxesQ[i][1];
                float bw = boxesQ[i][2], bh = boxesQ[i][3];
                float x1 = Math.max(0f, (cx - bw / 2) * 640 * scaleW);
                float y1 = Math.max(0f, (cy - bh / 2) * 640 * scaleH);
                float x2 = Math.min(origW, (cx + bw / 2) * 640 * scaleW);
                float y2 = Math.min(origH, (cy + bh / 2) * 640 * scaleH);
                if (x2 <= x1 || y2 <= y1) continue;
                dets.add(new Det(maxCls, maxScore, x1, y1, x2, y2, null, null));
            }
        } finally {
            tensor.close();
        }
        return dets;
    }

    // ── Filtering ─────────────────────────────────────────────────────────────

    private List<Det> filterByClass(List<Det> raw) {
        Map<Integer, List<Det>> byClass = new HashMap<>();
        for (Det d : raw) byClass.computeIfAbsent(d.cls, k -> new ArrayList<>()).add(d);

        List<Det> result = new ArrayList<>();
        for (var entry : byClass.entrySet()) {
            int cls = entry.getKey();
            List<Det> sorted = new ArrayList<>(entry.getValue());
            sorted.sort((a, b) -> Float.compare(area(b), area(a)));
            int keep = (cls == 1 || cls == 5) ? 2 : 1; // gloves/shoes: keep 2
            result.addAll(sorted.subList(0, Math.min(keep, sorted.size())));
        }
        return result;
    }

    // ── Person-box containment filter ─────────────────────────────────────────

    // PPE boxes must be inside the largest person box, with up to 15% overflow tolerance.
    private static final float PERSON_TOL = 0.15f;

    private List<Det> filterByPersonBox(List<Det> dets) {
        Det person = dets.stream().filter(d -> d.cls == 3).findFirst().orElse(null);
        if (person == null) return dets; // no person detected — keep all PPEs
        return dets.stream()
                .filter(d -> d.cls == 3 || nearlyInside(d, person))
                .collect(java.util.stream.Collectors.toList());
    }

    private boolean nearlyInside(Det ppe, Det person) {
        float tolW = PERSON_TOL * (ppe.x2 - ppe.x1);
        float tolH = PERSON_TOL * (ppe.y2 - ppe.y1);
        return ppe.x1 >= person.x1 - tolW
            && ppe.x2 <= person.x2 + tolW
            && ppe.y1 >= person.y1 - tolH
            && ppe.y2 <= person.y2 + tolH;
    }

    // ── Binary classifiers ────────────────────────────────────────────────────

    private List<Det> refine(List<Det> dets, BufferedImage orig, boolean isFrench) throws OrtException {
        List<Det> result = new ArrayList<>();
        for (Det d : dets) {
            if (d.cls == 0) {
                float[] cr = runClassifier(crop(orig, d), glasses, false); // CE
                result.add(d.withRefinement(cr[0] >= 0.65f, cr[0]));
            } else if (d.cls == 5) {
                float[] cr = runClassifier(crop(orig, d), shoes, true);    // BCE
                result.add(d.withRefinement(cr[0] >= 0.60f, cr[0]));
            } else {
                result.add(d);
            }
        }
        return result;
    }

    /**
     * @param bce true = single sigmoid output, false = 2-logit softmax
     * @return [probPos]
     */
    private float[] runClassifier(BufferedImage cropImg, OrtSession sess, boolean bce)
            throws OrtException {
        BufferedImage r = resize(cropImg, 224, 224);
        float[] mean = {0.485f, 0.456f, 0.406f};
        float[] std  = {0.229f, 0.224f, 0.225f};
        FloatBuffer buf = FloatBuffer.allocate(3 * 224 * 224);
        for (int c = 0; c < 3; c++) {
            for (int y = 0; y < 224; y++) {
                for (int x = 0; x < 224; x++) {
                    int rgb = r.getRGB(x, y);
                    float v = switch (c) {
                        case 0 -> ((rgb >> 16) & 0xff) / 255f;
                        case 1 -> ((rgb >> 8)  & 0xff) / 255f;
                        default -> (rgb & 0xff)         / 255f;
                    };
                    buf.put((v - mean[c]) / std[c]);
                }
            }
        }
        buf.rewind();

        String inputName = sess.getInputNames().iterator().next();
        OnnxTensor tensor = OnnxTensor.createTensor(env, buf, new long[]{1, 3, 224, 224});
        try (OrtSession.Result res = sess.run(Collections.singletonMap(inputName, tensor))) {
            float[][] logits = (float[][]) ((OnnxTensor) res.get(0)).getValue();
            if (bce) {
                float p = (float)(1.0 / (1.0 + Math.exp(-logits[0][0])));
                return new float[]{p};
            } else {
                float l0 = logits[0][0], l1 = logits[0][1];
                float maxL = Math.max(l0, l1);
                float e0 = (float) Math.exp(l0 - maxL), e1 = (float) Math.exp(l1 - maxL);
                return new float[]{e1 / (e0 + e1)};
            }
        } finally {
            tensor.close();
        }
    }

    // ── Result mapping ─────────────────────────────────────────────────────────

    private List<ItemResult> toItemResults(List<RequiredItem> required, List<Det> dets) {
        List<ItemResult> out = new ArrayList<>();
        for (RequiredItem item : required) {
            int clsIdx = codeToIdx(item.code());
            if (clsIdx < 0) {
                out.add(new ItemResult(item.code(), item.label(), true, 0f, false, false));
                continue;
            }
            List<Det> matching = dets.stream().filter(d -> d.cls == clsIdx).toList();
            boolean needsPair = clsIdx == 1 || clsIdx == 5;

            if (matching.isEmpty()) {
                out.add(new ItemResult(item.code(), item.label(), false, 0f, false, false));
            } else if (needsPair && matching.size() == 1) {
                Det d = matching.get(0);
                float conf = d.refinedScore != null ? d.refinedScore : d.score;
                out.add(new ItemResult(item.code(), item.label(), true, conf, true, false));
            } else {
                if (clsIdx == 0 || clsIdx == 5) {
                    List<Det> safeOnes = matching.stream()
                        .filter(d -> Boolean.TRUE.equals(d.isSafetyGear)).toList();
                    if (safeOnes.isEmpty()) {
                        float conf = matching.get(0).refinedScore != null
                            ? matching.get(0).refinedScore : matching.get(0).score;
                        out.add(new ItemResult(item.code(), item.label(), false, conf, false, true));
                    } else {
                        float conf = safeOnes.stream()
                            .map(d -> d.refinedScore != null ? d.refinedScore : d.score)
                            .max(Float::compare).orElse(0f);
                        out.add(new ItemResult(item.code(), item.label(), true, conf, false, false));
                    }
                } else {
                    float conf = matching.stream()
                        .map(d -> d.score).max(Float::compare).orElse(0f);
                    out.add(new ItemResult(item.code(), item.label(), true, conf, false, false));
                }
            }
        }
        return out;
    }

    private int codeToIdx(String code) {
        return switch (code) {
            case "SAFETY_GLASSES" -> 0;
            case "GLOVES"         -> 1;
            case "HELMET"         -> 2;
            case "HIGH_VIS_VEST"  -> 4;
            case "SAFETY_SHOES"   -> 5;
            default               -> -1;
        };
    }

    // ── Annotation drawing ────────────────────────────────────────────────────

    private byte[] drawBoxes(BufferedImage img, List<Det> dets, boolean isFrench) throws Exception {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int thick = Math.max(2, img.getWidth() / 250);
        g.setStroke(new BasicStroke(thick));
        Font font = new Font("SansSerif", Font.BOLD, Math.max(12, img.getWidth() / 50));
        g.setFont(font);

        for (Det d : dets) {
            boolean wrongType = (d.cls == 0 || d.cls == 5) && Boolean.FALSE.equals(d.isSafetyGear);
            Color color = wrongType ? new Color(255, 120, 0) : classColor(d.cls);
            g.setColor(color);
            g.drawRect((int) d.x1, (int) d.y1, (int)(d.x2 - d.x1), (int)(d.y2 - d.y1));

            String label = label(d, isFrench);
            FontMetrics fm = g.getFontMetrics();
            int ty = Math.max(0, (int) d.y1 - fm.getHeight());
            g.fillRect((int) d.x1, ty, fm.stringWidth(label) + 6, fm.getHeight() + 4);
            g.setColor(Color.BLACK);
            g.drawString(label, (int) d.x1 + 3, ty + fm.getAscent() + 2);
        }
        g.dispose();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", bos);
        return bos.toByteArray();
    }

    private Color classColor(int cls) {
        return switch (cls) {
            case 0 -> new Color(0, 220, 255);
            case 1 -> new Color(0, 255, 120);
            case 2 -> new Color(255, 200, 0);
            case 3 -> new Color(100, 130, 255);
            case 4 -> new Color(255, 80, 200);
            case 5 -> new Color(80, 255, 80);
            default -> Color.WHITE;
        };
    }

    private String label(Det d, boolean fr) {
        if (d.cls == 0)
            return Boolean.TRUE.equals(d.isSafetyGear)
                ? (fr ? "Lun. protection" : "Safety glasses")
                : (fr ? "Lunettes lambda" : "Regular glasses");
        if (d.cls == 5)
            return Boolean.TRUE.equals(d.isSafetyGear)
                ? (fr ? "Chss. sécurité" : "Safety shoe")
                : (fr ? "Chss. ordinaire" : "Regular shoe");
        String[] fr_labels = {"Lunettes", "Gant", "Casque", "Personne", "Gilet HV", "Chaussure"};
        String[] en_labels = {"Glasses", "Glove", "Helmet", "Person", "HV Vest", "Shoe"};
        return fr ? fr_labels[d.cls] : en_labels[d.cls];
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private byte[] makeSideBySide(BufferedImage orig, byte[] annotatedJpeg) throws Exception {
        BufferedImage ann = ImageIO.read(new ByteArrayInputStream(annotatedJpeg));
        int targetH = 600;
        int origW  = (int)(orig.getWidth()  * (double) targetH / orig.getHeight());
        int annW   = (int)(ann.getWidth()   * (double) targetH / ann.getHeight());
        int divider = 4;
        BufferedImage canvas = new BufferedImage(origW + divider + annW, targetH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setColor(new Color(40, 40, 40));
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        g.drawImage(orig, 0, 0, origW, targetH, null);
        g.drawImage(ann, origW + divider, 0, annW, targetH, null);
        g.dispose();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(canvas, "jpg", bos);
        return bos.toByteArray();
    }

    private BufferedImage resize(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    private BufferedImage crop(BufferedImage src, Det d) {
        int x = (int) d.x1, y = (int) d.y1;
        int w = Math.max(1, (int)(d.x2 - d.x1)), h = Math.max(1, (int)(d.y2 - d.y1));
        x = Math.max(0, Math.min(x, src.getWidth() - 1));
        y = Math.max(0, Math.min(y, src.getHeight() - 1));
        w = Math.min(w, src.getWidth() - x);
        h = Math.min(h, src.getHeight() - y);
        return src.getSubimage(x, y, w, h);
    }

    private BufferedImage copy(BufferedImage src) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return copy;
    }

    private float area(Det d) { return (d.x2 - d.x1) * (d.y2 - d.y1); }

    // ── Internal detection record ─────────────────────────────────────────────

    private record Det(int cls, float score, float x1, float y1, float x2, float y2,
                       Boolean isSafetyGear, Float refinedScore) {
        Det withRefinement(boolean safety, float rScore) {
            return new Det(cls, score, x1, y1, x2, y2, safety, rScore);
        }
    }
}
