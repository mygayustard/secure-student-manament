package com.example.studentmanagement.student;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByRegistrationNumberIgnoreCase(String registrationNumber);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByRegistrationNumberIgnoreCaseAndIdNot(String registrationNumber, Long id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
