package com.stellarix.hse.controller;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stellarix.hse.entity.AuthRequest;
import com.stellarix.hse.entity.Commentaire;
import com.stellarix.hse.entity.Hse;
import com.stellarix.hse.entity.MesureControle;
import com.stellarix.hse.entity.Question;
import com.stellarix.hse.entity.Toko5;
import com.stellarix.hse.repository.CommentaireRepository;
import com.stellarix.hse.repository.HseRepository;
import com.stellarix.hse.repository.MesureControleRepository;
import com.stellarix.hse.repository.QuestionRepository;
import com.stellarix.hse.repository.Toko5Repository;
import com.stellarix.hse.service.AccountService;
import com.stellarix.hse.service.ErrorResponse;
import com.stellarix.hse.service.JwtService;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(path="/hse")
@Slf4j
public class HseApi {
	
	private AccountService service;

    private JwtService jwtService;

    private AuthenticationManager authenticationManager;
    
    private final HseRepository hseRepository;
    
    private UserDetailsService userDetailsService;
	
    private Toko5Repository toko5Repository;
    
    private QuestionRepository questionRepository;
    
    private CommentaireRepository commentaireRepository;
    
    private MesureControleRepository mesureControleRepository;
    
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    public HseApi(AccountService service, JwtService jwtService, AuthenticationManager authenticationManager, HseRepository hseRepository, 
    		UserDetailsService userDetailsService, Toko5Repository toko5Repository, QuestionRepository questionRepository,
    		CommentaireRepository commentaireRepository, MesureControleRepository mesureControleRepository, SimpMessagingTemplate messagingTemplate) {
    	this.service = service;
    	this.jwtService = jwtService;
    	this.authenticationManager = authenticationManager;
    	this.hseRepository = hseRepository;
    	this.userDetailsService = userDetailsService;
    	this.toko5Repository = toko5Repository;
    	this.questionRepository = questionRepository;
    	this.commentaireRepository = commentaireRepository;
    	this.mesureControleRepository = mesureControleRepository;
    	this.messagingTemplate = messagingTemplate;
    }
    
    
	@CrossOrigin
	@GetMapping(path="test")
	public String test() throws Exception {
        return "You're able to access unprotected stellarix hse url";
    }
	
	
	@PostMapping("/signin")
    public ResponseEntity<Map<String, String>> authenticateAndGetToken(@RequestBody AuthRequest authRequest, HttpServletResponse response) throws StreamWriteException, DatabindException, IOException {
        log.info("SIGN IN API");
		Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
        );
        if (authentication.isAuthenticated()) {
        	Optional<Hse> account = hseRepository.findByUsernameOrEmail(authRequest.getUsername(), authRequest.getUsername());
            Hse user = account.get();
          //return jwtService.generateToken(user.getEmail());
            Map<String, String> tokens =  jwtService.generateToken(user.getEmail());
            String refreshToken = tokens.get("refresh_token");
            tokens.remove("refresh_token");
            
            
            ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                    .httpOnly(true)
                    .secure(false) // Set to true in production with HTTPS
                    .path("/")
                    .maxAge(7 * 24 * 60 * 60)
                    .sameSite("Lax") // Or "Strict" or "None" (None requires Secure=true)
                    .build();
                
                return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(tokens);
            
        } else {
            throw new UsernameNotFoundException("Invalid user request!");
        }
    }
	
//	@PostMapping("/signup")
//    public String addNewUser(@RequestBody UserInfo userInfo) {
//        return service.addUser(userInfo);
//    }
	
	
	@GetMapping("/protected_url")
	//@PreAuthorize("hasAuthority('HSE')")
	public String testProtected() {
		try {
	        messagingTemplate.convertAndSend("/topic/test", "websocket test");
	        log.info("WebSocket notification sent successfully");
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification", e);
        }
		return "You're able to access protected HSE url that require authentication and authorization!";
	}
	
	@PostMapping("/verify_token")
	public Map<String, Boolean> verifyToken(@RequestHeader(value = "Authorization", required = false) String token){
		log.info("calling verify token api");
		token = token.substring(7);
		//log.info(jwtService.validateToken(token)? "it is fk true" : "it is fk false");
		boolean state = jwtService.validateToken(token);
		log.info(Boolean.valueOf(state).toString());
		Map<String, Boolean> temp = new HashMap<>();
		temp.put("token_state", Boolean.valueOf(state));
		return temp;
	}
	
	
	@PostMapping("/refresh_token")
    public ResponseEntity<?> refreshToken(
            @CookieValue(value = "refresh_token", required = false) String refreshTokenCookie, 
            HttpServletRequest request,
            HttpServletResponse response) {
		log.info(String.format("calling refresh token api %s",refreshTokenCookie));
		
		
		log.info("=== REQUEST HEADERS ===");
	    Collections.list(request.getHeaderNames())
	        .forEach(headerName -> {
	            String headerValue = request.getHeader(headerName);
	            log.info("{}: {}", headerName, headerValue);
	        });
	    
	    // Check cookies
	    Cookie[] cookies = request.getCookies();
	    if (cookies == null) {
	        log.warn("=== NO COOKIES IN REQUEST ===");
	    } else {
	        log.info("=== COOKIES FOUND ({}) ===", cookies.length);
	        for (Cookie cookie : cookies) {
	            log.info("Cookie - Name: '{}', Value: '{}...', Path: {}, Domain: {}, Secure: {}, HttpOnly: {}",
	                cookie.getName(),
	                cookie.getValue() != null && cookie.getValue().length() > 10 
	                    ? cookie.getValue().substring(0, 10) + "..." 
	                    : cookie.getValue(),
	                cookie.getPath(),
	                cookie.getDomain(),
	                cookie.getSecure(),
	                cookie.isHttpOnly() // Note: This might not be accurate in request
	            );
	        }
	    }
	    
	    
		
        String refreshToken = refreshTokenCookie ;
        
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Refresh token missing");
        }
        
        try {
            // Validate refresh token (different validation than access token)
            if (!jwtService.validateToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid refresh token");
            }
            
            String username = jwtService.extractUsername(refreshToken);
            
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
//            // Check if refresh token is revoked/blacklisted
//            if (tokenBlacklistService.isTokenRevoked(refreshToken)) {
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body(new ErrorResponse("Token revoked"));
//            }
            
            String newAccessToken = jwtService.generateAccessToken(username);
            
            // Optionally generate new refresh token (rotate refresh tokens)
            //String newRefreshToken = jwtService.generateRefreshToken(username);
            
            // Revoke old refresh token
            //tokenBlacklistService.revokeToken(refreshToken);
            
            // Set refresh token as HTTP-only cookie
            //Cookie refreshCookie = new Cookie("refresh_token", newRefreshToken);
            //refreshCookie.setHttpOnly(true);
//            refreshCookie.setSecure(true); // Use HTTPS in production
//            refreshCookie.setPath("/api/auth/refresh");
//            refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
//            response.addCookie(refreshCookie);
            
            // Return new access token
//            AuthResponse authResponse = AuthResponse.builder()
//                .accessToken(newAccessToken)
//                .tokenType("Bearer")
//                .expiresIn(3600) // 1 hour
//                .build();
            Map<String,String> responseMap = new HashMap<>();
            responseMap.put("access_token",newAccessToken);
            return ResponseEntity.ok(responseMap);
            
        } catch (ExpiredJwtException e) { //no expiredjwtexception here throwed anywhere 
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).
                body("your refresh token is expired");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Server error");
        }
    }
	
	@PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/hse/refresh_token");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return ResponseEntity.ok("Logged out successfully");
    }
	
	
	@GetMapping("/toko5s")
	public List<Toko5> getListToko5ByDate(@RequestParam("date") String date) throws Exception{
		//add string format verification;
		return toko5Repository.findByDate(date);	
	}
	
	
	@PreAuthorize("permitAll()")
	@PostMapping("/toko5s")
	public Toko5 newToko5(@RequestBody Toko5 toko5) throws Exception{
		log.info(toko5.toString());
		Toko5 savedToko5 = toko5Repository.save(toko5);
		
		try {
	        messagingTemplate.convertAndSend("/topic/toko5s/new", savedToko5);
	        log.info("WebSocket notification sent successfully");
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification", e);
        }
		return savedToko5;
	}
	
	@GetMapping("/toko5s/toko5/{id}")
	public Toko5 getToko5(@PathVariable("id") String id) throws Exception{
		Optional<Toko5> opt = toko5Repository.findById(UUID.fromString(id));
		if(opt.isEmpty()) return null;
		List<Question> problems = questionRepository.findToko5ListProblem(UUID.fromString(id));
		Toko5 toko5 = opt.get();
		toko5.setListProblem(problems);
		return opt.get();
	}
	
	
	@PutMapping("/toko5s/toko5/{id}")
	public Toko5 updateToko5(@PathVariable("id") String id, Toko5 updated) throws Exception{
		Optional<Toko5> opt = toko5Repository.findById(UUID.fromString(id));
		if(opt.isEmpty()) return null;
		return toko5Repository.save(updated);
	}
	
	
	@GetMapping("/toko5s/toko5/{id}/problems")
	public List<Question> getListProblem(@PathVariable("id") String id) throws Exception{
		return questionRepository.findToko5ListProblem(UUID.fromString(id));
	}
	
	@GetMapping("/toko5s/toko5/{id}/comments")
	public List<Commentaire> getListCommentaire(@PathVariable("id") String id) throws Exception{
		return commentaireRepository.findByToko5Id(UUID.fromString(id));
	}
	
	@GetMapping("/toko5s/toko5/{id}/mesures")
	public List<MesureControle> getListMesure(@PathVariable("id") String id) throws Exception{
		return mesureControleRepository.findByToko5Id(UUID.fromString(id));
	}
	
	@DeleteMapping("/toko5s/commentaires/{id}")
	public String deleteCommentaire(@PathVariable("id") int comId) throws Exception{
		commentaireRepository.deleteById(comId);
		return "Comment deleted successfully";
	}
	
	@PutMapping("/toko5s/commentaires/{id}")
	public String updateCommentaire(@PathVariable("id") int id, @RequestParam("commentaire") String commentaire) throws Exception{
		Optional<Commentaire> opt = commentaireRepository.findById(id);
		if(opt.isPresent()) {
			Commentaire com = opt.get();
			com.setCommentaire(commentaire);
			commentaireRepository.save(com);
			return "comment updated successfully";
		}
		return "that toko5 comment doesn't exist";
	}
}
	