//package com.stellarix.hse.configuration;
//
//import com.google.auth.oauth2.GoogleCredentials;
//import com.google.firebase.FirebaseApp;
//import com.google.firebase.FirebaseOptions;
//import org.springframework.context.annotation.Configuration;
//
//import jakarta.annotation.PostConstruct;
//
//import java.io.FileInputStream;
//import java.io.IOException;
//
//@Configuration
//public class FirebaseConfig {
//
//    @PostConstruct
//    public void initialize() throws IOException {
//        FileInputStream serviceAccount = new FileInputStream("src/main/resources/stx-hse-firebase-adminsdk-fbsvc-fd0d27385b.json");
//
//        FirebaseOptions options = FirebaseOptions.builder()
//                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
//                .build();
//
//        if (FirebaseApp.getApps().isEmpty()) {
//            FirebaseApp.initializeApp(options);
//        }
//    }
//}