package com.example.attendance_Backend.service;

import com.example.attendance_Backend.dto.DateAnalyticsDTO;
import com.example.attendance_Backend.dto.SubjectAnalyticsDTO;
import com.example.attendance_Backend.model.Teacher;
import com.example.attendance_Backend.repository.AttendanceRepository;
import com.example.attendance_Backend.repository.DepartmentRepository;
import com.example.attendance_Backend.repository.TeacherRepository;
import org.springframework.stereotype.Service;

import com.example.attendance_Backend.model.Admin;
import com.example.attendance_Backend.security.AdminContextHolder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class TeacherService {

    private final TeacherRepository repository;
    private final AttendanceRepository attendanceRepository;
    private final PasswordService passwordService;
    private final DepartmentRepository departmentRepository;
    private final com.example.attendance_Backend.repository.AdminRepository adminRepository;

    public TeacherService(
            TeacherRepository repository,
            AttendanceRepository attendanceRepository,
            PasswordService passwordService,
            DepartmentRepository departmentRepository,
            com.example.attendance_Backend.repository.AdminRepository adminRepository) {
        this.repository = repository;
        this.attendanceRepository = attendanceRepository;
        this.passwordService = passwordService;
        this.departmentRepository = departmentRepository;
        this.adminRepository = adminRepository;
    }

    public Teacher registerTeacher(Teacher teacher) {
        Long adminId = AdminContextHolder.getAdminId();

        if (adminId != null) {
            Admin admin = new Admin();
            admin.setId(adminId);
            teacher.setAdmin(admin);
        } else if (teacher.getSchoolCode() != null && !teacher.getSchoolCode().isBlank()) {
            Admin admin = adminRepository.findBySchoolCode(teacher.getSchoolCode())
                    .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid School Code"));
            teacher.setAdmin(admin);
        } else if (teacher.getAdmin() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Admin context or School Code required for teacher registration");
        }

        // ensure every teacher has a role; default for self‑registration is TEACHER
        if (teacher.getRole() == null || teacher.getRole().isBlank()) {
            teacher.setRole("TEACHER");
        }
        teacher.setPassword(passwordService.encode(teacher.getPassword()));
        return repository.save(teacher);
    }

    public boolean emailExists(String email) {
        return repository.existsByEmail(email);
    }

    public Optional<Teacher> login(String email, String password) {
        return repository.findByEmail(email)
                .filter(teacher -> passwordService.matches(password, teacher.getPassword()));
    }

    public Optional<Teacher> getTeacherByEmail(String email) {
        return repository.findByEmail(email);
    }

    public List<SubjectAnalyticsDTO> getSubjectAnalytics() {
        Long adminId = com.example.attendance_Backend.security.AdminContextHolder.getAdminId();
        if (adminId != null)
            return attendanceRepository.getSubjectAnalyticsByAdminId(adminId);
        return java.util.Collections.emptyList();
    }

    public List<SubjectAnalyticsDTO> getDepartmentAnalytics() {
        Long adminId = com.example.attendance_Backend.security.AdminContextHolder.getAdminId();
        if (adminId != null)
            return attendanceRepository.getDepartmentAnalyticsByAdminId(adminId);
        return java.util.Collections.emptyList();
    }

    public List<DateAnalyticsDTO> getDateAnalytics() {
        Long adminId = com.example.attendance_Backend.security.AdminContextHolder.getAdminId();
        if (adminId != null)
            return attendanceRepository.getDateAnalyticsByAdminId(adminId);
        return java.util.Collections.emptyList();
    }

    // -------------------------
    // Admin CRUD Methods (Add These)
    // -------------------------

    // Get all teachers (for admin dashboard)
    public List<Teacher> getAllTeachers() {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId != null) {
            return repository.findByAdminId(adminId);
        }
        return repository.findAll();
    }

    // Get teacher by ID
    public Optional<Teacher> getTeacherById(Integer id) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId != null) {
            return repository.findByIdAndAdminId(id, adminId);
        }
        return repository.findById(id);
    }

    // Update teacher
    public Teacher updateTeacher(Integer id, Teacher teacherDetails) {
        Teacher teacher = getTeacherById(id)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        teacher.setName(teacherDetails.getName());
        teacher.setDepartment(teacherDetails.getDepartment());
        teacher.setEmail(teacherDetails.getEmail());
        teacher.setMobilenumber(teacherDetails.getMobilenumber());
        // allow admin to change role too if passed
        if (teacherDetails.getRole() != null) {
            teacher.setRole(teacherDetails.getRole());
        }
        if (teacherDetails.getPassword() != null && !teacherDetails.getPassword().isBlank()) {
            teacher.setPassword(passwordService.encode(teacherDetails.getPassword()));
        }

        return repository.save(teacher);
    }

    public Teacher updateTeacherDepartment(Integer teacherId, Integer departmentId) {
        Teacher teacher = getTeacherById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        if (departmentId != null) {
            teacher.setDepartment(departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new RuntimeException("Department not found")));
        } else {
            teacher.setDepartment(null);
        }

        return repository.save(teacher);
    }

    // Delete teacher
    public void deleteTeacher(Integer id) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId != null) {
            Teacher teacher = repository.findByIdAndAdminId(id, adminId)
                    .orElseThrow(() -> new RuntimeException("Teacher not found or unauthorized"));
            repository.delete(teacher);
        } else {
            if (!repository.existsById(id)) {
                throw new RuntimeException("Teacher not found");
            }
            repository.deleteById(id);
        }
    }

    /**
     * Save/update the browser fingerprint (device ID) for a teacher.
     * Called automatically when teacher opens the dashboard.
     */
    public void saveDeviceId(Integer teacherId, String deviceId) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return;
        repository.findByIdAndAdminId(teacherId, adminId).ifPresent(teacher -> {
            teacher.setDeviceId(deviceId);
            repository.save(teacher);
        });
    }

    /**
     * Resolve the stored device ID for a teacher by their DB id.
     */
    public Optional<String> getDeviceId(Integer teacherId) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return Optional.empty();
        return repository.findByIdAndAdminId(teacherId, adminId).map(Teacher::getDeviceId);
    }

    public List<com.example.attendance_Backend.model.User> getStudentsForTeacher(int teacherId, Integer classId,
            Integer divisionId) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return java.util.Collections.emptyList();

        if (classId == null && divisionId == null) {
            return attendanceRepository.getStudentsForTeacher(teacherId, adminId);
        }
        return attendanceRepository.getFilteredStudentsForTeacher(teacherId, classId, divisionId, adminId);
    }

    public void updatePassword(Integer id, String currentPassword, String newPassword) {
        Long adminId = AdminContextHolder.getAdminId();
        Teacher teacher = (adminId != null)
                ? repository.findByIdAndAdminId(id, adminId)
                        .orElseThrow(() -> new RuntimeException("Teacher not found or unauthorized"))
                : repository.findById(id).orElseThrow(() -> new RuntimeException("Teacher not found"));

        if (!passwordService.matches(currentPassword, teacher.getPassword())) {
            throw new RuntimeException("Current password does not match");
        }

        teacher.setPassword(passwordService.encode(newPassword));
        repository.save(teacher);
    }

    public void resetPassword(String email, String mobile, String newPassword) {
        Teacher teacher = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Teacher not found with this email"));

        if (teacher.getMobilenumber() == null || !teacher.getMobilenumber().equals(mobile)) {
            throw new RuntimeException("Mobile number does not match our records");
        }

        teacher.setPassword(passwordService.encode(newPassword));
        repository.save(teacher);
    }
}
