package com.example.student.controller;

import java.util.Locale;
import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.student.dto.StudentForm;
import com.example.student.entity.Student;
import com.example.student.service.StudentService;

import jakarta.validation.Valid;

@Controller
public class StudentController {

    private static final String STUDENT_ATTRIBUTE = "student";
    private static final String STUDENTS_ATTRIBUTE = "students";
    private static final String EMAIL_FIELD = "email";

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/")
    public String showForm(Model model) {
        model.addAttribute(STUDENT_ATTRIBUTE, new StudentForm());
        model.addAttribute(STUDENTS_ATTRIBUTE, studentService.getAllStudents());
        return STUDENTS_ATTRIBUTE;
    }

    @PostMapping("/save")
    public String saveStudent(
            @Valid @ModelAttribute(STUDENT_ATTRIBUTE) StudentForm form,
            BindingResult result,
            Model model) {
        String email = form.getEmail();
        if (email == null || email.isBlank()) {
            result.rejectValue(EMAIL_FIELD, "required", "Email is required");
        } else {
            email = normalizeEmail(email);
            form.setEmail(email);
            if (!result.hasFieldErrors(EMAIL_FIELD) && studentService.emailExists(email)) {
                result.rejectValue(EMAIL_FIELD, "duplicate", "This email is already registered");
            }
        }

        if (result.hasErrors()) {
            model.addAttribute(STUDENTS_ATTRIBUTE, studentService.getAllStudents());
            return STUDENTS_ATTRIBUTE;
        }

        Student student = new Student();
        student.setName(form.getName());
        String validEmail = Objects.requireNonNull(email);
        student.setEmail(validEmail);
        student.setCourse(form.getCourse());

        try {
            studentService.saveStudent(student);
        } catch (DataIntegrityViolationException exception) {
            if (studentService.emailExists(validEmail)) {
                result.rejectValue(EMAIL_FIELD, "duplicate", "This email is already registered");
                model.addAttribute(STUDENTS_ATTRIBUTE, studentService.getAllStudents());
                return STUDENTS_ATTRIBUTE;
            }
            throw exception;
        }

        return "redirect:/";
    }

    @GetMapping("/delete/{id}")
    public String deleteStudent(@PathVariable @NonNull Long id) {
        studentService.deleteStudent(id);
        return "redirect:/";
    }

    @NonNull
    private static String normalizeEmail(@Nullable String email) {
        if (email == null) {
            throw new IllegalArgumentException("Email is required");
        }
        return Objects.requireNonNull(email.trim().toLowerCase(Locale.ROOT));
    }
}
