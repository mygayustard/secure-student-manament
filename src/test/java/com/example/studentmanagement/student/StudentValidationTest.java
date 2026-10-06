package com.example.studentmanagement.student;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudentValidationTest {

    private final Validator validator;

    StudentValidationTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    @Test
    void validPhoneNumberShouldPassValidation() {
        Student student = new Student();
        student.setFullName("Alice Example");
        student.setEmail("alice@example.com");
        student.setRegistrationNumber("STD-001");
        student.setPhone("(255) 712-345-678");

        assertTrue(validator.validate(student).isEmpty());
    }

    @Test
    void invalidPhoneNumberShouldFailValidation() {
        Student student = new Student();
        student.setFullName("Alice Example");
        student.setEmail("alice@example.com");
        student.setRegistrationNumber("STD-002");
        student.setPhone("123456789");

        assertFalse(validator.validate(student).isEmpty());
    }
}
