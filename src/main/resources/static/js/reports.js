// This file powers report export and trainee CSV import interactions.
import { api } from "./api.js";
import { showMessage } from "./ui.js";

const msg = document.getElementById("msg");
const exportForm = document.getElementById("exportForm");
const importForm = document.getElementById("importForm");
const output = document.getElementById("output");
const downloadBox = document.getElementById("downloadBox");

function renderDownloadLink(data) {
    if (!downloadBox) return;
    downloadBox.innerHTML = "";
    const fileName = data?.fileName;
    const downloadPath = data?.downloadPath;
    if (!fileName && !downloadPath) {
        return;
    }
    const url = downloadPath || api.reportDownloadUrl(fileName);
    const link = document.createElement("a");
    link.href = url;
    link.textContent = `Download ${fileName || "report"}`;
    link.className = "secondary";
    link.setAttribute("download", fileName || "");
    downloadBox.appendChild(link);
}

exportForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = Object.fromEntries(new FormData(exportForm).entries());
    try {
        const data = await api.exportReport(payload.type, payload.format);
        output.textContent = JSON.stringify(data, null, 2);
        renderDownloadLink(data);
        showMessage(msg, "Export metadata generated.");
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});

importForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const file = new FormData(importForm).get("file");
    if (!file || file.size === 0) {
        showMessage(msg, "Please select a file.", false);
        return;
    }
    try {
        const data = await api.importTrainees(file);
        output.textContent = JSON.stringify(data, null, 2);
        if (downloadBox) downloadBox.innerHTML = "";
        showMessage(msg, "Import processed.");
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});

