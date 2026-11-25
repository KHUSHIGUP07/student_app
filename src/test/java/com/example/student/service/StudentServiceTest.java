package com.example.student.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.student.entity.Student;
import com.example.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudentService studentService;

    @Test
    void savesStudent() {
        Student student = new Student();
        when(studentRepository.save(student)).thenReturn(student);

        assertSame(student, studentService.saveStudent(student));
    }

    @Test
    void getsStudentsInIdOrder() {
        List<Student> students = List.of(new Student());
        when(studentRepository.findAllByOrderByIdAsc()).thenReturn(students);

        assertEquals(students, studentService.getAllStudents());
    }

    @Test
    void findsStudentById() {
        Student student = new Student();
        when(studentRepository.findById(7L)).thenReturn(Optional.of(student));

        assertEquals(Optional.of(student), studentService.getStudentById(7L));
    }

    @Test
    void checksWhetherStudentExists() {
        when(studentRepository.existsById(7L)).thenReturn(true);

        assertTrue(studentService.studentExists(7L));
    }

    @Test
    void checksForEmailIgnoringCase() {
        when(studentRepository.existsByEmailIgnoreCase("anne@example.com")).thenReturn(true);

        assertTrue(studentService.emailExists("anne@example.com"));
    }

    @Test
    void checksForEmailOwnedByAnotherStudent() {
        when(studentRepository.existsByEmailIgnoreCaseAndIdNot("anne@example.com", 7L))
                .thenReturn(true);

        assertTrue(studentService.emailExistsForAnotherStudent("anne@example.com", 7L));
    }

    @Test
    void deletesStudentById() {
        studentService.deleteStudent(7L);

        verify(studentRepository).deleteById(7L);
    }
}
