package com.example.attendance_Backend.service;

import com.example.attendance_Backend.dto.EmployeeDTO;
import com.example.attendance_Backend.model.User;
import com.example.attendance_Backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    @Autowired
    private UserRepository userRepository;

    public Page<EmployeeDTO> getAllEmployees(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::convertToEmployeeDTO);
    }

    public EmployeeDTO getEmployeeByID(Long employeeID) {
        return userRepository.findById(employeeID.intValue())
                .map(this::convertToEmployeeDTO)
                .orElse(null);
    }

    public List<EmployeeDTO> searchEmployees(String keyword) {
        List<User> users = userRepository.findAll().stream()
                .filter(u -> (u.getName() != null && u.getName().toLowerCase().contains(keyword.toLowerCase())) ||
                             (u.getEmail() != null && u.getEmail().toLowerCase().contains(keyword.toLowerCase())))
                .collect(Collectors.toList());
        return users.stream()
                .map(this::convertToEmployeeDTO)
                .collect(Collectors.toList());
    }

    public Long getEmployeeCount() {
        return userRepository.count();
    }

    private EmployeeDTO convertToEmployeeDTO(User user) {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setUserId((long) user.getId());
        dto.setEmployeeID("EMP" + user.getId());
        dto.setEmployeeName(user.getName() != null ? user.getName() : "");
        dto.setEmail(user.getEmail() != null ? user.getEmail() : "");
        dto.setPhoneNumber(user.getMobilenumber() != null ? user.getMobilenumber() : "");
        String depName = user.getClassMaster() != null ? user.getClassMaster().getClassName() : "Not Assigned";
        dto.setDepartment(depName);
        dto.setDesignation("Employee");
        dto.setStatus("Active");
        dto.setRole(user.getRole() != null ? user.getRole() : "User");
        return dto;
    }
}
