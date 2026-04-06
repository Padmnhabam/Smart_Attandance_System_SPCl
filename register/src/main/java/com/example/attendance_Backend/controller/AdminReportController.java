package com.example.attendance_Backend.controller;

import com.example.attendance_Backend.dto.*;
import com.example.attendance_Backend.service.TeacherAssignmentService;
import com.example.attendance_Backend.service.TeacherReportService;
import com.example.attendance_Backend.model.Teacher;
import com.example.attendance_Backend.service.TeacherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/reports")
@CrossOrigin(origins = "*")
public class AdminReportController {

    private final TeacherReportService reportService;
    private final TeacherAssignmentService assignmentService;
    private final TeacherService teacherService;

    public AdminReportController(TeacherReportService reportService,
            TeacherAssignmentService assignmentService,
            TeacherService teacherService) {
        this.reportService = reportService;
        this.assignmentService = assignmentService;
        this.teacherService = teacherService;
    }

    /**
     * Class-wise attendance report
     * GET /api/admin/reports/class-attendance
     */
    @GetMapping("/class-attendance")
    public ResponseEntity<List<ClassAttendanceDTO>> getClassAttendance() {
        return ResponseEntity.ok(reportService.getClassAttendanceReport());
    }

    /**
     * Low attendance students
     * GET /api/admin/reports/low-attendance?threshold=75
     */
    @GetMapping("/low-attendance")
    public ResponseEntity<List<LowAttendanceStudentDTO>> getLowAttendance(
            @RequestParam(defaultValue = "75") double threshold) {
        return ResponseEntity.ok(reportService.getLowAttendanceStudents(threshold));
    }

    /**
     * One teacher's assignments + subject report
     * GET /api/admin/reports/teacher/{teacherId}
     */
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<Map<String, Object>> getTeacherReport(@PathVariable int teacherId) {
        Map<String, Object> report = new HashMap<>();
        report.put("assignments", assignmentService.getAssignmentsForTeacher(teacherId));
        return ResponseEntity.ok(report);
    }

    /**
     * Summary of all teachers with their assignments
     * GET /api/admin/reports/all-teachers
     */
    @GetMapping("/all-teachers")
    public ResponseEntity<List<Map<String, Object>>> getAllTeachersReport() {
        List<Teacher> teachers = teacherService.getAllTeachers();
        List<Map<String, Object>> result = teachers.stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("email", t.getEmail());
            m.put("department", t.getDepartment());
            m.put("assignments", assignmentService.getAssignmentsForTeacher(t.getId()));
            return m;
        }).toList();
        return ResponseEntity.ok(result);
    }
}
