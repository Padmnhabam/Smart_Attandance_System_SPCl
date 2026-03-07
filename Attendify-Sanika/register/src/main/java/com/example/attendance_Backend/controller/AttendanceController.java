package com.example.attendance_Backend.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.attendance_Backend.dto.AttendanceDTO;
import com.example.attendance_Backend.dto.DateAnalyticsDTO;
import com.example.attendance_Backend.dto.StudentAttendanceDTO;
import com.example.attendance_Backend.dto.SubjectAnalyticsDTO;
import com.example.attendance_Backend.model.Attendance;
import com.example.attendance_Backend.model.User;
import com.example.attendance_Backend.model.Admin;
import com.example.attendance_Backend.model.AttendanceSession;
import com.example.attendance_Backend.repository.AttendanceRepository;
import com.example.attendance_Backend.repository.AttendanceSessionRepository;
import com.example.attendance_Backend.repository.SubjectMasterRepository;
import com.example.attendance_Backend.repository.UserRepository;
import com.example.attendance_Backend.service.TeacherAttendanceService;
import com.example.attendance_Backend.service.TeacherService;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "*")
public class AttendanceController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private AttendanceSessionRepository sessionRepository;

    @Autowired
    private SubjectMasterRepository subjectMasterRepository;

    private final TeacherAttendanceService attendanceService;

    public AttendanceController(TeacherAttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    // 1️⃣ STUDENT MARKS ATTENDANCE
    @PostMapping("/mark")
    public Map<String, String> markAttendance(
            @RequestParam String rollNo,
            @RequestParam String deviceId,
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam String sessionId) {
        Map<String, String> response = new HashMap<>();

        if (sessionId == null || sessionId.isEmpty()) {
            response.put("message", "Session ID required ❌");
            return response;
        }

        AttendanceSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            response.put("message", "Invalid QR session ❌");
            return response;
        }

        Admin admin = session.getAdmin();
        if (admin == null) {
            response.put("message", "Session not associated with any admin ❌");
            return response;
        }
        Long adminId = admin.getId();

        User user = userRepository.findByRollNoAndAdminId(rollNo, adminId).orElse(null);
        if (user == null) {
            response.put("message", "Invalid Roll Number for this organization");
            return response;
        }

        // ⏳ check expiry
        if (session.getExpiryTime().isBefore(LocalDateTime.now())) {
            response.put("message", "QR expired ❌");
            return response;
        }
        // geofence check
        double teacherLat = session.getTeacherLat();
        double teacherLng = session.getTeacherLng();
        double distance = calculateDistance(teacherLat, teacherLng, latitude, longitude);
        if (distance > session.getRadiusKm()) { // default 0.1 km
            response.put("message", "You are outside classroom range ❌");
            return response;
        }
        LocalDate today = LocalDate.now();
        // ✅ Check if student already marked for THIS specific session
        boolean alreadyMarked = attendanceRepository.existsByUser_IdAndSessionIdAndAdminId(user.getId(), sessionId,
                adminId);
        if (alreadyMarked) {
            response.put("message", "Attendance already marked for this session");
            return response;
        }
        // 🔒 Block same device for THIS specific session (proxy prevention)
        boolean deviceUsed = attendanceRepository.existsByDeviceIdAndSessionIdAndAdminId(deviceId, sessionId, adminId);
        if (deviceUsed) {
            response.put("message", "Attendance already marked from this device ❌");
            return response;
        }
        Attendance attendance = new Attendance();
        attendance.setUser(user);
        attendance.setSubjectMaster(session.getSubjectMaster());
        attendance.setDate(today);
        attendance.setStatus("Present");
        attendance.setDeviceId(deviceId);
        attendance.setSessionId(sessionId);
        attendance.setAdmin(admin);

        // Fix: Set class and division from student profile for reporting
        attendance.setClassMaster(user.getClassMaster());
        attendance.setDivisionMaster(user.getDivisionMaster());

        attendanceRepository.save(attendance);
        response.put("message", "Attendance marked successfully ✅");
        return response;
    }

    // helper for geofence
    private double calculateDistance(double lat1, double lon1,
            double lat2, double lon2) {
        final int R = 6371; // Earth radius in KM
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2)
                        * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // returns distance in KM
    }

    // 2️⃣ TEACHER FINALIZE (MARK ABSENT)
    @PostMapping("/finalize")
    public String finalizeAttendance(
            @RequestParam String subject,
            @RequestParam String className,
            @RequestParam(required = false) String divisionId) {
        LocalDate today = LocalDate.now();

        Integer cId = null;
        try {
            cId = Integer.parseInt(className);
        } catch (Exception e) {
        }

        Integer sId = null;
        try {
            sId = Integer.parseInt(subject);
        } catch (Exception e) {
        }

        Long adminId = com.example.attendance_Backend.security.AdminContextHolder.getAdminId();
        if (adminId == null)
            return "Admin context required";
        Admin admin = new Admin();
        admin.setId(adminId);

        Integer dId = null;
        try {
            if (divisionId != null && !divisionId.isEmpty() && !divisionId.equals("undefined")) {
                dId = Integer.parseInt(divisionId);
            }
        } catch (Exception e) {
        }

        if (cId == null || sId == null)
            return "Invalid IDs";

        List<User> students;
        if (dId != null) {
            students = userRepository.findByClassMaster_IdAndDivisionMaster_IdAndAdminId(cId, dId, adminId);
        } else {
            students = userRepository.findByClassMaster_IdAndAdminId(cId, adminId);
        }

        for (User user : students) {

            boolean alreadyMarked = attendanceRepository.existsByUser_IdAndDateAndSubjectMaster_IdAndAdminId(
                    user.getId(), today, sId, adminId);

            if (!alreadyMarked) {
                Attendance attendance = new Attendance();
                attendance.setUser(user);
                attendance.setSubjectMaster(subjectMasterRepository.findById(sId).orElse(null));
                attendance.setDate(today);
                attendance.setStatus("Absent");
                attendance.setAdmin(admin);

                // Fix: Set class and division from student profile for reporting
                attendance.setClassMaster(user.getClassMaster());
                attendance.setDivisionMaster(user.getDivisionMaster());

                attendanceRepository.save(attendance);
            }
        }
        return "Attendance finalized successfully";
    }

    // TEACHER VIEW
    @GetMapping("/teacher/students")
    public List<User> getTeacherStudents(
            @RequestParam int teacherId,
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Integer divisionId) {
        return teacherService.getStudentsForTeacher(teacherId, classId, divisionId);
    }

    @GetMapping("/teacher/student-list")
    public List<StudentAttendanceDTO> studentTab(
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Integer divisionId,
            @RequestParam(required = false) Integer subjectId) {
        Long adminId = com.example.attendance_Backend.security.AdminContextHolder.getAdminId();
        if (adminId == null)
            return java.util.Collections.emptyList();

        if (classId == null && divisionId == null && subjectId == null) {
            return attendanceRepository.getStudentTabData(adminId);
        }
        return attendanceRepository.getFilteredStudentTabData(classId, divisionId, subjectId, adminId);
    }

    @GetMapping("/report")
    public List<AttendanceDTO> generateReport(
            @RequestParam String className) {
        Long adminId = com.example.attendance_Backend.security.AdminContextHolder.getAdminId();
        if (adminId == null)
            return java.util.Collections.emptyList();
        return attendanceRepository.attendanceReportByClass(className, adminId);
    }

    @GetMapping("/analytics/subject")
    public List<SubjectAnalyticsDTO> getSubjectAnalytics() {
        return teacherService.getSubjectAnalytics();
    }

    @GetMapping("/analytics/department")
    public List<SubjectAnalyticsDTO> departmentAnalytics() {
        return teacherService.getDepartmentAnalytics();
    }

    @GetMapping("/analytics/date")
    public List<DateAnalyticsDTO> dateAnalytics() {
        return teacherService.getDateAnalytics();
    }

    @GetMapping("/check/{id}")
    public String checkAttendance(@PathVariable int id) {
        return attendanceService.checkAttendanceAndNotify(id);
    }

    @PostMapping("/manual")
    public Map<String, String> markManualAttendance(
            @RequestParam String rollNo,
            @RequestParam String subject,
            @RequestParam String status) {

        Map<String, String> response = new HashMap<>();

        Optional<User> studentOptional = userRepository.findByRollNo(rollNo);

        if (studentOptional.isEmpty()) {
            response.put("message", "Student not found ❌");
            return response;
        }

        Long adminId = com.example.attendance_Backend.security.AdminContextHolder.getAdminId();
        if (adminId == null) {
            response.put("message", "Admin context required ❌");
            return response;
        }
        Admin admin = new Admin();
        admin.setId(adminId);

        User student = studentOptional.get();
        if (!adminId.equals(student.getAdmin() != null ? student.getAdmin().getId() : null)) {
            response.put("message", "Student does not belong to your organization ❌");
            return response;
        }

        LocalDate today = LocalDate.now();

        Integer subjectId = null;
        try {
            subjectId = Integer.parseInt(subject);
        } catch (Exception e) {
        }
        if (subjectId == null) {
            response.put("message", "Invalid subject ID");
            return response;
        }

        // Check if already marked
        boolean alreadyMarked = attendanceRepository.existsByUser_IdAndDateAndSubjectMaster_IdAndAdminId(
                student.getId(), today, subjectId, adminId);

        if (alreadyMarked) {
            response.put("message", "Attendance already marked for today ❌");
            return response;
        }

        Attendance attendance = new Attendance();
        attendance.setUser(student);
        attendance.setSubjectMaster(subjectMasterRepository.findById(subjectId).orElse(null));
        attendance.setDate(today);
        attendance.setStatus(status); // Present or Absent
        attendance.setDeviceId("MANUAL"); // mark as manual entry
        attendance.setAdmin(admin);

        // Fix: Set class and division from student profile for reporting
        attendance.setClassMaster(student.getClassMaster());
        attendance.setDivisionMaster(student.getDivisionMaster());

        attendanceRepository.save(attendance);

        response.put("message", "Manual attendance saved successfully ✅");
        return response;
    }

    // ==========================================
    // MONTHLY EXCEL REPORT ENDPOINT
    // ==========================================

    /**
     * Returns a structured monthly attendance report for Excel download.
     * Response: { subjects: [...], students: [{rollNo, name,
     * subjects:{s1:{present,total,pct}}, overall:{present,total,pct}}] }
     */
    @GetMapping("/monthly-report")
    public Map<String, Object> getMonthlyReport(
            @RequestParam int classId,
            @RequestParam int divisionId,
            @RequestParam int month,
            @RequestParam int year) {

        Long adminId = com.example.attendance_Backend.security.AdminContextHolder.getAdminId();
        if (adminId == null)
            return java.util.Collections.emptyMap();

        List<Object[]> rows = attendanceRepository.getMonthlyReportRaw(classId, divisionId, month, year, adminId);

        // Pivot: studentId -> subject -> {present, total}
        LinkedHashMap<Integer, Map<String, Object>> studentMap = new LinkedHashMap<>();
        java.util.TreeSet<String> subjectSet = new java.util.TreeSet<>();

        for (Object[] row : rows) {
            int userId = ((Number) row[0]).intValue();
            String rollNo = (String) row[1];
            String name = (String) row[2];
            String subject = (String) row[3];
            long present = ((Number) row[4]).longValue();
            long total = ((Number) row[5]).longValue();

            subjectSet.add(subject);

            studentMap.computeIfAbsent(userId, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("rollNo", rollNo);
                m.put("name", name);
                m.put("subjects", new LinkedHashMap<String, Object>());
                return m;
            });

            @SuppressWarnings("unchecked")
            Map<String, Object> subjectData = (Map<String, Object>) studentMap.get(userId).get("subjects");
            Map<String, Object> subEntry = new LinkedHashMap<>();
            subEntry.put("present", present);
            subEntry.put("total", total);
            subEntry.put("pct", total == 0 ? 0 : Math.round(present * 100.0 / total));
            subjectData.put(subject, subEntry);
        }

        // Compute overall per student
        List<Map<String, Object>> studentList = new ArrayList<>();
        for (Map<String, Object> s : studentMap.values()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> subs = (Map<String, Object>) s.get("subjects");
            long totalPresent = 0, totalTotal = 0;
            for (Object sv : subs.values()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> sv2 = (Map<String, Object>) sv;
                totalPresent += ((Number) sv2.get("present")).longValue();
                totalTotal += ((Number) sv2.get("total")).longValue();
            }
            Map<String, Object> overall = new LinkedHashMap<>();
            overall.put("present", totalPresent);
            overall.put("total", totalTotal);
            overall.put("pct", totalTotal == 0 ? 0 : Math.round(totalPresent * 100.0 / totalTotal));
            s.put("overall", overall);
            studentList.add(s);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("subjects", new ArrayList<>(subjectSet));
        result.put("students", studentList);
        return result;
    }

}
