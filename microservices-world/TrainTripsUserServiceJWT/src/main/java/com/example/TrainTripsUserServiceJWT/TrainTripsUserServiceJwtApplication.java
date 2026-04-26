package com.example.TrainTripsUserServiceJWT;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;

@SpringBootApplication
public class TrainTripsUserServiceJwtApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrainTripsUserServiceJwtApplication.class, args);
	}

}


@Configuration
class SecurityConfig {


	private static final String SECRET = "qwerty"; // min. 256 bits

	@Bean
	public JwtEncoder jwtEncoder() {
		System.out.println("jwtEncoder");
		SecretKeySpec secretKey = new SecretKeySpec(SECRET.getBytes(), "HmacSHA256");
		return NimbusJwtEncoder.withSecretKey(secretKey).build();
	}

	@Bean
	public JwtDecoder jwtDecoder() {
		System.out.println("jwtDecoder");
		SecretKeySpec secretKey = new SecretKeySpec(SECRET.getBytes(), "HmacSHA256");
		return NimbusJwtDecoder.withSecretKey(secretKey).build();
	}

	@Bean
	public UserDetailsService userDetailsService() {
		UserDetails user = User
				.withUsername("admin")
				.password(PasswordEncoderFactories.createDelegatingPasswordEncoder().encode("admin123"))
				.roles("ADMIN")
				.build();

		return new InMemoryUserDetailsManager(user);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(request -> request
						.requestMatchers("/external", "/token").permitAll()
						.requestMatchers("/validate").authenticated()
				)
				.sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(new JwtAuthenticationConverter()))
				)
				.userDetailsService(userDetailsService())
				.build();
	}

}

@Data
class AuthRequest {
	private String username;
	private String password;
}


@RestController
@RequiredArgsConstructor
class UserController {

	private final JwtEncoder jwtEncoder;
	private final UserDetailsService userDetailsService;
	private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

	@PostMapping("/token")
	public ResponseEntity<?> token(@RequestBody AuthRequest request) {

		System.out.println("token endpoint is invoked with username: " + request.getUsername());
		UserDetails user;
		try {
			user = userDetailsService.loadUserByUsername(request.getUsername());
		} catch (UsernameNotFoundException e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Niepoprawny login lub hasło");
		}

		if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Niepoprawny login lub hasło");
		}
		System.out.println("user " + user.getUsername() + " has been verified correctly");

		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(user.getUsername())
				.issuer("TrainTripsUserServiceJWT")
				.issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(120))
				.claim("roles", user.getAuthorities())
				.build();

		JwsHeader jwsHeader = JwsHeader.with(() -> "HS256").build();
		JwtEncoderParameters jwtEncoderParameters = JwtEncoderParameters.from(jwsHeader, claims);

		String tokenValue = jwtEncoder.encode(jwtEncoderParameters).getTokenValue();
		System.out.println("Generated token for user " + user.getUsername() + ": " + tokenValue);

		return ResponseEntity.ok(tokenValue);
	}

	@GetMapping("/validate")
	public ResponseEntity<String> validateToken(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok("Token poprawny. Użytkownik: " + jwt.getSubject());
	}

	@GetMapping("/external")
	public String external() {
		return "Publiczny endpoint – dostęp bez logowania";
	}
}