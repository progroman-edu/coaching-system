// This file powers trainee CRUD, filtering, and photo upload interactions.
import { api } from "./api.js";
import { clearMessage, escapeHtml, fillTableBody, showMessage } from "./ui.js";

const msg = document.getElementById("msg");
const tbody = document.getElementById("traineeRows");
const form = document.getElementById("traineeForm");
const filterForm = document.getElementById("filterForm");
const photoForm = document.getElementById("photoForm");
const formTitle = document.getElementById("traineeFormTitle");
const saveBtn = document.getElementById("saveTraineeBtn");
const cancelEditBtn = document.getElementById("cancelEditBtn");
const syncRatingsBtn = document.getElementById("syncRatingsBtn");
const syncModeSelect = document.getElementById("syncMode");

let editingId = null;
let traineeCache = [];
let lastFilterParams = { rankingOrder: "asc" };

function resetEditMode() {
    editingId = null;
    form?.reset();
    if (form?.elements?.id) form.elements.id.value = "";
    if (formTitle) formTitle.textContent = "Create Trainee";
    if (saveBtn) saveBtn.textContent = "Save Trainee";
    if (cancelEditBtn) cancelEditBtn.style.display = "none";
}

function startEdit(trainee) {
    if (!form || !trainee) return;
    editingId = Number(trainee.id);
    if (form.elements.id) form.elements.id.value = String(trainee.id ?? "");
    form.elements.name.value = trainee.name ?? "";
    form.elements.age.value = trainee.age ?? "";
    form.elements.address.value = trainee.address ?? "";
    form.elements.gradeLevel.value = trainee.gradeLevel ?? "";
    form.elements.courseStrand.value = trainee.courseStrand ?? "";
    form.elements.chessUsername.value = trainee.chessUsername ?? "";
    if (formTitle) formTitle.textContent = `Edit Trainee #${editingId}`;
    if (saveBtn) saveBtn.textContent = "Update Trainee";
    if (cancelEditBtn) cancelEditBtn.style.display = "";
    form.scrollIntoView({ behavior: "smooth", block: "start" });
}

async function loadTrainees(params = {}) {
    try {
        const data = await api.listTrainees(params);
        traineeCache = data;
        lastFilterParams = params;
        const rows = data.map((t, index) => `
            <tr>
                <td>${index + 1}</td>
                <td>${escapeHtml(t.name ?? "")}</td>
                <td>${escapeHtml(t.age ?? "")}</td>
                <td>${escapeHtml(t.courseStrand ?? "")}</td>
                <td>${escapeHtml(t.currentRating ?? "")}</td>
                <td>${escapeHtml(t.highestRating ?? "")}</td>
                <td>${escapeHtml(t.currentRatingMode ?? "")}</td>
                <td>${escapeHtml(t.ranking ?? "")}</td>
                <td>${escapeHtml(t.chessUsername ?? "")}</td>
                <td>${escapeHtml(t.photoPath ?? "")}</td>
                <td>
                    <button data-edit="${Number(t.id)}" class="secondary">Edit</button>
                    <button data-del="${Number(t.id)}" class="danger">Delete</button>
                </td>
            </tr>
        `).join("");
        fillTableBody(tbody, rows);
    } catch (err) {
        showMessage(msg, err.message, false);
    }
}

form?.addEventListener("submit", async (e) => {
    e.preventDefault();
    clearMessage(msg);
    const payload = Object.fromEntries(new FormData(form).entries());
    delete payload.id;
    payload.age = Number(payload.age);
    try {
        if (editingId) {
            await api.updateTrainee(editingId, payload);
            showMessage(msg, `Trainee ${editingId} updated.`);
        } else {
            await api.createTrainee(payload);
            showMessage(msg, "Trainee created.");
        }
        resetEditMode();
        await loadTrainees(lastFilterParams);
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});

filterForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const params = Object.fromEntries(new FormData(filterForm).entries());
    await loadTrainees(params);
});

tbody?.addEventListener("click", async (e) => {
    const editId = e.target?.dataset?.edit;
    if (editId) {
        const trainee = traineeCache.find((t) => Number(t.id) === Number(editId));
        if (!trainee) {
            showMessage(msg, "Trainee not found in current list.", false);
            return;
        }
        startEdit(trainee);
        return;
    }

    const id = e.target?.dataset?.del;
    if (!id) return;
    try {
        await api.deleteTrainee(id);
        showMessage(msg, `Trainee ${id} deleted.`);
        if (editingId && Number(editingId) === Number(id)) {
            resetEditMode();
        }
        await loadTrainees(lastFilterParams);
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});

cancelEditBtn?.addEventListener("click", () => {
    resetEditMode();
    clearMessage(msg);
});

syncRatingsBtn?.addEventListener("click", async () => {
    clearMessage(msg);
    if (!traineeCache.length) {
        showMessage(msg, "No trainees loaded to sync.", false);
        return;
    }
    const targets = traineeCache.filter((t) => t.chessUsername && String(t.chessUsername).trim().length > 0);
    if (!targets.length) {
        showMessage(msg, "No trainees with Chess.com usernames to sync.", false);
        return;
    }
    const mode = syncModeSelect?.value || "rapid";
    let successCount = 0;
    const failures = [];
    const modeCounts = { rapid: 0, blitz: 0, bullet: 0 };
    for (const trainee of targets) {
        try {
            const result = await api.syncTraineeChessComRating(trainee.id, mode);
            successCount += 1;
            const resolvedMode = result?.mode;
            if (resolvedMode && modeCounts[resolvedMode] !== undefined) {
                modeCounts[resolvedMode] += 1;
            }
        } catch (err) {
            failures.push(`${trainee.name ?? "Trainee"} (#${trainee.id}): ${err.message}`);
        }
    }
    await loadTrainees(lastFilterParams);
    const modeSummary = Object.entries(modeCounts)
        .filter(([, count]) => count > 0)
        .map(([key, count]) => `${key}: ${count}`)
        .join(", ");
    if (failures.length) {
        showMessage(
            msg,
            `Synced ${successCount}/${targets.length} ratings (${modeSummary || mode}). Failures: ${failures.join(" | ")}`,
            false
        );
        return;
    }
    showMessage(msg, `Synced ${successCount}/${targets.length} Chess.com ratings (${modeSummary || mode}).`);
});

photoForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const data = new FormData(photoForm);
    const traineeId = data.get("traineeId");
    const file = data.get("file");
    if (!traineeId || !file || file.size === 0) {
        showMessage(msg, "Trainee ID and image file are required.", false);
        return;
    }
    try {
        await api.uploadTraineePhoto(traineeId, file);
        showMessage(msg, "Photo uploaded.");
        photoForm.reset();
        await loadTrainees(lastFilterParams);
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});

loadTrainees({ rankingOrder: "asc" });

