// This file powers report export and trainee CSV import interactions.
import { api } from "./api.js";
import { showMessage } from "./ui.js";

const msg = document.getElementById("msg");
const exportForm = document.getElementById("exportForm");
const importForm = document.getElementById("importForm");
const output = document.getElementById("output");
const downloadWrap = document.getElementById("downloadWrap");
const downloadLink = document.getElementById("downloadLink");

function clearDownloadLink() {
    if (downloadLink) {
        downloadLink.setAttribute("href", "#");
    }
    if (downloadWrap) {
        downloadWrap.style.display = "none";
    }
}

function setDownloadLink(path) {
    if (!downloadLink || !downloadWrap) {
        return;
    }
    const href = String(path ?? "").trim();
    if (!href) {
        clearDownloadLink();
        return;
    }
    downloadLink.setAttribute("href", href);
    downloadWrap.style.display = "block";
}

exportForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = Object.fromEntries(new FormData(exportForm).entries());
    clearDownloadLink();
    try {
        const data = await api.exportReport(payload.type, payload.format);
        output.textContent = JSON.stringify(data, null, 2);
        if (data?.downloadPath) {
            setDownloadLink(data.downloadPath);
        }
        showMessage(msg, "Export metadata generated.");
    } catch (err) {
        clearDownloadLink();
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
        showMessage(msg, "Import processed.");
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});

clearDownloadLink();

