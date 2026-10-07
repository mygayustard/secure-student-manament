package com.example.studentmanagement.student;

import java.time.Instant;
import java.time.LocalDate;

public record StudentResponse(
        Long id,
        String registrationNumber,
        String fullName,
        String email,
        String phone,
        Student.Gender gender,
        LocalDate dateOfBirth,
        Instant createdAt,
        Instant updatedAt) {

    public static StudentResponse from(Student student) {
        return new StudentResponse(student.getId(), student.getRegistrationNumber(), student.getFullName(),
                student.getEmail(), student.getPhone(), student.getGender(), student.getDateOfBirth(),
                student.getCreatedAt(), student.getUpdatedAt());
    }
}
