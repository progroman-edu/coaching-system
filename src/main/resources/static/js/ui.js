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
