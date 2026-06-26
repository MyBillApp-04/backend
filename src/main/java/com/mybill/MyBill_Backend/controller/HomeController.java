package com.mybill.MyBill_Backend.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HomeController {

    @GetMapping("/home")
    public String home(Authentication authentication) {
        String email = authentication.getName();
        return "Welcome " + email;
    }
}