package com.stellarix.hse.controller;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stellarix.hse.entity.AuthRequest;
import com.stellarix.hse.service.AccountService;
import com.stellarix.hse.service.JwtService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping(path="/hse")
public class HseApi {
	
	private AccountService service;

    private JwtService jwtService;

    private AuthenticationManager authenticationManager;
	
	
	@CrossOrigin
	@GetMapping(path="test")
	public String test() throws Exception {
        return "You're able to access hse apis that require authentication and authorization!";
    }
	
	
//	@GetMapping("/user/profile")
//    @PreAuthorize("hasAuthority('HSE')")
//    public String userProfile() {
//        return "User profile is shown here";
//    }
//	
	
	@PostMapping("/signin")
    public String authenticateAndGetToken(@RequestBody AuthRequest authRequest) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
        );
        if (authentication.isAuthenticated()) {
            return jwtService.generateToken(authRequest.getUsername());
        } else {
            throw new UsernameNotFoundException("Invalid user request!");
        }
    }
	
//	@PostMapping("/signup")
//    public String addNewUser(@RequestBody UserInfo userInfo) {
//        return service.addUser(userInfo);
//    }
}
	