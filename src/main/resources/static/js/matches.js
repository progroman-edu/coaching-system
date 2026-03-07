// This file powers match creation, pairing generation, and result submission interactions.
import { api } from "./api.js";
import { clearMessage, fillTableBody, showMessage } from "./ui.js";

const msg = document.getElementById("msg");
const scheduleForm = document.getElementById("scheduleForm");
const pairForm = document.getElementById("pairForm");
const resultForm = document.getElementById("resultForm");
const historyForm = document.getElementById("historyForm");
const tbody = document.getElementById("matchRows");
const scheduleProfiles = document.getElementById("scheduleProfiles");
const pairProfiles = document.getElementById("pairProfiles");
const historyProfiles = document.getElementById("historyProfiles");
const scheduleSelectedCount = document.getElementById("scheduleSelectedCount");
const pairSelectedCount = document.getElementById("pairSelectedCount");
const historySelectedLabel = document.getElementById("historySelectedLabel");
const loadingOverlay = document.getElementById("loadingOverlay");
const resultMatchSelect = resultForm?.querySelector('select[name="matchRef"]');
const scheduleSelectedIds = new Set();
const pairSelectedIds = new Set();
const traineesById = new Map();
const offlineMatchesByRef = new Map();
let loadingCount = 0;

function parseIds(idsText) {
    return idsText
        .split(",")
        .map((x) => Number(x.trim()))
        .filter((x) => Number.isFinite(x));
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
}

function initialsOf(name) {
    const parts = String(name ?? "").trim().split(/\s+/).filter(Boolean);
    if (parts.length === 0) return "?";
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return (parts[0][0] + parts[1][0]).toUpperCase();
}

function setLoading(active) {
    // Keep a counter so overlapping async calls do not hide the loader too early.
    if (!loadingOverlay) return;
    loadingCount = Math.max(0, loadingCount + (active ? 1 : -1));
    const busy = loadingCount > 0;
    loadingOverlay.classList.toggle("active", busy);
    loadingOverlay.setAttribute("aria-hidden", busy ? "false" : "true");
}

async function withLoading(task) {
    // Standard wrapper: show loader before async work, always hide after.
    setLoading(true);
    try {
        return await task();
    } finally {
        setLoading(false);
    }
}

function shortDate(value) {
    // Convert YYYY-MM-DD from backend into MM/DD/YY for display IDs.
    const text = String(value ?? "");
    const parts = text.split("-");
    if (parts.length !== 3) return text;
    const mm = parts[1]?.padStart(2, "0");
    const dd = parts[2]?.padStart(2, "0");
    const yy = parts[0]?.slice(-2);
    return `${mm}/${dd}/${yy}`;
}

function winnerSide(resultText) {
    // Normalize different result formats into a side ("white"/"black"/"draw").
    const value = String(resultText ?? "").trim().toLowerCase();
    if (!value) return "";
    if (value.includes("draw")) return "draw";
    if (value.includes("white")) return "white";
    if (value.includes("black")) return "black";
    if (value === "1-0") return "white";
    if (value === "0-1") return "black";
    if (value === "1/2-1/2") return "draw";
    return "";
}

function nameResultClass(side, winner) {
    if (winner === "draw" || !winner) return "";
    return side === winner ? "name-win" : "name-loss";
}

function buildOfflineMatchRefRow(match) {
    // Build the requested F2F match reference format and apply win/loss highlighting.
    const winner = winnerSide(match?.result);
    const whiteClass = nameResultClass("white", winner);
    const blackClass = nameResultClass("black", winner);
    const refText = `${shortDate(match?.scheduledDate)}-${match?.whitePlayer}(white)-vs-${match?.blackPlayer}(black)`;
    return {
        refText,
        html: `${shortDate(match?.scheduledDate)}-<span class="${whiteClass}">${escapeHtml(match?.whitePlayer ?? "")}</span>(white)-vs-<span class="${blackClass}">${escapeHtml(match?.blackPlayer ?? "")}</span>(black)`
    };
}

function isWithinStartDateToToday(dateText, startDateText) {
    // Date filter behavior: selected date is start date; end date is today's date.
    const value = String(dateText ?? "").trim();
    if (!value) return false;
    if (!startDateText) return true;
    const matchDate = new Date(`${value}T00:00:00`);
    const startDate = new Date(`${startDateText}T00:00:00`);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    if (Number.isNaN(matchDate.getTime()) || Number.isNaN(startDate.getTime())) {
        return false;
    }
    return matchDate >= startDate && matchDate <= today;
}

function applyHistoryFilters(rows, filterDate, filterFormat) {
    // Apply date range + optional format filter for history only.
    return rows.filter((row) => {
        if (!isWithinStartDateToToday(row.date, filterDate)) {
            return false;
        }
        if (filterFormat && filterFormat !== "ALL" && String(row.format ?? "").toUpperCase() !== filterFormat) {
            return false;
        }
        return true;
    });
}

function refreshResultMatchOptions() {
    // Result form uses a human-readable match ref while storing real backend matchId internally.
    if (!resultMatchSelect) return;
    const options = ['<option value="">Select a loaded F2F match</option>'];
    for (const [key, entry] of offlineMatchesByRef.entries()) {
        const label = `${entry.displayRef} | ${entry.match.result ?? "PENDING"}`;
        options.push(`<option value="${escapeHtml(key)}">${escapeHtml(label)}</option>`);
    }
    resultMatchSelect.innerHTML = options.join("");
}

function syncSelectedIds(form, selectedIds, countEl) {
    const input = form?.querySelector('input[name="traineeIds"]');
    const values = [...selectedIds];
    if (input) input.value = values.join(",");
    if (countEl) countEl.textContent = `Selected: ${values.length}`;
}

function bindParticipantPicker(container, form, selectedIds, countEl) {
    // Multi-select participant cards for schedule/pairing forms.
    container?.addEventListener("click", (e) => {
        const card = e.target.closest("[data-participant-id]");
        if (!card) return;
        const id = Number(card.dataset.participantId);
        if (!Number.isFinite(id)) return;

        if (selectedIds.has(id)) {
            selectedIds.delete(id);
            card.classList.remove("selected");
            card.setAttribute("aria-pressed", "false");
        } else {
            selectedIds.add(id);
            card.classList.add("selected");
            card.setAttribute("aria-pressed", "true");
        }
        syncSelectedIds(form, selectedIds, countEl);
    });
}

function bindSingleParticipantPicker(container, form, selectedLabelEl) {
    // Single-select participant card for history lookup.
    container?.addEventListener("click", (e) => {
        const card = e.target.closest("[data-participant-id]");
        if (!card) return;
        const id = Number(card.dataset.participantId);
        if (!Number.isFinite(id)) return;

        const allCards = container.querySelectorAll("[data-participant-id]");
        allCards.forEach((item) => {
            item.classList.remove("selected");
            item.setAttribute("aria-pressed", "false");
        });
        card.classList.add("selected");
        card.setAttribute("aria-pressed", "true");

        const input = form?.querySelector('input[name="traineeId"]');
        if (input) input.value = String(id);

        const nameEl = card.querySelector(".participant-name");
        const selectedName = nameEl?.textContent?.trim() || `ID ${id}`;
        if (selectedLabelEl) selectedLabelEl.textContent = `Selected: ${selectedName}`;
    });
}

async function loadParticipantProfiles() {
    // Load once, then reuse map for history mode checks and rendering.
    if (!scheduleProfiles && !pairProfiles && !historyProfiles) return;
    try {
        const trainees = await withLoading(() => api.listTrainees({ page: 0, size: 500 }));
        traineesById.clear();
        trainees.forEach((t) => traineesById.set(Number(t.id), t));
        const cardHtml = trainees.map((t) => `
            <button type="button" class="participant-card" data-participant-id="${t.id}" aria-pressed="false">
                <span class="participant-avatar">
                    ${t.photoPath ? `<img src="${escapeHtml(t.photoPath)}" alt="${escapeHtml(t.name)} photo">` : escapeHtml(initialsOf(t.name))}
                </span>
                <span>
                    <span class="participant-name">${escapeHtml(t.name)}</span>
                    <span class="participant-id">ID: ${t.id}</span>
                </span>
            </button>
        `).join("");
        if (scheduleProfiles) scheduleProfiles.innerHTML = cardHtml || "<div>No trainees found.</div>";
        if (pairProfiles) pairProfiles.innerHTML = cardHtml || "<div>No trainees found.</div>";
        if (historyProfiles) historyProfiles.innerHTML = cardHtml || "<div>No trainees found.</div>";
        syncSelectedIds(scheduleForm, scheduleSelectedIds, scheduleSelectedCount);
        syncSelectedIds(pairForm, pairSelectedIds, pairSelectedCount);
        if (historySelectedLabel) historySelectedLabel.textContent = "Selected: none";
    } catch (err) {
        showMessage(msg, err.message, false);
    }
}

function renderMatches(data, filters = {}) {
    // Offline/F2F history table rendering.
    const filterDate = String(filters.dateFilter ?? "").trim();
    const filterFormat = String(filters.formatFilter ?? "ALL").toUpperCase();
    const normalized = data.map((m) => ({
        raw: m,
        date: String(m?.scheduledDate ?? ""),
        format: String(m?.format ?? "").toUpperCase()
    }));
    const filtered = applyHistoryFilters(normalized, filterDate, filterFormat);
    const rows = filtered.map(({ raw: m }, index) => {
        const offlineRef = buildOfflineMatchRefRow(m);
        const key = `${offlineRef.refText}#${m?.matchId ?? index}`;
        offlineMatchesByRef.set(key, { displayRef: offlineRef.refText, match: m });
        return `
            <tr>
                <td>${offlineRef.html}</td>
                <td>${escapeHtml(m.scheduledDate ?? "")}</td>
                <td>${escapeHtml(m.format ?? "")}</td>
                <td>${escapeHtml(m.whitePlayer ?? "")}</td>
                <td>${escapeHtml(m.blackPlayer ?? "")}</td>
                <td>-</td>
                <td>${escapeHtml(m.result ?? "")}</td>
            </tr>
        `;
    }).join("");
    fillTableBody(tbody, rows);
    refreshResultMatchOptions();
}

function formatUnixDate(seconds) {
    if (!Number.isFinite(seconds) || seconds <= 0) return "";
    return new Date(seconds * 1000).toISOString().slice(0, 10);
}

function onlineOutcome(playerResult) {
    const value = String(playerResult ?? "").toLowerCase();
    const drawResults = new Set([
        "agreed", "repetition", "stalemate", "insufficient", "50move", "timevsinsufficient"
    ]);
    if (value === "win") return "Win";
    if (drawResults.has(value)) return "Draw";
    if (!value) return "";
    return "Loss";
}

function renderOnlineMatches(data, username, filters = {}) {
    // Online/Chess.com history table rendering.
    const groupedGames = data?.groupedGames ?? {};
    const groups = ["rapid", "blitz", "bullet", "daily", "other"];
    const normalizedUsername = String(username ?? "").toLowerCase();
    const games = groups.flatMap((group) => Array.isArray(groupedGames[group]) ? groupedGames[group] : []);
    const rows = games
        .sort((a, b) => Number(b?.end_time ?? 0) - Number(a?.end_time ?? 0))
        .map((g) => ({
            game: g,
            date: formatUnixDate(Number(g?.end_time ?? 0)),
            format: String(g?.time_class ?? "").toUpperCase()
        }));
    const filterDate = String(filters.dateFilter ?? "").trim();
    const filterFormat = String(filters.formatFilter ?? "ALL").toUpperCase();
    const filtered = applyHistoryFilters(rows, filterDate, filterFormat);
    const rowHtml = filtered.map(({ game: g }) => {
        const whiteUser = g?.white?.username ?? "";
        const blackUser = g?.black?.username ?? "";
        const whiteResult = g?.white?.result ?? "";
        const blackResult = g?.black?.result ?? "";
        const playerIsWhite = String(whiteUser).toLowerCase() === normalizedUsername;
        const playerResult = playerIsWhite ? whiteResult : blackResult;
        const detailsLink = g?.url ? `<a href="${escapeHtml(g.url)}" target="_blank" rel="noopener">View</a>` : "";
        const ecoText = String(g?.eco ?? "").trim();
        // Chess.com ECO URL tail is used as opening name when available.
        const opening = ecoText
            ? decodeURIComponent(ecoText.split("/").pop() || "").replaceAll("-", " ")
            : "";
        const displayId = `${formatUnixDate(Number(g?.end_time ?? 0))}-${whiteUser}(white)-vs-${blackUser}(black)`;
        return `
            <tr>
                <td>${escapeHtml(displayId)}</td>
                <td>${formatUnixDate(Number(g?.end_time ?? 0))}</td>
                <td>${escapeHtml(String(g?.time_class ?? "").toUpperCase())}</td>
                <td>${escapeHtml(whiteUser)} (${escapeHtml(whiteResult)})</td>
                <td>${escapeHtml(blackUser)} (${escapeHtml(blackResult)})</td>
                <td>${escapeHtml(opening || "-")}</td>
                <td>${onlineOutcome(playerResult)} ${detailsLink}</td>
            </tr>
        `;
    }).join("");
    fillTableBody(tbody, rowHtml);
}

scheduleForm?.addEventListener("submit", async (e) => {
    // Create one scheduled match using selected participants.
    e.preventDefault();
    clearMessage(msg);
    const payload = Object.fromEntries(new FormData(scheduleForm).entries());
    payload.traineeIds = parseIds(payload.traineeIds);
    if (payload.traineeIds.length === 0) {
        showMessage(msg, "Select at least one participant profile.", false);
        return;
    }
    try {
        const data = await withLoading(() => api.createMatch(payload));
        renderMatches([data]);
        showMessage(msg, "Match schedule created.");
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});

pairForm?.addEventListener("submit", async (e) => {
    // Generate pairings (Swiss or Round Robin) from selected participants.
    e.preventDefault();
    clearMessage(msg);
    const payload = Object.fromEntries(new FormData(pairForm).entries());
    payload.roundNumber = Number(payload.roundNumber);
    payload.traineeIds = parseIds(payload.traineeIds);
    if (payload.traineeIds.length === 0) {
        showMessage(msg, "Select at least one participant profile.", false);
        return;
    }
    try {
        const data = await withLoading(() => (
            payload.format === "SWISS"
                ? api.generateSwiss(payload)
                : api.generateRoundRobin(payload)
        ));
        renderMatches(data);
        showMessage(msg, `${payload.format} pairings generated.`);
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});

resultForm?.addEventListener("submit", async (e) => {
    // Submit result to backend using hidden matchId resolved from selected match ref.
    e.preventDefault();
    const payload = Object.fromEntries(new FormData(resultForm).entries());
    payload.matchId = Number(payload.matchId);
    payload.whiteTraineeId = Number(payload.whiteTraineeId);
    payload.blackTraineeId = Number(payload.blackTraineeId);
    payload.whiteScore = Number(payload.whiteScore);
    payload.blackScore = Number(payload.blackScore);
    if (!Number.isFinite(payload.matchId) || payload.matchId <= 0) {
        showMessage(msg, "Select a valid loaded F2F match first.", false);
        return;
    }
    try {
        await withLoading(() => api.submitMatchResult(payload));
        showMessage(msg, "Match result recorded.");
        resultForm.reset();
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});

historyForm?.addEventListener("submit", async (e) => {
    // History mode switch: OFFLINE uses local matches, ONLINE uses Chess.com API proxy.
    e.preventDefault();
    clearMessage(msg);
    const historyData = new FormData(historyForm);
    const traineeId = Number(historyData.get("traineeId"));
    const historyMode = String(historyData.get("historyMode") ?? "OFFLINE").toUpperCase();
    const dateFilter = String(historyData.get("dateFilter") ?? "").trim();
    const formatFilter = String(historyData.get("formatFilter") ?? "ALL").toUpperCase();
    const filters = { dateFilter, formatFilter };
    if (!Number.isFinite(traineeId) || traineeId <= 0) {
        showMessage(msg, "Select one trainee profile to load history.", false);
        return;
    }
    try {
        if (historyMode === "ONLINE") {
            const trainee = traineesById.get(traineeId);
            const username = trainee?.chessUsername;
            if (!username) {
                showMessage(msg, "Selected trainee has no Chess.com username.", false);
                return;
            }
            const data = await withLoading(() => api.getChessComAllModeHistory(username, 6));
            renderOnlineMatches(data, username, filters);
            showMessage(msg, `Loaded Chess.com history for ${username}.`);
            return;
        }
        const data = await withLoading(() => api.getMatchHistory(traineeId));
        renderMatches(data, filters);
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});

resultMatchSelect?.addEventListener("change", () => {
    // On match ref selection, hydrate hidden matchId + player IDs for result submission.
    const key = String(resultMatchSelect.value ?? "");
    const match = offlineMatchesByRef.get(key)?.match;
    const hiddenIdInput = resultForm?.querySelector('input[name="matchId"]');
    const whiteInput = resultForm?.querySelector('input[name="whiteTraineeId"]');
    const blackInput = resultForm?.querySelector('input[name="blackTraineeId"]');
    if (hiddenIdInput) hiddenIdInput.value = match?.matchId ? String(match.matchId) : "";
    if (whiteInput) whiteInput.value = match?.whiteTraineeId ? String(match.whiteTraineeId) : "";
    if (blackInput) blackInput.value = match?.blackTraineeId ? String(match.blackTraineeId) : "";
});

bindParticipantPicker(scheduleProfiles, scheduleForm, scheduleSelectedIds, scheduleSelectedCount);
bindParticipantPicker(pairProfiles, pairForm, pairSelectedIds, pairSelectedCount);
bindSingleParticipantPicker(historyProfiles, historyForm, historySelectedLabel);
loadParticipantProfiles();

