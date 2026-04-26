package com.example.UserServiceClientKeycloak;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.AbstractUserRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class UserServiceClientKeycloakApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserServiceClientKeycloakApplication.class, args);
	}

	private static final String REALM_NAME = "train-trips-realm";

	@Bean
    Keycloak keycloak() {
		return KeycloakBuilder.builder()
				.serverUrl("http://localhost:8999")
				.realm(REALM_NAME)
				.clientId("admin-cli")
				.grantType(OAuth2Constants.PASSWORD)
				.username("wojtek")
				.password("wojtek123")
				.build();
	}

//	@Bean
//    CommandLineRunner commandLineRunner(Keycloak keycloak) {
//		return args -> {
//			List<UserRepresentation> users = keycloak.realm(REALM_NAME)
//					.users()
//					.search("zzpj", false);
//
//			List<String> usernames = users.stream()
//					.map(AbstractUserRepresentation::getUsername)
//					.toList();
//
//			System.out.println("Users in Keycloak in realm : " + usernames);
//		};
//	}

	@Value("${my.db.password}")
	String password;


	@Bean
	CommandLineRunner commandLineRunner() {
		return args -> {

			System.out.println("Moje hasło to: " + password);

		};
	}
}
