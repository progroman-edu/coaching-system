// This file powers the trainee roster browse page.
import { api } from "./api.js";
import { clearMessage, escapeHtml, showMessage } from "./ui.js";
import {
    formatAttendance,
    formatLastActivity,
    getEvaluationBadgeText,
    getRankBadgeClass,
    getTraineeDetailHref,
    getTraineeEvaluationHref,
    isDeletePlayerEnabled,
    loadCoachNotes,
    renderAvatarImage,
    saveCoachNotes
} from "./trainee-common.js";

const msg = document.getElementById("msg");
const filterForm = document.getElementById("filterForm");
const activeFilterChips = document.getElementById("activeFilterChips");
const clearFiltersBtn = document.getElementById("clearFiltersBtn");
const prevPageBtn = document.getElementById("prevPageBtn");
const nextPageBtn = document.getElementById("nextPageBtn");
const pageIndicator = document.getElementById("pageIndicator");
const traineeCards = document.getElementById("traineeCards");

let traineeCache = [];
let hasNextPage = false;

const rosterState = {
    page: 0,
    size: 12
};

function getFilterParamsFromForm() {
    const raw = Object.fromEntries(new FormData(filterForm).entries());
    const params = {
        search: raw.search?.trim() || undefined,
        ratingMin: raw.ratingMin || undefined,
        department: raw.department?.trim() || undefined,
        rankingOrder: "asc",
        page: rosterState.page,
        size: Number(raw.size || rosterState.size)
    };
    rosterState.size = Number(params.size);
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
    chips.push(`Page Size: ${params.size}`);
    activeFilterChips.innerHTML = chips.map((chip) => `<span class="filter-chip">${escapeHtml(chip)}</span>`).join("");
}

function renderTraineeCards() {
    if (!traineeCards) {
        return;
    }
    const notes = loadCoachNotes();
    const deleteEnabled = isDeletePlayerEnabled();
    const cards = traineeCache.map((trainee) => {
        const rank = Number(trainee.ranking || 0);
        const note = String(notes[trainee.id] || "").trim();
        return `
            <article class="license-card">
                <div class="license-top">
                    <div>
                        <div class="license-title">Chess Coaching System</div>
                        <div class="license-subtitle">Trainee Profile Card</div>
                    </div>
                    <span class="${getRankBadgeClass(rank)}">#${escapeHtml(rank || "-")}</span>
                </div>
                <div class="license-body">
                    <div class="license-photo-shell">
                        ${renderAvatarImage(trainee, "license-photo")}
                    </div>
                    <div>
                        <div class="license-name">${escapeHtml(trainee.name || "Unnamed Trainee")}</div>
                        <div class="license-dept">${escapeHtml(trainee.department || "Department not set")}</div>
                        <div class="license-rating-grid">
                            <div class="license-rating-pill">
                                <span class="license-rating-label">Rapid</span>
                                <span class="license-rating-value">${escapeHtml(trainee.rapidCurrentRating ?? 1200)}</span>
                            </div>
                            <div class="license-rating-pill">
                                <span class="license-rating-label">Blitz</span>
                                <span class="license-rating-value">${escapeHtml(trainee.blitzCurrentRating ?? 1200)}</span>
                            </div>
                            <div class="license-rating-pill">
                                <span class="license-rating-label">Bullet</span>
                                <span class="license-rating-value">${escapeHtml(trainee.bulletCurrentRating ?? 1200)}</span>
                            </div>
                        </div>
                        <div class="license-meta-row">
                            <span class="license-meta-chip">Grade ${escapeHtml(trainee.gradeLevel || "-")}</span>
                            <span class="license-meta-chip">Attendance ${escapeHtml(formatAttendance(trainee.attendancePercentageLast30Days))}</span>
                            <span class="license-meta-chip">Last Active ${escapeHtml(formatLastActivity(trainee.lastActivityAt))}</span>
                            <span class="license-meta-chip">${escapeHtml(getEvaluationBadgeText(trainee.id))}</span>
                        </div>
                    </div>
                </div>
                <div class="license-footer">
                    <div class="license-note">${escapeHtml(note || "No coach note")}</div>
                    <div class="license-footer-actions">
                        <button data-view="${Number(trainee.id)}" type="button">View</button>
                        <button data-evaluate="${Number(trainee.id)}" class="secondary" type="button">Evaluation</button>
                        <button data-edit="${Number(trainee.id)}" class="secondary" type="button">Edit</button>
                        <button data-note="${Number(trainee.id)}" class="secondary" type="button">Note</button>
                        ${deleteEnabled ? `<button data-del="${Number(trainee.id)}" class="danger" type="button">Delete</button>` : ""}
                    </div>
                </div>
            </article>
        `;
    }).join("");

    traineeCards.innerHTML = cards || `<div class="panel-empty">No trainees match the current filters.</div>`;
}

function updatePaginationUI() {
    if (pageIndicator) pageIndicator.textContent = `Page ${rosterState.page + 1}`;
    if (prevPageBtn) prevPageBtn.disabled = rosterState.page <= 0;
    if (nextPageBtn) nextPageBtn.disabled = !hasNextPage;
}

async function loadTrainees() {
    try {
        const params = getFilterParamsFromForm();
        renderActiveFilterChips(params);
        const data = await api.listTrainees(params);
        traineeCache = data;
        hasNextPage = data.length === Number(params.size);
        renderTraineeCards();
        updatePaginationUI();
    } catch (err) {
        showMessage(msg, err.message, false);
    }
}

filterForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    rosterState.page = 0;
    await loadTrainees();
});

clearFiltersBtn?.addEventListener("click", async () => {
    filterForm?.reset();
    if (filterForm?.elements?.size) filterForm.elements.size.value = "12";
    rosterState.page = 0;
    rosterState.size = 12;
    await loadTrainees();
});

prevPageBtn?.addEventListener("click", async () => {
    if (rosterState.page <= 0) return;
    rosterState.page -= 1;
    await loadTrainees();
});

nextPageBtn?.addEventListener("click", async () => {
    if (!hasNextPage) return;
    rosterState.page += 1;
    await loadTrainees();
});

traineeCards?.addEventListener("click", async (e) => {
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

    const editId = actionButton.dataset?.edit;
    if (editId) {
        window.location.href = `/register.html?edit=${encodeURIComponent(editId)}`;
        return;
    }

    const noteId = actionButton.dataset?.note;
    if (noteId) {
        const trainee = traineeCache.find((item) => Number(item.id) === Number(noteId));
        if (!trainee) {
            return;
        }
        const notes = loadCoachNotes();
        const current = notes[noteId] || "";
        const nextValue = prompt(`Coach note for ${trainee.name}:`, current);
        if (nextValue === null) {
            return;
        }
        notes[noteId] = nextValue.trim();
        saveCoachNotes(notes);
        renderTraineeCards();
        return;
    }

    const deleteId = actionButton.dataset?.del;
    if (!deleteId) {
        return;
    }
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

window.addEventListener("app:settings-changed", () => {
    renderTraineeCards();
});

loadTrainees();
