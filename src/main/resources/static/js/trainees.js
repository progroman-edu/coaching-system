// This file powers trainee CRUD, filtering, dashboard table, and photo upload interactions.
import { api } from "./api.js";
import { clearMessage, escapeHtml, fillTableBody, showMessage } from "./ui.js";

const msg = document.getElementById("msg");
const tbody = document.getElementById("traineeRows");
const form = document.getElementById("traineeForm");
const filterForm = document.getElementById("filterForm");
const photoForm = document.getElementById("photoForm");
const photoProfiles = document.getElementById("photoProfiles");
const photoSelectedLabel = document.getElementById("photoSelectedLabel");
const formTitle = document.getElementById("traineeFormTitle");
const saveBtn = document.getElementById("saveTraineeBtn");
const cancelEditBtn = document.getElementById("cancelEditBtn");
const syncRatingsBtn = document.getElementById("syncRatingsBtn");
const clearFiltersBtn = document.getElementById("clearFiltersBtn");
const syncModeSelect = document.getElementById("syncMode");
const modePeakHeader = document.getElementById("modePeakHeader");
const activeFilterChips = document.getElementById("activeFilterChips");
const prevPageBtn = document.getElementById("prevPageBtn");
const nextPageBtn = document.getElementById("nextPageBtn");
const pageIndicator = document.getElementById("pageIndicator");
const sortButtons = Array.from(document.querySelectorAll(".sort-btn"));

const NOTE_STORAGE_KEY = "traineeCoachNotes";

let editingId = null;
let traineeCache = [];
let hasNextPage = false;

const tableState = {
    sortKey: "ranking",
    sortDir: "asc",
    page: 0,
    size: 20
};

function loadCoachNotes() {
    try {
        return JSON.parse(localStorage.getItem(NOTE_STORAGE_KEY) || "{}");
    } catch {
        return {};
    }
}

function saveCoachNotes(notes) {
    localStorage.setItem(NOTE_STORAGE_KEY, JSON.stringify(notes));
}

function getSelectedMode() {
    return (syncModeSelect?.value || "rapid").toLowerCase();
}

function modeLabel(mode) {
    const normalizedMode = (mode || "").toLowerCase();
    if (normalizedMode === "blitz") return "Blitz";
    if (normalizedMode === "bullet") return "Bullet";
    return "Rapid";
}

function getSelectedModeHighestRating(trainee) {
    const mode = getSelectedMode();
    if (mode === "blitz" && trainee?.blitzHighestRating != null) return trainee.blitzHighestRating;
    if (mode === "bullet" && trainee?.bulletHighestRating != null) return trainee.bulletHighestRating;
    if (mode === "rapid" && trainee?.rapidHighestRating != null) return trainee.rapidHighestRating;
    return trainee?.rapidCurrentRating ?? "";
}

function getRankBadgeClass(rank) {
    if (rank === 1) return "rank-badge rank-1";
    if (rank === 2) return "rank-badge rank-2";
    if (rank === 3) return "rank-badge rank-3";
    return "rank-badge";
}

function formatDelta(delta) {
    const value = Number(delta ?? 0);
    if (value > 0) return `<span class="delta-up">+${escapeHtml(value)}</span>`;
    if (value < 0) return `<span class="delta-down">${escapeHtml(value)}</span>`;
    return `<span class="delta-flat">0</span>`;
}

function formatAttendance(value) {
    if (value == null) return "0%";
    return `${Number(value).toFixed(1)}%`;
}

function formatLastActivity(value) {
    if (!value) return "N/A";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "N/A";
    return date.toLocaleDateString();
}

function initials(name) {
    const value = String(name || "").trim();
    if (!value) return "NA";
    return value
        .split(/\s+/)
        .slice(0, 2)
        .map((part) => part[0]?.toUpperCase() || "")
        .join("") || "NA";
}

function renderPhotoPicker(trainees) {
    if (!photoProfiles) {
        return;
    }
    const rows = trainees.map((trainee) => `
        <button type="button" class="participant-card" data-participant-id="${Number(trainee.id)}" aria-pressed="false">
            <span class="participant-avatar">
                ${trainee.photoPath ? `<img src="${escapeHtml(trainee.photoPath)}" alt="${escapeHtml(trainee.name)} photo">` : escapeHtml(initials(trainee.name))}
            </span>
            <span>
                <span class="participant-name">${escapeHtml(trainee.name)}</span>
            </span>
        </button>
    `).join("");
    photoProfiles.innerHTML = rows || "<div>No trainees found.</div>";
    if (photoSelectedLabel) {
        photoSelectedLabel.textContent = "Selected: none";
    }
}

async function loadPhotoPickerProfiles() {
    try {
        const trainees = await api.listTrainees({ page: 0, size: 500, rankingOrder: "asc" });
        renderPhotoPicker(trainees);
    } catch (err) {
        showMessage(msg, err.message, false);
    }
}

function updateModeAwareLabels() {
    const label = modeLabel(getSelectedMode());
    if (modePeakHeader) modePeakHeader.textContent = `Mode Peak (${label})`;
}

function updateSortButtonState() {
    for (const btn of sortButtons) {
        const isActive = btn.dataset.sort === tableState.sortKey;
        btn.classList.toggle("active", isActive);
    }
}

function resetEditMode() {
    editingId = null;
    form?.reset();
    if (form?.elements?.id) form.elements.id.value = "";
    if (formTitle) formTitle.textContent = "Create Trainee";
    if (saveBtn) saveBtn.textContent = "Save Trainee";
    if (cancelEditBtn) cancelEditBtn.style.display = "none";
    updateModeAwareLabels();
}

function startEdit(trainee) {
    if (!form || !trainee) return;
    editingId = Number(trainee.id);
    if (form.elements.id) form.elements.id.value = String(trainee.id ?? "");
    form.elements.name.value = trainee.name ?? "";
    form.elements.age.value = trainee.age ?? "";
    form.elements.address.value = trainee.address ?? "";
    form.elements.gradeLevel.value = trainee.gradeLevel ?? "";
    form.elements.department.value = trainee.department ?? "";
    form.elements.chessUsername.value = trainee.chessUsername ?? "";
    if (formTitle) formTitle.textContent = "Edit Trainee";
    if (saveBtn) saveBtn.textContent = "Update Trainee";
    if (cancelEditBtn) cancelEditBtn.style.display = "";
    form.scrollIntoView({ behavior: "smooth", block: "start" });
}

function getFilterParamsFromForm() {
    const raw = Object.fromEntries(new FormData(filterForm).entries());
    const params = {
        search: raw.search?.trim() || undefined,
        ratingMin: raw.ratingMin || undefined,
        department: raw.department?.trim() || undefined,
        rankingOrder: raw.rankingOrder || "asc",
        page: tableState.page,
        size: Number(raw.size || tableState.size)
    };
    tableState.size = Number(params.size);
    return params;
}

function renderActiveFilterChips(params) {
    const chips = [];
    if (params.search) chips.push(`Search: ${params.search}`);
    if (params.department) chips.push(`Department: ${params.department}`);
    if (params.ratingMin) chips.push(`Rating ≥ ${params.ratingMin}`);
    chips.push(`Page Size: ${params.size}`);
    activeFilterChips.innerHTML = chips.map((chip) => `<span class="filter-chip">${escapeHtml(chip)}</span>`).join("");
}

function compareValues(left, right, key) {
    const leftValue = key === "modePeak" ? getSelectedModeHighestRating(left) : left?.[key];
    const rightValue = key === "modePeak" ? getSelectedModeHighestRating(right) : right?.[key];
    if (leftValue == null && rightValue == null) return 0;
    if (leftValue == null) return 1;
    if (rightValue == null) return -1;
    if (typeof leftValue === "string" || typeof rightValue === "string") {
        return String(leftValue).localeCompare(String(rightValue));
    }
    if (key === "lastActivityAt") {
        return new Date(leftValue).getTime() - new Date(rightValue).getTime();
    }
    return Number(leftValue) - Number(rightValue);
}

function getSortedRows(rows) {
    return [...rows].sort((a, b) => {
        const result = compareValues(a, b, tableState.sortKey);
        return tableState.sortDir === "asc" ? result : -result;
    });
}

function renderTraineeTable() {
    const notes = loadCoachNotes();
    const rows = getSortedRows(traineeCache).map((t) => {
        const rank = Number(t.ranking || 0);
        const note = String(notes[t.id] || "").trim();
        const photoPath = t.photoPath ? escapeHtml(t.photoPath) : "";
        const avatar = photoPath
            ? `<img class="trainee-avatar" src="${photoPath}" alt="${escapeHtml(t.name)}">`
            : `<span class="trainee-avatar-fallback">${escapeHtml(initials(t.name))}</span>`;
        return `
            <tr>
                <td><span class="${getRankBadgeClass(rank)}">#${escapeHtml(rank || "-")}</span></td>
                <td>
                    <div class="trainee-id-cell">
                        ${avatar}
                        <div>
                            <div class="trainee-name">${escapeHtml(t.name ?? "")}</div>
                            <div class="trainee-meta">Age ${escapeHtml(t.age ?? "-")} • ${escapeHtml(t.gradeLevel ?? "-")}</div>
                        </div>
                    </div>
                </td>
                <td>
                    <span class="rating-main">${escapeHtml(t.rapidCurrentRating ?? "")}</span>
                </td>
                <td>${formatDelta(t.latestRatingChange)}</td>
                <td>
                    <span class="rating-main">${escapeHtml(getSelectedModeHighestRating(t) ?? "")}</span>
                    <span class="rating-sub"><span class="mode-chip">${escapeHtml(modeLabel(getSelectedMode()))}</span></span>
                </td>
                <td>${escapeHtml(t.department ?? "")}</td>
                <td>${escapeHtml(formatAttendance(t.attendancePercentageLast30Days))}</td>
                <td>${escapeHtml(formatLastActivity(t.lastActivityAt))}</td>
                <td>${escapeHtml(t.chessUsername ?? "N/A")}</td>
                <td>${escapeHtml(note || "No notes")}</td>
                <td>
                    <div class="actions-inline">
                        <button data-view="${Number(t.id)}" type="button">View</button>
                        <button data-edit="${Number(t.id)}" class="secondary" type="button">Edit</button>
                        <button data-note="${Number(t.id)}" class="secondary" type="button">Note</button>
                        <button data-del="${Number(t.id)}" class="danger" type="button">Delete</button>
                    </div>
                </td>
            </tr>
        `;
    }).join("");
    fillTableBody(tbody, rows);
}

function updatePaginationUI() {
    if (pageIndicator) pageIndicator.textContent = `Page ${tableState.page + 1}`;
    if (prevPageBtn) prevPageBtn.disabled = tableState.page <= 0;
    if (nextPageBtn) nextPageBtn.disabled = !hasNextPage;
}

async function loadTrainees() {
    try {
        const params = getFilterParamsFromForm();
        renderActiveFilterChips(params);
        const data = await api.listTrainees(params);
        traineeCache = data;
        hasNextPage = data.length === Number(params.size);
        renderTraineeTable();
        updatePaginationUI();
    } catch (err) {
        showMessage(msg, err.message, false);
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
        await loadTrainees();
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});

filterForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    tableState.page = 0;
    await loadTrainees();
});

tbody?.addEventListener("click", async (e) => {
    const viewId = e.target?.dataset?.view;
    if (viewId) {
        window.location.href = `/trainees/${viewId}`;
        return;
    }

    const noteId = e.target?.dataset?.note;
    if (noteId) {
        const trainee = traineeCache.find((t) => Number(t.id) === Number(noteId));
        if (!trainee) return;
        const notes = loadCoachNotes();
        const current = notes[noteId] || "";
        const nextValue = prompt(`Coach note for ${trainee.name}:`, current);
        if (nextValue === null) return;
        notes[noteId] = nextValue.trim();
        saveCoachNotes(notes);
        renderTraineeTable();
        return;
    }

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
        await loadTrainees();
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
    for (const trainee of targets) {
        try {
            await api.syncTraineeChessComRating(trainee.id, mode);
            successCount += 1;
        } catch (err) {
            failures.push(`${trainee.name ?? "Trainee"}: ${err.message}`);
        }
    }
    await loadTrainees();
    if (failures.length) {
        showMessage(msg, `Synced ${successCount}/${targets.length}. Failures: ${failures.join(" | ")}`, false);
        return;
    }
    showMessage(msg, `Synced ${successCount}/${targets.length} Chess.com ratings.`);
});

syncModeSelect?.addEventListener("change", () => {
    updateModeAwareLabels();
    renderTraineeTable();
});

clearFiltersBtn?.addEventListener("click", async () => {
    filterForm?.reset();
    if (filterForm?.elements?.rankingOrder) filterForm.elements.rankingOrder.value = "asc";
    if (filterForm?.elements?.size) filterForm.elements.size.value = "20";
    tableState.page = 0;
    tableState.size = 20;
    await loadTrainees();
});

prevPageBtn?.addEventListener("click", async () => {
    if (tableState.page <= 0) return;
    tableState.page -= 1;
    await loadTrainees();
});

nextPageBtn?.addEventListener("click", async () => {
    if (!hasNextPage) return;
    tableState.page += 1;
    await loadTrainees();
});

sortButtons.forEach((button) => {
    button.addEventListener("click", () => {
        const clickedKey = button.dataset.sort;
        if (!clickedKey) return;
        if (tableState.sortKey === clickedKey) {
            tableState.sortDir = tableState.sortDir === "asc" ? "desc" : "asc";
        } else {
            tableState.sortKey = clickedKey;
            tableState.sortDir = clickedKey === "ranking" ? "asc" : "desc";
        }
        updateSortButtonState();
        renderTraineeTable();
    });
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
        if (photoSelectedLabel) {
            photoSelectedLabel.textContent = "Selected: none";
        }
        const cards = photoProfiles?.querySelectorAll("[data-participant-id]") || [];
        cards.forEach((item) => {
            item.classList.remove("selected");
            item.setAttribute("aria-pressed", "false");
        });
        await loadTrainees();
        await loadPhotoPickerProfiles();
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});

updateModeAwareLabels();
updateSortButtonState();
loadTrainees();
loadPhotoPickerProfiles();
