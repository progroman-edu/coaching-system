import { api } from "./api.js";
import { fillTableBody, showMessage } from "./ui.js";

const msg = document.getElementById("msg");
const recordForm = document.getElementById("attendanceForm");
const reportForm = document.getElementById("reportForm");
const tbody = document.getElementById("attendanceRows");

recordForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = Object.fromEntries(new FormData(recordForm).entries());
    payload.traineeId = Number(payload.traineeId);
    payload.present = payload.present === "true";
    try {
        await api.recordAttendance(payload);
        showMessage(msg, "Attendance recorded.");
        recordForm.reset();
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});

reportForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = Object.fromEntries(new FormData(reportForm).entries());
    try {
        const data = await api.attendanceReport(payload.startDate, payload.endDate, payload.traineeId || undefined);
        const rows = data.map((r) => `
            <tr>
                <td>${r.traineeId ?? ""}</td>
                <td>${r.traineeName ?? ""}</td>
                <td>${r.sessionsPresent ?? 0}</td>
                <td>${r.totalSessions ?? 0}</td>
                <td>${Number(r.attendancePercentage ?? 0).toFixed(2)}%</td>
            </tr>
        `).join("");
        fillTableBody(tbody, rows);
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});
