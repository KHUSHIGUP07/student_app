package com.example.student.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import com.example.student.dto.StudentForm;
import com.example.student.entity.Student;
import com.example.student.service.StudentService;

@ExtendWith(MockitoExtension.class)
class StudentControllerBehaviorTest {

    @Mock
    private StudentService studentService;

    @InjectMocks
    private StudentController studentController;

    @Test
    void showsTheRegistrationFormAndOrderedStudents() {
        List<Student> students = List.of(new Student());
        when(studentService.getAllStudents()).thenReturn(students);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = studentController.showForm(model);

        assertEquals("students", view);
        assertTrue(model.get("student") instanceof StudentForm);
        assertEquals(students, model.get("students"));
    }

    @Test
    void returnsTheFormWhenBindingHasErrors() {
        StudentForm form = student("Anne", "anne@example.com");
        BindingResult result = new BeanPropertyBindingResult(form, "student");
        result.rejectValue("name", "invalid", "Invalid name");
        when(studentService.emailExists("anne@example.com")).thenReturn(false);
        when(studentService.getAllStudents()).thenReturn(List.of());

        String view = studentController.saveStudent(form, result, new ExtendedModelMap());

        assertEquals("students", view);
        verify(studentService, never()).saveStudent(org.mockito.ArgumentMatchers.any(Student.class));
    }

    @Test
    void normalizesEmailBeforeCheckingAndSaving() {
        StudentForm form = student("Anne", "ANNE@example.com");
        BindingResult result = new BeanPropertyBindingResult(form, "student");
        when(studentService.emailExists("anne@example.com")).thenReturn(false);

        String view = studentController.saveStudent(form, result, new ExtendedModelMap());

        assertEquals("redirect:/", view);
        org.mockito.ArgumentCaptor<Student> savedStudent =
                org.mockito.ArgumentCaptor.forClass(Student.class);
        verify(studentService).saveStudent(savedStudent.capture());
        assertEquals("anne@example.com", savedStudent.getValue().getEmail());
    }

    @Test
    void reportsDuplicateEmailWhenConcurrentInsertViolatesUniqueConstraint() {
        StudentForm form = student("Anne", "anne@example.com");
        BindingResult result = new BeanPropertyBindingResult(form, "student");
        when(studentService.emailExists("anne@example.com")).thenReturn(false, true);
        doThrow(new DataIntegrityViolationException("duplicate email"))
                .when(studentService).saveStudent(org.mockito.ArgumentMatchers.any(Student.class));
        when(studentService.getAllStudents()).thenReturn(List.of());

        String view = studentController.saveStudent(form, result, new ExtendedModelMap());

        assertEquals("students", view);
        assertTrue(result.hasFieldErrors("email"));
    }

    @Test
    void rethrowsUnrelatedDataIntegrityErrors() {
        StudentForm form = student("Anne", "anne@example.com");
        BindingResult result = new BeanPropertyBindingResult(form, "student");
        when(studentService.emailExists("anne@example.com")).thenReturn(false, false);
        doThrow(new DataIntegrityViolationException("unrelated constraint"))
                .when(studentService).saveStudent(org.mockito.ArgumentMatchers.any(Student.class));

        assertThrows(DataIntegrityViolationException.class,
                () -> studentController.saveStudent(form, result, new ExtendedModelMap()));
    }

    @Test
    void redirectsAfterDeletingStudent() {
        assertEquals("redirect:/", studentController.deleteStudent(7L));
        verify(studentService).deleteStudent(7L);
    }

    private static StudentForm student(String name, String email) {
        StudentForm form = new StudentForm();
        form.setName(name);
        form.setEmail(email);
        form.setCourse("Java");
        return form;
    }
}
