package com.example.UserServiceClient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@SpringBootApplication
public class UserServiceClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserServiceClientApplication.class, args);
	}

}

@Controller
class HomeController {

	@GetMapping("/hello")
	public ResponseEntity<String> home(@AuthenticationPrincipal OidcUser user) {
		String hello = "Hi, " + user.getName();
		System.out.println(hello);
		return ResponseEntity.ok(hello);
	}
}
