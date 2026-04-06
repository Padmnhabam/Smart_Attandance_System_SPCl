package com.example.attendance_Backend.controller;

import com.example.attendance_Backend.model.LeaveRequest;
import com.example.attendance_Backend.service.LeaveRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/leave-requests")
@CrossOrigin(origins = "*", maxAge = 3600)
public class LeaveRequestControllerV2 {

    @Autowired
    private LeaveRequestService leaveRequestService;

    @GetMapping
    public ResponseEntity<List<LeaveRequest>> getAllLeaveRequests() {
        return ResponseEntity.ok(leaveRequestService.getAllLeaveRequests());
    }

    @GetMapping("/{leaveRequestID}")
    public ResponseEntity<LeaveRequest> getLeaveRequestByID(@PathVariable Long leaveRequestID) {
        LeaveRequest leaveRequest = leaveRequestService.getLeaveRequestByID(leaveRequestID);
        if (leaveRequest != null) {
            return ResponseEntity.ok(leaveRequest);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/employee/{employeeID}")
    public ResponseEntity<List<LeaveRequest>> getLeaveRequestsByEmployeeID(@PathVariable Long employeeID) {
        List<LeaveRequest> requests = leaveRequestService.getLeaveRequestByStudentID(employeeID);
        return ResponseEntity.ok(requests);
    }

    @PostMapping
    public ResponseEntity<LeaveRequest> submitLeaveRequest(@RequestBody LeaveRequest leaveRequest) {
        LeaveRequest saved = leaveRequestService.saveLeaveRequest(leaveRequest);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{leaveRequestID}/approve")
    public ResponseEntity<Map<String, Object>> approveLeaveRequest(@PathVariable Long leaveRequestID) {
        LeaveRequest request = leaveRequestService.getLeaveRequestByID(leaveRequestID);
        if (request != null) {
            request.setStatus("Approved");
            leaveRequestService.saveLeaveRequest(request);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Leave request approved");
            response.put("leaveRequestID", leaveRequestID);
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{leaveRequestID}/reject")
    public ResponseEntity<Map<String, Object>> rejectLeaveRequest(@PathVariable Long leaveRequestID) {
        LeaveRequest request = leaveRequestService.getLeaveRequestByID(leaveRequestID);
        if (request != null) {
            request.setStatus("Rejected");
            leaveRequestService.saveLeaveRequest(request);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Leave request rejected");
            response.put("leaveRequestID", leaveRequestID);
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/manager/{managerId}/pending")
    public ResponseEntity<List<LeaveRequest>> getPendingLeaveRequests(@PathVariable Long managerId) {
        List<LeaveRequest> requests = leaveRequestService.getAllLeaveRequests();
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/employee/{employeeID}/balance")
    public ResponseEntity<Map<String, Object>> getLeaveBalance(@PathVariable Long employeeID) {
        Map<String, Object> response = new HashMap<>();
        response.put("employeeID", employeeID);
        response.put("totalLeaves", 20);
        response.put("usedLeaves", 5);
        response.put("balanceLeaves", 15);
        return ResponseEntity.ok(response);
    }
}
