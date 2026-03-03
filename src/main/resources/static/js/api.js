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
    getPerformance: (traineeId) => request(`/analytics/performance/${traineeId}`),
    getRatingTrend: (traineeId) => request(`/analytics/rating-trend/${traineeId}`),

    listTrainees: (params = {}) => {
        const query = new URLSearchParams(params);
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

