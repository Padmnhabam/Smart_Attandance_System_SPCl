package com.example.attendance_Backend.service;

import com.example.attendance_Backend.model.Teacher;
import com.example.attendance_Backend.model.TeacherTimetable;
import com.example.attendance_Backend.model.TimetableStructure;
import com.example.attendance_Backend.repository.TeacherTimetableRepository;
import com.example.attendance_Backend.repository.TeacherRepository;
import com.example.attendance_Backend.repository.TimetableStructureRepository;
import com.example.attendance_Backend.repository.ClassMasterRepository;
import com.example.attendance_Backend.repository.DivisionMasterRepository;
import com.example.attendance_Backend.repository.SubjectMasterRepository;
import com.example.attendance_Backend.model.Admin;
import com.example.attendance_Backend.security.AdminContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TeacherTimetableService {

    private final TeacherTimetableRepository ttRepo;
    private final TeacherRepository teacherRepo;
    private final TimetableStructureRepository structureRepo;
    private final ClassMasterRepository classMasterRepo;
    private final DivisionMasterRepository divisionMasterRepo;
    private final SubjectMasterRepository subjectMasterRepo;

    public TeacherTimetableService(
            TeacherTimetableRepository ttRepo,
            TeacherRepository teacherRepo,
            TimetableStructureRepository structureRepo,
            ClassMasterRepository classMasterRepo,
            DivisionMasterRepository divisionMasterRepo,
            SubjectMasterRepository subjectMasterRepo) {
        this.ttRepo = ttRepo;
        this.teacherRepo = teacherRepo;
        this.structureRepo = structureRepo;
        this.classMasterRepo = classMasterRepo;
        this.divisionMasterRepo = divisionMasterRepo;
        this.subjectMasterRepo = subjectMasterRepo;
    }

    /** Full week grid for this teacher */
    public List<TeacherTimetable> getFullTimetable(int teacherId) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return java.util.Collections.emptyList();
        return ttRepo.findByTeacherIdAndAdminId(teacherId, adminId);
    }

    /**
     * Today's LECTURE slots for QR dropdown.
     * Returns list with label like "Period 1 — Java - MCA I-A — Room 201"
     */
    public List<TeacherTimetable> getTodayLectures(int teacherId) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return java.util.Collections.emptyList();

        String today = LocalDate.now()
                .getDayOfWeek()
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                .toUpperCase(); // e.g. MONDAY
        return ttRepo.findByTeacherIdAndDayAndAdminId(teacherId, today, adminId);
    }

    /** Save or update a single timetable cell */
    public TeacherTimetable saveSlot(int teacherId, int slotId, String day,
            Map<String, Object> body) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            throw new RuntimeException("Admin context required");

        TeacherTimetable tt = ttRepo
                .findByTeacherIdAndSlotIdAndDayOfWeekAndAdminId(teacherId, slotId, day.toUpperCase(), adminId)
                .orElse(new TeacherTimetable());

        Teacher teacher = teacherRepo.findByIdAndAdminId(teacherId, adminId)
                .orElseThrow(() -> new RuntimeException("Teacher not found or unauthorized"));
        TimetableStructure slot = structureRepo.findByIdAndAdminId(slotId, adminId)
                .orElseThrow(() -> new RuntimeException("Slot not found or unauthorized"));

        tt.setTeacher(teacher);
        tt.setSlot(slot);
        tt.setDayOfWeek(day.toUpperCase());

        if (tt.getAdmin() == null) {
            Admin admin = new Admin();
            admin.setId(adminId);
            tt.setAdmin(admin);
        }

        // Handle classMasterId or classId or nested classMaster object
        Integer classId = extractId(body, "classMasterId", "classId", "classMaster");
        if (classId != null) {
            tt.setClassMaster(classMasterRepo.findByIdAndAdminId(classId, adminId).orElse(null));
        }

        Integer divisionId = extractId(body, "divisionMasterId", "divisionId", "divisionMaster");
        if (divisionId != null) {
            tt.setDivisionMaster(divisionMasterRepo.findByIdAndAdminId(divisionId, adminId).orElse(null));
        }

        Integer subjectId = extractId(body, "subjectMasterId", "subjectId", "subjectMaster");
        if (subjectId != null) {
            tt.setSubjectMaster(subjectMasterRepo.findByIdAndAdminId(subjectId, adminId).orElse(null));
        }

        tt.setRoomNo((String) body.getOrDefault("roomNo", ""));
        return ttRepo.save(tt);
    }

    private Integer extractId(Map<String, Object> body, String idKey1, String idKey2, String objKey) {
        Object val = body.get(idKey1);
        if (val == null)
            val = body.get(idKey2);

        if (val instanceof Integer)
            return (Integer) val;
        if (val instanceof String && !((String) val).isEmpty())
            return Integer.parseInt((String) val);

        Object obj = body.get(objKey);
        if (obj instanceof Map) {
            Object idVal = ((Map<?, ?>) obj).get("id");
            if (idVal instanceof Integer)
                return (Integer) idVal;
            if (idVal instanceof String && !((String) idVal).isEmpty())
                return Integer.parseInt((String) idVal);
        }
        return null;
    }
}
