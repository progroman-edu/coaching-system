// This file powers match creation, pairing generation, and result submission interactions.
import { api } from "./api.js";
import { fillTableBody, showMessage } from "./ui.js";

const msg = document.getElementById("msg");
const scheduleForm = document.getElementById("scheduleForm");
const pairForm = document.getElementById("pairForm");
const resultForm = document.getElementById("resultForm");
const historyForm = document.getElementById("historyForm");
const tbody = document.getElementById("matchRows");

function parseIds(idsText) {
    return idsText
        .split(",")
        .map((x) => Number(x.trim()))
        .filter((x) => Number.isFinite(x));
}

function renderMatches(data) {
    const rows = data.map((m) => `
        <tr>
            <td>${m.matchId ?? ""}</td>
            <td>${m.scheduledDate ?? ""}</td>
            <td>${m.format ?? ""}</td>
            <td>${m.whitePlayer ?? ""}</td>
            <td>${m.blackPlayer ?? ""}</td>
            <td>${m.result ?? ""}</td>
        </tr>
    `).join("");
    fillTableBody(tbody, rows);
}

scheduleForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = Object.fromEntries(new FormData(scheduleForm).entries());
    payload.traineeIds = parseIds(payload.traineeIds);
    try {
        const data = await api.createMatch(payload);
        renderMatches([data]);
        showMessage(msg, "Match schedule created.");
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});

pairForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = Object.fromEntries(new FormData(pairForm).entries());
    payload.roundNumber = Number(payload.roundNumber);
    payload.traineeIds = parseIds(payload.traineeIds);
    try {
        const data = payload.format === "SWISS"
            ? await api.generateSwiss(payload)
            : await api.generateRoundRobin(payload);
        renderMatches(data);
        showMessage(msg, `${payload.format} pairings generated.`);
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});

resultForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = Object.fromEntries(new FormData(resultForm).entries());
    payload.matchId = Number(payload.matchId);
    payload.whiteTraineeId = Number(payload.whiteTraineeId);
    payload.blackTraineeId = Number(payload.blackTraineeId);
    payload.whiteScore = Number(payload.whiteScore);
    payload.blackScore = Number(payload.blackScore);
    try {
        await api.submitMatchResult(payload);
        showMessage(msg, "Match result recorded.");
        resultForm.reset();
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});

historyForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const traineeId = Number(new FormData(historyForm).get("traineeId"));
    try {
        const data = await api.getMatchHistory(traineeId);
        renderMatches(data);
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});

