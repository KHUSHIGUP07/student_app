package com.example.student.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.servlet.ServletException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.student.entity.Student;
import com.example.student.service.StudentService;

@WebMvcTest(StudentApiController.class)
class StudentApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @Test
    void listsStudents() throws Exception {
        when(studentService.getAllStudents()).thenReturn(List.of(student(1L, "Anne", "anne@example.com")));

        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Anne"));
    }

    @Test
    void getsStudentById() throws Exception {
        when(studentService.getStudentById(1L))
                .thenReturn(Optional.of(student(1L, "Anne", "anne@example.com")));

        mockMvc.perform(get("/api/students/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("anne@example.com"));
    }

    @Test
    void returnsNotFoundForMissingStudent() throws Exception {
        when(studentService.getStudentById(50L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/students/50"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createsStudentAndReturnsLocation() throws Exception {
        when(studentService.emailExists("anne@example.com")).thenReturn(false);
        when(studentService.saveStudent(any(Student.class))).thenAnswer(invocation -> {
            Student saved = invocation.getArgument(0);
            saved.setId(11L);
            return saved;
        });

        mockMvc.perform(post("/api/students")
                        .contentType(jsonMediaType())
                        .content(studentJson("Anne", "ANNE@example.com")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/students/11"))
                .andExpect(jsonPath("$.email").value("anne@example.com"));
    }

    @Test
    void rejectsDuplicateEmail() throws Exception {
        when(studentService.emailExists("anne@example.com")).thenReturn(true);

        mockMvc.perform(post("/api/students")
                        .contentType(jsonMediaType())
                        .content(studentJson("Anne", "ANNE@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("This email is already registered"));
    }

    @Test
    void rejectsInvalidStudentInput() throws Exception {
        mockMvc.perform(post("/api/students")
                        .contentType(jsonMediaType())
                        .content(studentJson("Anne2", "not-an-email")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reportsDuplicateEmailWhenConcurrentCreateHitsUniqueConstraint() throws Exception {
        when(studentService.emailExists("anne@example.com")).thenReturn(false, true);
        doThrow(new DataIntegrityViolationException("duplicate email"))
                .when(studentService).saveStudent(any(Student.class));

        mockMvc.perform(post("/api/students")
                        .contentType(jsonMediaType())
                        .content(studentJson("Anne", "anne@example.com")))
                .andExpect(status().isConflict());
    }

    @Test
    void rethrowsUnrelatedCreateConstraintFailure() {
        when(studentService.emailExists("anne@example.com")).thenReturn(false, false);
        doThrow(new DataIntegrityViolationException("unrelated constraint"))
                .when(studentService).saveStudent(any(Student.class));

        assertThrows(ServletException.class, () -> mockMvc.perform(post("/api/students")
                .contentType(jsonMediaType())
                .content(studentJson("Anne", "anne@example.com"))));
    }

    @Test
    void returnsNotFoundWhenUpdatingMissingStudent() throws Exception {
        when(studentService.studentExists(50L)).thenReturn(false);

        mockMvc.perform(put("/api/students/50")
                        .contentType(jsonMediaType())
                        .content(studentJson("Anne", "anne@example.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsEmailUsedByAnotherStudentWhenUpdating() throws Exception {
        when(studentService.studentExists(7L)).thenReturn(true);
        when(studentService.emailExistsForAnotherStudent("anne@example.com", 7L)).thenReturn(true);

        mockMvc.perform(put("/api/students/7")
                        .contentType(jsonMediaType())
                        .content(studentJson("Anne", "anne@example.com")))
                .andExpect(status().isConflict());
    }

    @Test
    void updatesStudentAndUsesPathId() throws Exception {
        when(studentService.studentExists(7L)).thenReturn(true);
        when(studentService.emailExistsForAnotherStudent("anne@example.com", 7L)).thenReturn(false);
        when(studentService.saveStudent(any(Student.class))).thenAnswer(invocation ->
                invocation.getArgument(0));

        mockMvc.perform(put("/api/students/7")
                        .contentType(jsonMediaType())
                        .content("{\"id\":99,\"name\":\"Anne\",\"email\":\"ANNE@example.com\",\"course\":\"Java\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.email").value("anne@example.com"));
    }

    @Test
    void reportsDuplicateEmailWhenConcurrentUpdateHitsUniqueConstraint() throws Exception {
        when(studentService.studentExists(7L)).thenReturn(true);
        when(studentService.emailExistsForAnotherStudent("anne@example.com", 7L))
                .thenReturn(false, true);
        doThrow(new DataIntegrityViolationException("duplicate email"))
                .when(studentService).saveStudent(any(Student.class));

        mockMvc.perform(put("/api/students/7")
                        .contentType(jsonMediaType())
                        .content(studentJson("Anne", "anne@example.com")))
                .andExpect(status().isConflict());
    }

    @Test
    void rethrowsUnrelatedUpdateConstraintFailure() {
        when(studentService.studentExists(7L)).thenReturn(true);
        when(studentService.emailExistsForAnotherStudent("anne@example.com", 7L))
                .thenReturn(false, false);
        doThrow(new DataIntegrityViolationException("unrelated constraint"))
                .when(studentService).saveStudent(any(Student.class));

        assertThrows(ServletException.class, () -> mockMvc.perform(put("/api/students/7")
                .contentType(jsonMediaType())
                .content(studentJson("Anne", "anne@example.com"))));
    }

    @Test
    void returnsNotFoundWhenDeletingMissingStudent() throws Exception {
        when(studentService.studentExists(50L)).thenReturn(false);

        mockMvc.perform(delete("/api/students/50"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletesExistingStudent() throws Exception {
        when(studentService.studentExists(7L)).thenReturn(true);

        mockMvc.perform(delete("/api/students/7"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(studentService).deleteStudent(7L);
    }

    private static Student student(Long id, String name, String email) {
        Student student = new Student();
        student.setId(id);
        student.setName(name);
        student.setEmail(email);
        student.setCourse("Java");
        return student;
    }

    private static String studentJson(String name, String email) {
        return "{\"name\":\"" + name + "\",\"email\":\"" + email + "\",\"course\":\"Java\"}";
    }

    private static MediaType jsonMediaType() {
        return Objects.requireNonNull(MediaType.APPLICATION_JSON);
    }
}
