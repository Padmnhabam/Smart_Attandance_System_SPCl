package com.example.attendance_Backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

import com.example.attendance_Backend.model.AttendanceSession;

public interface AttendanceSessionRepository
                extends JpaRepository<AttendanceSession, String> {

        long countByDivisionMaster_Id(Integer divisionId);

        long countBySubjectMaster_Id(Integer subjectId);

        @Query("SELECT s FROM AttendanceSession s WHERE s.timetableSlotId = :timetableSlotId AND s.admin.id = :adminId AND s.expiryTime BETWEEN :startOfDay AND :endOfDay")
        List<AttendanceSession> findByTimetableSlotIdAndExpiryTimeBetweenAndAdminId(
                        @org.springframework.data.repository.query.Param("timetableSlotId") Integer timetableSlotId,
                        @org.springframework.data.repository.query.Param("adminId") Long adminId,
                        @org.springframework.data.repository.query.Param("startOfDay") java.time.LocalDateTime startOfDay,
                        @org.springframework.data.repository.query.Param("endOfDay") java.time.LocalDateTime endOfDay);
}
