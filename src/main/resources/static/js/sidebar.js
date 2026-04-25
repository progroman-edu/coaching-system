// This file toggles sidebar visibility with a hamburger button.
const layout = document.querySelector(".layout");
const sidebar = document.querySelector(".sidebar");
const toggleBtn = document.getElementById("sidebarToggle");
const STATE_KEY = "sidebarOpen";
const THEME_KEY = "themeMode";
const root = document.documentElement;

function getStoredTheme() {
    const stored = localStorage.getItem(THEME_KEY);
    if (stored === "light" || stored === "dark") {
        return stored;
    }
    return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
}

function applyTheme(theme) {
    root.setAttribute("data-theme", theme);
    root.style.colorScheme = theme;
}

function ensureThemeToggle() {
    let button = document.getElementById("themeToggle");
    if (!button) {
        button = document.createElement("button");
        button.id = "themeToggle";
        button.type = "button";
        button.className = "theme-toggle secondary";
        button.setAttribute("aria-label", "Toggle color theme");
        document.body.appendChild(button);
    }
    return button;
}

function updateThemeToggleLabel(button, theme) {
    button.textContent = theme === "dark" ? "Light" : "Dark";
    button.title = theme === "dark" ? "Switch to light mode" : "Switch to dark mode";
}

const initialTheme = getStoredTheme();
applyTheme(initialTheme);

const themeToggleBtn = ensureThemeToggle();
updateThemeToggleLabel(themeToggleBtn, initialTheme);
themeToggleBtn.addEventListener("click", () => {
    const current = root.getAttribute("data-theme") === "dark" ? "dark" : "light";
    const next = current === "dark" ? "light" : "dark";
    applyTheme(next);
    localStorage.setItem(THEME_KEY, next);
    updateThemeToggleLabel(themeToggleBtn, next);
});

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
