package com.example.attendance_Backend.service;

import com.example.attendance_Backend.dto.*;
import com.example.attendance_Backend.model.User;
import com.example.attendance_Backend.repository.AttendanceRepository;
import com.example.attendance_Backend.security.AdminContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TeacherReportService {

    private final AttendanceRepository attendanceRepository;
    private final com.example.attendance_Backend.repository.ClassSubjectRepository classSubjectRepository;
    private final com.example.attendance_Backend.repository.UserRepository userRepository;

    public TeacherReportService(AttendanceRepository attendanceRepository,
            com.example.attendance_Backend.repository.ClassSubjectRepository classSubjectRepository,
            com.example.attendance_Backend.repository.UserRepository userRepository) {
        this.attendanceRepository = attendanceRepository;
        this.classSubjectRepository = classSubjectRepository;
        this.userRepository = userRepository;
    }

    // =====================
    // TEACHER REPORTS
    // =====================

    /** Teacher's subject-wise attendance stats */
    public List<TeacherSubjectReportDTO> getSubjectReport(int teacherId) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId != null) {
            return attendanceRepository.getTeacherSubjectReportByAdminId(adminId);
        }
        return attendanceRepository.getTeacherSubjectReport(teacherId);
    }

    /** Teacher's class-wise attendance stats */
    public List<ClassAttendanceDTO> getClassReport(int teacherId) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId != null) {
            return attendanceRepository.getTeacherClassReportByAdminId(adminId);
        }
        return attendanceRepository.getTeacherClassReport(teacherId);
    }

    /** Get distinct students from teacher's sessions */
    public List<Map<String, Object>> getStudentsForTeacher(int teacherId) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return Collections.emptyList();

        List<User> users = attendanceRepository.getStudentsForTeacher(teacherId, adminId);
        return users.stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getName());
            m.put("rollNo", u.getRollNo());
            m.put("className", u.getClassMaster() != null ? u.getClassMaster().getClassName() : null);
            return m;
        }).collect(Collectors.toList());
    }

    /** Student's subject-wise breakdown (teacher viewing a specific student) */
    public List<StudentSubjectSummaryDTO> getStudentSubjectSummary(int studentId) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return Collections.emptyList();

        // Verify student belongs to this admin
        if (userRepository.findByIdAndAdminId(studentId, adminId).isEmpty()) {
            return Collections.emptyList();
        }

        return attendanceRepository.getStudentSubjectSummary(studentId, adminId);
    }

    /** Student's date-wise records with optional subject and date filter */
    public List<StudentDateRecordDTO> getStudentDateRecords(int studentId,
            String subject,
            LocalDate fromDate,
            LocalDate toDate) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return Collections.emptyList();

        // Verify student belongs to this admin
        if (userRepository.findByIdAndAdminId(studentId, adminId).isEmpty()) {
            return Collections.emptyList();
        }

        return attendanceRepository.getStudentDateRecords(studentId, adminId, subject, fromDate, toDate);
    }

    // =====================
    // STUDENT ANALYTICS
    // =====================

    /** Student's monthly attendance breakdown */
    public List<MonthlyAttendanceDTO> getMonthlyAttendance(int studentId) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return Collections.emptyList();

        // Verify student belongs to this admin
        if (userRepository.findByIdAndAdminId(studentId, adminId).isEmpty()) {
            return Collections.emptyList();
        }

        return attendanceRepository.getMonthlyAttendance(studentId, adminId);
    }

    // =====================
    // ADMIN REPORTS
    // =====================

    /** Class-wise attendance report */
    public List<ClassAttendanceDTO> getClassAttendanceReport() {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId != null) {
            return attendanceRepository.getClassAttendanceReportByAdminId(adminId);
        }
        return Collections.emptyList(); // Legacy method was removed, returning empty if no context
    }

    /** Low attendance students below given threshold */
    public List<LowAttendanceStudentDTO> getLowAttendanceStudents(double threshold) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId != null) {
            return attendanceRepository.getLowAttendanceStudentsByAdminId(threshold, adminId);
        }
        return Collections.emptyList(); // Legacy method was removed, returning empty if no context
    }

    /** consolidated class report (All Students vs All Subjects of that class) */
    public ConsolidatedAttendanceDTO getConsolidatedClassReport(int classId) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return null;

        // 1. Get all subjects mapped to this class
        List<com.example.attendance_Backend.model.ClassSubject> classSubjects = classSubjectRepository
                .findByClassMasterIdAndAdminId(classId, adminId);
        List<String> subjectNames = classSubjects.stream()
                .map(cs -> cs.getSubjectMaster().getSubjectName())
                .collect(Collectors.toList());

        // 2. Get all students of this class
        List<User> students = userRepository.findByClassMaster_IdAndAdminId(classId, adminId);

        List<StudentConsolidatedDTO> studentDTOs = new ArrayList<>();

        for (User student : students) {
            Map<String, Double> subjectPercentages = new HashMap<>();
            long totalPresent = 0;
            long totalClasses = 0;

            // Fetch summary for this student
            List<StudentSubjectSummaryDTO> summaries = attendanceRepository.getStudentSubjectSummary(student.getId(),
                    adminId);
            Map<String, StudentSubjectSummaryDTO> summaryMap = summaries.stream()
                    .collect(Collectors.toMap(StudentSubjectSummaryDTO::getSubject, s -> s));

            for (String subName : subjectNames) {
                StudentSubjectSummaryDTO summary = summaryMap.get(subName);
                if (summary != null) {
                    subjectPercentages.put(subName, summary.getPercentage());
                    totalPresent += summary.getPresentCount();
                    totalClasses += summary.getTotalClasses();
                } else {
                    subjectPercentages.put(subName, 0.0);
                }
            }

            double overall = totalClasses > 0 ? (totalPresent * 100.0 / totalClasses) : 0.0;
            studentDTOs.add(
                    new StudentConsolidatedDTO(student.getName(), student.getRollNo(), subjectPercentages, overall));
        }

        return new ConsolidatedAttendanceDTO(subjectNames, studentDTOs);
    }
}
