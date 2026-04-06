package com.example.attendance_Backend.service;

import com.example.attendance_Backend.dto.ManagerDTO;
import com.example.attendance_Backend.model.Teacher;
import com.example.attendance_Backend.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ManagerService {

    @Autowired
    private TeacherRepository teacherRepository;

    public Page<ManagerDTO> getAllManagers(Pageable pageable) {
        return teacherRepository.findAll(pageable).map(this::convertToManagerDTO);
    }

    public ManagerDTO getManagerByID(Long managerID) {
        return teacherRepository.findById(managerID.intValue())
                .map(this::convertToManagerDTO)
                .orElse(null);
    }

    public List<ManagerDTO> searchManagers(String keyword) {
        List<Teacher> teachers = teacherRepository.findAll().stream()
                .filter(t -> t.getName() != null && t.getName().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
        return teachers.stream()
                .map(this::convertToManagerDTO)
                .collect(Collectors.toList());
    }

    public Long getManagerCount() {
        return teacherRepository.count();
    }

    private ManagerDTO convertToManagerDTO(Teacher teacher) {
        ManagerDTO dto = new ManagerDTO();
        dto.setManagerId((long) teacher.getId());
        // dto.setManagerID("MGR" + teacher.getId()); // Lombok not generating this setter
        dto.setManagerName(teacher.getName() != null ? teacher.getName() : "");
        dto.setEmail(teacher.getEmail() != null ? teacher.getEmail() : "");
        dto.setPhoneNumber(teacher.getMobilenumber() != null ? teacher.getMobilenumber() : "");
        String depName = teacher.getDepartment() != null ? teacher.getDepartment().toString() : "Not Assigned";
        dto.setDepartment(depName);
        dto.setDesignation("Manager");
        dto.setStatus("Active");
        return dto;
    }
}
