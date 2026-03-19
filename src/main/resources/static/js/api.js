// This file contains reusable fetch-based API client helpers for frontend pages.
const API_BASE = "/api";

async function request(path, options = {}) {
    const response = await fetch(`${API_BASE}${path}`, {
        headers: {
            ...(!(options.body instanceof FormData) ? { "Content-Type": "application/json" } : {}),
            ...(options.headers || {})
        },
        ...options
    });
    const json = await response.json().catch(() => ({}));
    if (!response.ok || json.success === false) {
        const message = json.message || `Request failed (${response.status})`;
        throw new Error(message);
    }
    return json.data;
}

export const api = {
    getDashboard: () => request("/analytics/dashboard"),
    getRatingTrend: (traineeId) => request(`/analytics/rating-trend/${traineeId}`),
    getChessComAllModeHistory: (username, limitArchives) =>
        request(`/chesscom/${encodeURIComponent(username)}/match-history/all-modes${limitArchives ? `?limitArchives=${encodeURIComponent(limitArchives)}` : ""}`),
    syncTraineeChessComRating: (traineeId, mode = "rapid") =>
        request(`/chesscom/trainees/${traineeId}/sync-rating?mode=${encodeURIComponent(mode)}`, { method: "POST" }),

    listTrainees: (params = {}) => {
        const query = new URLSearchParams();
        Object.entries(params).forEach(([key, value]) => {
            if (value === undefined || value === null || value === "") {
                return;
            }
            query.set(key, String(value));
        });
        return request(`/trainees?${query.toString()}`);
    },
    createTrainee: (payload) => request("/trainees", { method: "POST", body: JSON.stringify(payload) }),
    updateTrainee: (id, payload) => request(`/trainees/${id}`, { method: "PUT", body: JSON.stringify(payload) }),
    deleteTrainee: (id) => request(`/trainees/${id}`, { method: "DELETE" }),
    uploadTraineePhoto: (id, file) => {
        const formData = new FormData();
        formData.append("file", file);
        return request(`/trainees/${id}/photo`, { method: "POST", body: formData });
    },

    recordAttendance: (payload) => request("/attendance", { method: "POST", body: JSON.stringify(payload) }),
    attendanceReport: (startDate, endDate, traineeId) => {
        const params = new URLSearchParams({ startDate, endDate, ...(traineeId ? { traineeId } : {}) });
        return request(`/attendance/report?${params.toString()}`);
    },

    createMatch: (payload) => request("/matches", { method: "POST", body: JSON.stringify(payload) }),
    generateSwiss: (payload) => request("/matches/generate/swiss", { method: "POST", body: JSON.stringify(payload) }),
    generateRoundRobin: (payload) => request("/matches/generate/round-robin", { method: "POST", body: JSON.stringify(payload) }),
    submitMatchResult: (payload) => request("/matches/result", { method: "POST", body: JSON.stringify(payload) }),
    getMatchHistory: (traineeId) => request(`/matches/history/${traineeId}`),
    getTraineeRatingHistory: (traineeId) => request(`/matches/history/${traineeId}/ratings`),

    exportReport: (type, format) => {
        const params = new URLSearchParams({ type, format });
        return request(`/reports/export?${params.toString()}`);
    },
    importTrainees: (file) => {
        const formData = new FormData();
        formData.append("file", file);
        return request("/reports/import/trainees", { method: "POST", body: formData });
    }
};

