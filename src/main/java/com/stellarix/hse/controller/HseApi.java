package com.stellarix.hse.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path="api/hse/")
public class HseApi {
	
	@Autowired
	public HseApi() {
		
	}
	
	@CrossOrigin
	@GetMapping(path="test")
	public String test() throws Exception {
        return "You're able to access hse apis that require authentication and authorization!";
    }
	
}
