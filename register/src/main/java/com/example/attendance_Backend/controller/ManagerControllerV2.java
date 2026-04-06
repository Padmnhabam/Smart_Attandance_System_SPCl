package com.example.attendance_Backend.controller;

import com.example.attendance_Backend.dto.ManagerDTO;
import com.example.attendance_Backend.service.ManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/managers")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ManagerControllerV2 {

    @Autowired
    private ManagerService managerService;

    @GetMapping
    public ResponseEntity<Page<ManagerDTO>> getAllManagers(Pageable pageable) {
        return ResponseEntity.ok(managerService.getAllManagers(pageable));
    }

    @GetMapping("/{managerID}")
    public ResponseEntity<ManagerDTO> getManagerByID(@PathVariable Long managerID) {
        ManagerDTO manager = managerService.getManagerByID(managerID);
        if (manager != null) {
            return ResponseEntity.ok(manager);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<ManagerDTO>> searchManagers(@RequestParam String keyword) {
        List<ManagerDTO> results = managerService.searchManagers(keyword);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getManagerCount() {
        Map<String, Object> response = new HashMap<>();
        response.put("totalManagers", managerService.getManagerCount());
        return ResponseEntity.ok(response);
    }
}
