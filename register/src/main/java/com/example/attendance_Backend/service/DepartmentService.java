package com.example.attendance_Backend.service;

import com.example.attendance_Backend.dto.DepartmentDTO;
import com.example.attendance_Backend.model.ClassEntity;
import com.example.attendance_Backend.repository.ClassRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartmentService {

    @Autowired
    private ClassRepository classRepository;

    public Page<DepartmentDTO> getAllDepartments(Pageable pageable) {
        return classRepository.findAll(pageable).map(this::convertToDepartmentDTO);
    }

    public DepartmentDTO getDepartmentByID(Long departmentID) {
        var optional = classRepository.findById(departmentID);
        return optional.map(this::convertToDepartmentDTO).orElse(null);
    }

    public List<DepartmentDTO> searchDepartments(String keyword) {
        List<ClassEntity> classes = classRepository.findAll().stream()
                .filter(c -> c.getClassName() != null && c.getClassName().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
        return classes.stream()
                .map(this::convertToDepartmentDTO)
                .collect(Collectors.toList());
    }

    public Long getDepartmentCount() {
        return classRepository.count();
    }

    private DepartmentDTO convertToDepartmentDTO(ClassEntity classEntity) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setDepartmentId((long) classEntity.getId());
        dto.setDepartmentName(classEntity.getClassName() != null ? classEntity.getClassName() : "");
        dto.setDepartmentCode(classEntity.getSubject() != null ? classEntity.getSubject() : "DEPT" + classEntity.getId());
        dto.setManagerName(classEntity.getTeacherName() != null ? classEntity.getTeacherName() : "Not Assigned");
        dto.setEmployeeCount(0);
        return dto;
    }
}
