package com.example.blablacar.model;

import com.example.blablacar.enums.AuthProvider;
import com.example.blablacar.enums.Gender;
import com.example.blablacar.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity class representing a user in the system
 */
@Builder
@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email")
})
public class User {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;


    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column
    private LocalDate birthday;

    @Column(nullable = false)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column
    private Gender gender;

    // Default constructor required by JPA
    public User() {
    }

    // Constructor for traditional authentication
    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.provider = AuthProvider.LOCAL;
        this.providerId = null;
        this.role = Role.ROLE_USER;
    }

    // Constructor for traditional authentication
    public User(String name, String email, String password, Gender gender) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.provider = AuthProvider.LOCAL;
        this.providerId = null;
        this.role = Role.ROLE_USER;
        this.gender = gender;
    }

    // Constructor for OAuth2 authentication
    public User(String name, String email, AuthProvider provider, String providerId) {
        this.name = name;
        this.email = email;
        this.provider = provider;
        this.providerId = providerId;
        this.role = Role.ROLE_USER;
    }

    // Constructor for OAuth2 authentication
    public User(String name, String email, AuthProvider provider, String providerId, Gender gender) {
        this.name = name;
        this.email = email;
        this.provider = provider;
        this.providerId = providerId;
        this.role = Role.ROLE_USER;
        this.gender = gender;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", provider=" + provider +
                ", role='" + role + '\'' +
                ", gender=" + gender +
                '}';
    }
}
