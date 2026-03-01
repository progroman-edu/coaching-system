import { api } from "./api.js";
import { showMessage } from "./ui.js";

const msg = document.getElementById("msg");
const totalTrainees = document.getElementById("totalTrainees");
const avgRating = document.getElementById("avgRating");
const attendancePct = document.getElementById("attendancePct");
const matchesPlayed = document.getElementById("matchesPlayed");
const trendForm = document.getElementById("trendForm");
const trendTraineeId = document.getElementById("trendTraineeId");
const trendOutput = document.getElementById("trendOutput");

async function loadDashboard() {
    try {
        const data = await api.getDashboard();
        totalTrainees.textContent = data.totalTrainees ?? 0;
        avgRating.textContent = Number(data.averageRating ?? 0).toFixed(2);
        attendancePct.textContent = `${Number(data.attendancePercentage ?? 0).toFixed(1)}%`;
        matchesPlayed.textContent = data.matchesPlayed ?? 0;
    } catch (err) {
        showMessage(msg, err.message, false);
    }
}

loadDashboard();

trendForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
        const data = await api.getRatingTrend(Number(trendTraineeId.value));
        trendOutput.textContent = JSON.stringify(data, null, 2);
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});
