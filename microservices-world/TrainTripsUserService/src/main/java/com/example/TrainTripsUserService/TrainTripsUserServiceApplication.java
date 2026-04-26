package com.example.TrainTripsUserService;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class TrainTripsUserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrainTripsUserServiceApplication.class, args);
    }

}

@RestController
@RequiredArgsConstructor
class UserController {

    @GetMapping("/internal")
    public String getInternalMessage() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) auth.getPrincipal();
        return String.format("Hello %s!", user.getUsername());
    }

    @GetMapping("/external")
    public String getExternalMessage() {
        return "Hello all viewers!";
    }
}

@RequiredArgsConstructor
@Configuration
class SecurityConfig {

    private final DatabaseUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/external").permitAll()   // każdy może wejść
                        .anyRequest().authenticated()               // reszta wymaga logowania
                )
                .userDetailsService(userDetailsService)   // nasza implementacja UserDetailsService
                .formLogin(form -> form
                        .defaultSuccessUrl("/internal", true)      // po zalogowaniu przekierowanie
                )
                .build();
    }

//	@Bean
//	public UserDetailsService userDetailsService() {
//
//		UserDetails admin = User.withUsername("admin")
//				.password(passwordEncoder().encode("admin123"))
//				.roles("ADMIN")
//				.build();
//
//		UserDetails user = User.withUsername("student")
//				.password(passwordEncoder().encode("student123"))
//				.roles("USER")
//				.build();
//
//		return new InMemoryUserDetailsManager(admin, user);
//	}

    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

}


@Repository
interface UserRepository extends JpaRepository<UserEntity, String> {
}

@Service
@RequiredArgsConstructor
class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {

        UserEntity entity = userRepository.findById(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return User.withUsername(entity.getUsername())
                .password(entity.getPassword())     // hash Argon2 z bazy
                .roles(entity.getRole())
                .build();
    }
}

@Component
@RequiredArgsConstructor
class UserShowRoom implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        if (userRepository.existsById("admin")) {
            return; // nie duplikujemy użytkownika
        }

        UserEntity admin = new UserEntity();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123")); // Argon2 hash
        admin.setRole("ADMIN");

        userRepository.save(admin);

        System.out.println(">>> Admin user created");
        System.out.println(">>> Username: admin");
        System.out.println(">>> Password: admin123");
        System.out.println(">>> Argon2 hash: " + admin.getPassword());
    }
}