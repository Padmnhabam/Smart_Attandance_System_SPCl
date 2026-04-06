package com.example.attendance_Backend.repository;

import com.example.attendance_Backend.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Integer> {

    // Admin-filtered methods
    List<Teacher> findByAdminId(Long adminId);

    List<Teacher> findByAdminId(Long adminId, org.springframework.data.domain.Pageable pageable);

    Optional<Teacher> findByIdAndAdminId(Integer id, Long adminId);

    long countByAdminId(Long adminId);

    Optional<Teacher> findByEmailAndPasswordAndAdminId(String email, String password, Long adminId);

    Optional<Teacher> findByEmailAndAdminId(String email, Long adminId);

    boolean existsByEmailAndAdminId(String email, Long adminId);

    // Legacy methods
    Optional<Teacher> findByEmailAndPassword(String email, String password);

    Optional<Teacher> findByEmail(String email);

    boolean existsByEmail(String email);
}
