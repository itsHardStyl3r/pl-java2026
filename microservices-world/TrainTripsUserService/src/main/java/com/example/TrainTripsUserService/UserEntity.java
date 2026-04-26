package com.example.TrainTripsUserService;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "TRAIN_USERS")
public class UserEntity {

    @Id
    private String username;
    private String password;   // zakodowane Argon2
    private String role;
}
