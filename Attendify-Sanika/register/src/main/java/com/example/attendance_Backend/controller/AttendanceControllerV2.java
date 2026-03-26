package com.example.attendance_Backend.controller;

import com.example.attendance_Backend.model.Attendance;
import com.example.attendance_Backend.service.StudAttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/attendance")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AttendanceControllerV2 {

    @Autowired
    private StudAttendanceService studAttendanceService;

    @GetMapping
    public ResponseEntity<List<Attendance>> getAllAttendance() {
        return ResponseEntity.ok(studAttendanceService.getAllAttendance());
    }

    @GetMapping("/{attendanceID}")
    public ResponseEntity<Attendance> getAttendanceByID(@PathVariable Long attendanceID) {
        Attendance attendance = studAttendanceService.getAttendanceByID(attendanceID);
        if (attendance != null) {
            return ResponseEntity.ok(attendance);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/employee/{employeeID}")
    public ResponseEntity<List<Attendance>> getAttendanceByEmployeeID(@PathVariable Long employeeID) {
        List<Attendance> records = studAttendanceService.getAttendanceByStudentID(employeeID);
        return ResponseEntity.ok(records);
    }

    @PostMapping
    public ResponseEntity<Attendance> recordAttendance(@RequestBody Attendance attendance) {
        Attendance saved = studAttendanceService.saveAttendance(attendance);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/check-in")
    public ResponseEntity<Map<String, Object>> checkIn(@RequestParam Long employeeID, @RequestParam String location) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Check-in recorded");
        response.put("employeeID", employeeID);
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/check-out")
    public ResponseEntity<Map<String, Object>> checkOut(@RequestParam Long employeeID, @RequestParam String location) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Check-out recorded");
        response.put("employeeID", employeeID);
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/monthly/{employeeID}")
    public ResponseEntity<List<Attendance>> getMonthlyAttendance(
            @PathVariable Long employeeID,
            @RequestParam int month,
            @RequestParam int year) {
        List<Attendance> records = studAttendanceService.getAttendanceByStudentID(employeeID);
        return ResponseEntity.ok(records);
    }
}
