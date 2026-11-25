package com.example.student.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import com.example.student.dto.StudentForm;
import com.example.student.entity.Student;
import com.example.student.service.StudentService;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@ExtendWith(MockitoExtension.class)
class StudentValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @Mock
    private StudentService studentService;

    @InjectMocks
    private StudentController studentController;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void acceptsLetterNamesAndWellFormedEmail() {
        StudentForm student = student("Anne-Marie O'Neil", "anne.oneil@example.com");

        assertTrue(validator.validate(student).isEmpty());
    }

    @Test
    void rejectsNumbersInName() {
        StudentForm student = student("Anne2", "anne@example.com");

        Set<ConstraintViolation<StudentForm>> violations = validator.validate(student);

        assertTrue(violations.stream().anyMatch(violation ->
                violation.getPropertyPath().toString().equals("name")));
    }

    @Test
    void rejectsMalformedEmail() {
        StudentForm student = student("Anne", "anne@invalid");

        Set<ConstraintViolation<StudentForm>> violations = validator.validate(student);

        assertTrue(violations.stream().anyMatch(violation ->
                violation.getPropertyPath().toString().equals("email")));
    }

    @Test
    void preventsRegisteringAnEmailThatAlreadyExistsIgnoringCase() {
        StudentForm student = student("Anne", "ANNE@example.com");
        BindingResult result = new BeanPropertyBindingResult(student, "student");
        when(studentService.emailExists("anne@example.com")).thenReturn(true);
        when(studentService.getAllStudents()).thenReturn(List.of());

        String view = studentController.saveStudent(student, result, new ExtendedModelMap());

        assertTrue(result.hasFieldErrors("email"));
        var fieldError = Objects.requireNonNull(result.getFieldError("email"));
        assertEquals("This email is already registered", fieldError.getDefaultMessage());
        assertEquals("students", view);
        verify(studentService, never()).saveStudent(any(Student.class));
    }

    private static StudentForm student(String name, String email) {
        StudentForm form = new StudentForm();
        form.setName(name);
        form.setEmail(email);
        form.setCourse("Java");
        return form;
    }
}
