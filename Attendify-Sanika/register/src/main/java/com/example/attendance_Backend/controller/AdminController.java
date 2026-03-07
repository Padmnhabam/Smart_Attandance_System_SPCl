package com.example.attendance_Backend.controller;

import com.example.attendance_Backend.dto.AuthRequest;
import com.example.attendance_Backend.model.Teacher;
import com.example.attendance_Backend.model.User;
import com.example.attendance_Backend.repository.UserRepository;
import com.example.attendance_Backend.service.AdminService;
import com.example.attendance_Backend.service.AuthService;
import com.example.attendance_Backend.service.TeacherService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final TeacherService teacherService;
    private final AdminService adminService;
    private final AuthService authService;
    private final UserRepository userRepository;

    public AdminController(
            TeacherService teacherService,
            AdminService adminService,
            AuthService authService,
            UserRepository userRepository) {
        this.teacherService = teacherService;
        this.adminService = adminService;
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginAdmin(@RequestBody AuthRequest request) {
        return authService.loginAdmin(request.getEmail(), request.getPassword())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(401).body(Map.of("message", "Invalid email or password")));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerAdmin(@RequestBody com.example.attendance_Backend.model.Admin admin) {
        try {
            com.example.attendance_Backend.model.Admin savedAdmin = adminService.registerAdmin(admin);
            savedAdmin.setPassword(null);
            return ResponseEntity.ok(savedAdmin);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Get all teachers
    @GetMapping("/teachers")
    public List<Teacher> getAllTeachers() {
        return teacherService.getAllTeachers();
    }

    // Get teacher by ID
    @GetMapping("/teachers/{id}")
    public ResponseEntity<?> getTeacherById(@PathVariable Integer id) {
        Optional<Teacher> teacherOpt = teacherService.getTeacherById(id);

        if (teacherOpt.isPresent()) {
            return ResponseEntity.ok(teacherOpt.get()); // Teacher object
        } else {
            return ResponseEntity.status(404).body("Teacher not found"); // String error
        }
    }

    // Delete teacher
    @DeleteMapping("/teachers/{id}")
    public ResponseEntity<?> deleteTeacher(@PathVariable Integer id) {
        try {
            teacherService.deleteTeacher(id);
            return ResponseEntity.ok("Teacher deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @GetMapping("/stats")
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTeachers", adminService.getTotalTeachers());
        stats.put("totalStudents", adminService.getTotalStudents());
        stats.put("totalClasses", adminService.getTotalClasses());
        stats.put("todaysAttendancePercent", adminService.getTodaysAttendancePercent());
        return stats;
    }

    // Get all students as flat maps (no circular reference)
    @GetMapping("/students")
    public List<Map<String, Object>> getAllStudents() {
        // Use teacherService or studAttendanceService which is context-aware, not raw
        // userRepo directly.
        // There is no dedicated studentService in this controller, so we'll inject
        // AdminContextHolder to the repo call or just use a generic find
        Long adminId = com.example.attendance_Backend.security.AdminContextHolder.getAdminId();
        List<User> users;
        if (adminId != null) {
            users = userRepository.findByAdminId(adminId);
        } else {
            users = userRepository.findAll();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getName());
            m.put("rollNo", u.getRollNo());
            m.put("email", u.getEmail());
            m.put("mobilenumber", u.getMobilenumber());
            m.put("address", u.getAddress());
            if (u.getClassMaster() != null) {
                m.put("classId", u.getClassMaster().getId());
                m.put("className", u.getClassMaster().getClassName());
            } else {
                m.put("classId", null);
                m.put("className", null);
            }
            if (u.getDivisionMaster() != null) {
                m.put("divisionId", u.getDivisionMaster().getId());
                m.put("divisionName", u.getDivisionMaster().getDivisionName());
            } else {
                m.put("divisionId", null);
                m.put("divisionName", null);
            }
            result.add(m);
        }
        return result;
    }

}
