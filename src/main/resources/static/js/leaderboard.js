// This file powers the trainee leaderboard page.
import { api } from "./api.js";
import { clearMessage, escapeHtml, fillTableBody, showMessage } from "./ui.js";
import {
    formatAttendance,
    formatDelta,
    formatLastActivity,
    getEvaluationBadgeText,
    getModeHighestRating,
    getRankBadgeClass,
    getTraineeDetailHref,
    getTraineeEvaluationHref,
    isDeletePlayerEnabled,
    loadCoachNotes,
    modeLabel,
    renderAvatarImage,
    saveCoachNotes
} from "./trainee-common.js";

const msg = document.getElementById("msg");
const tbody = document.getElementById("traineeRows");
const filterForm = document.getElementById("filterForm");
const syncRatingsBtn = document.getElementById("syncRatingsBtn");
const clearFiltersBtn = document.getElementById("clearFiltersBtn");
const syncModeSelect = document.getElementById("syncMode");
const modePeakHeader = document.getElementById("modePeakHeader");
const activeFilterChips = document.getElementById("activeFilterChips");
const prevPageBtn = document.getElementById("prevPageBtn");
const nextPageBtn = document.getElementById("nextPageBtn");
const pageIndicator = document.getElementById("pageIndicator");
const sortButtons = Array.from(document.querySelectorAll(".sort-btn"));

let traineeCache = [];
let hasNextPage = false;

const tableState = {
    sortKey: "ranking",
    sortDir: "asc",
    page: 0,
    size: 20
};

function getSelectedMode() {
    return (syncModeSelect?.value || "rapid").toLowerCase();
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
    if (!activeFilterChips) {
        return;
    }
    const chips = [];
    if (params.search) chips.push(`Search: ${params.search}`);
    if (params.department) chips.push(`Department: ${params.department}`);
    if (params.ratingMin) chips.push(`Rating >= ${params.ratingMin}`);
    chips.push(`Ranking: ${params.rankingOrder === "desc" ? "Bottom Rank First" : "Top Rank First"}`);
    chips.push(`Page Size: ${params.size}`);
    activeFilterChips.innerHTML = chips.map((chip) => `<span class="filter-chip">${escapeHtml(chip)}</span>`).join("");
}

function compareValues(left, right, key) {
    const leftValue = key === "modePeak" ? getModeHighestRating(left, getSelectedMode()) : left?.[key];
    const rightValue = key === "modePeak" ? getModeHighestRating(right, getSelectedMode()) : right?.[key];
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
    const deleteEnabled = isDeletePlayerEnabled();
    const rows = getSortedRows(traineeCache).map((trainee) => {
        const rank = Number(trainee.ranking || 0);
        const note = String(notes[trainee.id] || "").trim();
        return `
            <tr>
                <td><span class="${getRankBadgeClass(rank)}">#${escapeHtml(rank || "-")}</span></td>
                <td>
                    <div class="trainee-id-cell">
                        ${renderAvatarImage(trainee, "trainee-avatar")}
                        <div>
                            <div class="trainee-name">${escapeHtml(trainee.name ?? "")}</div>
                            <div class="trainee-meta">Age ${escapeHtml(trainee.age ?? "-")} • ${escapeHtml(trainee.gradeLevel ?? "-")} • ${escapeHtml(getEvaluationBadgeText(trainee.id))}</div>
                        </div>
                    </div>
                </td>
                <td><span class="rating-main">${escapeHtml(trainee.rapidCurrentRating ?? "")}</span></td>
                <td>${formatDelta(trainee.latestRatingChange)}</td>
                <td>
                    <span class="rating-main">${escapeHtml(getModeHighestRating(trainee, getSelectedMode()) ?? "")}</span>
                    <span class="rating-sub"><span class="mode-chip">${escapeHtml(modeLabel(getSelectedMode()))}</span></span>
                </td>
                <td>${escapeHtml(trainee.department ?? "")}</td>
                <td>${escapeHtml(formatAttendance(trainee.attendancePercentageLast30Days))}</td>
                <td>${escapeHtml(formatLastActivity(trainee.lastActivityAt))}</td>
                <td>${escapeHtml(trainee.chessUsername ?? "N/A")}</td>
                <td>${escapeHtml(note || "No notes")}</td>
                <td>
                    <div class="actions-inline">
                        <button data-view="${Number(trainee.id)}" type="button">View</button>
                        <button data-evaluate="${Number(trainee.id)}" class="secondary" type="button">Evaluation</button>
                        <button data-edit="${Number(trainee.id)}" class="secondary" type="button">Edit</button>
                        <button data-note="${Number(trainee.id)}" class="secondary" type="button">Note</button>
                        ${deleteEnabled ? `<button data-del="${Number(trainee.id)}" class="danger" type="button">Delete</button>` : ""}
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

filterForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    tableState.page = 0;
    await loadTrainees();
});

clearFiltersBtn?.addEventListener("click", async () => {
    filterForm?.reset();
    if (filterForm?.elements?.rankingOrder) filterForm.elements.rankingOrder.value = "asc";
    if (filterForm?.elements?.size) filterForm.elements.size.value = "20";
    if (syncModeSelect) syncModeSelect.value = "rapid";
    tableState.page = 0;
    tableState.size = 20;
    updateModeAwareLabels();
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

tbody?.addEventListener("click", async (e) => {
    const actionButton = e.target?.closest("button");
    if (!actionButton) {
        return;
    }

    const viewId = actionButton.dataset?.view;
    if (viewId) {
        window.location.href = getTraineeDetailHref(viewId);
        return;
    }

    const evaluateId = actionButton.dataset?.evaluate;
    if (evaluateId) {
        window.location.href = getTraineeEvaluationHref(evaluateId);
        return;
    }

    const noteId = actionButton.dataset?.note;
    if (noteId) {
        const trainee = traineeCache.find((item) => Number(item.id) === Number(noteId));
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

    const editId = actionButton.dataset?.edit;
    if (editId) {
        window.location.href = `/register.html?edit=${encodeURIComponent(editId)}`;
        return;
    }

    const deleteId = actionButton.dataset?.del;
    if (!deleteId) return;
    if (!isDeletePlayerEnabled()) {
        showMessage(msg, "Enable player deletion in Settings before deleting trainees.", false);
        return;
    }
    try {
        await api.deleteTrainee(deleteId);
        clearMessage(msg);
        showMessage(msg, `Trainee ${deleteId} deleted.`);
        await loadTrainees();
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});

syncRatingsBtn?.addEventListener("click", async () => {
    clearMessage(msg);
    if (!traineeCache.length) {
        showMessage(msg, "No trainees loaded to sync.", false);
        return;
    }
    const targets = traineeCache.filter((trainee) => trainee.chessUsername && String(trainee.chessUsername).trim().length > 0);
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

window.addEventListener("app:settings-changed", () => {
    renderTraineeTable();
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

updateModeAwareLabels();
updateSortButtonState();
loadTrainees();
