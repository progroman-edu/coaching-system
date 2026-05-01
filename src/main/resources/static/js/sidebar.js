// This file handles sidebar state, theme mode, nav icons, and sidebar settings.
const layout = document.querySelector(".layout");
const sidebar = document.querySelector(".sidebar");
const toggleBtn = document.getElementById("sidebarToggle");
const root = document.documentElement;

const STATE_KEY = "sidebarOpen";
const THEME_KEY = "themeMode";
const DELETE_KEY = "allowPlayerDeletion";

const NAV_ICON_MAP = {
    "/index.html": "/icons/dsahboard.png",
    "/trainees.html": "/icons/users.png",
    "/register.html": "/icons/register.png",
    "/leaderboard.html": "/icons/leaderboard.png",
    "/matches.html": "/icons/tournament.png",
    "/bracket.html": "/icons/tournament.png",
    "/match-history.html": "/icons/history.png",
    "/attendance.html": "/icons/attendance.png",
    "/reports.html": "/icons/report.png"
};

function getStoredTheme() {
    const stored = localStorage.getItem(THEME_KEY);
    if (stored === "light" || stored === "dark") {
        return stored;
    }
    return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

function getDeleteSetting() {
    return localStorage.getItem(DELETE_KEY) === "1";
}

function applyTheme(theme) {
    root.setAttribute("data-theme", theme);
    root.style.colorScheme = theme;
}

function notifySettingsChange() {
    window.dispatchEvent(new CustomEvent("app:settings-changed", {
        detail: {
            theme: root.getAttribute("data-theme") === "dark" ? "dark" : "light",
            deleteEnabled: getDeleteSetting()
        }
    }));
}

function decorateNavLinks() {
    if (!sidebar) {
        return;
    }
    const links = sidebar.querySelectorAll(".nav-link");
    links.forEach((link) => {
        if (link.querySelector(".nav-link-label")) {
            return;
        }
        const label = link.textContent.trim();
        const href = new URL(link.getAttribute("href"), window.location.origin).pathname;
        const icon = NAV_ICON_MAP[href];
        const iconMarkup = icon
            ? `<span class="nav-link-icon-wrap"><img class="nav-link-icon" src="${icon}" alt="" loading="lazy"></span>`
            : `<span class="nav-link-icon-wrap"></span>`;
        link.innerHTML = `${iconMarkup}<span class="nav-link-label">${label}</span>`;
    });
}

function ensureSettingsFooter() {
    if (!sidebar) {
        return null;
    }
    let footer = sidebar.querySelector(".sidebar-footer");
    if (!footer) {
        footer = document.createElement("div");
        footer.className = "sidebar-footer";
        footer.innerHTML = `
            <div class="sidebar-settings-head">
                <img src="/icons/settings.png" alt="" loading="lazy">
                <span>Settings</span>
            </div>
            <div class="sidebar-setting-card">
                <div class="sidebar-setting-row">
                    <div class="sidebar-setting-copy">
                        <strong>Delete Players</strong>
                        <span>Unlock delete actions in trainee views.</span>
                    </div>
                    <label class="setting-switch">
                        <input id="deletePlayerToggle" type="checkbox" aria-label="Enable trainee deletion">
                        <span class="setting-switch-slider"></span>
                    </label>
                </div>
                <div class="sidebar-setting-row">
                    <div class="sidebar-setting-copy">
                        <strong>Theme</strong>
                        <span>Switch the workspace appearance.</span>
                    </div>
                    <div class="theme-segmented" role="group" aria-label="Theme mode">
                        <button id="themeLightBtn" type="button" class="secondary">Light</button>
                        <button id="themeDarkBtn" type="button" class="secondary">Dark</button>
                    </div>
                </div>
            </div>
        `;
        sidebar.appendChild(footer);
    }
    return footer;
}

function syncSettingsUI() {
    const theme = root.getAttribute("data-theme") === "dark" ? "dark" : "light";
    const deleteToggle = document.getElementById("deletePlayerToggle");
    const themeLightBtn = document.getElementById("themeLightBtn");
    const themeDarkBtn = document.getElementById("themeDarkBtn");

    if (deleteToggle) {
        deleteToggle.checked = getDeleteSetting();
    }
    if (themeLightBtn) {
        themeLightBtn.classList.toggle("active", theme === "light");
    }
    if (themeDarkBtn) {
        themeDarkBtn.classList.toggle("active", theme === "dark");
    }
}

function bindSettingsFooter() {
    const deleteToggle = document.getElementById("deletePlayerToggle");
    const themeLightBtn = document.getElementById("themeLightBtn");
    const themeDarkBtn = document.getElementById("themeDarkBtn");

    deleteToggle?.addEventListener("change", () => {
        localStorage.setItem(DELETE_KEY, deleteToggle.checked ? "1" : "0");
        notifySettingsChange();
    });

    themeLightBtn?.addEventListener("click", () => {
        applyTheme("light");
        localStorage.setItem(THEME_KEY, "light");
        syncSettingsUI();
        notifySettingsChange();
    });

    themeDarkBtn?.addEventListener("click", () => {
        applyTheme("dark");
        localStorage.setItem(THEME_KEY, "dark");
        syncSettingsUI();
        notifySettingsChange();
    });
}

applyTheme(getStoredTheme());
decorateNavLinks();
ensureSettingsFooter();
syncSettingsUI();
bindSettingsFooter();

if (layout && sidebar && toggleBtn) {
    if (localStorage.getItem(STATE_KEY) === "1") {
        layout.classList.add("sidebar-open");
        root.classList.add("sidebar-open-persist");
    }

    const updateButton = () => {
        toggleBtn.innerHTML = layout.classList.contains("sidebar-open") ? "&#10005;" : "&#9776;";
    };

    toggleBtn.addEventListener("click", () => {
        const isOpen = layout.classList.toggle("sidebar-open");
        localStorage.setItem(STATE_KEY, isOpen ? "1" : "0");
        root.classList.toggle("sidebar-open-persist", isOpen);
        updateButton();
    });

    updateButton();
}
