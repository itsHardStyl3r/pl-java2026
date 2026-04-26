package com.example.UserServiceClientKeycloak.web;

import java.security.Principal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalController {

    @GetMapping("/")
    public String home() {
        return "Public endpoint. Open /internal to trigger Keycloak login.";
    }

    @GetMapping("/internal")
    public String internal(Principal principal) {
        return "Internal endpoint for: " + principal.getName();
    }
}

