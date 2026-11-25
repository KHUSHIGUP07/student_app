package com.example.student.controller;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.student.dto.StudentForm;
import com.example.student.dto.StudentResponse;
import com.example.student.entity.Student;
import com.example.student.service.StudentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/students")
public class StudentApiController {

    private final StudentService studentService;

    public StudentApiController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public List<StudentResponse> getAllStudents() {
        return studentService.getAllStudents().stream()
                .map(StudentResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getStudent(@PathVariable @NonNull Long id) {
        return studentService.getStudentById(id)
                .map(StudentResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Object> createStudent(@Valid @RequestBody StudentForm form) {
        String email = normalizedEmail(form.getEmail());
        Student student = toEntity(form, email);
        if (studentService.emailExists(email)) {
            return duplicateEmailResponse();
        }

        try {
            Student savedStudent = studentService.saveStudent(student);
            URI location = URI.create("/api/students/" + savedStudent.getId());
            return ResponseEntity.created(Objects.requireNonNull(location))
                    .body((Object) StudentResponse.from(savedStudent));
        } catch (DataIntegrityViolationException exception) {
            if (studentService.emailExists(email)) {
                return duplicateEmailResponse();
            }
            throw exception;
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> updateStudent(
            @PathVariable @NonNull Long id,
            @Valid @RequestBody StudentForm form) {
        if (!studentService.studentExists(id)) {
            return ResponseEntity.notFound().build();
        }

        String email = normalizedEmail(form.getEmail());
        Student student = toEntity(form, email);
        if (studentService.emailExistsForAnotherStudent(email, id)) {
            return duplicateEmailResponse();
        }

        student.setId(id);
        try {
            return ResponseEntity.ok((Object) StudentResponse.from(studentService.saveStudent(student)));
        } catch (DataIntegrityViolationException exception) {
            if (studentService.emailExistsForAnotherStudent(email, id)) {
                return duplicateEmailResponse();
            }
            throw exception;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable @NonNull Long id) {
        if (!studentService.studentExists(id)) {
            return ResponseEntity.notFound().build();
        }

        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    @NonNull
    private static String normalizedEmail(@Nullable String email) {
        if (email == null) {
            throw new IllegalArgumentException("Email is required");
        }
        return Objects.requireNonNull(email.trim().toLowerCase(Locale.ROOT));
    }

    private static Student toEntity(StudentForm form, String email) {
        Student student = new Student();
        student.setName(form.getName());
        student.setEmail(email);
        student.setCourse(form.getCourse());
        return student;
    }

    private static ResponseEntity<Object> duplicateEmailResponse() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "This email is already registered");
        return ResponseEntity.status(HttpStatus.CONFLICT).body((Object) problem);
    }
}
