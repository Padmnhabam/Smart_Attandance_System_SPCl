package com.example.attendance_Backend.controller;

import com.example.attendance_Backend.dto.EmployeeDTO;
import com.example.attendance_Backend.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/employees")
@CrossOrigin(origins = "*", maxAge = 3600)
public class EmployeeControllerV2 {

    @Autowired
    private EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<Page<EmployeeDTO>> getAllEmployees(Pageable pageable) {
        return ResponseEntity.ok(employeeService.getAllEmployees(pageable));
    }

    @GetMapping("/{employeeID}")
    public ResponseEntity<EmployeeDTO> getEmployeeByID(@PathVariable Long employeeID) {
        EmployeeDTO employee = employeeService.getEmployeeByID(employeeID);
        if (employee != null) {
            return ResponseEntity.ok(employee);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<EmployeeDTO>> searchEmployees(@RequestParam String keyword) {
        List<EmployeeDTO> results = employeeService.searchEmployees(keyword);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getEmployeeCount() {
        Map<String, Object> response = new HashMap<>();
        response.put("totalEmployees", employeeService.getEmployeeCount());
        return ResponseEntity.ok(response);
    }
}
