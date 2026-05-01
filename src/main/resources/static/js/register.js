// This file powers trainee registration, editing, and photo uploads.
import { api } from "./api.js";
import { clearMessage, escapeHtml, showMessage } from "./ui.js";
import { renderAvatarImage } from "./trainee-common.js";

const msg = document.getElementById("msg");
const pageTitle = document.getElementById("pageTitle");
const pageSubtitle = document.getElementById("pageSubtitle");
const form = document.getElementById("traineeForm");
const photoForm = document.getElementById("photoForm");
const photoProfiles = document.getElementById("photoProfiles");
const photoSelectedLabel = document.getElementById("photoSelectedLabel");
const formTitle = document.getElementById("traineeFormTitle");
const saveBtn = document.getElementById("saveTraineeBtn");
const cancelEditBtn = document.getElementById("cancelEditBtn");

let editingId = null;

function applyPageState(isEditMode, traineeName = "") {
    if (pageTitle) {
        pageTitle.textContent = isEditMode ? "Edit Trainee" : "Register Trainee";
    }
    if (pageSubtitle) {
        pageSubtitle.textContent = isEditMode
            ? `Updating record for ${traineeName || "selected trainee"}.`
            : "Create a new trainee profile and prepare the roster for coaching.";
    }
    if (formTitle) {
        formTitle.textContent = isEditMode ? "Edit Trainee" : "Register New Trainee";
    }
    if (saveBtn) {
        saveBtn.textContent = isEditMode ? "Update Trainee" : "Save Trainee";
    }
    if (cancelEditBtn) {
        cancelEditBtn.style.display = isEditMode ? "" : "none";
    }
}

function renderPhotoPicker(trainees, selectedId = null) {
    if (!photoProfiles) {
        return;
    }
    const rows = trainees.map((trainee) => {
        const isSelected = Number(selectedId) === Number(trainee.id);
        return `
            <button type="button" class="participant-card${isSelected ? " selected" : ""}" data-participant-id="${Number(trainee.id)}" aria-pressed="${isSelected ? "true" : "false"}">
                <span class="participant-avatar participant-avatar-photo">
                    ${renderAvatarImage(trainee, "participant-avatar-image")}
                </span>
                <span>
                    <span class="participant-name">${escapeHtml(trainee.name)}</span>
                    <span class="participant-id">${escapeHtml(trainee.department || "Department not set")}</span>
                </span>
            </button>
        `;
    }).join("");
    photoProfiles.innerHTML = rows || "<div>No trainees found.</div>";

    const hidden = photoForm?.querySelector('input[name="traineeId"]');
    if (hidden) {
        hidden.value = selectedId ? String(selectedId) : "";
    }
    if (photoSelectedLabel) {
        const selected = trainees.find((item) => Number(item.id) === Number(selectedId));
        photoSelectedLabel.textContent = selected ? `Selected: ${selected.name}` : "Selected: none";
    }
}

async function loadPhotoPickerProfiles(selectedId = null) {
    try {
        const trainees = await api.listTrainees({ page: 0, size: 500, rankingOrder: "asc" });
        renderPhotoPicker(trainees, selectedId);
    } catch (err) {
        showMessage(msg, err.message, false);
    }
}

function resetEditMode() {
    editingId = null;
    form?.reset();
    if (form?.elements?.id) {
        form.elements.id.value = "";
    }
    applyPageState(false);
    history.replaceState({}, "", "/register.html");
}

function startEdit(trainee) {
    if (!form || !trainee) {
        return;
    }
    editingId = Number(trainee.id);
    if (form.elements.id) form.elements.id.value = String(trainee.id ?? "");
    form.elements.name.value = trainee.name ?? "";
    form.elements.age.value = trainee.age ?? "";
    form.elements.address.value = trainee.address ?? "";
    form.elements.gradeLevel.value = trainee.gradeLevel ?? "";
    form.elements.department.value = trainee.department ?? "";
    form.elements.chessUsername.value = trainee.chessUsername ?? "";
    applyPageState(true, trainee.name);
    history.replaceState({}, "", `/register.html?edit=${encodeURIComponent(trainee.id)}`);
}

async function loadEditFromQuery() {
    const params = new URLSearchParams(window.location.search);
    const editId = Number(params.get("edit"));
    if (!Number.isFinite(editId) || editId <= 0) {
        resetEditMode();
        await loadPhotoPickerProfiles();
        return;
    }

    try {
        const trainee = await api.getTrainee(editId);
        startEdit(trainee);
        await loadPhotoPickerProfiles(editId);
    } catch (err) {
        showMessage(msg, err.message, false);
        resetEditMode();
        await loadPhotoPickerProfiles();
    }
}

photoProfiles?.addEventListener("click", (e) => {
    const card = e.target.closest("[data-participant-id]");
    if (!card) {
        return;
    }
    const id = Number(card.dataset.participantId);
    if (!Number.isFinite(id)) {
        return;
    }

    const cards = photoProfiles.querySelectorAll("[data-participant-id]");
    cards.forEach((item) => {
        item.classList.remove("selected");
        item.setAttribute("aria-pressed", "false");
    });
    card.classList.add("selected");
    card.setAttribute("aria-pressed", "true");

    const hidden = photoForm?.querySelector('input[name="traineeId"]');
    if (hidden) {
        hidden.value = String(id);
    }
    const name = card.querySelector(".participant-name")?.textContent?.trim() || `ID ${id}`;
    if (photoSelectedLabel) {
        photoSelectedLabel.textContent = `Selected: ${name}`;
    }
});

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
        await loadPhotoPickerProfiles();
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});

cancelEditBtn?.addEventListener("click", async () => {
    clearMessage(msg);
    resetEditMode();
    await loadPhotoPickerProfiles();
});

photoForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const data = new FormData(photoForm);
    const traineeId = data.get("traineeId");
    const file = data.get("file");
    if (!traineeId || !file || file.size === 0) {
        showMessage(msg, "Select a trainee profile and image file.", false);
        return;
    }
    try {
        await api.uploadTraineePhoto(traineeId, file);
        showMessage(msg, "Photo uploaded.");
        photoForm.reset();
        await loadPhotoPickerProfiles(editingId || Number(traineeId));
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});

applyPageState(false);
loadEditFromQuery();
