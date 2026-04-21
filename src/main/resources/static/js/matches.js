// This file powers match creation, pairing generation, and result submission interactions.
import { api } from "./api.js";
import { clearMessage, escapeHtml, fillTableBody, showMessage } from "./ui.js";

const msg = document.getElementById("msg");
const scheduleForm = document.getElementById("scheduleForm");
const pairForm = document.getElementById("pairForm");
const resultForm = document.getElementById("resultForm");
const historyForm = document.getElementById("historyForm");
const tbody = document.getElementById("matchRows");
const ratingTbody = document.getElementById("ratingRows");
const scheduleProfiles = document.getElementById("scheduleProfiles");
const pairProfiles = document.getElementById("pairProfiles");
const historyProfiles = document.getElementById("historyProfiles");
const scheduleSelectedCount = document.getElementById("scheduleSelectedCount");
const pairSelectedCount = document.getElementById("pairSelectedCount");
const historySelectedLabel = document.getElementById("historySelectedLabel");
const resultMatchSelect = resultForm?.querySelector('select[name="matchRef"]');
const resultTypeSelect = resultForm?.querySelector('select[name="resultType"]');
const swissBracket = document.getElementById("swissBracket");
const scheduleDateInput = scheduleForm?.querySelector('input[name="scheduledDate"]');
const historyModeButtons = document.querySelectorAll("#historyModeTabs [data-history-mode]");
const historyModeInput = historyForm?.querySelector('input[name="historyMode"]');
const scheduleSelectedIds = new Set();
const pairSelectedIds = new Set();
const traineesById = new Map();
const offlineMatchesByRef = new Map();
let latestSwissPairings = [];

function parseIds(idsText) {
    return idsText
        .split(",")
        .map((x) => Number(x.trim()))
        .filter((x) => Number.isFinite(x));
}

function toLocalIsoDate(date) {
    const localTime = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
    return localTime.toISOString().slice(0, 10);
}

function isFutureIsoDate(value) {
    const dateText = String(value ?? "").trim();
    if (!dateText) return false;
    return dateText > toLocalIsoDate(new Date());
}

function initialsOf(name) {
    const parts = String(name ?? "").trim().split(/\s+/).filter(Boolean);
    if (parts.length === 0) return "?";
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
    return (parts[0][0] + parts[1][0]).toUpperCase();
}

function setLoading(active) {
    // Global loader is managed centrally in api.js.
}

async function withLoading(task) {
    setLoading(true);
    try {
        return await task();
    } finally {
        setLoading(false);
    }
}

function shortDate(value) {
    const text = String(value ?? "");
    const parts = text.split("-");
    if (parts.length !== 3) return text;
    const mm = parts[1]?.padStart(2, "0");
    const dd = parts[2]?.padStart(2, "0");
    const yy = parts[0]?.slice(-2);
    return `${mm}/${dd}/${yy}`;
}

function winnerSide(resultText) {
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

function initHistoryModeTabs() {
    historyModeButtons.forEach((button) => {
        button.addEventListener("click", () => {
            const mode = String(button.dataset.historyMode ?? "").trim().toUpperCase();
            if (!mode) return;
            if (historyModeInput) historyModeInput.value = mode;
            historyModeButtons.forEach((item) => {
                const isActive = item === button;
                item.classList.toggle("active", isActive);
                item.setAttribute("aria-selected", isActive ? "true" : "false");
            });
        });
    });
}

function initScheduleDateInput() {
    if (!scheduleDateInput) return;
    const todayIso = toLocalIsoDate(new Date());
    scheduleDateInput.setAttribute("max", todayIso);
    if (!scheduleDateInput.value) {
        scheduleDateInput.value = todayIso;
    }
}

function refreshResultMatchOptions() {
    if (!resultMatchSelect) return;
    const options = ['<option value="">Select a loaded F2F match</option>'];
    for (const [key, entry] of offlineMatchesByRef.entries()) {
        const label = `${entry.displayRef} | ${entry.match.result ?? "PENDING"}`;
        options.push(`<option value="${escapeHtml(key)}">${escapeHtml(label)}</option>`);
    }
    resultMatchSelect.innerHTML = options.join("");
}

function findOfflineMatchEntryById(matchId) {
    for (const [key, entry] of offlineMatchesByRef.entries()) {
        if (Number(entry?.match?.matchId) === Number(matchId)) {
            return { key, entry };
        }
    }
    return null;
}

function updateCachedResult(matchId, resultText) {
    const result = String(resultText ?? "");
    const hit = findOfflineMatchEntryById(matchId);
    if (hit?.entry?.match) {
        hit.entry.match.result = result;
    }
    latestSwissPairings = latestSwissPairings.map((pairing) => (
        Number(pairing?.matchId) === Number(matchId)
            ? { ...pairing, result }
            : pairing
    ));
}

function resultDisplayText(resultText) {
    const value = String(resultText ?? "").trim().toUpperCase();
    if (!value) return "PENDING";
    if (value === "WHITE_WIN") return "1-0";
    if (value === "BLACK_WIN") return "0-1";
    if (value === "DRAW") return "0.5-0.5";
    return value;
}

function resultChipClass(resultText) {
    const side = winnerSide(resultText);
    if (side === "white") return "win";
    if (side === "black") return "loss";
    if (side === "draw") return "draw";
    return "";
}

function renderSwissBracket() {
    if (!swissBracket) return;
    if (!latestSwissPairings.length) {
        swissBracket.classList.add("empty");
        swissBracket.innerHTML = '<div class="swiss-empty">Generate SWISS pairings to show the bracket.</div>';
        return;
    }
    swissBracket.classList.remove("empty");
    const roundLabel = latestSwissPairings[0]?.roundNumber ? `Round ${latestSwissPairings[0].roundNumber}` : "Swiss Pairings";
    const cards = latestSwissPairings.map((match, index) => {
        const winner = winnerSide(match?.result);
        const whiteClass = winner === "draw" ? "draw" : (winner === "white" ? "white-win" : (winner === "black" ? "white-loss" : ""));
        const blackClass = winner === "draw" ? "draw" : (winner === "black" ? "black-win" : (winner === "white" ? "black-loss" : ""));
        const blackName = String(match?.blackPlayer ?? "").trim();
        const bye = !blackName || blackName.toUpperCase() === "BYE";
        return `
            <article class="swiss-match-card">
                <div class="swiss-match-head">
                    <span>Board ${index + 1}</span>
                    <span class="swiss-result-chip ${resultChipClass(match?.result)}">${escapeHtml(resultDisplayText(match?.result))}</span>
                </div>
                <button
                    type="button"
                    class="swiss-player ${whiteClass}"
                    data-match-id="${escapeHtml(match?.matchId ?? "")}"
                    data-player-side="white"
                    ${bye ? "disabled" : ""}>${escapeHtml(match?.whitePlayer ?? "-")}</button>
                <div class="swiss-vs">VS</div>
                <button
                    type="button"
                    class="swiss-player ${bye ? "bye" : blackClass}"
                    data-match-id="${escapeHtml(match?.matchId ?? "")}"
                    data-player-side="black"
                    ${bye ? "disabled" : ""}>${escapeHtml(bye ? "BYE" : blackName)}</button>
            </article>
        `;
    }).join("");
    swissBracket.innerHTML = `
        <div class="swiss-round">
            <div class="swiss-round-title">${escapeHtml(roundLabel)}</div>
            <div class="swiss-grid">${cards}</div>
        </div>
    `;
}

async function submitBracketWinner(matchId, winner) {
    if (!Number.isFinite(matchId) || matchId <= 0) return;
    const found = latestSwissPairings.find((match) => Number(match?.matchId) === Number(matchId));
    if (!found) {
        showMessage(msg, "Select a valid loaded match first.", false);
        return;
    }
    if (String(found?.result ?? "").toUpperCase() === "BYE" || String(found?.blackPlayer ?? "").toUpperCase() === "BYE") {
        showMessage(msg, "This pairing is a bye and does not require a result.", false);
        return;
    }
    if (winnerSide(found?.result)) {
        showMessage(msg, "Result already recorded for this pairing.", false);
        return;
    }
    const payload = {
        matchId,
        whiteScore: winner === "white" ? 1.0 : 0.0,
        blackScore: winner === "black" ? 1.0 : 0.0
    };
    try {
        await withLoading(() => api.submitMatchResult(payload));
        updateCachedResult(matchId, winner === "white" ? "WHITE_WIN" : "BLACK_WIN");
        refreshResultMatchOptions();
        renderSwissBracket();
        const selected = findOfflineMatchEntryById(matchId);
        if (resultMatchSelect && selected) {
            resultMatchSelect.value = selected.key;
        }
        const hiddenIdInput = resultForm?.querySelector('input[name="matchId"]');
        if (hiddenIdInput) hiddenIdInput.value = String(matchId);
        if (resultTypeSelect) resultTypeSelect.value = winner === "white" ? "WHITE_WIN" : "BLACK_WIN";
        showMessage(msg, "Match result recorded.");
    } catch (err) {
        showMessage(msg, err.message, false);
    }
}

function syncSelectedIds(form, selectedIds, countEl) {
    const input = form?.querySelector('input[name="traineeIds"]');
    const values = [...selectedIds];
    if (input) input.value = values.join(",");
    if (countEl) countEl.textContent = `Selected: ${values.length}`;
}

function bindParticipantPicker(container, form, selectedIds, countEl) {
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

function formatDateTime(value) {
    const date = new Date(String(value ?? ""));
    if (Number.isNaN(date.getTime())) {
        return "";
    }
    return date.toLocaleString();
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

function safeHttpUrl(value) {
    try {
        const url = new URL(String(value ?? ""), window.location.origin);
        return url.protocol === "http:" || url.protocol === "https:" ? url.href : "";
    } catch {
        return "";
    }
}

function renderOnlineMatches(data, username, filters = {}) {
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
        const detailsUrl = safeHttpUrl(g?.url);
        const detailsLink = detailsUrl ? `<a href="${escapeHtml(detailsUrl)}" target="_blank" rel="noopener">View</a>` : "";
        const ecoText = String(g?.eco ?? "").trim();
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

function renderRatingHistory(rows) {
    const html = (rows ?? []).map((entry) => `
        <tr>
            <td>${escapeHtml(entry?.id ?? "")}</td>
            <td>${escapeHtml(entry?.traineeId ?? "")}</td>
            <td>${escapeHtml(entry?.matchHistoryId ?? "")}</td>
            <td>${escapeHtml(entry?.oldRating ?? "")}</td>
            <td>${escapeHtml(entry?.newRating ?? "")}</td>
            <td>${escapeHtml(formatDateTime(entry?.createdAt))}</td>
            <td>${escapeHtml(formatDateTime(entry?.updatedAt))}</td>
        </tr>
    `).join("");
    fillTableBody(ratingTbody, html);
}

scheduleForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    clearMessage(msg);
    const payload = Object.fromEntries(new FormData(scheduleForm).entries());
    payload.traineeIds = parseIds(payload.traineeIds);
    if (isFutureIsoDate(payload.scheduledDate)) {
        showMessage(msg, "Scheduled date cannot be in the future.", false);
        return;
    }
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
    e.preventDefault();
    clearMessage(msg);
    const payload = Object.fromEntries(new FormData(pairForm).entries());
    const selectedFormat = String(payload.format ?? "SWISS");
    payload.roundNumber = Number(payload.roundNumber);
    payload.traineeIds = parseIds(payload.traineeIds);
    delete payload.format;
    if (payload.traineeIds.length === 0) {
        showMessage(msg, "Select at least one participant profile.", false);
        return;
    }
    try {
        const data = await withLoading(() => (
            selectedFormat === "SWISS"
                ? api.generateSwiss(payload)
                : api.generateRoundRobin(payload)
        ));
        renderMatches(data);
        latestSwissPairings = selectedFormat === "SWISS" ? [...data] : [];
        renderSwissBracket();
        showMessage(msg, `${selectedFormat} pairings generated.`);
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});

resultForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = Object.fromEntries(new FormData(resultForm).entries());
    payload.matchId = Number(payload.matchId);
    const resultType = String(payload.resultType ?? "");
    if (resultType === "WHITE_WIN") {
        payload.whiteScore = 1.0;
        payload.blackScore = 0.0;
    } else if (resultType === "BLACK_WIN") {
        payload.whiteScore = 0.0;
        payload.blackScore = 1.0;
    } else if (resultType === "DRAW") {
        payload.whiteScore = 0.5;
        payload.blackScore = 0.5;
    } else {
        showMessage(msg, "Select a valid result.", false);
        return;
    }
    delete payload.resultType;
    if (!Number.isFinite(payload.matchId) || payload.matchId <= 0) {
        showMessage(msg, "Select a valid loaded F2F match first.", false);
        return;
    }
    try {
        await withLoading(() => api.submitMatchResult(payload));
        updateCachedResult(payload.matchId, resultType);
        refreshResultMatchOptions();
        renderSwissBracket();
        showMessage(msg, "Match result recorded.");
        if (resultTypeSelect) resultTypeSelect.value = "";
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});

historyForm?.addEventListener("submit", async (e) => {
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
        const ratingHistoryPromise = api.getTraineeRatingHistory(traineeId);
        if (historyMode === "ONLINE") {
            const trainee = traineesById.get(traineeId);
            const username = trainee?.chessUsername;
            if (!username) {
                showMessage(msg, "Selected trainee has no Chess.com username.", false);
                return;
            }
            const [data, ratingHistory] = await withLoading(() => Promise.all([
                api.getChessComAllModeHistory(username, 6),
                ratingHistoryPromise
            ]));
            renderOnlineMatches(data, username, filters);
            renderRatingHistory(ratingHistory);
            showMessage(msg, `Loaded Chess.com history for ${username}.`);
            return;
        }
        const [data, ratingHistory] = await withLoading(() => Promise.all([
            api.getMatchHistory(traineeId),
            ratingHistoryPromise
        ]));
        renderMatches(data, filters);
        renderRatingHistory(ratingHistory);
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});

resultMatchSelect?.addEventListener("change", () => {
    const key = String(resultMatchSelect.value ?? "");
    const match = offlineMatchesByRef.get(key)?.match;
    const hiddenIdInput = resultForm?.querySelector('input[name="matchId"]');
    if (hiddenIdInput) hiddenIdInput.value = match?.matchId ? String(match.matchId) : "";
    if (!resultTypeSelect) return;
    const side = winnerSide(match?.result);
    if (side === "white") resultTypeSelect.value = "WHITE_WIN";
    else if (side === "black") resultTypeSelect.value = "BLACK_WIN";
    else if (side === "draw") resultTypeSelect.value = "DRAW";
    else resultTypeSelect.value = "";
});

swissBracket?.addEventListener("dblclick", (e) => {
    const playerButton = e.target.closest("[data-match-id][data-player-side]");
    if (!playerButton) return;
    const side = String(playerButton.dataset.playerSide ?? "").toLowerCase();
    if (side !== "white" && side !== "black") return;
    const matchId = Number(playerButton.dataset.matchId);
    void submitBracketWinner(matchId, side);
});

bindParticipantPicker(scheduleProfiles, scheduleForm, scheduleSelectedIds, scheduleSelectedCount);
bindParticipantPicker(pairProfiles, pairForm, pairSelectedIds, pairSelectedCount);
bindSingleParticipantPicker(historyProfiles, historyForm, historySelectedLabel);
initHistoryModeTabs();
initScheduleDateInput();
renderSwissBracket();
loadParticipantProfiles();
