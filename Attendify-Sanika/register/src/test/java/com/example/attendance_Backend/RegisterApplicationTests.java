package com.example.attendance_Backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.attendance_Backend.repository.*;
import com.example.attendance_Backend.model.*;
import com.example.attendance_Backend.dto.StudentAttendanceDTO;
import java.util.*;

@SpringBootTest
class RegisterApplicationTests {

	@Autowired
	private ClassMasterRepository classMasterRepository;
	@Autowired
	private DivisionMasterRepository divisionMasterRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private AdminRepository adminRepository;
	@Autowired
	private AttendanceRepository attendanceRepository;
	@Autowired
	private TeacherRepository teacherRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void diagnosticFullDump() {
		System.out.println("--- DIAGNOSTIC: Full Data Dump ---");
		
		System.out.println("\n--- Admins ---");
		adminRepository.findAll().forEach(a -> 
			System.out.println("Admin: " + a.getName() + " (ID: " + a.getId() + ")"));

		System.out.println("\n--- Classes ---");
		classMasterRepository.findAll().forEach(c -> 
			System.out.println("Class: " + c.getClassName() + " (ID: " + c.getId() + ") - AdminID: " + (c.getAdmin() != null ? c.getAdmin().getId() : "NULL")));

		System.out.println("\n--- Divisions ---");
		divisionMasterRepository.findAll().forEach(d -> 
			System.out.println("Division: " + d.getDivisionName() + " (ID: " + d.getId() + ") - ClassID: " + (d.getClassMaster() != null ? d.getClassMaster().getId() : "NULL")));

		System.out.println("\n--- Users (All) ---");
		userRepository.findAll().forEach(u -> 
			System.out.println("User: " + u.getName() + " (ID: " + u.getId() + ") - Role: " + u.getRole() + " - AdminID: " + (u.getAdmin() != null ? u.getAdmin().getId() : "NULL") + " - ClassID: " + (u.getClassMaster() != null ? u.getClassMaster().getId() : "NULL") + " - Roll: " + u.getRollNo()));

		System.out.println("\n--- Teachers ---");
		teacherRepository.findAll().forEach(t -> 
			System.out.println("Teacher: " + t.getName() + " (ID: " + t.getId() + ") - AdminID: " + (t.getAdmin() != null ? t.getAdmin().getId() : "NULL")));

		System.out.println("--- END DIAGNOSTIC ---");
	}

	@Test
	void testRepositoryQueryManual() {
		System.out.println("--- TESTING: Manual Query Execution ---");
		// These values should match what's in the DB based on previous output
		Long adminId = 10L; 
		// Test with native query
		List<Object[]> allStudents = attendanceRepository.getStudentTabDataNative(adminId);
		System.out.println("getStudentTabDataNative(10) size: " + allStudents.size());
		for (Object[] row : allStudents) {
			System.out.println("  Student: " + row[2] + " (Roll: " + row[1] + ") Class: " + row[3] + " Status: " + row[5]);
		}

		System.out.println("--- END TESTING ---");
	}

	@Test
	void debugQueryStepByStep() {
		System.out.println("--- DEBUG: Step-by-step query analysis ---");

		// Step 1: Does findByAdminId work?
		List<User> byAdmin = userRepository.findByAdminId(10L);
		System.out.println("Step1 findByAdminId(10): " + byAdmin.size());

		// Step 2: Are the users marked as STUDENT role?
		for (User u : byAdmin) {
			System.out.println("  User " + u.getId() + ": role='" + u.getRole() + "'");
			System.out.println("    classMaster=" + (u.getClassMaster() != null ? u.getClassMaster().getId() + "/" + u.getClassMaster().getClassName() : "NULL"));
		}

		// Step 3: Try getStudentTabDataNative with admin ID 10
		List<Object[]> result = attendanceRepository.getStudentTabDataNative(10L);
		System.out.println("Step3 getStudentTabDataNative(10): " + result.size());
		for (Object[] row : result) {
			System.out.println("  Student: " + row[2] + " Roll: " + row[1]);
		}

		System.out.println("--- END DEBUG ---");
	}

}
