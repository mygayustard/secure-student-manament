package com.example.studentmanagement.student;

import com.example.studentmanagement.exception.DuplicateResourceException;
import com.example.studentmanagement.exception.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Transactional
    public StudentResponse create(StudentRequest request) {
        ensureUnique(request, null);
        Student student = new Student();
        applyRequest(student, request);
        return save(student);
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getAll() {
        return studentRepository.findAll().stream().map(StudentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public StudentResponse getById(Long id) {
        return StudentResponse.from(findStudent(id));
    }

    @Transactional
    public StudentResponse update(Long id, StudentRequest request) {
        Student student = findStudent(id);
        ensureUnique(request, id);
        applyRequest(student, request);
        return save(student);
    }

    @Transactional
    public void delete(Long id) {
        studentRepository.delete(findStudent(id));
    }

    private Student findStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id " + id));
    }

    private void ensureUnique(StudentRequest request, Long currentId) {
        boolean registrationNumberExists = currentId == null
                ? studentRepository.existsByRegistrationNumberIgnoreCase(request.registrationNumber())
                : studentRepository.existsByRegistrationNumberIgnoreCaseAndIdNot(
                        request.registrationNumber(), currentId);
        if (registrationNumberExists) {
            throw new DuplicateResourceException("Registration number is already in use");
        }

        boolean emailExists = currentId == null
                ? studentRepository.existsByEmailIgnoreCase(request.email())
                : studentRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), currentId);
        if (emailExists) {
            throw new DuplicateResourceException("Email is already in use");
        }
    }

    private void applyRequest(Student student, StudentRequest request) {
        student.setRegistrationNumber(request.registrationNumber());
        student.setFullName(request.fullName());
        student.setEmail(request.email());
        student.setPhone(request.phone());
        student.setGender(Student.Gender.valueOf(request.gender()));
        student.setDateOfBirth(request.dateOfBirth());
    }

    private StudentResponse save(Student student) {
        try {
            return StudentResponse.from(studentRepository.saveAndFlush(student));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateResourceException("Registration number or email is already in use");
        }
    }
}
