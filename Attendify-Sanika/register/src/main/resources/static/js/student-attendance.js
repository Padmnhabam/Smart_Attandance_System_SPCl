const params = new URLSearchParams(window.location.search);
const className = params.get("class");
const sessionId = params.get("sessionId");
const msgEl = document.getElementById("msg");

let deviceId = null;

// Validation check
if (!className || !sessionId) {
  msgEl.textContent = "Invalid QR link (Missing parameters) ❌";
  msgEl.style.color = "red";
}

// 🔹 Generate unique device ID
function loadDeviceId() {
  if (typeof FingerprintJS === 'undefined') {
    setTimeout(loadDeviceId, 500);
    return;
  }
  const fpPromise = FingerprintJS.load();
  fpPromise
    .then(fp => fp.get())
    .then(result => {
      deviceId = result.visitorId;
      console.log("Device ID loaded:", deviceId);
    })
    .catch(err => {
      console.error("FingerprintJS error:", err);
      msgEl.textContent = "Security verification failed to load ❌";
    });
}

// Load device ID when page opens
document.addEventListener("DOMContentLoaded", loadDeviceId);

document.getElementById("attendanceForm").addEventListener("submit", e => {
  e.preventDefault();

  const rollNo = document.getElementById("rollNo").value.trim();

  if (!deviceId) {
    msgEl.textContent = "Still verifying your device... please try again in a second. ⏳";
    msgEl.style.color = "orange";
    return;
  }

  if (!navigator.geolocation) {
    msgEl.textContent = "Location not supported (Please use a modern browser) ❌";
    msgEl.style.color = "red";
    return;
  }

  msgEl.textContent = "Getting location... 📍";
  msgEl.style.color = "blue";

  // 🔹 Get current location
  navigator.geolocation.getCurrentPosition(
    position => {
      const latitude = position.coords.latitude;
      const longitude = position.coords.longitude;

      msgEl.textContent = "Marking attendance... ⏳";
      msgEl.style.color = "blue";

      // ✅ Properly formatted form data
      const formData = new URLSearchParams();
      formData.append("rollNo", rollNo);
      formData.append("deviceId", deviceId);
      formData.append("latitude", latitude);
      formData.append("longitude", longitude);
      formData.append("sessionId", sessionId);

      fetch("/api/attendance/mark", {
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded"
        },
        body: formData.toString()
      })
        .then(res => {
          if (!res.ok) {
            return res.text().then(text => { throw new Error(text || "Server Error") });
          }
          return res.json();
        })
        .then(data => {
          msgEl.textContent = data.message;
          if (data.message.includes("successfully") || data.message.includes("✅")) {
            msgEl.style.color = "green";
          } else {
            msgEl.style.color = "red";
          }
        })
        .catch(error => {
          console.error(error);
          msgEl.textContent = "Error: " + (error.message || "Failed to mark attendance") + " ❌";
          msgEl.style.color = "red";
        });
    },
    error => {
      console.error(error);
      let errorMsg = "Location permission denied ❌";
      if (error.code === 3) errorMsg = "Location request timed out ❌";
      msgEl.textContent = errorMsg;
      msgEl.style.color = "red";
    },
    { enableHighAccuracy: true, timeout: 10000 }
  );
});
