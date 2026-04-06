package com.example.attendance_Backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.attendance_Backend.service.StudAttendanceService;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/dashboard")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DashboardControllerV2 {

    @Autowired
    private StudAttendanceService attendanceService;

    @GetMapping("/employee/{employeeID}")
    public ResponseEntity<Map<String, Object>> getEmployeeDashboard(@PathVariable Long employeeID) {
        Map<String, Object> dashboard = new HashMap<>();
        
        dashboard.put("employeeID", employeeID);
        dashboard.put("todayStatus", "Present");
        dashboard.put("checkInTime", "09:00 AM");
        dashboard.put("checkOutTime", null);
        dashboard.put("workingHours", 0);
        
        dashboard.put("attendanceStats", Map.of(
            "presentDays", 18,
            "absentDays", 2,
            "leavesDays", 0,
            "attendancePercentage", 90
        ));
        
        dashboard.put("leaveBalance", Map.of(
            "paidLeaves", 15,
            "casualLeaves", 8,
            "sickLeaves", 5
        ));
        
        dashboard.put("upcomingLeaves", 0);
        dashboard.put("alerts", new String[]{});
        
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/manager/{managerId}")
    public ResponseEntity<Map<String, Object>> getManagerDashboard(@PathVariable Long managerId) {
        Map<String, Object> dashboard = new HashMap<>();
        
        dashboard.put("managerId", managerId);
        dashboard.put("teamSize", 15);
        dashboard.put("presentToday", 14);
        dashboard.put("absentToday", 1);
        dashboard.put("leaveToday", 0);
        
        dashboard.put("teamAttendance", Map.of(
            "presentPercentage", 93.3,
            "absentPercentage", 6.7
        ));
        
        dashboard.put("pendingApprovals", 3);
        dashboard.put("alerts", new String[]{});
        
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/admin")
    public ResponseEntity<Map<String, Object>> getAdminDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        
        dashboard.put("totalEmployees", 150);
        dashboard.put("totalManagers", 12);
        dashboard.put("totalDepartments", 5);
        
        dashboard.put("todayStatus", Map.of(
            "present", 145,
            "absent", 3,
            "leave", 2
        ));
        
        dashboard.put("systemHealth", Map.of(
            "databaseStatus", "OK",
            "serverStatus", "OK",
            "apiStatus", "OK"
        ));
        
        dashboard.put("pendingTasks", 5);
        dashboard.put("alerts", new String[]{});
        
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        summary.put("date", System.currentTimeMillis());
        summary.put("totalEmployees", 150);
        summary.put("attendanceToday", 145);
        summary.put("absentToday", 3);
        summary.put("leaveToday", 2);
        summary.put("attendancePercentage", 96.7);
        
        return ResponseEntity.ok(summary);
    }
}
