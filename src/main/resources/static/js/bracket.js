import { api } from "./api.js";
import { clearMessage, escapeHtml, showMessage } from "./ui.js";

const msg = document.getElementById("msg");
const matchSelect = document.getElementById("matchSelect");
const loadBracketBtn = document.getElementById("loadBracketBtn");
const refreshBtn = document.getElementById("refreshBtn");
const rollbackBtn = document.getElementById("rollbackBtn");
const bracketContainer = document.getElementById("bracketContainer");
const bracketScroll = document.getElementById("bracketScroll");
const noBracketMsg = document.getElementById("noBracketMsg");
const matchDetailsContainer = document.getElementById("matchDetailsContainer");
const detailFormat = document.getElementById("detailFormat");
const detailParticipants = document.getElementById("detailParticipants");
const detailRound = document.getElementById("detailRound");
const detailTotalRounds = document.getElementById("detailTotalRounds");
const prevRoundBtn = document.getElementById("prevRoundBtn");
const nextRoundBtn = document.getElementById("nextRoundBtn");
const currentRoundLabel = document.getElementById("currentRoundLabel");
const EDIT_RESULTS_KEY = "allowMatchResultEditing";
const SELECTED_GROUP_KEY = "tournamentBracketSelectedGroup";
const CURRENT_ROUND_KEY = "tournamentBracketCurrentRound";

let tournamentGroups = [];
let selectedGroupKey = readSelectedGroupKey();
let currentRoundView = readCurrentRoundView();

function readSelectedGroupKey() {
    try {
        return sessionStorage.getItem(SELECTED_GROUP_KEY) || "";
    } catch {
        return "";
    }
}

function saveSelectedGroupKey() {
    try {
        if (selectedGroupKey) {
            sessionStorage.setItem(SELECTED_GROUP_KEY, selectedGroupKey);
        } else {
            sessionStorage.removeItem(SELECTED_GROUP_KEY);
        }
    } catch {
        // The bracket still works without persisted selection.
    }
}

function readCurrentRoundView() {
    try {
        const saved = sessionStorage.getItem(CURRENT_ROUND_KEY);
        return saved ? Number(saved) : 1;
    } catch {
        return 1;
    }
}

function saveCurrentRoundView() {
    try {
        sessionStorage.setItem(CURRENT_ROUND_KEY, String(currentRoundView));
    } catch {
        // The bracket still works without persisted round view.
    }
}

function normalize(text) {
    return String(text ?? "").trim();
}

function normalizeFormat(format) {
    return normalize(format).toUpperCase() || "MATCH";
}

function cssToken(value) {
    return normalize(value).toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-+|-+$/g, "") || "scheduled";
}

function scoreText(value) {
    const number = Number(value ?? 0);
    if (!Number.isFinite(number)) return "0";
    return Number.isInteger(number) ? String(number) : number.toFixed(1).replace(/\.0$/, "");
}

function calculateExpectedRounds(format, participantCount) {
    if (participantCount <= 1) return 1;
    if (format === "SWISS") {
        return Math.max(Math.ceil(Math.log(participantCount) / Math.log(2)), 2);
    }
    if (format === "ROUND_ROBIN") {
        return Math.max(participantCount - 1, 1);
    }
    return 1;
}

function normalizeResultType(resultType) {
    const value = normalize(resultType).toUpperCase();
    if (value === "WHITE_WIN" || value === "1-0") return "WHITE_WIN";
    if (value === "BLACK_WIN" || value === "0-1") return "BLACK_WIN";
    if (value === "DRAW" || value === "0.5-0.5" || value === "1/2-1/2") return "DRAW";
    return "";
}

function scoresForResultType(resultType) {
    if (resultType === "WHITE_WIN") {
        return { whiteScore: 1.0, blackScore: 0.0 };
    }
    if (resultType === "BLACK_WIN") {
        return { whiteScore: 0.0, blackScore: 1.0 };
    }
    if (resultType === "DRAW") {
        return { whiteScore: 0.5, blackScore: 0.5 };
    }
    return null;
}

function isByeMatch(match) {
    const result = normalize(match?.result).toUpperCase();
    const blackName = normalize(match?.blackPlayer).toUpperCase();
    return result === "BYE" || blackName === "BYE";
}

function isCancelledMatch(match) {
    return normalize(match?.status).toUpperCase() === "CANCELLED";
}

function isCompletedMatch(match) {
    if (isByeMatch(match)) return true;
    if (normalizeResultType(match?.result)) return true;
    return normalize(match?.status).toUpperCase() === "COMPLETED";
}

function isPendingMatch(match) {
    if (isCancelledMatch(match) || isCompletedMatch(match)) return false;
    return normalize(match?.status).toUpperCase() === "SCHEDULED";
}

function resultDisplayText(match) {
    if (isByeMatch(match)) return "BYE";
    const result = normalizeResultType(match?.result);
    if (result === "WHITE_WIN") return "1-0";
    if (result === "BLACK_WIN") return "0-1";
    if (result === "DRAW") return "0.5-0.5";
    const raw = normalize(match?.result).toUpperCase();
    if (raw === "ROLLED_BACK") return "Rolled back";
    if (raw === "CANCELLED") return "Cancelled";
    return raw ? raw.replaceAll("_", " ") : "Pending";
}

function winnerSide(match) {
    const result = normalizeResultType(match?.result);
    if (result === "WHITE_WIN") return "white";
    if (result === "BLACK_WIN") return "black";
    if (result === "DRAW") return "draw";
    return "";
}

function playerResultClass(side, winner) {
    if (winner === "draw") return "draw";
    if (!winner) return "";
    return side === winner ? "winner" : "loss";
}

function statusLabelForMatch(match) {
    if (isByeMatch(match)) return "BYE";
    return normalize(match?.status).toUpperCase() || "SCHEDULED";
}

function statusClassForMatch(match) {
    if (isByeMatch(match)) return "bye";
    return cssToken(statusLabelForMatch(match));
}

function getEditResultsSetting() {
    return localStorage.getItem(EDIT_RESULTS_KEY) === "1";
}

function groupLabel(group) {
    const stats = computeTournamentStats(group);
    return `${group.format} · ${stats.generatedRounds}/${stats.expectedRounds} rounds · ${stats.totalBoards} board${stats.totalBoards === 1 ? "" : "s"}`;
}

function addParticipant(participants, id, name) {
    const numericId = Number(id);
    if (!Number.isFinite(numericId) || numericId <= 0) return;
    if (!participants.has(numericId)) {
        participants.set(numericId, normalize(name) || `Player ${numericId}`);
    }
}

function buildTournamentGroups(matches) {
    const groupsMap = new Map();
    matches.forEach((match) => {
        const format = normalizeFormat(match?.format);
        const key = format;
        if (!groupsMap.has(key)) {
            groupsMap.set(key, {
                key,
                format,
                matches: [],
                rounds: new Map()
            });
        }
        const group = groupsMap.get(key);
        const roundNumber = Math.max(1, Number(match?.roundNumber ?? 1) || 1);
        group.matches.push(match);
        if (!group.rounds.has(roundNumber)) {
            group.rounds.set(roundNumber, []);
        }
        group.rounds.get(roundNumber).push(match);
    });

    return Array.from(groupsMap.values()).map((group) => {
        group.matches.sort((a, b) => {
            const roundDelta = Number(a?.roundNumber ?? 0) - Number(b?.roundNumber ?? 0);
            if (roundDelta !== 0) return roundDelta;
            const dateDelta = normalize(a?.scheduledDate).localeCompare(normalize(b?.scheduledDate));
            if (dateDelta !== 0) return dateDelta;
            return Number(a?.matchId ?? 0) - Number(b?.matchId ?? 0);
        });
        group.rounds.forEach((roundMatches) => {
            roundMatches.sort((a, b) => Number(a?.matchId ?? 0) - Number(b?.matchId ?? 0));
        });
        return group;
    }).sort((a, b) => a.format.localeCompare(b.format));
}

function computeTournamentStats(group) {
    const participants = new Map();
    group.matches.forEach((match) => {
        addParticipant(participants, match?.whitePlayerId, match?.whitePlayer);
        if (!isByeMatch(match)) {
            addParticipant(participants, match?.blackPlayerId, match?.blackPlayer);
        }
    });

    const maxGeneratedRound = Array.from(group.rounds.keys()).reduce((max, round) => Math.max(max, round), 0);
    const expectedRounds = Math.max(maxGeneratedRound, calculateExpectedRounds(group.format, participants.size));
    const projectedRounds = Array.from({ length: expectedRounds }, (_, index) => {
        const roundNumber = index + 1;
        const matches = group.rounds.get(roundNumber) ?? [];
        const completed = matches.filter(isCompletedMatch).length;
        const cancelled = matches.filter(isCancelledMatch).length;
        const pending = matches.filter(isPendingMatch).length;
        let status = "PROJECTED";
        if (matches.length > 0) {
            if (cancelled > 0) {
                status = "CANCELLED";
            } else if (pending > 0) {
                status = "IN_PROGRESS";
            } else {
                status = "COMPLETED";
            }
        }
        return { roundNumber, matches, completed, cancelled, pending, status };
    });

    const totalBoards = group.matches.length;
    const completedBoards = group.matches.filter(isCompletedMatch).length;
    const cancelledBoards = group.matches.filter(isCancelledMatch).length;
    const pendingBoards = group.matches.filter(isPendingMatch).length;
    const generatedRounds = Array.from(group.rounds.keys()).length;
    const currentPendingRound = projectedRounds.find((round) => round.pending > 0);
    const firstProjectedRound = projectedRounds.find((round) => round.matches.length === 0);
    const currentRound = currentPendingRound?.roundNumber
        ?? firstProjectedRound?.roundNumber
        ?? maxGeneratedRound
        ?? 1;
    const complete = totalBoards > 0
        && maxGeneratedRound >= expectedRounds
        && pendingBoards === 0
        && cancelledBoards === 0
        && projectedRounds.every((round) => round.matches.length > 0 && round.pending === 0 && round.cancelled === 0);

    return {
        participants,
        participantCount: participants.size,
        expectedRounds,
        generatedRounds,
        maxGeneratedRound,
        currentRound,
        projectedRounds,
        totalBoards,
        completedBoards,
        cancelledBoards,
        pendingBoards,
        complete
    };
}

function buildStandings(group) {
    const stats = computeTournamentStats(group);
    const standings = new Map();
    stats.participants.forEach((name, id) => {
        standings.set(id, {
            id,
            name,
            score: 0,
            buchholz: 0,
            wins: 0,
            draws: 0,
            losses: 0,
            byes: 0,
            played: 0,
            opponents: new Set()
        });
    });

    group.matches.forEach((match) => {
        const whiteId = Number(match?.whitePlayerId);
        const blackId = Number(match?.blackPlayerId);
        const whiteRow = standings.get(whiteId);
        const blackRow = standings.get(blackId);
        if (!whiteRow) return;

        if (isByeMatch(match)) {
            whiteRow.score += 1;
            whiteRow.byes += 1;
            return;
        }
        if (!blackRow || !isCompletedMatch(match) || isCancelledMatch(match)) {
            return;
        }

        whiteRow.played += 1;
        blackRow.played += 1;
        whiteRow.opponents.add(blackId);
        blackRow.opponents.add(whiteId);

        const result = normalizeResultType(match?.result);
        if (result === "WHITE_WIN") {
            whiteRow.score += 1;
            whiteRow.wins += 1;
            blackRow.losses += 1;
        } else if (result === "BLACK_WIN") {
            blackRow.score += 1;
            blackRow.wins += 1;
            whiteRow.losses += 1;
        } else if (result === "DRAW") {
            whiteRow.score += 0.5;
            blackRow.score += 0.5;
            whiteRow.draws += 1;
            blackRow.draws += 1;
        }
    });

    standings.forEach((row) => {
        row.buchholz = Array.from(row.opponents)
            .reduce((total, opponentId) => total + (standings.get(opponentId)?.score ?? 0), 0);
    });

    const rows = Array.from(standings.values()).sort((a, b) => {
        if (b.score !== a.score) return b.score - a.score;
        if (b.buchholz !== a.buchholz) return b.buchholz - a.buchholz;
        if (b.wins !== a.wins) return b.wins - a.wins;
        if (b.draws !== a.draws) return b.draws - a.draws;
        return a.name.localeCompare(b.name);
    });

    let previous = null;
    rows.forEach((row, index) => {
        const tied = previous
            && row.score === previous.score
            && row.buchholz === previous.buchholz
            && row.wins === previous.wins
            && row.draws === previous.draws
            && row.losses === previous.losses;
        row.rank = tied ? previous.rank : index + 1;
        previous = row;
    });
    return rows;
}

function renderGroupOptions() {
    if (!matchSelect) return;
    const options = ["<option value=''>Select a tournament</option>"];
    tournamentGroups.forEach((group) => {
        options.push(`<option value="${escapeHtml(group.key)}">${escapeHtml(groupLabel(group))}</option>`);
    });
    matchSelect.innerHTML = options.join("");
    if (!selectedGroupKey && tournamentGroups.length) {
        selectedGroupKey = tournamentGroups[0].key;
    }
    if (selectedGroupKey) {
        matchSelect.value = selectedGroupKey;
    }
    saveSelectedGroupKey();
}

function renderSummaryCards(stats) {
    return `
        <div class="bracket-summary">
            <div class="bracket-summary-card"><span>Players</span><strong>${escapeHtml(String(stats.participantCount))}</strong></div>
            <div class="bracket-summary-card"><span>Rounds</span><strong>${escapeHtml(`${stats.generatedRounds}/${stats.expectedRounds}`)}</strong></div>
            <div class="bracket-summary-card"><span>Completed</span><strong>${escapeHtml(`${stats.completedBoards}/${stats.totalBoards}`)}</strong></div>
            <div class="bracket-summary-card"><span>Pending</span><strong>${escapeHtml(String(stats.pendingBoards))}</strong></div>
        </div>
    `;
}

function renderRoundProjectionTable(stats) {
    const rows = stats.projectedRounds.map((round) => `
        <tr>
            <td><strong>Round ${escapeHtml(String(round.roundNumber))}</strong></td>
            <td><span class="bracket-status-chip ${escapeHtml(cssToken(round.status))}">${escapeHtml(round.status.replaceAll("_", " "))}</span></td>
            <td>${round.matches.length ? escapeHtml(String(round.matches.length)) : "Projected"}</td>
            <td>${round.matches.length ? escapeHtml(`${round.completed}/${round.matches.length}`) : "-"}</td>
            <td>${round.pending ? escapeHtml(String(round.pending)) : "-"}</td>
        </tr>
    `).join("");
    return `
        <section class="bracket-section">
            <div class="bracket-section-head">
                <h3>All Rounds</h3>
                <span>${escapeHtml(String(stats.expectedRounds))} projected</span>
            </div>
            <div class="bracket-table-wrap">
                <table class="bracket-round-table">
                    <thead>
                        <tr>
                            <th>Round</th>
                            <th>Status</th>
                            <th>Boards</th>
                            <th>Results</th>
                            <th>Open</th>
                        </tr>
                    </thead>
                    <tbody>${rows}</tbody>
                </table>
            </div>
        </section>
    `;
}

function renderMatchRow(match, index) {
    const whiteName = normalize(match?.whitePlayer) || "-";
    const blackName = normalize(match?.blackPlayer) || "BYE";
    const bye = isByeMatch(match);
    const winner = winnerSide(match);
    const statusLabel = statusLabelForMatch(match);
    const completed = isCompletedMatch(match);
    const cancelled = isCancelledMatch(match);
    const matchId = Number(match?.matchId);
    const canMutate = Number.isFinite(matchId) && matchId > 0 && !cancelled;
    const editEnabled = getEditResultsSetting() && completed && !bye && !cancelled;
    const rollbackLabel = completed && !bye ? "Undo" : "Cancel";

    return `
        <tr>
            <td class="bracket-board-cell">Board ${escapeHtml(String(index + 1))}</td>
            <td>
                <span class="bracket-player-name ${escapeHtml(playerResultClass("white", winner))}">${escapeHtml(whiteName)}</span>
                <span class="bracket-player-side">White</span>
            </td>
            <td>
                <span class="bracket-player-name ${escapeHtml(bye ? "bye" : playerResultClass("black", winner))}">${escapeHtml(bye ? "BYE" : blackName)}</span>
                <span class="bracket-player-side">Black</span>
            </td>
            <td>
                <span class="bracket-result-chip ${escapeHtml(statusClassForMatch(match))}">${escapeHtml(resultDisplayText(match))}</span>
                <span class="bracket-status-meta">${escapeHtml(statusLabel)}</span>
            </td>
            <td class="bracket-actions-cell">
                ${canMutate ? `<button type="button" class="danger compact" data-rollback-match-id="${escapeHtml(String(matchId))}">${escapeHtml(rollbackLabel)}</button>` : ""}
                ${editEnabled ? `<button type="button" class="secondary compact" data-edit-match-id="${escapeHtml(String(matchId))}">Edit</button>` : ""}
            </td>
        </tr>
    `;
}

function renderRoundBoard(round) {
    if (!round.matches.length) {
        return `
            <article class="bracket-round-card projected">
                <div class="bracket-round-card-head">
                    <h3>Round ${escapeHtml(String(round.roundNumber))}</h3>
                    <span class="bracket-status-chip projected">Projected</span>
                </div>
                <div class="bracket-empty-state">Pairings not generated yet.</div>
            </article>
        `;
    }

    return `
        <article class="bracket-round-card">
            <div class="bracket-round-card-head">
                <h3>Round ${escapeHtml(String(round.roundNumber))}</h3>
                <span class="bracket-status-chip ${escapeHtml(cssToken(round.status))}">${escapeHtml(round.status.replaceAll("_", " "))}</span>
            </div>
            <div class="bracket-board-table-wrap">
                <table class="bracket-board-table">
                    <thead>
                        <tr>
                            <th>Board</th>
                            <th>White</th>
                            <th>Black</th>
                            <th>Result</th>
                            <th></th>
                        </tr>
                    </thead>
                    <tbody>${round.matches.map((match, index) => renderMatchRow(match, index)).join("")}</tbody>
                </table>
            </div>
        </article>
    `;
}

function renderRoundBoards(stats) {
    const currentRound = Math.min(Math.max(currentRoundView, 1), stats.expectedRounds);
    const round = stats.projectedRounds[currentRound - 1];

    if (!round) {
        return `
            <section class="bracket-section">
                <div class="bracket-section-head">
                    <h3>Round Boards</h3>
                    <span>${escapeHtml(String(stats.totalBoards))} total boards</span>
                </div>
                <div class="bracket-empty-state">No rounds available.</div>
            </section>
        `;
    }

    return `
        <section class="bracket-section">
            <div class="bracket-section-head">
                <h3>Round Boards</h3>
                <span>${escapeHtml(String(stats.totalBoards))} total boards</span>
            </div>
            <div class="bracket-rounds-board">
                ${renderRoundBoard(round)}
            </div>
        </section>
    `;
}

function renderFinalResults(group, stats) {
    const rows = buildStandings(group).map((row) => `
        <tr>
            <td><span class="bracket-rank rank-${escapeHtml(String(Math.min(row.rank, 3)))}">${escapeHtml(String(row.rank))}</span></td>
            <td><strong>${escapeHtml(row.name)}</strong></td>
            <td>${escapeHtml(scoreText(row.score))}</td>
            <td>${escapeHtml(`${row.wins}-${row.draws}-${row.losses}`)}</td>
            <td>${escapeHtml(scoreText(row.buchholz))}</td>
            <td>${escapeHtml(String(row.byes))}</td>
        </tr>
    `).join("");

    if (!stats.complete) {
        const remainingRounds = stats.projectedRounds.filter((round) => round.matches.length === 0).length;
        const remainingText = remainingRounds > 0
            ? `${remainingRounds} round${remainingRounds === 1 ? "" : "s"} not generated`
            : `${stats.pendingBoards} board${stats.pendingBoards === 1 ? "" : "s"} open`;
        return `
            <section class="bracket-section final-results muted">
                <div class="bracket-section-head">
                    <h3>Final Results</h3>
                    <span>${escapeHtml(remainingText)}</span>
                </div>
                <div class="bracket-empty-state">Final standings appear after every projected round has a result.</div>
            </section>
        `;
    }

    return `
        <section class="bracket-section final-results complete">
            <div class="bracket-section-head">
                <h3>Final Results</h3>
                <span class="bracket-status-chip completed">Complete</span>
            </div>
            <div class="bracket-table-wrap">
                <table class="bracket-final-table">
                    <thead>
                        <tr>
                            <th>Rank</th>
                            <th>Player</th>
                            <th>Score</th>
                            <th>W-D-L</th>
                            <th>Buchholz</th>
                            <th>Byes</th>
                        </tr>
                    </thead>
                    <tbody>${rows}</tbody>
                </table>
            </div>
        </section>
    `;
}

function updateRoundNavigation(stats) {
    if (!prevRoundBtn || !nextRoundBtn || !currentRoundLabel) return;

    const totalRounds = stats.expectedRounds;
    const currentRound = Math.min(Math.max(currentRoundView, 1), totalRounds);

    currentRoundLabel.textContent = `Round ${currentRound} of ${totalRounds}`;
    prevRoundBtn.disabled = currentRound <= 1;
    nextRoundBtn.disabled = currentRound >= totalRounds;
}

function goToPreviousRound() {
    if (currentRoundView > 1) {
        currentRoundView--;
        saveCurrentRoundView();
        const group = tournamentGroups.find((item) => item.key === selectedGroupKey);
        if (group) {
            const stats = computeTournamentStats(group);
            updateRoundNavigation(stats);
            bracketScroll.innerHTML = `
                <div class="bracket-shell">
                    <div class="bracket-title-row">
                        <div>
                            <h2>${escapeHtml(group.format)} Tournament</h2>
                            <p>${escapeHtml(groupLabel(group))}</p>
                        </div>
                        <span class="bracket-status-chip ${escapeHtml(cssToken(stats.complete ? "COMPLETED" : stats.totalBoards > 0 ? "IN_PROGRESS" : "SCHEDULED"))}">${escapeHtml((stats.complete ? "COMPLETED" : stats.totalBoards > 0 ? "IN_PROGRESS" : "SCHEDULED").replaceAll("_", " "))}</span>
                    </div>
                    ${renderSummaryCards(stats)}
                    ${renderRoundBoards(stats)}
                </div>
            `;
        }
    }
}

function goToNextRound() {
    const group = tournamentGroups.find((item) => item.key === selectedGroupKey);
    if (group) {
        const stats = computeTournamentStats(group);
        if (currentRoundView < stats.expectedRounds) {
            currentRoundView++;
            saveCurrentRoundView();
            updateRoundNavigation(stats);
            bracketScroll.innerHTML = `
                <div class="bracket-shell">
                    <div class="bracket-title-row">
                        <div>
                            <h2>${escapeHtml(group.format)} Tournament</h2>
                            <p>${escapeHtml(groupLabel(group))}</p>
                        </div>
                        <span class="bracket-status-chip ${escapeHtml(cssToken(stats.complete ? "COMPLETED" : stats.totalBoards > 0 ? "IN_PROGRESS" : "SCHEDULED"))}">${escapeHtml((stats.complete ? "COMPLETED" : stats.totalBoards > 0 ? "IN_PROGRESS" : "SCHEDULED").replaceAll("_", " "))}</span>
                    </div>
                    ${renderSummaryCards(stats)}
                    ${renderRoundBoards(stats)}
                </div>
            `;
        }
    }
}

function renderSelectedGroup() {
    if (!bracketContainer || !bracketScroll || !noBracketMsg || !matchDetailsContainer) return;
    const group = tournamentGroups.find((item) => item.key === selectedGroupKey);
    if (!group) {
        bracketContainer.style.display = "none";
        noBracketMsg.style.display = "block";
        matchDetailsContainer.style.display = "none";
        if (rollbackBtn) rollbackBtn.disabled = true;
        return;
    }

    const stats = computeTournamentStats(group);
    bracketContainer.style.display = "block";
    noBracketMsg.style.display = "none";
    matchDetailsContainer.style.display = "block";
    if (rollbackBtn) rollbackBtn.disabled = false;

    // Reset current round view if it's out of bounds
    if (currentRoundView > stats.expectedRounds || currentRoundView < 1) {
        currentRoundView = 1;
        saveCurrentRoundView();
    }

    if (detailFormat) detailFormat.textContent = group.format;
    if (detailParticipants) detailParticipants.textContent = String(stats.participantCount);
    if (detailRound) detailRound.textContent = String(stats.currentRound);
    if (detailTotalRounds) detailTotalRounds.textContent = String(stats.expectedRounds);

    updateRoundNavigation(stats);

    const tournamentStatus = stats.complete
        ? "COMPLETED"
        : stats.totalBoards > 0
            ? "IN_PROGRESS"
            : "SCHEDULED";

    bracketScroll.innerHTML = `
        <div class="bracket-shell">
            <div class="bracket-title-row">
                <div>
                    <h2>${escapeHtml(group.format)} Tournament</h2>
                    <p>${escapeHtml(groupLabel(group))}</p>
                </div>
                <span class="bracket-status-chip ${escapeHtml(cssToken(tournamentStatus))}">${escapeHtml(tournamentStatus.replaceAll("_", " "))}</span>
            </div>
            ${renderSummaryCards(stats)}
            ${renderRoundBoards(stats)}
            ${renderFinalResults(group, stats)}
        </div>
    `;
}

async function loadMatches(options = {}) {
    if (options.clearMessage !== false) {
        clearMessage(msg);
    }
    const preferredFormat = normalizeFormat(options.preferredFormat);
    if (preferredFormat && preferredFormat !== "MATCH") {
        selectedGroupKey = preferredFormat;
    }
    try {
        const matches = await api.listMatches();
        tournamentGroups = buildTournamentGroups(matches);
        if (selectedGroupKey && !tournamentGroups.some((group) => group.key === selectedGroupKey)) {
            selectedGroupKey = "";
        }
        renderGroupOptions();
        saveSelectedGroupKey();
        renderSelectedGroup();
    } catch (error) {
        showMessage(msg, error.message, false);
    }
}

async function handleRollback(matchId) {
    if (!Number.isFinite(matchId) || matchId <= 0) return;
    if (!confirm("Cancel this board or undo its recorded result?")) {
        return;
    }
    try {
        await api.rollbackMatch(matchId);
        showMessage(msg, "Board updated.");
        await loadMatches();
    } catch (error) {
        showMessage(msg, error.message, false);
    }
}

async function promptEditMatchResult(matchId) {
    const match = tournamentGroups
        .flatMap((group) => group.matches)
        .find((item) => Number(item?.matchId) === Number(matchId));
    if (!match) return;
    if (!isCompletedMatch(match) || isByeMatch(match)) {
        showMessage(msg, "Only completed played boards can be edited.", false);
        return;
    }

    const currentResult = normalizeResultType(match?.result) || "WHITE_WIN";
    const nextResult = window.prompt("Enter new result (WHITE_WIN, BLACK_WIN, DRAW)", currentResult);
    if (nextResult === null) {
        return;
    }

    const normalizedResult = normalizeResultType(nextResult);
    const scorePayload = scoresForResultType(normalizedResult);
    if (!scorePayload) {
        showMessage(msg, "Enter WHITE_WIN, BLACK_WIN, or DRAW.", false);
        return;
    }

    try {
        await api.rollbackMatch(matchId);
        await api.submitMatchResult({ matchId, ...scorePayload });
        showMessage(msg, "Match result updated.");
        await loadMatches();
    } catch (error) {
        try {
            await loadMatches();
        } catch {
            // Keep the edit error visible if refresh also fails.
        }
        showMessage(msg, error.message, false);
    }
}

matchSelect?.addEventListener("change", () => {
    selectedGroupKey = String(matchSelect.value ?? "");
    saveSelectedGroupKey();
    renderSelectedGroup();
});

loadBracketBtn?.addEventListener("click", () => {
    selectedGroupKey = String(matchSelect?.value ?? "");
    saveSelectedGroupKey();
    renderSelectedGroup();
});

refreshBtn?.addEventListener("click", () => {
    void loadMatches();
});

prevRoundBtn?.addEventListener("click", () => {
    goToPreviousRound();
});

nextRoundBtn?.addEventListener("click", () => {
    goToNextRound();
});

rollbackBtn?.addEventListener("click", () => {
    const group = tournamentGroups.find((item) => item.key === selectedGroupKey);
    const match = group?.matches?.find((item) => Number(item?.matchId) > 0 && !isCancelledMatch(item));
    void handleRollback(Number(match?.matchId));
});

bracketScroll?.addEventListener("click", (event) => {
    const button = event.target.closest("[data-rollback-match-id]");
    if (!button) return;
    const matchId = Number(button.dataset.rollbackMatchId);
    void handleRollback(matchId);
});

bracketScroll?.addEventListener("click", (event) => {
    const button = event.target.closest("[data-edit-match-id]");
    if (!button) return;
    const matchId = Number(button.dataset.editMatchId);
    void promptEditMatchResult(matchId);
});

void loadMatches();

window.addEventListener("app:tournament-generated", (event) => {
    const format = normalizeFormat(event.detail?.format);
    void loadMatches({ clearMessage: false, preferredFormat: format });
});

window.addEventListener("app:tournament-bracket-tab-opened", () => {
    if (!tournamentGroups.length) {
        void loadMatches({ clearMessage: false });
    }
});

window.addEventListener("app:settings-changed", () => {
    renderSelectedGroup();
});
