package com.example.attendance_Backend.controller;

import com.example.attendance_Backend.model.AttendanceSession;
import com.example.attendance_Backend.service.AttendanceSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/shifts")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ShiftControllerV2 {

    @Autowired
    private AttendanceSessionService attendanceSessionService;

    @GetMapping
    public ResponseEntity<List<AttendanceSession>> getAllShifts() {
        return ResponseEntity.ok(attendanceSessionService.getAllAttendanceSessions());
    }

    @GetMapping("/{shiftID}")
    public ResponseEntity<AttendanceSession> getShiftByID(@PathVariable String shiftID) {
        AttendanceSession shift = attendanceSessionService.getAttendanceSessionByID(shiftID);
        if (shift != null) {
            return ResponseEntity.ok(shift);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<AttendanceSession> createShift(@RequestBody AttendanceSession shift) {
        AttendanceSession saved = attendanceSessionService.saveAttendanceSession(shift);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{shiftID}")
    public ResponseEntity<AttendanceSession> updateShift(@PathVariable String shiftID, @RequestBody AttendanceSession shift) {
        shift.setId(shiftID);
        AttendanceSession updated = attendanceSessionService.saveAttendanceSession(shift);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/employee/{employeeID}")
    public ResponseEntity<AttendanceSession> getEmployeeShift(@PathVariable Long employeeID) {
        List<AttendanceSession> shifts = attendanceSessionService.getAllAttendanceSessions();
        if (!shifts.isEmpty()) {
            return ResponseEntity.ok(shifts.get(0));
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<AttendanceSession>> searchShifts(@RequestParam String keyword) {
        List<AttendanceSession> shifts = attendanceSessionService.getAllAttendanceSessions();
        return ResponseEntity.ok(shifts);
    }
}
