// This file toggles sidebar visibility with a hamburger button.
const layout = document.querySelector(".layout");
const sidebar = document.querySelector(".sidebar");
const toggleBtn = document.getElementById("sidebarToggle");
const STATE_KEY = "sidebarOpen";
const root = document.documentElement;

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
