package com.example.blablacar.model.user;

import com.example.blablacar.enums.AuthProvider;
import com.example.blablacar.enums.Gender;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;

import java.io.Serializable;
import java.util.List;
import java.time.LocalDate;
import java.util.Objects;
import com.example.blablacar.enums.Role;

/**
 * Entity class representing a user in the system.
 */
@Entity
@Table(name = "users")
public class User implements Serializable {

    @Id
    @GeneratedValue
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false, length = 20)
    private String phoneNumber;

    @Column(nullable = false)
    private LocalDate birthday;

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
    public User(String name, String email, AuthProvider provider, String providerId, Gender gender) {
        this.name = name;
        this.email = email;
        this.provider = provider;
        this.providerId = providerId;
        this.role = Role.ROLE_USER;
        this.gender = gender;
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

    public void setRole(Role role) {
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

    public List<UserReview> getReviewsGiven() {
        return reviewsGiven;
    }

    public void setReviewsGiven(List<UserReview> reviewsGiven) {
        this.reviewsGiven = reviewsGiven;
    }

    public List<UserReview> getReviewsReceived() {
        return reviewsReceived;
    }

    public void setReviewsReceived(List<UserReview> reviewsReceived) {
        this.reviewsReceived = reviewsReceived;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public List<UserCar> getCars() {
        return cars;
    }

    public void setCars(List<UserCar> cars) {
        this.cars = cars;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id && Objects.equals(name, user.name) && Objects.equals(email, user.email) && Objects.equals(password, user.password) && provider == user.provider && Objects.equals(providerId, user.providerId) && role == user.role && Objects.equals(phoneNumber, user.phoneNumber) && Objects.equals(birthday, user.birthday) && gender == user.gender && Objects.equals(reviewsGiven, user.reviewsGiven) && Objects.equals(reviewsReceived, user.reviewsReceived) && Objects.equals(userInfo, user.userInfo) && Objects.equals(cars, user.cars);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, email, password, provider, providerId, role, phoneNumber, birthday, gender, reviewsGiven, reviewsReceived, userInfo, cars);
    }
}
