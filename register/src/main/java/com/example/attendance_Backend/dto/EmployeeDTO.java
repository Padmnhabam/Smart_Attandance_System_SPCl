package com.example.attendance_Backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDTO {
    private Long userId;
    private String employeeID;
    private String employeeName;
    private String email;
    private String phoneNumber;
    private String department;
    private String designation;
    private String status;
    private String role;
}
