package com.example.attendance_Backend.controller;

import com.example.attendance_Backend.dto.DepartmentDTO;
import com.example.attendance_Backend.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/departments")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DepartmentControllerV2 {

    @Autowired
    private DepartmentService departmentService;

    @GetMapping
    public ResponseEntity<Page<DepartmentDTO>> getAllDepartments(Pageable pageable) {
        return ResponseEntity.ok(departmentService.getAllDepartments(pageable));
    }

    @GetMapping("/{departmentID}")
    public ResponseEntity<DepartmentDTO> getDepartmentByID(@PathVariable Long departmentID) {
        DepartmentDTO department = departmentService.getDepartmentByID(departmentID);
        if (department != null) {
            return ResponseEntity.ok(department);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<DepartmentDTO>> searchDepartments(@RequestParam String keyword) {
        List<DepartmentDTO> results = departmentService.searchDepartments(keyword);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getDepartmentCount() {
        Map<String, Object> response = new HashMap<>();
        response.put("totalDepartments", departmentService.getDepartmentCount());
        return ResponseEntity.ok(response);
    }
}
