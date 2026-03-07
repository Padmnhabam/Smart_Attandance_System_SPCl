package com.example.attendance_Backend.controller;

import com.example.attendance_Backend.dto.*;
import com.example.attendance_Backend.model.User;
import com.example.attendance_Backend.service.StudAttendanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student")
@CrossOrigin(origins = "*")
public class StudentController {

    private final StudAttendanceService service;

    public StudentController(StudAttendanceService service) {
        this.service = service;
    }

    @GetMapping("/dashboard/{id}")
    public DashboardDTO getDashboard(@PathVariable int id) {
        return service.getDashboardData(id);
    }

    @GetMapping("/attendance/{id}")
    public List<AttendanceDTO> getAttendance(@PathVariable int id) {
        return service.getAttendanceList(id);
    }

    @GetMapping("/summary/subject/{id}")
    public List<StudentSubjectSummaryDTO> getSubjectSummary(@PathVariable int id) {
        return service.getSubjectWiseSummary(id);
    }

    // =========================
    // Full CRUD for Admin Panel
    // =========================

    // GET all students
    @GetMapping
    public List<User> getAllStudents() {
        return service.getAllStudents();
    }

    // GET student by rollNo
    @GetMapping("/{rollNo}")
    public ResponseEntity<User> getStudent(@PathVariable String rollNo) {
        return service.getStudentByRollNo(rollNo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST create new student
    @PostMapping
    public User createStudent(@RequestBody User user) {
        return service.saveStudent(user);
    }

    // PUT update student
    @PutMapping("/{rollNo}")
    public ResponseEntity<User> updateStudent(@PathVariable String rollNo, @RequestBody User updatedStudent) {
        return service.updateStudent(rollNo, updatedStudent)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE student
    @DeleteMapping("/{rollNo}")
    public ResponseEntity<Void> deleteStudent(@PathVariable String rollNo) {
        return service.deleteStudent(rollNo)
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }

}
