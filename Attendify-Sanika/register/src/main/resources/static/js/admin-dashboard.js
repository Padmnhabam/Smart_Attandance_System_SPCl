let classAttendanceChartInstance;
let trendChartInstance;
let subjectChartInstance;
let currentType = "student"; // default
const API_BASE = "";
const originalFetch = window.fetch.bind(window);

window.fetch = (input, init = {}) => {
    const url = typeof input === "string" ? input : (input && input.url) ? input.url : "";
    const isApiCall = url.startsWith(`${API_BASE}/api/`) || url.startsWith("/api/");

    if (!isApiCall) {
        return originalFetch(input, init);
    }

    const token = localStorage.getItem("authToken");
    const headers = new Headers(init.headers || {});
    if (token && !headers.has("Authorization")) {
        headers.set("Authorization", `Bearer ${token}`);
    }

    return originalFetch(input, { ...init, headers, cache: 'no-store' });
};

// =========================
// Show Section
// =========================
function showSection(section) {
    const sections = ['dashboard', 'students', 'teachers', 'classes', 'attendance', 'reports', 'settings', 'timetable', 'masterdata'];
    sections.forEach(s => {
        const el = document.getElementById(`${s}-section`);
        if (el) el.style.display = s === section ? 'block' : 'none';
    });

    if (section === 'reports') loadReports();
    if (section === 'timetable') loadTimetableSection();
    if (section === 'masterdata') switchMdTab('dept');
    if (section === 'students') initStudentsSection();
    if (section === 'teachers') initTeachersSection();

    document.querySelectorAll('.nav-link').forEach(link => link.classList.remove('active'));
    document.querySelector(`.nav-link[onclick="showSection('${section}')"]`)?.classList.add('active');
}

// =========================
// STUDENTS SECTION
// =========================
let _allStudents = [];

async function initStudentsSection() {
    // Load departments into filter
    const deptSel = document.getElementById('studentDeptFilter');
    if (deptSel.options.length <= 1) {
        try {
            const res = await fetch('/api/master/departments');
            const depts = await res.json();
            depts.forEach(d => deptSel.innerHTML += `<option value="${d.id}">${d.departmentName}</option>`);
        } catch (e) { console.error(e); }
    }
    // Load all students
    try {
        console.log("Fetching students from /api/admin/students...");
        const res = await fetch('/api/admin/students'); // returns flat map with classId, className, divisionId, divisionName
        if (!res.ok) throw new Error(`Fetch failed: ${res.status}`);
        _allStudents = await res.json();
        console.log(`Loaded ${_allStudents.length} students:`, _allStudents);
        applyStudentFilters();
        document.getElementById('totalStudents').innerText = _allStudents.length;
    } catch (e) {
        console.error('Student load error:', e);
        document.getElementById('studentTableBody').innerHTML = `<tr><td colspan="7" style="text-align:center;color:red;padding:32px">Error loading students: ${e.message}</td></tr>`;
    }
}

async function onStudentDeptChange() {
    const deptId = document.getElementById('studentDeptFilter').value;
    const classSel = document.getElementById('studentClassFilter');
    const divSel = document.getElementById('studentDivFilter');
    classSel.innerHTML = '<option value="">All Classes</option>';
    divSel.innerHTML = '<option value="">All Divisions</option>';
    if (deptId) {
        try {
            const res = await fetch('/api/master/classes');
            const classes = await res.json();
            const filtered = classes.filter(c => c.department && String(c.department.id) === String(deptId));
            filtered.forEach(c => classSel.innerHTML += `<option value="${c.id}">${c.className}</option>`);
        } catch (e) { console.error(e); }
    }
    applyStudentFilters();
}

async function onStudentClassChange() {
    const classId = document.getElementById('studentClassFilter').value;
    const divSel = document.getElementById('studentDivFilter');
    divSel.innerHTML = '<option value="">All Divisions</option>';
    if (classId) {
        try {
            const res = await fetch(`/api/master/classes/${classId}/divisions`);
            const divs = await res.json();
            divs.forEach(d => divSel.innerHTML += `<option value="${d.id}">${d.divisionName}</option>`);
        } catch (e) { console.error(e); }
    }
    applyStudentFilters();
}

function applyStudentFilters() {
    const classId = document.getElementById('studentClassFilter').value;
    const divId = document.getElementById('studentDivFilter').value;
    const q = (document.getElementById('studentSearchInput').value || '').toLowerCase().trim();

    let filtered = _allStudents.filter(s => {
        // s fields are flat: classId, className, divisionId, divisionName (from /api/admin/students)
        const matchClass = !classId || String(s.classId) === String(classId);
        const matchDiv = !divId || String(s.divisionId) === String(divId);
        const matchQ = !q || [s.name, s.rollNo, s.email, s.mobilenumber, s.className, s.divisionName].some(v => v && String(v).toLowerCase().includes(q));
        return matchClass && matchDiv && matchQ;
    });

    const tbody = document.getElementById('studentTableBody');
    document.getElementById('studentCount').textContent = `(${filtered.length} of ${_allStudents.length})`;

    if (!filtered.length) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center;color:#94a3b8;padding:32px">No students match the selected filters.</td></tr>`;
        return;
    }

    tbody.innerHTML = filtered.map(s => `
        <tr>
            <td><strong>${s.rollNo || '--'}</strong></td>
            <td>${s.name || '--'}</td>
            <td>${s.className || '--'}</td>
            <td>${s.divisionName || '--'}</td>
            <td>${s.email || '--'}</td>
            <td>${s.mobilenumber || '--'}</td>
            <td><span class="status-badge status-active">Active</span></td>
        </tr>`).join('');
}

// =========================
// TEACHERS SECTION
// =========================
let _allTeachers = [];

async function initTeachersSection() {
    const deptSel = document.getElementById('teacherDeptFilter');
    if (deptSel.options.length <= 1) {
        try {
            const res = await fetch('/api/master/departments');
            const depts = await res.json();
            depts.forEach(d => deptSel.innerHTML += `<option value="${d.id}">${d.departmentName}</option>`);
        } catch (e) { console.error(e); }
    }
    try {
        console.log("Fetching teachers from /api/admin/teachers...");
        const res = await fetch('/api/admin/teachers');
        if (!res.ok) throw new Error(`Fetch failed: ${res.status}`);
        _allTeachers = await res.json();
        console.log(`Loaded ${_allTeachers.length} teachers:`, _allTeachers);
        applyTeacherFilters();
        document.getElementById('totalTeachers').innerText = _allTeachers.length;
    } catch (e) {
        console.error('Teacher load error:', e);
        document.getElementById('teacherTableBody').innerHTML = `<tr><td colspan="6" style="text-align:center;color:red;padding:32px">Error loading teachers: ${e.message}</td></tr>`;
    }
}

function applyTeacherFilters() {
    const deptId = document.getElementById('teacherDeptFilter').value;
    const q = (document.getElementById('teacherSearchInput').value || '').toLowerCase().trim();

    let filtered = _allTeachers.filter(t => {
        // department can be either a string or an object {id, departmentName}
        const deptName = t.department && typeof t.department === 'object'
            ? t.department.departmentName
            : (t.department || '');
        const deptIdVal = t.department && typeof t.department === 'object'
            ? String(t.department.id)
            : '';

        const matchDept = !deptId || deptIdVal === String(deptId);
        const matchQ = !q || [t.name, t.email, t.mobilenumber, deptName].some(v => v && String(v).toLowerCase().includes(q));
        return matchDept && matchQ;
    });

    const tbody = document.getElementById('teacherTableBody');
    document.getElementById('teacherCount').textContent = `(${filtered.length} of ${_allTeachers.length})`;

    if (!filtered.length) {
        tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;color:#94a3b8;padding:32px">No teachers match the selected filters.</td></tr>`;
        return;
    }

    tbody.innerHTML = filtered.map(t => {
        const deptName = t.department && typeof t.department === 'object'
            ? t.department.departmentName
            : (t.department || '--');
        return `
        <tr>
            <td>${t.id}</td>
            <td><strong>${t.name}</strong></td>
            <td><span class="dept-badge">${deptName}</span></td>
            <td>${t.email}</td>
            <td>${t.mobilenumber || '--'}</td>
            <td><button class="tt-action-btn" onclick="viewTeacher('${t.id}')" title="View"><i class="fas fa-eye"></i></button></td>
        </tr>`;
    }).join('');
}


// =========================
// Chart 1: Class-wise Attendance Bar
// =========================
async function loadClassAttendanceChart() {
    try {
        const res = await fetch('/api/admin/reports/class-attendance');
        if (!res.ok) return;
        const data = await res.json();
        const ctx = document.getElementById('classAttendanceChart');
        if (!ctx) return;
        if (classAttendanceChartInstance) classAttendanceChartInstance.destroy();

        const labels = data.map(d => d.className || d.label || 'Unknown');
        const percents = data.map(d => d.totalCount > 0 ? Math.round((d.presentCount / d.totalCount) * 100) : 0);
        const colors = percents.map(p => p >= 75 ? '#10b981' : p >= 50 ? '#f59e0b' : '#ef4444');

        classAttendanceChartInstance = new Chart(ctx, {
            type: 'bar',
            data: {
                labels,
                datasets: [{ label: 'Attendance %', data: percents, backgroundColor: colors, borderRadius: 6 }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    y: { beginAtZero: true, max: 100, ticks: { callback: v => v + '%' }, grid: { color: '#f1f5f9' } },
                    x: { grid: { display: false } }
                }
            }
        });
    } catch (err) { console.error('Class chart error', err); }
}

// =========================
// Chart 2: 7-Day Daily Trend Line
// =========================
async function loadTrendChart() {
    try {
        const res = await fetch('/api/attendance/analytics/date');
        if (!res.ok) return;
        const raw = await res.json();
        const ctx = document.getElementById('trendChart');
        if (!ctx) return;
        if (trendChartInstance) trendChartInstance.destroy();

        // Take last 7 entries
        const data = raw.slice(-7);
        const labels = data.map(d => {
            const dt = new Date(d.date);
            return dt.toLocaleDateString('en-IN', { day: 'numeric', month: 'short' });
        });
        const percents = data.map(d => d.totalCount > 0 ? Math.round((d.presentCount / d.totalCount) * 100) : 0);

        trendChartInstance = new Chart(ctx, {
            type: 'line',
            data: {
                labels,
                datasets: [{
                    label: 'Present %',
                    data: percents,
                    fill: true,
                    borderColor: '#10b981',
                    backgroundColor: 'rgba(16,185,129,0.12)',
                    pointBackgroundColor: '#10b981',
                    tension: 0.4,
                    borderWidth: 2.5
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    y: { beginAtZero: true, max: 100, ticks: { callback: v => v + '%' }, grid: { color: '#f1f5f9' } },
                    x: { grid: { display: false } }
                }
            }
        });
    } catch (err) { console.error('Trend chart error', err); }
}

// =========================
// Chart 3: Subject Engagement Grouped Bar
// =========================
async function loadSubjectChart() {
    try {
        const res = await fetch('/api/attendance/analytics/subject');
        if (!res.ok) return;
        const data = await res.json();
        const ctx = document.getElementById('subjectChart');
        if (!ctx) return;
        if (subjectChartInstance) subjectChartInstance.destroy();

        const labels = data.slice(0, 8).map(d => d.label || d.subjectName || 'N/A');
        const totals = data.slice(0, 8).map(d => d.totalCount || 0);
        const presents = data.slice(0, 8).map(d => d.presentCount || 0);

        subjectChartInstance = new Chart(ctx, {
            type: 'bar',
            data: {
                labels,
                datasets: [
                    { label: 'Total', data: totals, backgroundColor: '#6366f133', borderColor: '#6366f1', borderWidth: 1.5, borderRadius: 4 },
                    { label: 'Present', data: presents, backgroundColor: '#10b981aa', borderColor: '#10b981', borderWidth: 1.5, borderRadius: 4 }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { position: 'top', labels: { boxWidth: 12, font: { size: 11 } } } },
                scales: {
                    y: { beginAtZero: true, grid: { color: '#f1f5f9' } },
                    x: { grid: { display: false }, ticks: { font: { size: 10 } } }
                }
            }
        });
    } catch (err) { console.error('Subject chart error', err); }
}

// =========================
// Chart 4: Low Attendance Alert List
// =========================
async function loadLowAttendanceAlerts() {
    const container = document.getElementById('lowAttendanceList');
    if (!container) return;
    try {
        const res = await fetch('/api/admin/reports/low-attendance?threshold=75');
        if (!res.ok) { container.innerHTML = '<p style="color:#94a3b8;text-align:center;padding:20px">No data available</p>'; return; }
        const students = await res.json();

        if (!students.length) {
            container.innerHTML = '<p style="color:#10b981;text-align:center;padding:20px">🎉 All students are above 75%</p>';
            return;
        }
        container.innerHTML = students.slice(0, 10).map(s => {
            const pct = s.totalCount > 0 ? Math.round((s.presentCount / s.totalCount) * 100) : 0;
            const color = pct < 50 ? '#ef4444' : '#f59e0b';
            return `<div style="display:flex;justify-content:space-between;align-items:center;padding:8px 12px;margin-bottom:6px;background:#fff;border-left:4px solid ${color};border-radius:6px;box-shadow:0 1px 4px rgba(0,0,0,0.06)">
                <div>
                    <div style="font-weight:600;font-size:13px;color:#1e293b">${s.studentName || s.name || 'Unknown'}</div>
                    <div style="font-size:11px;color:#64748b">${s.rollNo || ''} &bull; ${s.className || ''}</div>
                </div>
                <span style="font-weight:700;color:${color};font-size:16px">${pct}%</span>
            </div>`;
        }).join('');
        if (students.length > 10) {
            container.innerHTML += `<p style="text-align:center;color:#94a3b8;font-size:12px;margin-top:6px">+${students.length - 10} more students</p>`;
        }
    } catch (err) {
        console.error('Low attendance error', err);
        container.innerHTML = '<p style="color:#94a3b8;text-align:center;padding:20px">Could not load data</p>';
    }
}

// =========================
// Load Dashboard Stats (totals only)
// =========================
async function loadAttendanceOverview() {
    try {
        const res = await fetch('/api/admin/stats');
        if (!res.ok) return;
        const stats = await res.json();
        document.getElementById('totalStudents').innerText = stats.totalStudents || 0;
        document.getElementById('totalTeachers').innerText = stats.totalTeachers || 0;
        document.getElementById('totalClasses').innerText = stats.totalClasses || 0;
        document.getElementById('todaysAttendancePercent').innerText = (stats.todaysAttendancePercent || 0) + '%';
    } catch (err) { console.error(err); }
}

// =========================
// Update Dashboard Stats
// =========================
function updateDashboardStats(data, type = 'student') {
    if (type === 'student') {
        document.getElementById('totalStudents').innerText = data.length;
        document.getElementById('totalClasses').innerText = [...new Set(data.map(s => s.className))].length;
    } else if (type === 'teacher') {
        document.getElementById('totalTeachers').innerText = data.length;
        document.getElementById('totalClasses').innerText = [...new Set(data.map(t => t.department || 'Unknown'))].length;
    }
}

// =========================
// Open Modal (Teacher/Student)
function openModal(type) {
    document.getElementById('modalTitle').innerText = `Add New ${type === 'student' ? 'Student' : 'Teacher'}`;
    document.getElementById('departmentField').style.display = type === 'teacher' ? 'block' : 'none';
    document.getElementById('classField').style.display = type === 'student' ? 'block' : 'none';
    document.getElementById('addModal').style.display = 'block';
}

// =========================
// Close Modal
// =========================
function closeModal() {
    document.getElementById('addForm').reset();
    document.getElementById('addModal').style.display = 'none';
}

// =========================
// Placeholder View/Edit/Delete
function viewStudent(rollNo) { alert(`View student: ${rollNo}`); }
function editStudent(rollNo) { alert(`Edit student: ${rollNo}`); }
function deleteStudent(rollNo) { alert(`Delete student: ${rollNo}`); }
function viewTeacher(id) { alert(`View teacher: ${id}`); }

// =========================
// Load Reports
// =========================
async function loadReports() {
    const deptSel = document.getElementById('reportDeptFilter');
    const classSel = document.getElementById('reportClassFilter');
    const divSel = document.getElementById('reportDivFilter');
    const monthSel = document.getElementById('reportMonth');
    const yearInput = document.getElementById('reportYear');

    // Populate Year
    const now = new Date();
    yearInput.value = now.getFullYear();
    monthSel.value = now.getMonth() + 1;

    // Populate Departments
    if (deptSel.options.length <= 1) {
        try {
            const res = await fetch('/api/master/departments');
            const depts = await res.json();
            depts.forEach(d => deptSel.innerHTML += `<option value="${d.id}">${d.departmentName}</option>`);
        } catch (e) { console.error(e); }
    }
}

async function onReportDeptChange() {
    const deptId = document.getElementById('reportDeptFilter').value;
    const classSel = document.getElementById('reportClassFilter');
    const divSel = document.getElementById('reportDivFilter');
    classSel.innerHTML = '<option value="">Select Class</option>';
    divSel.innerHTML = '<option value="">Select Division</option>';
    if (deptId) {
        try {
            const res = await fetch('/api/master/classes');
            const classes = await res.json();
            const filtered = classes.filter(c => c.department && String(c.department.id) === String(deptId));
            filtered.forEach(c => classSel.innerHTML += `<option value="${c.id}">${c.className}</option>`);
        } catch (e) { console.error(e); }
    }
}

async function onReportClassChange() {
    const classId = document.getElementById('reportClassFilter').value;
    const divSel = document.getElementById('reportDivFilter');
    divSel.innerHTML = '<option value="">Select Division</option>';
    if (classId) {
        try {
            const res = await fetch(`/api/master/classes/${classId}/divisions`);
            const divs = await res.json();
            divs.forEach(d => divSel.innerHTML += `<option value="${d.id}">${d.divisionName}</option>`);
        } catch (e) { console.error(e); }
    }
}

let _reportData = null; // Cache for export

async function generateReport() {
    const classId = document.getElementById('reportClassFilter').value;
    const divId = document.getElementById('reportDivFilter').value;
    const month = document.getElementById('reportMonth').value;
    const year = document.getElementById('reportYear').value;

    if (!classId || !divId) {
        alert("Please select Class and Division");
        return;
    }

    const wrapper = document.getElementById('reportTableWrapper');
    wrapper.innerHTML = '<p style="text-align:center;padding:40px">Generating report...</p>';
    document.getElementById('exportBtn').style.display = 'none';

    try {
        const res = await fetch(`/api/attendance/monthly-report?classId=${classId}&divisionId=${divId}&month=${month}&year=${year}`);
        if (!res.ok) throw new Error("Failed to fetch report data");
        const data = await res.json();
        _reportData = data;

        if (!data.students || data.students.length === 0) {
            wrapper.innerHTML = '<p style="color:#94a3b8;text-align:center;padding:40px">No attendance records found for this period.</p>';
            return;
        }

        renderReportTable(data);
        document.getElementById('exportBtn').style.display = 'inline-block';
    } catch (err) {
        console.error(err);
        wrapper.innerHTML = `<p style="color:#ef4444;text-align:center;padding:40px">Error: ${err.message}</p>`;
    }
}

function renderReportTable(data) {
    const { subjects, students } = data;
    const wrapper = document.getElementById('reportTableWrapper');

    let html = `<table>
        <thead>
            <tr>
                <th rowspan="2">Roll No</th>
                <th rowspan="2">Student Name</th>
                ${subjects.map(sub => `<th colspan="3" style="text-align:center">${sub}</th>`).join('')}
                <th colspan="3" style="text-align:center; background:#f8fafc">Overall</th>
            </tr>
            <tr>
                ${subjects.map(() => `<th>P</th><th>Total</th><th style="font-size:10px">%</th>`).join('')}
                <th style="background:#f8fafc">P</th><th style="background:#f8fafc">Total</th><th style="background:#f8fafc; font-size:10px">%</th>
            </tr>
        </thead>
        <tbody>
            ${students.map(s => `
                <tr>
                    <td><strong>${s.rollNo}</strong></td>
                    <td style="white-space:nowrap">${s.name}</td>
                    ${subjects.map(sub => {
        const entry = s.subjects[sub] || { present: 0, total: 0, pct: 0 };
        const color = entry.pct >= 75 ? '#10b981' : entry.pct >= 50 ? '#f59e0b' : '#ef4444';
        return `<td>${entry.present}</td><td>${entry.total}</td><td style="color:${color};font-weight:700">${entry.pct}%</td>`;
    }).join('')}
                    <td style="background:#f8fafc">${s.overall.present}</td>
                    <td style="background:#f8fafc">${s.overall.total}</td>
                    <td style="background:#f8fafc; color:${s.overall.pct >= 75 ? '#10b981' : '#ef4444'}; font-weight:800">${s.overall.pct}%</td>
                </tr>
            `).join('')}
        </tbody>
    </table>`;

    wrapper.innerHTML = html;
}

function exportToCSV() {
    if (!_reportData) return;
    const { subjects, students } = _reportData;

    // Header row 1
    let csv = "Roll No,Name,";
    subjects.forEach(sub => {
        csv += `"${sub} (Present)","${sub} (Total)","${sub} (%)",`;
    });
    csv += "Overall (Present),Overall (Total),Overall (%)\n";

    // Data rows
    students.forEach(s => {
        csv += `"${s.rollNo}","${s.name}",`;
        subjects.forEach(sub => {
            const entry = s.subjects[sub] || { present: 0, total: 0, pct: 0 };
            csv += `${entry.present},${entry.total},${entry.pct},`;
        });
        csv += `${s.overall.present},${s.overall.total},${s.overall.pct}\n`;
    });

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement("a");
    const url = URL.createObjectURL(blob);
    link.setAttribute("href", url);
    link.setAttribute("download", `Attendance_Report_${document.getElementById('reportMonth').value}_${document.getElementById('reportYear').value}.csv`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}

// =========================
// Logout
function logout() {
    localStorage.clear();
    // use local path to avoid duplicating /public when already under /public
    window.location.href = 'login.html';
}

// ==========================================================
// ===== TIMETABLE SECTION JS ================================
// ==========================================================

let _ttSlots = [];  // cache of period slots

// Called when Timetable nav is clicked
async function loadTimetableSection() {
    await loadSlots();
    await loadTeachersDropdown();
}

// ---- Sub-tab toggle ----
function switchTimetableTab(tab) {
    document.getElementById('tt-panel-slots').style.display = tab === 'slots' ? 'block' : 'none';
    document.getElementById('tt-panel-grid').style.display = tab === 'grid' ? 'block' : 'none';
    document.getElementById('tt-tab-slots').classList.toggle('active', tab === 'slots');
    document.getElementById('tt-tab-grid').classList.toggle('active', tab === 'grid');
}

// ---- Period Slots ----
async function loadSlots() {
    try {
        const res = await fetch('/api/admin/timetable/structure');
        if (!res.ok) throw new Error('Failed to fetch slots');
        _ttSlots = await res.json();
        renderSlotsTable(_ttSlots);
    } catch (err) {
        console.error(err);
        document.getElementById('slotsTableBody').innerHTML =
            `<tr><td colspan="6" style="text-align:center;color:#ef4444">Failed to load slots.</td></tr>`;
    }
}

function renderSlotsTable(slots) {
    const tbody = document.getElementById('slotsTableBody');
    if (!slots.length) {
        tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;color:#64748b">No period slots defined yet. Click "Add Slot" to get started.</td></tr>`;
        return;
    }
    tbody.innerHTML = slots.map(s => `
        <tr>
            <td>${s.slotOrder}</td>
            <td><strong>${s.label}</strong></td>
            <td><span class="slot-type-badge ${s.slotType === 'LECTURE' ? 'badge-lecture' : 'badge-break'}">${s.slotType}</span></td>
            <td>${s.startTime ? s.startTime.substring(0, 5) : '--'}</td>
            <td>${s.endTime ? s.endTime.substring(0, 5) : '--'}</td>
            <td>
                <button class="tt-action-btn" onclick="openSlotModal(${JSON.stringify(s).replace(/"/g, '&quot;')})" title="Edit"><i class="fas fa-pen"></i></button>
                <button class="tt-action-btn danger" onclick="deleteSlot(${s.id})" title="Delete"><i class="fas fa-trash"></i></button>
            </td>
        </tr>`).join('');
}

function openSlotModal(slot) {
    document.getElementById('slotModalTitle').textContent = slot ? 'Edit Period Slot' : 'Add Period Slot';
    document.getElementById('slotId').value = slot ? slot.id : '';
    document.getElementById('slotOrder').value = slot ? slot.slotOrder : '';
    document.getElementById('slotLabel').value = slot ? slot.label : '';
    document.getElementById('slotType').value = slot ? slot.slotType : 'LECTURE';
    document.getElementById('slotStart').value = slot && slot.startTime ? slot.startTime.substring(0, 5) : '';
    document.getElementById('slotEnd').value = slot && slot.endTime ? slot.endTime.substring(0, 5) : '';
    document.getElementById('slotModal').classList.add('active');
}

function closeSlotModal() {
    document.getElementById('slotModal').classList.remove('active');
    document.getElementById('slotForm').reset();
}

async function saveSlot(e) {
    e.preventDefault();
    const id = document.getElementById('slotId').value;
    const body = {
        slotOrder: parseInt(document.getElementById('slotOrder').value),
        label: document.getElementById('slotLabel').value,
        slotType: document.getElementById('slotType').value,
        startTime: document.getElementById('slotStart').value,
        endTime: document.getElementById('slotEnd').value
    };
    const method = id ? 'PUT' : 'POST';
    const url = id ? `/api/admin/timetable/structure/${id}` : '/api/admin/timetable/structure';
    try {
        const res = await fetch(url, { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
        if (!res.ok) throw new Error('Save failed');
        closeSlotModal();
        await loadSlots();
    } catch (err) {
        console.error(err);
        alert('Failed to save slot: ' + err.message);
    }
}

async function deleteSlot(id) {
    if (!confirm('Delete this slot? This will remove it from all teacher timetables.')) return;
    try {
        const res = await fetch(`/api/admin/timetable/structure/${id}`, { method: 'DELETE' });
        if (!res.ok) throw new Error('Delete failed');
        await loadSlots();
    } catch (err) {
        console.error(err);
        alert('Failed to delete slot.');
    }
}

// ---- Teacher Timetable Grid ----
async function loadTeachersDropdown() {
    try {
        const res = await fetch('/api/admin/teachers');
        if (!res.ok) throw new Error('Failed to fetch teachers');
        const teachers = await res.json();
        const sel = document.getElementById('ttTeacherSelect');
        sel.innerHTML = '<option value="">-- Choose Teacher --</option>' +
            teachers.map(t => `<option value="${t.id}">${t.name} (${t.department || 'Dept N/A'})</option>`).join('');
    } catch (err) {
        console.error(err);
    }
}

async function loadTeacherTimetableGrid(teacherId) {
    const wrapper = document.getElementById('ttGridWrapper');
    if (!teacherId) {
        wrapper.innerHTML = '<p style="color:#64748b;text-align:center;padding:40px">Select a teacher to view their timetable grid.</p>';
        return;
    }
    wrapper.innerHTML = '<p style="text-align:center;padding:40px">Loading...</p>';
    try {
        // Load structure + teacher timetable data in parallel
        const [slotsRes, ttRes] = await Promise.all([
            fetch('/api/admin/timetable/structure'),
            fetch(`/api/teacher/timetable/${teacherId}`)
        ]);
        const slots = await slotsRes.json();
        const ttData = await ttRes.json();

        // Build lookup: `${slotId}_${day}` -> entry
        const lookup = {};
        ttData.forEach(row => {
            lookup[`${row.slot.id}_${row.dayOfWeek}`] = row;
        });

        const days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
        const dayLabels = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

        let html = `<div class="tt-grid-wrap"><table class="tt-grid">`;
        // Header row
        html += `<thead><tr><th class="tt-slot-col">Time / Slot</th>${dayLabels.map(d => `<th>${d}</th>`).join('')}</tr></thead><tbody>`;

        slots.forEach(slot => {
            const isBreak = slot.slotType === 'BREAK';
            html += `<tr class="${isBreak ? 'tt-row-break' : 'tt-row-lecture'}">`;
            // Slot label cell
            html += `<td class="tt-slot-label">
                <strong>${slot.label}</strong><br>
                <small>${slot.startTime ? slot.startTime.substring(0, 5) : ''} – ${slot.endTime ? slot.endTime.substring(0, 5) : ''}</small>
            </td>`;
            // Day cells
            days.forEach(day => {
                if (isBreak) {
                    html += `<td class="tt-break-cell">${slot.label}</td>`;
                } else {
                    const entry = lookup[`${slot.id}_${day}`];
                    const hasData = entry && (entry.subject || entry.className);
                    html += `<td class="tt-lecture-cell ${hasData ? 'filled' : 'empty'}" 
                        onclick="openCellModal(${teacherId}, ${slot.id}, '${day}', ${hasData ? `'${entry.className || ''}','${entry.division || ''}','${entry.subject || ''}','${entry.roomNo || ''}'` : `'','','',''`})"
                        title="Click to edit">
                        ${hasData ? `<div class="tt-cell-subject">${entry.subject}</div>
                            <div class="tt-cell-meta">${entry.className}${entry.division ? '-' + entry.division : ''}</div>
                            <div class="tt-cell-room"><i class="fas fa-door-open"></i> ${entry.roomNo || ''}</div>`
                            : '<div class="tt-cell-empty"><i class="fas fa-plus"></i></div>'}
                    </td>`;
                }
            });
            html += '</tr>';
        });
        html += '</tbody></table></div>';
        wrapper.innerHTML = html;
    } catch (err) {
        console.error(err);
        wrapper.innerHTML = '<p style="color:#ef4444;text-align:center;padding:40px">Failed to load timetable grid.</p>';
    }
}

function openCellModal(teacherId, slotId, day, className, division, subject, roomNo) {
    document.getElementById('cellTeacherId').value = teacherId;
    document.getElementById('cellSlotId').value = slotId;
    document.getElementById('cellDay').value = day;
    document.getElementById('cellClassName').value = className || '';
    document.getElementById('cellDivision').value = division || '';
    document.getElementById('cellSubject').value = subject || '';
    document.getElementById('cellRoomNo').value = roomNo || '';
    document.getElementById('cellModalTitle').textContent = `Edit — ${day.charAt(0) + day.slice(1).toLowerCase()}`;
    document.getElementById('cellModal').classList.add('active');
}

function closeCellModal() {
    document.getElementById('cellModal').classList.remove('active');
    document.getElementById('cellForm').reset();
}

async function saveCellData(e) {
    e.preventDefault();
    const teacherId = document.getElementById('cellTeacherId').value;
    const slotId = document.getElementById('cellSlotId').value;
    const day = document.getElementById('cellDay').value;
    const body = {
        className: document.getElementById('cellClassName').value,
        division: document.getElementById('cellDivision').value,
        subject: document.getElementById('cellSubject').value,
        roomNo: document.getElementById('cellRoomNo').value
    };
    try {
        const res = await fetch(`/api/teacher/timetable/${teacherId}/slot/${slotId}/${day}`,
            { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
        if (!res.ok) throw new Error('Save failed');
        closeCellModal();
        await loadTeacherTimetableGrid(teacherId);
    } catch (err) {
        console.error(err);
        alert('Failed to save: ' + err.message);
    }
}

// Close modals on backdrop click
window.addEventListener('click', (e) => {
    if (e.target.id === 'slotModal') closeSlotModal();
    if (e.target.id === 'cellModal') closeCellModal();
});



// ==========================================================
// ===== MASTER DATA SECTION JS =============================
// ==========================================================

function switchMdTab(tab) {
    const panels = ['dept', 'class', 'div', 'sub', 'map'];
    panels.forEach(p => {
        const panelEl = document.getElementById(`md-panel-${p}`);
        if (panelEl) panelEl.style.display = p === tab ? 'block' : 'none';
        const tabEl = document.getElementById(`md-tab-${p}`);
        if (tabEl) tabEl.classList.toggle('active', p === tab);
    });

    if (tab === 'dept') loadMdDepartments();
    if (tab === 'class') loadMdClasses();
    if (tab === 'div') loadMdDivisions();
    if (tab === 'sub') loadMdSubjects();
    if (tab === 'map') loadMdMapping();
}

async function loadMdDepartments() {
    try {
        const res = await fetch('/api/master/departments');
        const data = await res.json();
        const tbody = document.getElementById('mdDeptBody');
        tbody.innerHTML = data.map(d => `<tr><td>${d.id}</td><td>${d.departmentName}</td><td><button class="tt-action-btn danger" onclick="deleteMasterData('departments', ${d.id})"><i class="fas fa-trash"></i></button></td></tr>`).join('');
    } catch (e) { console.error(e); }
}

async function loadMdClasses() {
    try {
        const res = await fetch('/api/master/classes');
        const data = await res.json();
        document.getElementById('mdClassBody').innerHTML = data.map(c => `<tr><td>${c.id}</td><td>${c.className}</td><td>${c.department ? c.department.departmentName : '-'}</td><td><button class="tt-action-btn danger" onclick="deleteMasterData('classes', ${c.id})"><i class="fas fa-trash"></i></button></td></tr>`).join('');
    } catch (e) { console.error(e); }
}

async function loadMdDivisions() {
    try {
        const res = await fetch('/api/master/divisions');
        const data = await res.json();
        document.getElementById('mdDivBody').innerHTML = data.map(d => `<tr><td>${d.id}</td><td>${d.divisionName}</td><td>${d.classMaster ? d.classMaster.id : '-'}</td><td><button class="tt-action-btn danger" onclick="deleteMasterData('divisions', ${d.id})"><i class="fas fa-trash"></i></button></td></tr>`).join('');
    } catch (e) { console.error(e); }
}

async function loadMdSubjects() {
    try {
        const res = await fetch('/api/master/subjects');
        const data = await res.json();
        document.getElementById('mdSubBody').innerHTML = data.map(s => `<tr><td>${s.id}</td><td>${s.subjectName}</td><td>${s.department ? s.department.departmentName : '-'}</td><td><button class="tt-action-btn danger" onclick="deleteMasterData('subjects', ${s.id})"><i class="fas fa-trash"></i></button></td></tr>`).join('');
    } catch (e) { console.error(e); }
}

async function loadMdMapping() {
    try {
        const res = await fetch('/api/master/class-subjects');
        const data = await res.json();
        document.getElementById('mdMapBody').innerHTML = data.map(m => `<tr><td>${m.id}</td><td>${m.classMaster ? m.classMaster.className : '-'}</td><td>${m.subjectMaster ? m.subjectMaster.subjectName : '-'}</td><td><button class="tt-action-btn danger" onclick="deleteMasterData('class-subjects', ${m.id})"><i class="fas fa-trash"></i></button></td></tr>`).join('');
    } catch (e) { console.error(e); }
}

async function deleteMasterData(endpoint, id) {
    if (!confirm("Are you sure?")) return;
    try {
        const res = await fetch(`/api/master/${endpoint}/${id}`, { method: 'DELETE' });
        if (!res.ok) {
            const msg = await res.text();
            alert("Error: " + msg);
            return;
        }
        const activeTabEl = document.querySelector('#masterdata-section .tt-tab.active');
        const activeTab = activeTabEl ? activeTabEl.id.replace('md-tab-', '') : 'dept';
        switchMdTab(activeTab);
    } catch (e) {
        console.error(e);
        alert("Failed to delete.");
    }
}

async function openMdModal(type) {
    document.getElementById('mdType').value = type;
    document.getElementById('mdForm').reset();

    document.getElementById('mdNameGroup').style.display = 'none';
    document.getElementById('mdDeptMapGroup').style.display = 'none';
    document.getElementById('mdClassMapGroup').style.display = 'none';
    document.getElementById('mdSubMapGroup').style.display = 'none';

    document.getElementById('mdClassSelect').removeAttribute('required');
    document.getElementById('mdSubSelect').removeAttribute('required');

    let title = "Add";

    if (type === 'dept') {
        title = "Add Department";
        document.getElementById('mdNameLabel').innerText = "Department Name";
        document.getElementById('mdNameGroup').style.display = 'block';
    } else if (type === 'class') {
        title = "Add Class";
        document.getElementById('mdNameLabel').innerText = "Class Name";
        document.getElementById('mdNameGroup').style.display = 'block';
        document.getElementById('mdDeptMapGroup').style.display = 'block';
        await populateMdDropdown('/api/master/departments', 'mdDeptSelect', 'id', 'departmentName');
    } else if (type === 'div') {
        title = "Add Division";
        document.getElementById('mdNameLabel').innerText = "Division Name";
        document.getElementById('mdNameGroup').style.display = 'block';
        document.getElementById('mdClassMapGroup').style.display = 'block';
        document.getElementById('mdClassSelect').setAttribute('required', 'true');
        await populateMdDropdown('/api/master/classes', 'mdClassSelect', 'id', 'className');
    } else if (type === 'sub') {
        title = "Add Subject";
        document.getElementById('mdNameLabel').innerText = "Subject Name";
        document.getElementById('mdNameGroup').style.display = 'block';
        document.getElementById('mdDeptMapGroup').style.display = 'block';
        await populateMdDropdown('/api/master/departments', 'mdDeptSelect', 'id', 'departmentName');
    } else if (type === 'map') {
        title = "Map Class to Subject";
        document.getElementById('mdClassMapGroup').style.display = 'block';
        document.getElementById('mdSubMapGroup').style.display = 'block';
        document.getElementById('mdClassSelect').setAttribute('required', 'true');
        document.getElementById('mdSubSelect').setAttribute('required', 'true');
        await populateMdDropdown('/api/master/classes', 'mdClassSelect', 'id', 'className');
        await populateMdDropdown('/api/master/subjects', 'mdSubSelect', 'id', 'subjectName');
    }

    document.getElementById('mdModalTitle').innerText = title;
    document.getElementById('mdModal').style.display = 'block';
}

async function populateMdDropdown(url, selectId, valField, textField) {
    try {
        const res = await fetch(url);
        const data = await res.json();
        const sel = document.getElementById(selectId);
        sel.innerHTML = '<option value="">-- Select --</option>' + data.map(d => `<option value="${d[valField]}">${d[textField]}</option>`).join('');
    } catch (e) { console.error(e); }
}

async function saveMasterData(e) {
    e.preventDefault();
    const type = document.getElementById('mdType').value;
    let endpoint = "";
    let payload = {};

    if (type === 'dept') {
        endpoint = "departments";
        payload = { departmentName: document.getElementById('mdName').value };
    } else if (type === 'class') {
        endpoint = "classes";
        let deptVal = document.getElementById('mdDeptSelect').value;
        payload = {
            className: document.getElementById('mdName').value,
            department: deptVal ? { id: parseInt(deptVal) } : null
        };
    } else if (type === 'div') {
        endpoint = "divisions";
        payload = {
            divisionName: document.getElementById('mdName').value,
            classMaster: { id: parseInt(document.getElementById('mdClassSelect').value) }
        };
    } else if (type === 'sub') {
        endpoint = "subjects";
        let deptVal = document.getElementById('mdDeptSelect').value;
        payload = {
            subjectName: document.getElementById('mdName').value,
            department: deptVal ? { id: parseInt(deptVal) } : null
        };
    } else if (type === 'map') {
        endpoint = "class-subjects";
        payload = {
            classMaster: { id: parseInt(document.getElementById('mdClassSelect').value) },
            subjectMaster: { id: parseInt(document.getElementById('mdSubSelect').value) }
        };
    }

    try {
        const res = await fetch(`/api/master/${endpoint}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (!res.ok) {
            const msg = await res.text();
            alert("Error: " + msg);
            return;
        }
        document.getElementById('mdModal').style.display = 'none';
        switchMdTab(type);
    } catch (err) {
        console.error(err);
        alert('Failed to save data.');
    }
}


// =========================
// Initial Load
document.addEventListener("DOMContentLoaded", function () {
    const role = localStorage.getItem("role");
    const token = localStorage.getItem("authToken");
    if (role !== "admin" || !token) {
        // redirect within same folder, not up one level
        window.location.href = "login.html";
        return;
    }

    showSection('dashboard');
    loadAttendanceOverview();
    loadClassAttendanceChart();
    loadTrendChart();
    loadSubjectChart();
    loadLowAttendanceAlerts();
    // switchDataTab('teacher'); // Undefined function removed
});
