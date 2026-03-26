package com.example.attendance_Backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManagerDTO {
    private Long managerId;
    private String managerID;
    private String managerName;
    private String email;
    private String phoneNumber;
    private String department;
    private String designation;
    private String status;
    private Integer teamSize;
}
