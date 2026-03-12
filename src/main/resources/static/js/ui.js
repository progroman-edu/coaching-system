// This file provides shared frontend UI helper functions.
export function showMessage(el, text, ok = true) {
    if (!el) return;
    el.className = `msg ${ok ? "ok" : "err"}`;
    el.textContent = text;
    el.style.display = "block";
}

export function clearMessage(el) {
    if (!el) return;
    el.style.display = "none";
    el.textContent = "";
}

export function fillTableBody(tbody, rowsHtml) {
    if (!tbody) return;
    tbody.innerHTML = rowsHtml || "<tr><td colspan='20'>No data</td></tr>";
}

export function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
}

