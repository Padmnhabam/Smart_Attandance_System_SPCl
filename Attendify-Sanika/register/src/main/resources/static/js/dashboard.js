const API_BASE = "";
const originalFetch = window.fetch.bind(window);

window.fetch = (input, init = {}) => {
    const url = typeof input === "string" ? input : (input && input.url) ? input.url : "";
    const isApiCall = url.startsWith(`${API_BASE}/api/`) || url.startsWith("/api/");

    if (!isApiCall) {
        return originalFetch(input, init);
    }

    const token = (localStorage.getItem('studentAuthToken') || localStorage.getItem('authToken'));
    const headers = new Headers(init.headers || {});
    if (token && !headers.has("Authorization")) {
        headers.set("Authorization", `Bearer ${token}`);
    }

    return originalFetch(input, { ...init, headers });
};

// ----------------- GET LOGGED-IN USER -----------------
const user = JSON.parse((localStorage.getItem('studentLoggedUser') || localStorage.getItem('loggedUser')));
const role = (localStorage.getItem('studentRole') || localStorage.getItem('role'));
const token = (localStorage.getItem('studentAuthToken') || localStorage.getItem('authToken'));

if (!user || role !== "student" || !token) {
    // user not authenticated; redirect to login in same folder
    window.location.href = "login.html";

}

// ----------------- UPDATE WELCOME & PROFILE INFO -----------------
document.getElementById("welcomeText").innerText = "Welcome, " + user.name + " 👋";
document.getElementById("profileName").innerText = user.name;
document.getElementById("profileEmail").innerText = user.email;
document.getElementById("profileRoll").innerText = user.rollNo;
const displayClassName = user.classMaster ? user.classMaster.className : (user.className || "-");
const displayDivName = user.divisionMaster ? user.divisionMaster.divisionName : (user.divisionName || "");
document.getElementById("profileClass").innerText = displayClassName + (displayDivName ? " " + displayDivName : "");

// ----------------- FETCH DASHBOARD DATA -----------------
fetch(`/api/student/dashboard/${user.id}`)
    .then(res => {
        if (!res.ok) throw new Error("Failed to fetch dashboard data");
        return res.json();
    })
    .then(data => {
        // Update main cards
        document.querySelector(".card.blue p").innerText = data.totalClasses;
        document.querySelector(".card.green p").innerText = data.present;
        document.querySelector(".card.red p").innerText = data.absent;
        document.querySelector(".card.purple p").innerText = data.percentage + "%";
        document.getElementById("attendancePercent").innerText = data.percentage + "%";



        // Update Attendance Stats Cards
        const statCards = document.querySelectorAll(".attendance-stats .stat-card");

        // Present
        statCards[0].querySelector(".stat-value").innerText = data.present;
        statCards[0].querySelector(".stat-progress-bar.present").style.width = data.percentage + "%";

        // Absent
        statCards[1].querySelector(".stat-value").innerText = data.absent;
        statCards[1].querySelector(".stat-progress-bar.absent").style.width = (100 - data.percentage) + "%";

        // Load chart
        loadChart(data.present, data.absent);

        // Fetch settings to check threshold color
        fetch("/api/settings")
            .then(res => res.json())
            .then(setting => {
                const threshold = setting.attendanceThreshold;
                if (data.percentage < threshold) {
                    document.querySelector(".card.purple p").style.color = "red";
                    document.getElementById("attendancePercent").style.color = "red";
                } else {
                    document.querySelector(".card.purple p").style.color = "green";
                    document.getElementById("attendancePercent").style.color = "green";
                }
            })
            .catch(err => console.error("Could not fetch settings:", err));

    })
    .catch(err => {
        console.error(err);
        alert("Could not load dashboard data. Please try again later.");
    });

// ----------------- FETCH ATTENDANCE TABLE -----------------
fetch(`/api/student/attendance/${user.id}`)
    .then(res => {
        if (!res.ok) throw new Error("Failed to fetch attendance records");
        return res.json();
    })
    .then(records => {
        const tbody = document.querySelector("#attendance table tbody");
        tbody.innerHTML = ""; // clear previous rows

        records.forEach(r => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td>${r.date}</td>
                <td>${r.subject}</td>
                <td class="${r.status.toLowerCase()}">${r.status}</td>
            `;
            tbody.appendChild(tr);
        });

        setupFilters(); // initialize filter buttons after loading
    })
    .catch(err => {
        console.error(err);
        alert("Could not load attendance records. Please try again later.");
    });

// ----------------- FETCH SUBJECT-WISE SUMMARY -----------------
fetch(`/api/student/summary/subject/${user.id}`)
    .then(res => {
        if (!res.ok) throw new Error("Failed to fetch subject summary");
        return res.json();
    })
    .then(summaries => {
        const tbody = document.querySelector("#subjectSummaryTable tbody");
        if (!tbody) return;
        tbody.innerHTML = "";

        summaries.forEach(s => {
            const tr = document.createElement("tr");
            const perc = s.percentage.toFixed(1);
            tr.innerHTML = `
                <td>${s.subject}</td>
                <td>${s.presentCount}</td>
                <td>${s.totalClasses}</td>
                <td class="${s.percentage < 75 ? 'absent' : 'present'}">${perc}%</td>
            `;
            tbody.appendChild(tr);
        });
    })
    .catch(err => {
        console.error("Subject summary error:", err);
    });

// ----------------- LOAD CHART -----------------
function loadChart(present, absent) {
    const ctx = document.getElementById("attendanceChart");

    new Chart(ctx, {
        type: "doughnut",
        data: {
            labels: ["Present", "Absent"],
            datasets: [{
                data: [present, absent],
                backgroundColor: ["#1cc88a", "#e74a3b"],
                borderWidth: 0
            }]
        },
        options: {
            plugins: { legend: { position: "bottom" } },
            cutout: "70%"
        }
    });
}

// ----------------- SECTION SWITCH -----------------
function showSection(id) {
    document.querySelectorAll(".section").forEach(sec =>
        sec.classList.add("hidden")
    );
    document.getElementById(id).classList.remove("hidden");
}

// ----------------- LOGOUT -----------------
function logout() {
    localStorage.removeItem('studentAuthToken'); localStorage.removeItem('studentLoggedUser'); localStorage.removeItem('studentRole'); localStorage.removeItem('authToken'); localStorage.removeItem('loggedUser'); localStorage.removeItem('role');
    window.location.href = "login.html";
}

// ----------------- ATTENDANCE FILTERS -----------------
function setupFilters() {
    const buttons = document.querySelectorAll(".filter-btn");
    buttons.forEach(btn => {
        btn.addEventListener("click", () => {
            const filter = btn.innerText.toLowerCase(); // "all", "present", "absent", "this month"
            buttons.forEach(b => b.classList.remove("active"));
            btn.classList.add("active");

            document.querySelectorAll("#attendance table tbody tr").forEach(tr => {
                const status = tr.children[2].innerText.toLowerCase();
                const date = new Date(tr.children[0].innerText);
                const now = new Date();

                if (filter === "all") {
                    tr.style.display = "";
                } else if (filter === "this month") {
                    tr.style.display = (date.getMonth() === now.getMonth() &&
                        date.getFullYear() === now.getFullYear()) ? "" : "none";
                } else {
                    tr.style.display = (status === filter) ? "" : "none";
                }
            });
        });
    });
}



async function checkAttendanceStatus() {
    try {
        const user = JSON.parse((localStorage.getItem('studentLoggedUser') || localStorage.getItem('loggedUser')));
        const studentId = user.id;

        const response = await fetch(`/api/attendance/check/${studentId}`);
        const message = await response.text();

        showAttendanceNotification(message);

    } catch (error) {
        console.error("Error checking attendance:", error);
    }
}
function showAttendanceNotification(message) {

    const notificationDiv = document.getElementById("attendanceAlert");

    notificationDiv.innerText = message;

    if (message.includes("Warning")) {
        notificationDiv.style.backgroundColor = "#ffcccc";
        notificationDiv.style.color = "red";
    } else {
        notificationDiv.style.backgroundColor = "#ccffcc";
        notificationDiv.style.color = "green";
    }
}
document.addEventListener("DOMContentLoaded", function () {
    checkAttendanceStatus();
});

const notesList = document.getElementById("notesList");

fetch(`/api/notes/student/${user.id}`)
    .then(res => res.json())
    .then(data => {

        notesList.innerHTML = "";

        if (data.length === 0) {
            notesList.innerHTML = `<div style="text-align:center; padding:20px; color:#666;">No notes available for your class.</div>`;
            return;
        }

        data.forEach(note => {

            const noteCard = `
                <div class="note-card">
                    <div>
                        <div class="note-title">${note.fileName}</div>
                        <div class="note-subject">Subject: ${note.subject}</div>
                    </div>
                    <a href="${note.fileUrl}" 
                       target="_blank" 
                       class="note-btn">
                       📥 Download
                    </a>
                </div>
            `;

            notesList.innerHTML += noteCard;
        });
    });

// ================== LEAVE SYSTEM ==================

function submitLeave() {

    if (!user || !user.id) {
        alert("Session expired. Please login again.");
        window.location.href = "login.html";
        return;
    }

    const teacherId = document.getElementById("leaveTeacher").value;

    const leaveData = {
        studentId: user.id,
        studentName: user.name,
        className: (user.classMaster ? user.classMaster.className : "") + (user.divisionMaster ? " " + user.divisionMaster.divisionName : ""),
        fromDate: document.getElementById("fromDate").value,
        toDate: document.getElementById("toDate").value,
        reason: document.getElementById("reason").value,
        teacher: teacherId ? { id: parseInt(teacherId) } : null
    };

    if (!leaveData.fromDate) { alert("Please select 'From Date'"); return; }
    if (!leaveData.toDate) { alert("Please select 'To Date'"); return; }
    if (!leaveData.reason) { alert("Please enter a 'Reason'"); return; }
    if (!teacherId) { alert("Please select a 'Teacher'"); return; }

    fetch("/api/leave/submit", {   // ✅ corrected (no 'leaves')
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(leaveData)
    })
        .then(res => res.text())
        .then(msg => {
            alert(msg);
            //   loadStudentLeaves();
        })
        .catch(err => console.error("Submit error:", err));
}


function loadStudentLeaves() {

    if (!user || !user.id) {
        console.error("User not found");
        return;
    }

    fetch(`/api/leave/student/${user.id}`)  // ✅ use user.id
        .then(res => {
            if (!res.ok) throw new Error("Failed to fetch leave data");
            return res.json();
        })
        .then(data => {

            const tbody = document.querySelector("#leaveTable tbody");
            tbody.innerHTML = "";

            if (!Array.isArray(data) || data.length === 0) {
                tbody.innerHTML = `<tr><td colspan="4">No Leave Requests</td></tr>`;
                return;
            }

            data.forEach(leave => {
                tbody.innerHTML += `
          <tr>
            <td>${leave.fromDate}</td>
            <td>${leave.toDate}</td>
            <td>${leave.reason}</td>
            <td>${leave.status}</td>
          </tr>
        `;
            });

        })
        .catch(err => console.error("Student load error:", err));
}

document.addEventListener("DOMContentLoaded", function () {
    checkAttendanceStatus();
    loadStudentLeaves();
    loadClassTeachers();

    // ----------------- PROFILE SETUP LOGIC -----------------
    // Check if profile is setup (using both user object and current UI values as safety)
    const currentClass = document.getElementById("profileClass").textContent.trim();
    const hasClass = (user.classMaster && user.classMaster.id) || user.classId;
    const hasDiv = (user.divisionMaster && user.divisionMaster.id) || user.divisionId;

    let needsSetup = (!hasClass || !hasDiv || currentClass === "-");

    if (needsSetup) {
        const setupModal = document.getElementById("setupModal");
        if (setupModal) {
            setupModal.style.display = "flex";
            setupModal.classList.remove("hidden");
            loadSetupClasses();
        }
    } else {
        const modal = document.getElementById("setupModal");
        if (modal) {
            modal.style.display = "none";
            modal.classList.add("hidden");
        }
    }

    const setupClassMaster = document.getElementById("setupClassMaster");
    if (setupClassMaster) {
        setupClassMaster.addEventListener("change", function () {
            loadSetupDivisions(this.value);
        });
    }

    const saveSetupBtn = document.getElementById("saveSetupBtn");
    if (saveSetupBtn) {
        saveSetupBtn.addEventListener("click", saveSetup);
    }
});

async function loadClassTeachers() {
    const classId = user.classMaster ? user.classMaster.id : user.classId;
    const divisionId = user.divisionMaster ? user.divisionMaster.id : user.divisionId;

    if (!user || !classId || !divisionId) return;

    try {
        const res = await fetch(`/api/master/classes/${classId}/divisions/${divisionId}/teachers`);
        if (!res.ok) throw new Error("Failed to load teachers");
        const teachers = await res.json();
        const select = document.getElementById("leaveTeacher");
        if (!select) return;

        // Keep the first option
        select.innerHTML = '<option value="">Select Teacher</option>';

        teachers.forEach(t => {
            const opt = document.createElement("option");
            opt.value = t.id;
            opt.textContent = t.name;
            select.appendChild(opt);
        });
    } catch (err) {
        console.error("Error loading teachers:", err);
    }
}

async function loadSetupClasses() {
    try {
        const res = await fetch("/api/master/classes");
        const classes = await res.json();
        const select = document.getElementById("setupClassMaster");
        if (!select) return;
        classes.forEach(c => {
            const opt = document.createElement("option");
            opt.value = c.id;
            opt.textContent = c.className;
            select.appendChild(opt);
        });
    } catch (err) { console.error("Error loading classes:", err); }
}

async function loadSetupDivisions(classId) {
    const select = document.getElementById("setupDivisionMaster");
    if (!select) return;
    select.innerHTML = '<option value="">Select Division</option>';
    if (!classId) {
        select.disabled = true;
        return;
    }
    try {
        const res = await fetch(`/api/master/classes/${classId}/divisions`);
        const divs = await res.json();
        divs.forEach(d => {
            const opt = document.createElement("option");
            opt.value = d.id;
            opt.textContent = d.divisionName;
            select.appendChild(opt);
        });
        select.disabled = false;
    } catch (err) { console.error("Error loading divisions:", err); }
}

async function saveSetup() {
    const classId = document.getElementById("setupClassMaster").value;
    const divId = document.getElementById("setupDivisionMaster").value;
    const errorEl = document.getElementById("setupError");

    if (!classId || !divId) {
        errorEl.textContent = "Please select both Class and Division.";
        errorEl.style.display = "block";
        return;
    }

    try {
        const payload = {
            classMaster: { id: parseInt(classId) },
            divisionMaster: { id: parseInt(divId) }
        };

        const res = await fetch(`/api/users/${user.id}/class-division`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
                ...((localStorage.getItem('studentAuthToken') || localStorage.getItem('authToken')) ? { "Authorization": `Bearer ${(localStorage.getItem('studentAuthToken') || localStorage.getItem('authToken'))}` } : {})
            },
            body: JSON.stringify(payload)
        });

        if (!res.ok) throw new Error("Failed to update profile");

        const updatedUser = await res.json();

        // Ensure role is preserved in localStorage if not in response
        const currentRole = (localStorage.getItem('studentRole') || localStorage.getItem('role')) || "student";
        const currentToken = (localStorage.getItem('studentAuthToken') || localStorage.getItem('authToken'));

        localStorage.setItem('studentLoggedUser', JSON.stringify(updatedUser));
        localStorage.setItem('studentRole', currentRole);
        if (currentToken) localStorage.setItem('studentAuthToken', currentToken);

        // Success - hide modal and refresh or update UI
        document.getElementById("setupModal").style.display = "none";
        document.getElementById("setupModal").classList.add("hidden");

        // Reload page to reflect changes properly and refresh all dashboard data
        window.location.reload();
    } catch (err) {
        errorEl.textContent = err.message || "An error occurred.";
        errorEl.style.display = "block";
    }
}

