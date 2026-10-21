"use strict";

const API = "/api/students";

const $ = (id) => document.getElementById(id);

let allStudents = [];
let deletedStudents = [];
let currentSearch = "";
let sorted = false;

async function apiRequest(action = null, data = {}) {
    const options = {
        method: action ? "POST" : "GET",
        headers: { "Content-Type": "application/json" }
    };

    if (action) {
        options.body = JSON.stringify({ action, ...data });
    }

    const response = await fetch(API, options);

    const result = await response.json();

    if (!response.ok || result.error) {
        throw new Error(result.error || "Request failed.");
    }

    return result;
}

function escapeHTML(value) {
    return String(value).replace(/[&<>"']/g, (char) => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    })[char]);
}

function gradeFor(marks) {
    if (marks >= 90) return "A+";
    if (marks >= 80) return "A";
    if (marks >= 70) return "B";
    if (marks >= 60) return "C";
    if (marks >= 50) return "D";
    return "F";
}

function showMessage(id, message, error = false) {
    const element = $(id);
    element.textContent = message;
    element.style.color = error ? "#fb7185" : "#34d399";
}

function renderDashboard() {
    $("totalStudents").textContent = allStudents.length;

    const total = allStudents.reduce(
        (sum, student) => sum + Number(student.marks), 0
    );

    const average = allStudents.length
        ? total / allStudents.length
        : 0;

    const highest = allStudents.length
        ? Math.max(...allStudents.map(s => Number(s.marks)))
        : 0;

    $("averageMarks").textContent = average.toFixed(2);
    $("topMarks").textContent = highest.toFixed(2);
    $("recordCount").textContent =
        `${allStudents.length} record${allStudents.length === 1 ? "" : "s"}`;
}

function renderStudents() {
    const tbody = $("studentTableBody");

    const query = currentSearch.trim().toLowerCase();

    const displayed = allStudents.filter(student =>
        String(student.rollNo).includes(query) ||
        String(student.name).toLowerCase().includes(query)
    );

    if (displayed.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="empty">
                    ${allStudents.length ? "No matching students found." :
                "No student records yet. Add your first student above."}
                </td>
            </tr>`;
        return;
    }

    tbody.innerHTML = displayed.map(student => `
        <tr>
            <td>${escapeHTML(student.rollNo)}</td>
            <td>${escapeHTML(student.name)}</td>
            <td>${Number(student.marks).toFixed(2)}</td>
            <td>${gradeFor(Number(student.marks))}</td>
            <td>
                <button
                    class="btn danger"
                    data-delete="${Number(student.rollNo)}"
                    type="button">
                    Delete
                </button>
            </td>
        </tr>
    `).join("");
}

function renderDeleted() {
    const tbody = $("deletedTableBody");

    if (deletedStudents.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="4" class="empty">
                    No deleted records to restore.
                </td>
            </tr>`;
        return;
    }

    tbody.innerHTML = deletedStudents.map(student => `
        <tr>
            <td>${escapeHTML(student.rollNo)}</td>
            <td>${escapeHTML(student.name)}</td>
            <td>${Number(student.marks).toFixed(2)}</td>
            <td>
                <button
                    class="btn success"
                    data-restore="${Number(student.rollNo)}"
                    type="button">
                    Restore
                </button>
            </td>
        </tr>
    `).join("");
}

async function refreshData() {
    try {
        const result = await apiRequest();

        allStudents = result.students || [];
        deletedStudents = result.deletedStudents || [];

        renderDashboard();
        renderStudents();
        renderDeleted();
    } catch (error) {
        showMessage(
            "recordMessage",
            "Cannot connect to Java server. Run: java Student",
            true
        );
        console.error(error);
    }
}

$("studentForm").addEventListener("submit", async (event) => {
    event.preventDefault();

    const rollNo = Number($("rollNo").value);
    const name = $("name").value.trim();
    const marks = Number($("marks").value);

    if (!Number.isSafeInteger(rollNo) || rollNo < 1) {
        showMessage("formMessage", "Enter a valid positive roll number.", true);
        return;
    }

    if (!name) {
        showMessage("formMessage", "Enter the student's name.", true);
        return;
    }

    if (!Number.isFinite(marks) || marks < 0 || marks > 100) {
        showMessage("formMessage", "Marks must be between 0 and 100.", true);
        return;
    }

    try {
        const result = await apiRequest("add", { rollNo, name, marks });

        showMessage("formMessage", result.message || "Student added!");

        $("studentForm").reset();
        currentSearch = "";
        $("searchInput").value = "";

        await refreshData();
        $("rollNo").focus();
    } catch (error) {
        showMessage("formMessage", error.message, true);
    }
});

$("searchBtn").addEventListener("click", () => {
    currentSearch = $("searchInput").value;
    renderStudents();
});

$("searchInput").addEventListener("keydown", event => {
    if (event.key === "Enter") {
        event.preventDefault();
        currentSearch = $("searchInput").value;
        renderStudents();
    }
});

$("showAllBtn").addEventListener("click", () => {
    currentSearch = "";
    $("searchInput").value = "";
    renderStudents();
});

$("sortBtn").addEventListener("click", async () => {
    try {
        const result = await apiRequest("sort");

        showMessage(
            "recordMessage",
            result.message || "Students sorted by marks."
        );

        await refreshData();
    } catch (error) {
        showMessage("recordMessage", error.message, true);
    }
});

$("studentTableBody").addEventListener("click", async event => {
    const button = event.target.closest("[data-delete]");
    if (!button) return;

    const rollNo = Number(button.dataset.delete);

    if (!confirm(`Delete student with roll number ${rollNo}?`)) return;

    try {
        const result = await apiRequest("delete", { rollNo });

        showMessage("recordMessage", result.message || "Student deleted.");
        await refreshData();
    } catch (error) {
        showMessage("recordMessage", error.message, true);
    }
});

$("deletedTableBody").addEventListener("click", async event => {
    const button = event.target.closest("[data-restore]");
    if (!button) return;

    const rollNo = Number(button.dataset.restore);

    try {
        const result = await apiRequest("restore", { rollNo });

        showMessage("recordMessage", result.message || "Student restored.");
        await refreshData();
    } catch (error) {
        showMessage("recordMessage", error.message, true);
    }
});

refreshData();