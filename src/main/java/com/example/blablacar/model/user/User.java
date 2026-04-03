package com.example.blablacar.model.user;

import com.example.blablacar.model.enums.Gender;
import com.example.blablacar.model.enums.Role;
import com.example.blablacar.model.enums.AuthProvider;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Entity class representing a user in the system.
 */
@Entity
@Table(name = "users")
public class User implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    private String providerId;

    @Enumerated
    @Column(nullable = false)
    @ColumnDefault("1")
    private Role role;

    @Column(nullable = false, length = 20)
    private String phoneNumber;

    @Column(nullable = false)
    private LocalDate birthday;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private Gender gender;

    @OneToMany(mappedBy = "reviewer")
    private List<UserReview> reviewsGiven;

    @OneToMany(mappedBy = "targetUser")
    private List<UserReview> reviewsReceived;

    @OneToOne(mappedBy = "user")
    private UserInfo userInfo;

    @OneToMany(mappedBy = "user")
    private List<UserCar> cars;

    // Default constructor required by JPA
    public User() {
    }

    // Constructor for traditional authentication
    public User(String name, String email, String password, Gender gender, LocalDate birthday, String phoneNumber) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.provider = AuthProvider.LOCAL;
        this.providerId = null;
        this.role = Role.ROLE_USER;
        this.gender = gender;
        this.birthday = birthday;
        this.phoneNumber = phoneNumber;
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

    // For UserPrincipal
    public User(User user) {
        this.id = user.id;
        this.name = user.name;
        this.email = user.email;
        this.password = user.password;
        this.provider = user.provider;
        this.providerId = user.providerId;
        this.role = user.role;
        this.phoneNumber = user.phoneNumber;
        this.birthday = user.birthday;
        this.gender = user.gender;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public AuthProvider getProvider() {
        return provider;
    }

    public void setProvider(AuthProvider provider) {
        this.provider = provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(final Role role) {
        this.role = role;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(id);
    }
}
