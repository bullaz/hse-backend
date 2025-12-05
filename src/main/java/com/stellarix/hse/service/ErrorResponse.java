package com.stellarix.hse.service;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String error;
    private String message;
    private int status;
    private long timestamp;
    private String path;
    
    // Convenience constructor
    public ErrorResponse(String message) {
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.status = HttpStatus.BAD_REQUEST.value();
    }
}