// This file powers dashboard analytics interactions on the frontend.
import { api } from "./api.js";
import { showMessage } from "./ui.js";

const msg = document.getElementById("msg");
const totalTrainees = document.getElementById("totalTrainees");
const avgRating = document.getElementById("avgRating");
const attendancePct = document.getElementById("attendancePct");
const matchesPlayed = document.getElementById("matchesPlayed");
const trendForm = document.getElementById("trendForm");
const trendTraineeId = document.getElementById("trendTraineeId");
const trendCustomDateBtn = document.getElementById("trendCustomDateBtn");
const trendWeekStartDate = document.getElementById("trendWeekStartDate");
const trendMeta = document.getElementById("trendMeta");
const trendChartRapid = document.getElementById("trendChartRapid");
const trendChartBlitz = document.getElementById("trendChartBlitz");
const trendChartBullet = document.getElementById("trendChartBullet");
const traineeById = new Map();
let customDateEnabled = false;

function renderTrendEmpty(chartEl, message) {
    if (!chartEl) {
        return;
    }
    chartEl.innerHTML = `<div class="trend-empty">${message}</div>`;
}

function renderAllTrendEmpty(message) {
    renderTrendEmpty(trendChartRapid, message);
    renderTrendEmpty(trendChartBlitz, message);
    renderTrendEmpty(trendChartBullet, message);
}

function formatTrendDate(value) {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return "N/A";
    }
    return date.toLocaleDateString();
}

function toIsoDate(dateValue) {
    const date = new Date(dateValue);
    if (Number.isNaN(date.getTime())) {
        return "";
    }
    return date.toISOString().slice(0, 10);
}

function addDaysToIsoDate(isoDate, daysToAdd) {
    const date = new Date(`${isoDate}T00:00:00`);
    if (Number.isNaN(date.getTime())) {
        return "";
    }
    date.setDate(date.getDate() + daysToAdd);
    return toIsoDate(date);
}

function todayIsoDate() {
    return toIsoDate(new Date());
}

function defaultLast7DayRange() {
    const endDate = todayIsoDate();
    const startDate = addDaysToIsoDate(endDate, -6);
    return { startDate, endDate };
}

function getDateRangeFilter() {
    if (!customDateEnabled) {
        return defaultLast7DayRange();
    }

    const customStartDate = String(trendWeekStartDate?.value || "").trim();
    if (!customStartDate) {
        return defaultLast7DayRange();
    }

    const weekEndDate = addDaysToIsoDate(customStartDate, 6);
    const endDate = weekEndDate && weekEndDate < todayIsoDate() ? weekEndDate : todayIsoDate();
    return {
        startDate: customStartDate,
        endDate
    };
}

function inDateRange(isoDate, range) {
    if (!isoDate) {
        return false;
    }
    if (range.startDate && isoDate < range.startDate) {
        return false;
    }
    if (range.endDate && isoDate > range.endDate) {
        return false;
    }
    return true;
}

function buildOnlineTrendPoints(games, username, range) {
    const normalizedUsername = String(username || "").trim().toLowerCase();
    const rows = (Array.isArray(games) ? games : [])
        .map((game) => {
            const endTime = Number(game?.end_time ?? 0);
            if (!Number.isFinite(endTime) || endTime <= 0) {
                return null;
            }
            const isoDate = toIsoDate(endTime * 1000);
            if (!inDateRange(isoDate, range)) {
                return null;
            }
            const whiteUser = String(game?.white?.username ?? "").toLowerCase();
            const blackUser = String(game?.black?.username ?? "").toLowerCase();
            const isWhite = whiteUser === normalizedUsername;
            const isBlack = blackUser === normalizedUsername;
            if (!isWhite && !isBlack) {
                return null;
            }
            const rating = Number(isWhite ? game?.white?.rating : game?.black?.rating);
            if (!Number.isFinite(rating) || rating <= 0) {
                return null;
            }
            return {
                endTime,
                timestamp: new Date(endTime * 1000).toISOString(),
                rating
            };
        })
        .filter(Boolean)
        .sort((a, b) => a.endTime - b.endTime);

    return rows.map((row, index) => {
        const previous = index > 0 ? rows[index - 1].rating : row.rating;
        return {
            timestamp: row.timestamp,
            rating: row.rating,
            change: row.rating - previous
        };
    });
}

async function loadOnlineRatingTrend(trainee, range) {
    const username = String(trainee?.chessUsername || "").trim();
    if (!username) {
        throw new Error("Selected trainee has no Chess.com username.");
    }
    const history = await api.getChessComAllModeHistory(username, 12);
    const groupedGames = history?.groupedGames ?? {};

    const byMode = {
        rapid: buildOnlineTrendPoints(Array.isArray(groupedGames.rapid) ? groupedGames.rapid : [], username, range),
        blitz: buildOnlineTrendPoints(Array.isArray(groupedGames.blitz) ? groupedGames.blitz : [], username, range),
        bullet: buildOnlineTrendPoints(Array.isArray(groupedGames.bullet) ? groupedGames.bullet : [], username, range)
    };

    renderTrendChart(trendChartRapid, byMode.rapid);
    renderTrendChart(trendChartBlitz, byMode.blitz);
    renderTrendChart(trendChartBullet, byMode.bullet);

    if (!trendMeta) {
        return;
    }

    const rangeLabel = `${range.startDate} to ${range.endDate}`;
    const summary = ["rapid", "blitz", "bullet"].map((mode) => {
        const points = byMode[mode];
        if (!points.length) {
            return `${mode}: no games`;
        }
        const latest = points[points.length - 1];
        const delta = Number(latest.change ?? 0);
        const sign = delta > 0 ? "+" : "";
        return `${mode}: ${latest.rating} (${sign}${delta})`;
    }).join(" | ");

    trendMeta.textContent = `${trainee.name}: online trend for ${rangeLabel}. ${summary}.`;
}

function renderTrendChart(chartEl, points) {
    if (!chartEl) {
        return;
    }
    if (!Array.isArray(points) || points.length === 0) {
        renderTrendEmpty(chartEl, "No rating changes yet for this mode.");
        return;
    }

    const width = 760;
    const height = 280;
    const padding = { top: 16, right: 16, bottom: 36, left: 52 };

    const ratings = points.map((point) => Number(point.rating ?? 0));
    const minRating = Math.min(...ratings);
    const maxRating = Math.max(...ratings);
    const range = Math.max(1, maxRating - minRating);

    const xForIndex = (index) => {
        if (points.length === 1) {
            return (width - padding.left - padding.right) / 2 + padding.left;
        }
        return padding.left + (index / (points.length - 1)) * (width - padding.left - padding.right);
    };

    const yForRating = (rating) => {
        const normalized = (rating - minRating) / range;
        return height - padding.bottom - normalized * (height - padding.top - padding.bottom);
    };

    const polyline = points
        .map((point, index) => `${xForIndex(index)},${yForRating(Number(point.rating ?? 0))}`)
        .join(" ");

    const yTicks = 4;
    const yGrid = Array.from({ length: yTicks + 1 }, (_, idx) => {
        const ratio = idx / yTicks;
        const value = Math.round(maxRating - ratio * range);
        const y = padding.top + ratio * (height - padding.top - padding.bottom);
        return { y, value };
    });

    const labelIndices = [0, Math.floor((points.length - 1) / 2), points.length - 1]
        .filter((value, index, arr) => arr.indexOf(value) === index);

    const circles = points.map((point, index) => {
        const cx = xForIndex(index);
        const cy = yForRating(Number(point.rating ?? 0));
        const tooltip = `${formatTrendDate(point.timestamp)} | Rating ${point.rating} (${Number(point.change) >= 0 ? "+" : ""}${point.change})`;
        return `<circle cx="${cx}" cy="${cy}" r="3" class="trend-point"><title>${tooltip}</title></circle>`;
    }).join("");

    const yGridLines = yGrid.map((tick) => `
        <line x1="${padding.left}" y1="${tick.y}" x2="${width - padding.right}" y2="${tick.y}" class="trend-grid" />
        <text x="${padding.left - 8}" y="${tick.y + 4}" text-anchor="end" class="trend-axis-label">${tick.value}</text>
    `).join("");

    const xLabels = labelIndices.map((index) => {
        const x = xForIndex(index);
        return `<text x="${x}" y="${height - 12}" text-anchor="middle" class="trend-axis-label">${formatTrendDate(points[index].timestamp)}</text>`;
    }).join("");

    chartEl.innerHTML = `
        <svg viewBox="0 0 ${width} ${height}" class="trend-svg" preserveAspectRatio="none" role="presentation">
            ${yGridLines}
            <line x1="${padding.left}" y1="${height - padding.bottom}" x2="${width - padding.right}" y2="${height - padding.bottom}" class="trend-axis" />
            <line x1="${padding.left}" y1="${padding.top}" x2="${padding.left}" y2="${height - padding.bottom}" class="trend-axis" />
            <polyline points="${polyline}" class="trend-line" />
            ${circles}
            ${xLabels}
        </svg>
    `;
}

async function loadRatingTrend(traineeId, traineeName) {
    if (!traineeId) {
        renderAllTrendEmpty("Select a trainee to view rating changes over time.");
        if (trendMeta) {
            trendMeta.textContent = "Select a trainee to view rating changes over time.";
        }
        return;
    }

    const range = getDateRangeFilter();
    const trainee = traineeById.get(Number(traineeId));
    if (trainee?.chessUsername) {
        await loadOnlineRatingTrend(trainee ?? { name: traineeName, chessUsername: "" }, range);
        return;
    }

    const data = await api.getRatingTrend(traineeId);
    renderTrendChart(trendChartRapid, data);
    renderTrendEmpty(trendChartBlitz, "Online Blitz history unavailable for this trainee.");
    renderTrendEmpty(trendChartBullet, "Online Bullet history unavailable for this trainee.");

    if (!trendMeta) {
        return;
    }
    if (!data.length) {
        trendMeta.textContent = `${traineeName} has no local rating history yet.`;
        return;
    }
    const latest = data[data.length - 1];
    const delta = Number(latest.change ?? 0);
    const sign = delta > 0 ? "+" : "";
    trendMeta.textContent = `${traineeName}: local rapid trend ${latest.rating} (${sign}${delta}) across ${data.length} recorded changes (online blitz/bullet unavailable).`;
}

async function loadTrendTrainees() {
    if (!trendTraineeId) {
        return;
    }
    const trainees = await api.listTrainees({ size: 200, rankingOrder: "asc" });
    trendTraineeId.innerHTML = "";

    const placeholderOption = document.createElement("option");
    placeholderOption.value = "";
    placeholderOption.textContent = "Select trainee";
    trendTraineeId.appendChild(placeholderOption);

    traineeById.clear();
    for (const trainee of trainees) {
        const id = Number(trainee.id);
        const name = trainee.name ?? "Trainee";
        traineeById.set(id, trainee);
        const option = document.createElement("option");
        option.value = String(id);
        option.textContent = name;
        trendTraineeId.appendChild(option);
    }

    if (trainees.length) {
        const first = trainees[0];
        trendTraineeId.value = String(first.id);
        await loadRatingTrend(Number(first.id), first.name ?? "Trainee");
    } else {
        renderAllTrendEmpty("No trainees found. Add trainees to view rating changes.");
        if (trendMeta) {
            trendMeta.textContent = "No trainees found. Add trainees to view rating changes.";
        }
    }
}

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
loadTrendTrainees().catch((err) => {
    renderAllTrendEmpty("Unable to load trainees for rating graph.");
    showMessage(msg, err.message, false);
});

if (trendWeekStartDate) {
    trendWeekStartDate.max = todayIsoDate();
}

trendCustomDateBtn?.addEventListener("click", () => {
    customDateEnabled = !customDateEnabled;
    if (trendWeekStartDate) {
        trendWeekStartDate.style.display = customDateEnabled ? "" : "none";
        if (!customDateEnabled) {
            trendWeekStartDate.value = "";
        }
    }
    if (trendCustomDateBtn) {
        trendCustomDateBtn.textContent = customDateEnabled ? "Use Last 7 Days" : "Custom Date";
    }
});

trendForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
        const traineeId = Number(trendTraineeId.value);
        const selectedText = trendTraineeId.options[trendTraineeId.selectedIndex]?.textContent || "Selected trainee";
        const traineeName = selectedText;
        await loadRatingTrend(traineeId, traineeName);
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});

