package com.example.attendance_Backend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendance_Backend.dto.AttendanceDTO;
import com.example.attendance_Backend.dto.DateAnalyticsDTO;
import com.example.attendance_Backend.dto.StudentAttendanceDTO;
import com.example.attendance_Backend.dto.SubjectAnalyticsDTO;
import com.example.attendance_Backend.model.Attendance;

public interface AttendanceRepository extends JpaRepository<Attendance, Integer> {
    long countByDivisionMaster_Id(Integer divisionId);

    long countBySubjectMaster_Id(Integer subjectId);

    // Session-scoped duplicate checks (used in markAttendance)
    boolean existsByUser_IdAndSessionIdAndAdminId(int userId, String sessionId, Long adminId);

    boolean existsByDeviceIdAndSessionIdAndAdminId(String deviceId, String sessionId, Long adminId);

    @Transactional
    @Modifying
    @Query("DELETE FROM Attendance a WHERE a.sessionId = :sessionId AND a.admin.id = :adminId")
    void deleteBySessionIdAndAdminId(@Param("sessionId") String sessionId, @Param("adminId") Long adminId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.user.id = :id AND a.admin.id = :adminId")
    int totalClasses(@Param("id") int userId, @Param("adminId") Long adminId);

    @Query("""
                SELECT COUNT(a)
                FROM Attendance a
                WHERE a.user.id = :id AND LOWER(a.status) = 'present' AND a.admin.id = :adminId
            """)
    int presentCount(@Param("id") int userId, @Param("adminId") Long adminId);

    @Query("""
                SELECT COUNT(a)
                FROM Attendance a
                WHERE a.user.id = :id AND LOWER(a.status) = 'absent' AND a.admin.id = :adminId
            """)
    int absentCount(@Param("id") int userId, @Param("adminId") Long adminId);

    @Query("""
                SELECT new com.example.attendance_Backend.dto.AttendanceDTO(
                    a.date,
                    a.subjectMaster.subjectName,
                    a.status
                )
                FROM Attendance a
                WHERE a.user.id = :id AND a.admin.id = :adminId
            """)
    List<AttendanceDTO> attendanceList(@Param("id") int userId, @Param("adminId") Long adminId);

    boolean existsByUser_IdAndDateAndSubjectMaster_IdAndAdminId(
            int userId,
            LocalDate date,
            Integer subjectId,
            Long adminId);

    // 🔒 NEW — block same device
    @Query("SELECT COUNT(a) > 0 FROM Attendance a WHERE a.deviceId = :deviceId AND a.date = :date AND a.subjectMaster.id = :subjectId AND a.admin.id = :adminId")
    boolean existsByDeviceIdAndDateAndSubjectMaster_IdAndAdminId(
            @Param("deviceId") String deviceId,
            @Param("date") LocalDate date,
            @Param("subjectId") Integer subjectId,
            @Param("adminId") Long adminId);

    List<Attendance> findByDateAndSubjectMaster_IdAndAdminId(
            LocalDate date,
            Integer subjectId,
            Long adminId);

    @Query("""
                SELECT new com.example.attendance_Backend.dto.AttendanceDTO(
                    a.date,
                    a.subjectMaster.subjectName,
                    u.rollNo,
                    u.name,
                    a.status
                )
                FROM Attendance a
                JOIN a.user u
                WHERE a.subjectMaster.subjectName = :subject
                AND a.admin.id = :adminId
                ORDER BY a.date DESC
            """)
    List<AttendanceDTO> attendanceListForTeacher(
            @Param("subject") String subject,
            @Param("adminId") Long adminId);

    @Query("""
            SELECT new com.example.attendance_Backend.dto.StudentAttendanceDTO(
                u.id,
                u.rollNo,
                u.name,
                u.classMaster.className,
                COALESCE(a.subjectMaster.subjectName, '-'),
                COALESCE(a.status, 'Absent')
            )
            FROM User u
            LEFT JOIN Attendance a
                ON a.user = u
                AND a.date = CURRENT_DATE
                AND (:subjectId IS NULL OR a.subjectMaster.id = :subjectId)
            WHERE (:classId IS NULL OR u.classMaster.id = :classId)
            AND (:divisionId IS NULL OR u.divisionMaster.id = :divisionId)
            AND (u.role = 'STUDENT')
            AND (u.admin.id = :adminId)
            ORDER BY u.rollNo
            """)
    List<StudentAttendanceDTO> getFilteredStudentTabData(
            @Param("classId") Integer classId,
            @Param("divisionId") Integer divisionId,
            @Param("subjectId") Integer subjectId,
            @Param("adminId") Long adminId);

    @Query("""
            SELECT new com.example.attendance_Backend.dto.StudentAttendanceDTO(
                u.id,
                u.rollNo,
                u.name,
                u.classMaster.className,
                COALESCE(a.subjectMaster.subjectName, '-'),
                COALESCE(a.status, 'Absent')
            )
            FROM User u
            LEFT JOIN Attendance a
                ON a.user = u
                AND a.date = CURRENT_DATE
            WHERE u.role = 'STUDENT'
            AND u.admin.id = :adminId
            ORDER BY u.rollNo
            """)
    List<StudentAttendanceDTO> getStudentTabData(@Param("adminId") Long adminId);

    @Query("""
                SELECT a FROM Attendance a
                WHERE a.user.rollNo = :rollNo
                AND a.date = CURRENT_DATE
            """)
    Optional<Attendance> findByRollNo(@Param("rollNo") String rollNo);

    @Query("SELECT a FROM Attendance a WHERE a.user.rollNo = :rollNo AND a.admin.id = :adminId")
    Optional<Attendance> findByRollNoAndAdminId(@Param("rollNo") String rollNo, @Param("adminId") Long adminId);

    @Query("""
                SELECT new com.example.attendance_Backend.dto.AttendanceDTO(
                    a.date,
                    a.subjectMaster.subjectName,
                    u.rollNo,
                    u.name,
                    a.status
                )
                FROM Attendance a
                JOIN a.user u
                WHERE u.classMaster.className = :className
                AND a.admin.id = :adminId
                ORDER BY a.date DESC
            """)
    List<AttendanceDTO> attendanceReportByClass(@Param("className") String className, @Param("adminId") Long adminId);

    @Query("""
                SELECT new com.example.attendance_Backend.dto.SubjectAnalyticsDTO(
                    a.subjectMaster.subjectName,
                    COUNT(a),
                    COALESCE(SUM(CASE WHEN LOWER(a.status) = 'present' THEN 1 ELSE 0 END),0),
                    COALESCE(SUM(CASE WHEN LOWER(a.status) = 'absent' THEN 1 ELSE 0 END),0)
                )
                FROM Attendance a
                WHERE a.admin.id = :adminId
                GROUP BY a.subjectMaster.subjectName
            """)
    List<SubjectAnalyticsDTO> getSubjectAnalyticsByAdminId(@Param("adminId") Long adminId);

    @Query("""
                SELECT new com.example.attendance_Backend.dto.SubjectAnalyticsDTO(
                    u.classMaster.className,
                    COUNT(a),
                    COALESCE(SUM(CASE WHEN LOWER(a.status) = 'present' THEN 1 ELSE 0 END), 0),
                    COALESCE(SUM(CASE WHEN LOWER(a.status) = 'absent' THEN 1 ELSE 0 END), 0)
                )
                FROM Attendance a
                JOIN a.user u
                WHERE a.admin.id = :adminId
                GROUP BY u.classMaster.className
            """)
    List<SubjectAnalyticsDTO> getDepartmentAnalyticsByAdminId(@Param("adminId") Long adminId);

    @Query("""
                SELECT new com.example.attendance_Backend.dto.DateAnalyticsDTO(
                    a.date,
                    COUNT(a),
                    COALESCE(SUM(CASE WHEN LOWER(a.status) = 'present' THEN 1 ELSE 0 END), 0),
                    COALESCE(SUM(CASE WHEN LOWER(a.status) = 'absent' THEN 1 ELSE 0 END), 0)
                )
                FROM Attendance a
                WHERE a.admin.id = :adminId
                GROUP BY a.date
                ORDER BY a.date
            """)
    List<DateAnalyticsDTO> getDateAnalyticsByAdminId(@Param("adminId") Long adminId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE LOWER(a.status) = 'present' AND a.date = :today")
    int countPresentByDate(LocalDate today);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.date = :today")
    int countTotalByDate(LocalDate today);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE LOWER(a.status) = 'present' AND a.date = :today AND a.admin.id = :adminId")
    int countPresentByDateAndAdminId(@Param("today") LocalDate today, @Param("adminId") Long adminId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.date = :today AND a.admin.id = :adminId")
    int countTotalByDateAndAdminId(@Param("today") LocalDate today, @Param("adminId") Long adminId);

    int countByUserId(int userId);

    int countByUserIdAndStatus(int userId, String status);

    boolean existsByUserAndSubjectMaster_SubjectNameAndDate(
            com.example.attendance_Backend.model.User user,
            String subjectName,
            LocalDate date);

    // ==========================================
    // TEACHER REPORT QUERIES (using session_id JOIN)
    // ==========================================

    /**
     * Teacher's subject-wise attendance report — no session join needed.
     * Works with both old (sessionId=NULL) and new attendance records.
     */
    @Query("""
                SELECT new com.example.attendance_Backend.dto.TeacherSubjectReportDTO(
                    a.subjectMaster.subjectName,
                    SUM(CASE WHEN LOWER(a.status) = 'present' THEN 1L ELSE 0L END),
                    COUNT(a)
                )
                FROM Attendance a
                WHERE a.subjectMaster IS NOT NULL
                AND a.admin.id = :adminId
                GROUP BY a.subjectMaster.subjectName
            """)
    List<com.example.attendance_Backend.dto.TeacherSubjectReportDTO> getTeacherSubjectReportByAdminId(
            @Param("adminId") Long adminId);

    // Legacy method
    @Query("""
                SELECT new com.example.attendance_Backend.dto.TeacherSubjectReportDTO(
                    a.subjectMaster.subjectName,
                    SUM(CASE WHEN LOWER(a.status) = 'present' THEN 1L ELSE 0L END),
                    COUNT(a)
                )
                FROM Attendance a
                WHERE a.subjectMaster IS NOT NULL
                GROUP BY a.subjectMaster.subjectName
            """)
    List<com.example.attendance_Backend.dto.TeacherSubjectReportDTO> getTeacherSubjectReport(
            @Param("teacherId") int teacherId);

    /**
     * Get distinct students with optional class/division filter — no session join.
     */
    @Query("""
                SELECT DISTINCT u
                FROM Attendance a
                JOIN a.user u
                WHERE (:classId IS NULL OR u.classMaster.id = :classId)
                AND (:divisionId IS NULL OR u.divisionMaster.id = :divisionId)
                AND u.admin.id = :adminId
                ORDER BY u.name
            """)
    List<com.example.attendance_Backend.model.User> getFilteredStudentsForTeacher(
            @Param("teacherId") int teacherId,
            @Param("classId") Integer classId,
            @Param("divisionId") Integer divisionId,
            @Param("adminId") Long adminId);

    /** Get all distinct students who have any attendance record */
    @Query("""
                SELECT DISTINCT u
                FROM Attendance a
                JOIN a.user u
                WHERE u.admin.id = :adminId
                ORDER BY u.name
            """)
    List<com.example.attendance_Backend.model.User> getStudentsForTeacher(
            @Param("teacherId") int teacherId,
            @Param("adminId") Long adminId);

    /**
     * Teacher's class-wise attendance report — uses user's class, no session join.
     */
    @Query("""
                SELECT new com.example.attendance_Backend.dto.ClassAttendanceDTO(
                    u.classMaster.className,
                    SUM(CASE WHEN LOWER(a.status) = 'present' THEN 1L ELSE 0L END),
                    COUNT(a)
                )
                FROM Attendance a
                JOIN a.user u
                WHERE u.classMaster IS NOT NULL
                AND a.admin.id = :adminId
                GROUP BY u.classMaster.className
            """)
    List<com.example.attendance_Backend.dto.ClassAttendanceDTO> getTeacherClassReportByAdminId(
            @Param("adminId") Long adminId);

    // Legacy method
    @Query("""
                SELECT new com.example.attendance_Backend.dto.ClassAttendanceDTO(
                    u.classMaster.className,
                    SUM(CASE WHEN LOWER(a.status) = 'present' THEN 1L ELSE 0L END),
                    COUNT(a)
                )
                FROM Attendance a
                JOIN a.user u
                WHERE u.classMaster IS NOT NULL
                GROUP BY u.classMaster.className
            """)
    List<com.example.attendance_Backend.dto.ClassAttendanceDTO> getTeacherClassReport(
            @Param("teacherId") int teacherId);

    // ==========================================
    // STUDENT ANALYTICS QUERIES
    // ==========================================

    /** Student's subject-wise attendance summary */
    @Query("""
                SELECT new com.example.attendance_Backend.dto.StudentSubjectSummaryDTO(
                    a.subjectMaster.subjectName,
                    SUM(CASE WHEN LOWER(a.status) = 'present' THEN 1L ELSE 0L END),
                    COUNT(a)
                )
                FROM Attendance a
                WHERE a.user.id = :studentId
                AND a.admin.id = :adminId
                GROUP BY a.subjectMaster.subjectName
            """)
    List<com.example.attendance_Backend.dto.StudentSubjectSummaryDTO> getStudentSubjectSummary(
            @Param("studentId") int studentId,
            @Param("adminId") Long adminId);

    @Query("""
                SELECT new com.example.attendance_Backend.dto.StudentDateRecordDTO(
                    a.date, a.subjectMaster.subjectName, a.status
                )
                FROM Attendance a
                WHERE a.user.id = :studentId
                AND a.admin.id = :adminId
                AND (:subject IS NULL OR a.subjectMaster.subjectName = :subject)
                AND (:fromDate IS NULL OR a.date >= :fromDate)
                AND (:toDate IS NULL OR a.date <= :toDate)
                ORDER BY a.date DESC
            """)
    List<com.example.attendance_Backend.dto.StudentDateRecordDTO> getStudentDateRecords(
            @Param("studentId") int studentId,
            @Param("adminId") Long adminId,
            @Param("subject") String subject,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /** Student's monthly attendance breakdown */
    @Query("""
                SELECT new com.example.attendance_Backend.dto.MonthlyAttendanceDTO(
                    MONTH(a.date),
                    SUM(CASE WHEN LOWER(a.status) = 'present' THEN 1L ELSE 0L END),
                    COUNT(a)
                )
                FROM Attendance a
                WHERE a.user.id = :studentId
                AND a.admin.id = :adminId
                GROUP BY MONTH(a.date)
                ORDER BY MONTH(a.date)
            """)
    List<com.example.attendance_Backend.dto.MonthlyAttendanceDTO> getMonthlyAttendance(
            @Param("studentId") int studentId,
            @Param("adminId") Long adminId);

    // ==========================================
    // ADMIN REPORT QUERIES
    // ==========================================

    /** Class-wise attendance report for admin */
    @Query("""
                SELECT new com.example.attendance_Backend.dto.ClassAttendanceDTO(
                    u.classMaster.className,
                    SUM(CASE WHEN LOWER(a.status) = 'present' THEN 1L ELSE 0L END),
                    COUNT(a)
                )
                FROM Attendance a
                JOIN a.user u
                WHERE u.classMaster IS NOT NULL
                AND a.admin.id = :adminId
                GROUP BY u.classMaster.className
            """)
    List<com.example.attendance_Backend.dto.ClassAttendanceDTO> getClassAttendanceReportByAdminId(
            @Param("adminId") Long adminId);

    /** Low attendance students for admin alert */
    @Query("""
                SELECT new com.example.attendance_Backend.dto.LowAttendanceStudentDTO(
                    u.id,
                    u.name,
                    u.rollNo,
                    u.classMaster.className,
                    SUM(CASE WHEN LOWER(a.status) = 'present' THEN 1L ELSE 0L END),
                    COUNT(a)
                )
                FROM Attendance a
                JOIN a.user u
                WHERE a.admin.id = :adminId
                GROUP BY u.id, u.name, u.rollNo, u.classMaster.className
                HAVING (SUM(CASE WHEN LOWER(a.status) = 'present' THEN 1L ELSE 0L END) * 100.0 / COUNT(a)) < :threshold
            """)
    List<com.example.attendance_Backend.dto.LowAttendanceStudentDTO> getLowAttendanceStudentsByAdminId(
            @Param("threshold") double threshold,
            @Param("adminId") Long adminId);

    // ==========================================
    // MONTHLY EXCEL REPORT QUERY
    // ==========================================

    /**
     * Returns per-student, per-subject attendance counts for a given month/year,
     * filtered by class and division (required).
     * Each row: [userId (int), rollNo (String), name (String), subjectName
     * (String),
     * presentCount (Long), totalCount (Long)]
     */
    @Query("""
                SELECT a.user.id,
                       a.user.rollNo,
                       a.user.name,
                       a.subjectMaster.subjectName,
                       SUM(CASE WHEN LOWER(a.status) = 'present' THEN 1L ELSE 0L END),
                       COUNT(a)
                FROM Attendance a
                WHERE a.classMaster.id = :classId
                AND a.divisionMaster.id = :divisionId
                AND MONTH(a.date) = :month
                AND YEAR(a.date) = :year
                AND a.user.role = 'STUDENT'
                AND a.admin.id = :adminId
                GROUP BY a.user.id, a.user.rollNo, a.user.name, a.subjectMaster.subjectName
                ORDER BY a.user.rollNo ASC
            """)
    List<Object[]> getMonthlyReportRaw(
            @Param("classId") int classId,
            @Param("divisionId") int divisionId,
            @Param("month") int month,
            @Param("year") int year,
            @Param("adminId") Long adminId);

}
