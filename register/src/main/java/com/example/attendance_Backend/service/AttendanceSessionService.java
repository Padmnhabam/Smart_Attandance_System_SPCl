package com.example.attendance_Backend.service;

import com.example.attendance_Backend.model.AttendanceSession;
import com.example.attendance_Backend.repository.AttendanceSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AttendanceSessionService {

    @Autowired
    private AttendanceSessionRepository attendanceSessionRepository;

    public List<AttendanceSession> getAllAttendanceSessions() {
        return attendanceSessionRepository.findAll();
    }

    public AttendanceSession getAttendanceSessionByID(String sessionID) {
        return attendanceSessionRepository.findById(sessionID).orElse(null);
    }

    public AttendanceSession saveAttendanceSession(AttendanceSession session) {
        return attendanceSessionRepository.save(session);
    }

    public void deleteAttendanceSession(String sessionID) {
        attendanceSessionRepository.deleteById(sessionID);
    }

    public List<AttendanceSession> getAttendanceSessionByClassID(Long classID) {
        return attendanceSessionRepository.findAll().stream()
                .limit(100)
                .collect(java.util.stream.Collectors.toList());
    }

    public List<AttendanceSession> getAttendanceSessionBySessionName(String sessionName) {
        return attendanceSessionRepository.findAll().stream()
                .limit(100)
                .collect(java.util.stream.Collectors.toList());
    }
}
