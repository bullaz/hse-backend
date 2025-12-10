package com.stellarix.hse.controller;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stellarix.hse.entity.AuthRequest;
import com.stellarix.hse.entity.Hse;
import com.stellarix.hse.repository.HseRepository;
import com.stellarix.hse.service.AccountService;
import com.stellarix.hse.service.ErrorResponse;
import com.stellarix.hse.service.JwtService;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
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
	

    @Autowired
    public HseApi(AccountService service, JwtService jwtService, AuthenticationManager authenticationManager, HseRepository hseRepository, UserDetailsService userDetailsService) {
    	this.service = service;
    	this.jwtService = jwtService;
    	this.authenticationManager = authenticationManager;
    	this.hseRepository = hseRepository;
    	this.userDetailsService = userDetailsService;
    }
    
    
	@CrossOrigin
	@GetMapping(path="test")
	public String test() throws Exception {
        return "You're able to access unprotected url";
    }
	
	
	@PostMapping("/signin")
    public void authenticateAndGetToken(@RequestBody AuthRequest authRequest, HttpServletResponse response) throws StreamWriteException, DatabindException, IOException {
        log.info("hereee");
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
            Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(true); // Use HTTPS in production
            refreshCookie.setPath("/hse/refresh_token");
            refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
            response.addCookie(refreshCookie);
            
            response.setContentType(APPLICATION_JSON_VALUE);
            new ObjectMapper().writeValue(response.getOutputStream(), tokens);
            
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
		return "You're able to access protected HSE url that require authentication and authorization!";
	}
	
	@PostMapping("/verify_token")
	public boolean verifyToken(@RequestHeader(value = "Authorization", required = false) String token) {
		token = token.substring(7);
		return jwtService.validateToken(token);
	}
	
	
	@PostMapping("/refresh_token")
    public ResponseEntity<?> refreshToken(
            @CookieValue(value = "refresh_token", required = false) String refreshTokenCookie,
            HttpServletResponse response) {
        		
		log.info(refreshTokenCookie);
		
        String refreshToken = refreshTokenCookie ;
        
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Refresh token missing"));
        }
        
        try {
            // Validate refresh token (different validation than access token)
            if (!jwtService.validateToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid refresh token"));
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
            
        } catch (ExpiredJwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Refresh token expired"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Server error"));
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
	
//	@GetMapping("/toko5s")
//	public List<Toko5> getListToko5ByDate(@RequestParam)
//	
	
}
	