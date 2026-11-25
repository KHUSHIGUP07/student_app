package com.example.student.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.example.student.entity.Student;
import com.example.student.repository.StudentRepository;

@Service
public class StudentService {

    private static final Logger logger = LogManager.getLogger(StudentService.class);

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    public Student saveStudent(Student student) {
        if (student.getEmail() != null) {
            student.setEmail(student.getEmail().trim().toLowerCase(Locale.ROOT));
        }
        Student savedStudent = studentRepository.save(student);
        logger.info("Saved student id={}", savedStudent.getId());
        return savedStudent;
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAllByOrderByIdAsc();
    }

    public Optional<Student> getStudentById(@NonNull Long id) {
        return studentRepository.findById(id);
    }

    public boolean studentExists(@NonNull Long id) {
        return studentRepository.existsById(id);
    }

    public boolean emailExists(@NonNull String email) {
        return studentRepository.existsByEmailIgnoreCase(email);
    }

    public boolean emailExistsForAnotherStudent(@NonNull String email, @NonNull Long id) {
        return studentRepository.existsByEmailIgnoreCaseAndIdNot(email, id);
    }

    public void deleteStudent(@NonNull Long id) {
        studentRepository.deleteById(id);
        logger.info("Deleted student id={}", id);
    }
}
