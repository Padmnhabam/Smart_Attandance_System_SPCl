package com.example.attendance_Backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShiftDTO {
    private Long shiftId;
    private String shiftName;
    private String startTime;
    private String endTime;
    private String sessionName;
}
