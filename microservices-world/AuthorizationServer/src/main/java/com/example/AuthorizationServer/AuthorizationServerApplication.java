package com.example.AuthorizationServer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import java.util.UUID;

@SpringBootApplication
public class AuthorizationServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthorizationServerApplication.class, args);
	}

}


@Configuration
class AuthorizationServerConfig {



	@Bean
	public RegisteredClientRepository registeredClientRepository() {
		RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
				.clientId("user-service-client-via-spring-auth-server")
				.clientSecret("{noop}very-secret-code") // tylko do testów!
				.redirectUri("http://localhost:8081/login/oauth2/code/user-service-client-via-spring-auth-server")
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
				.scope(OidcScopes.OPENID)
				.scope("profile")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.build();

		return new InMemoryRegisteredClientRepository(registeredClient);
	}



	@Bean
	public UserDetailsService users() {
		return new InMemoryUserDetailsManager(
				User.withUsername("admin")
						.password("{noop}admin123") // tylko do testów!
						.authorities("ROLE_ADMIN")
						.passwordEncoder(password -> password) // no-op encoder
						.build(),
				User.withUsername("user1")
						.password("{noop}user123") // tylko do testów!
						.authorities("ROLE_USER")
						.passwordEncoder(password -> password) // no-op encoder
						.build());
	}

	@Bean
	public AuthorizationServerSettings authorizationServerSettings() {
		return AuthorizationServerSettings.builder()
				.issuer("http://auth.mydomain.com:8080")
				.build();
	}

}
