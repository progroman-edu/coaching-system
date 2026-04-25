// This file powers attendance recording and report loading interactions.
import { api } from "./api.js";
import { escapeHtml, fillTableBody, showMessage } from "./ui.js";

const msg = document.getElementById("msg");
const recordForm = document.getElementById("attendanceForm");
const reportForm = document.getElementById("reportForm");
const tbody = document.getElementById("attendanceRows");
const attendanceProfiles = document.getElementById("attendanceProfiles");
const reportProfiles = document.getElementById("reportProfiles");
const attendanceSelectedLabel = document.getElementById("attendanceSelectedLabel");
const reportSelectedLabel = document.getElementById("reportSelectedLabel");
const clearReportTraineeBtn = document.getElementById("clearReportTrainee");

function initialsOf(name) {
    const parts = String(name ?? "").trim().split(/\s+/).filter(Boolean);
    if (parts.length === 0) return "?";
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return (parts[0][0] + parts[1][0]).toUpperCase();
}

function renderParticipantCards(trainees) {
    const cardHtml = trainees.map((trainee) => `
        <button type="button" class="participant-card" data-participant-id="${trainee.id}" aria-pressed="false">
            <span class="participant-avatar">
                ${trainee.photoPath ? `<img src="${escapeHtml(trainee.photoPath)}" alt="${escapeHtml(trainee.name)} photo">` : escapeHtml(initialsOf(trainee.name))}
            </span>
            <span>
                <span class="participant-name">${escapeHtml(trainee.name)}</span>
            </span>
        </button>
    `).join("");

    if (attendanceProfiles) attendanceProfiles.innerHTML = cardHtml || "<div>No trainees found.</div>";
    if (reportProfiles) reportProfiles.innerHTML = cardHtml || "<div>No trainees found.</div>";
}

function bindSingleParticipantPicker(container, form, selectedLabelEl, options = {}) {
    const allowClear = Boolean(options.allowClear);
    const emptyLabel = options.emptyLabel || "Selected: none";
    const selectedLabelPrefix = options.selectedLabelPrefix || "Selected";

    container?.addEventListener("click", (e) => {
        const card = e.target.closest("[data-participant-id]");
        if (!card) return;
        const id = Number(card.dataset.participantId);
        if (!Number.isFinite(id)) return;

        const input = form?.querySelector('input[name="traineeId"]');
        const allCards = container.querySelectorAll("[data-participant-id]");
        const alreadySelected = card.classList.contains("selected");

        if (allowClear && alreadySelected) {
            allCards.forEach((item) => {
                item.classList.remove("selected");
                item.setAttribute("aria-pressed", "false");
            });
            if (input) input.value = "";
            if (selectedLabelEl) selectedLabelEl.textContent = emptyLabel;
            return;
        }

        allCards.forEach((item) => {
            item.classList.remove("selected");
            item.setAttribute("aria-pressed", "false");
        });
        card.classList.add("selected");
        card.setAttribute("aria-pressed", "true");
        if (input) input.value = String(id);

        const nameEl = card.querySelector(".participant-name");
        const selectedName = nameEl?.textContent?.trim() || `ID ${id}`;
        if (selectedLabelEl) selectedLabelEl.textContent = `${selectedLabelPrefix}: ${selectedName}`;
    });
}

function clearReportSelection() {
    const input = reportForm?.querySelector('input[name="traineeId"]');
    if (input) input.value = "";
    const cards = reportProfiles?.querySelectorAll("[data-participant-id]") || [];
    cards.forEach((item) => {
        item.classList.remove("selected");
        item.setAttribute("aria-pressed", "false");
    });
    if (reportSelectedLabel) reportSelectedLabel.textContent = "Selected: all trainees";
}

async function loadParticipantProfiles() {
    try {
        const trainees = await api.listTrainees({ page: 0, size: 500, rankingOrder: "asc" });
        renderParticipantCards(trainees);
        if (attendanceSelectedLabel) attendanceSelectedLabel.textContent = "Selected: none";
        if (reportSelectedLabel) reportSelectedLabel.textContent = "Selected: all trainees";
    } catch (err) {
        showMessage(msg, err.message, false);
    }
}

recordForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = Object.fromEntries(new FormData(recordForm).entries());
    payload.traineeId = Number(payload.traineeId);
    payload.present = payload.present === "true";
    if (!Number.isFinite(payload.traineeId) || payload.traineeId <= 0) {
        showMessage(msg, "Please select a trainee profile.", false);
        return;
    }
    try {
        await api.recordAttendance(payload);
        showMessage(msg, "Attendance recorded.");
        recordForm.reset();
        if (attendanceSelectedLabel) attendanceSelectedLabel.textContent = "Selected: none";
        const cards = attendanceProfiles?.querySelectorAll("[data-participant-id]") || [];
        cards.forEach((item) => {
            item.classList.remove("selected");
            item.setAttribute("aria-pressed", "false");
        });
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});

reportForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = Object.fromEntries(new FormData(reportForm).entries());
    try {
        const traineeId = payload.traineeId ? Number(payload.traineeId) : undefined;
        const data = await api.attendanceReport(payload.startDate, payload.endDate, traineeId);
        const rows = data.map((r) => `
            <tr>
                <td>${escapeHtml(r.traineeName ?? "")}</td>
                <td>${escapeHtml(r.sessionsPresent ?? 0)}</td>
                <td>${escapeHtml(r.totalSessions ?? 0)}</td>
                <td>${escapeHtml(Number(r.attendancePercentage ?? 0).toFixed(2))}%</td>
            </tr>
        `).join("");
        fillTableBody(tbody, rows);
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});

bindSingleParticipantPicker(attendanceProfiles, recordForm, attendanceSelectedLabel, {
    allowClear: false,
    selectedLabelPrefix: "Selected"
});

bindSingleParticipantPicker(reportProfiles, reportForm, reportSelectedLabel, {
    allowClear: true,
    emptyLabel: "Selected: all trainees",
    selectedLabelPrefix: "Filter"
});

clearReportTraineeBtn?.addEventListener("click", () => {
    clearReportSelection();
});

loadParticipantProfiles();

