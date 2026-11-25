package com.example.student.dto;

import com.example.student.entity.Student;

public record StudentResponse(Long id, String name, String email, String course) {

    public static StudentResponse from(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getName(),
                student.getEmail(),
                student.getCourse());
    }
}
