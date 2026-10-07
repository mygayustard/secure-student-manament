package com.example.studentmanagement.student;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Locale;

public record StudentRequest(
        @NotBlank(message = "Registration number is required")
        @Size(max = 30, message = "Registration number must be at most 30 characters")
        String registrationNumber,

        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 100, message = "Full name must be 2 to 100 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        String email,

        @Pattern(regexp = "^(?:\\+[1-9]\\d{7,14}|0\\d{9})$",
                message = "Phone number must be a valid international or Tanzanian number")
        String phone,

        @NotBlank(message = "Gender is required")
        @Pattern(regexp = "MALE|FEMALE|OTHER", message = "Gender must be MALE, FEMALE, or OTHER")
        String gender,

        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth) {

    public StudentRequest {
        if (registrationNumber != null) {
            registrationNumber = registrationNumber.trim();
        }
        if (fullName != null) {
            fullName = fullName.trim();
        }
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
        if (phone != null) {
            phone = phone.trim();
            if (phone.isEmpty()) {
                phone = null;
            }
        }
        if (gender != null) {
            gender = gender.trim();
        }
    }
}
