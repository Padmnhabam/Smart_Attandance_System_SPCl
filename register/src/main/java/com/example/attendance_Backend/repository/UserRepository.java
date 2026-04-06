package com.example.attendance_Backend.repository;

import com.example.attendance_Backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    // Admin-filtered methods
    long countByDivisionMaster_IdAndAdminId(Integer divisionId, Long adminId);

    long countByAdminId(Long adminId);

    Optional<User> findByIdAndAdminId(Integer id, Long adminId);

    Optional<User> findByEmailAndPasswordAndAdminId(String email, String password, Long adminId);

    Optional<User> findByEmailAndAdminId(String email, Long adminId);

    Optional<User> findByRollNoAndAdminId(String rollNo, Long adminId);

    List<User> findByClassMaster_IdAndAdminId(Integer classId, Long adminId);

    List<User> findByClassMaster_IdAndDivisionMaster_IdAndAdminId(Integer classId, Integer divisionId, Long adminId);

    List<User> findByClassMasterIsNotNullAndAdminId(Long adminId);

    List<User> findByAdminId(Long adminId);

    List<User> findByAdminId(Long adminId, org.springframework.data.domain.Pageable pageable);

    // Legacy methods
    long countByDivisionMaster_Id(Integer divisionId);

    Optional<User> findByEmailAndPassword(String email, String password);

    Optional<User> findByEmail(String email);

    Optional<User> findByRollNo(String rollNo);

    List<User> findByClassMaster_Id(Integer classId);

    List<User> findByClassMaster_IdAndDivisionMaster_Id(Integer classId, Integer divisionId);

    List<User> findByClassMasterIsNotNull();

}
