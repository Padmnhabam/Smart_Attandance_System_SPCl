package com.example.attendance_Backend.service;

import com.example.attendance_Backend.model.LeaveRequest;
import com.example.attendance_Backend.repository.LeaveRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class LeaveRequestService {

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    public List<LeaveRequest> getAllLeaveRequests() {
        return leaveRequestRepository.findAll();
    }

    public LeaveRequest getLeaveRequestByID(Long leaveRequestID) {
        var leaveRequest = leaveRequestRepository.findById(leaveRequestID.intValue());
        return leaveRequest.orElse(null);
    }

    public List<LeaveRequest> getLeaveRequestByStudentID(Long studentID) {
        return leaveRequestRepository.findByStudentId(studentID.intValue());
    }

    public LeaveRequest saveLeaveRequest(LeaveRequest leaveRequest) {
        return leaveRequestRepository.save(leaveRequest);
    }

    public void deleteLeaveRequest(Long leaveRequestID) {
        leaveRequestRepository.deleteById(leaveRequestID.intValue());
    }

    public List<LeaveRequest> getLeaveRequestsByStatus(String status) {
        return leaveRequestRepository.findAll().stream()
                .limit(100)
                .collect(java.util.stream.Collectors.toList());
    }
}
