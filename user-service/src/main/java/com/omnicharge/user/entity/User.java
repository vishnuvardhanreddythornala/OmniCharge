package com.omnicharge.user.entity;

import com.omnicharge.user.common.audit.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Email
    @Column(unique = true, nullable = true)
    private String email;  // Unique email (nullable for mobile-only users)

    @NotBlank
    @Column(nullable = false)
    private String fullName;

    @Pattern(regexp = "^\\+\\d{1,3}\\d{6,14}$", message = "Invalid mobile number")
    @Column(unique = true)
    private String mobileNumber;

    @Column(nullable = true)
    private String password; // BCrypts hash for LOCAL users, NULL for Google users

    @Column(unique = true, nullable = true)
    private String googleId; // Set only for Google users

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider authProvider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.ROLE_USER;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Boolean isMobileVerified = false;

    @Column(nullable = false)
    private Boolean isEmailVerified = false;
}
