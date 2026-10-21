(() => {
  "use strict";
  const KEY = "srm.students.v1", DELETED_KEY = "srm.deleted.v1";
  const $ = id => document.getElementById(id);
  const form = $("studentForm"), body = $("studentTableBody"), search = $("searchInput");
  let descending = true, timer;
  function load(key) { try { const x = JSON.parse(localStorage.getItem(key) || "[]"); return Array.isArray(x) ? x : []; } catch { return []; } }
  let students = load(KEY), deleted = load(DELETED_KEY);
  if (localStorage.getItem(KEY) === null) {
    students = [
      { roll: 101, name: "Aarav Menon", course: "B.Tech CSE", marks: 88 },
      { roll: 102, name: "Ananya Joseph", course: "B.Tech CSE", marks: 94 },
      { roll: 103, name: "Rahul Das", course: "B.Tech ECE", marks: 76 }
    ]; save();
  }
  function save() { localStorage.setItem(KEY, JSON.stringify(students)); localStorage.setItem(DELETED_KEY, JSON.stringify(deleted)); }
  function toast(message, error = false) { const el = $("toast"); el.textContent = message; el.classList.toggle("error", error); el.classList.add("show"); clearTimeout(timer); timer = setTimeout(() => el.classList.remove("show"), 2500); }
  function esc(value) { return String(value).replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c])); }
  function initials(name) { return name.trim().split(/\s+/).slice(0, 2).map(x => x[0] || "").join("").toUpperCase(); }
  function render() {
    const q = search.value.trim().toLowerCase();
    const shown = students.filter(s => String(s.roll).includes(q) || s.name.toLowerCase().includes(q) || s.course.toLowerCase().includes(q));
    $("totalStudents").textContent = students.length;
    $("recordCount").textContent = shown.length;
    if (students.length) { $("averageMarks").textContent = (students.reduce((a, s) => a + Number(s.marks), 0) / students.length).toFixed(1); $("topMarks").textContent = Math.max(...students.map(s => Number(s.marks))).toFixed(0) + "%"; }
    else { $("averageMarks").textContent = "—"; $("topMarks").textContent = "—"; }
    body.innerHTML = shown.map(s => `<tr><td><div class="person"><span class="avatar">${esc(initials(s.name))}</span><span><b>${esc(s.name)}</b><small>Student record</small></span></div></td><td><span class="roll">#${esc(s.roll)}</span></td><td>${esc(s.course)}</td><td><span class="marks">${Number(s.marks).toFixed(1).replace(/\.0$/, "")}%</span></td><td><button class="delete" data-delete="${esc(s.roll)}" type="button">Delete</button></td></tr>`).join("");
    $("emptyState").style.display = shown.length ? "none" : "block";
    $("showingText").textContent = `Showing ${shown.length} of ${students.length} record${students.length === 1 ? "" : "s"}`;
    $("restoreBtn").disabled = !deleted.length;
  }
  form.addEventListener("submit", e => {
    e.preventDefault();
    const roll = Number($("roll").value), name = $("name").value.trim(), course = $("course").value.trim(), marks = Number($("marks").value);
    if (!Number.isInteger(roll) || roll < 1) return toast("Enter a valid positive roll number.", true);
    if (students.some(s => Number(s.roll) === roll)) return toast("That roll number already exists.", true);
    if (!name || !course) return toast("Enter the student name and course.", true);
    if (!Number.isFinite(marks) || marks < 0 || marks > 100) return toast("Marks must be between 0 and 100.", true);
    students.push({ roll, name, course, marks }); save(); form.reset(); search.value = ""; render(); toast("Student record added successfully.");
  });
  body.addEventListener("click", e => {
    const button = e.target.closest("[data-delete]"); if (!button) return;
    const i = students.findIndex(s => Number(s.roll) === Number(button.dataset.delete)); if (i < 0) return;
    deleted.push(students.splice(i, 1)[0]); if (deleted.length > 20) deleted.shift(); save(); render(); toast("Record deleted. You can restore it.");
  });
  $("restoreBtn").addEventListener("click", () => {
    if (!deleted.length) return toast("There are no deleted records to restore.", true);
    const s = deleted[deleted.length - 1]; if (students.some(x => Number(x.roll) === Number(s.roll))) return toast("That roll number already exists.", true);
    students.push(deleted.pop()); save(); render(); toast("Last deleted record restored.");
  });
  search.addEventListener("input", render);
  $("sortBtn").addEventListener("click", () => {
    students.sort((a, b) => descending ? Number(b.marks) - Number(a.marks) : Number(a.marks) - Number(b.marks));
    descending = !descending; $("sortBtn").textContent = descending ? "Sort by marks ↓" : "Sort by marks ↑"; save(); render(); toast("Records sorted by marks.");
  });
  render();
})();
