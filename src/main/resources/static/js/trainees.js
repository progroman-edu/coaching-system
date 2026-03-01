import { api } from "./api.js";
import { clearMessage, fillTableBody, showMessage } from "./ui.js";

const msg = document.getElementById("msg");
const tbody = document.getElementById("traineeRows");
const form = document.getElementById("traineeForm");
const filterForm = document.getElementById("filterForm");
const photoForm = document.getElementById("photoForm");

async function loadTrainees(params = {}) {
    try {
        const data = await api.listTrainees(params);
        const rows = data.map((t) => `
            <tr>
                <td>${t.id ?? ""}</td>
                <td>${t.name ?? ""}</td>
                <td>${t.age ?? ""}</td>
                <td>${t.courseStrand ?? ""}</td>
                <td>${t.currentRating ?? ""}</td>
                <td>${t.ranking ?? ""}</td>
                <td>${t.photoPath ?? ""}</td>
                <td><button data-del="${t.id}" class="danger">Delete</button></td>
            </tr>
        `).join("");
        fillTableBody(tbody, rows);
    } catch (err) {
        showMessage(msg, err.message, false);
    }
}

form?.addEventListener("submit", async (e) => {
    e.preventDefault();
    clearMessage(msg);
    const payload = Object.fromEntries(new FormData(form).entries());
    payload.age = Number(payload.age);
    payload.currentRating = Number(payload.currentRating);
    payload.highestRating = payload.highestRating ? Number(payload.highestRating) : null;
    payload.ranking = payload.ranking ? Number(payload.ranking) : null;
    try {
        await api.createTrainee(payload);
        showMessage(msg, "Trainee created.");
        form.reset();
        await loadTrainees();
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});

filterForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const params = Object.fromEntries(new FormData(filterForm).entries());
    await loadTrainees(params);
});

tbody?.addEventListener("click", async (e) => {
    const id = e.target?.dataset?.del;
    if (!id) return;
    try {
        await api.deleteTrainee(id);
        showMessage(msg, `Trainee ${id} deleted.`);
        await loadTrainees();
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});

photoForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const data = new FormData(photoForm);
    const traineeId = data.get("traineeId");
    const file = data.get("file");
    if (!traineeId || !file || file.size === 0) {
        showMessage(msg, "Trainee ID and image file are required.", false);
        return;
    }
    try {
        await api.uploadTraineePhoto(traineeId, file);
        showMessage(msg, "Photo uploaded.");
        photoForm.reset();
        await loadTrainees();
    } catch (err) {
        showMessage(msg, err.message, false);
    }
});

loadTrainees();
