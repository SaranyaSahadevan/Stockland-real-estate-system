package com.stockland.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ErrorTestController {

    @GetMapping("/error-test")
    public String testError(@RequestParam int code) {
        throw switch (code) {
            case 400 -> new ResponseStatusException(HttpStatus.BAD_REQUEST);
            case 401 -> new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            case 403 -> new ResponseStatusException(HttpStatus.FORBIDDEN);
            case 404 -> new ResponseStatusException(HttpStatus.NOT_FOUND);
            case 500 -> new RuntimeException("Internal Server Error");
            case 503 -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
            default -> new RuntimeException("Unknown error");

            // http://localhost:8080/error-test?code=400
            // http://localhost:8080/error-test?code=401
            // http://localhost:8080/error-test?code=403
            // http://localhost:8080/error-test?code=404
            // http://localhost:8080/error-test?code=500
            // http://localhost:8080/error-test?code=503

        };
    }
}

