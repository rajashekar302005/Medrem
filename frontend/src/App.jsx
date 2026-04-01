import React, { useEffect, useMemo, useState } from "react";

const API_BASE = "http://localhost:8080/api";
const TOKEN_KEY = "medrem_jwt_token";
const USER_KEY = "medrem_username";

const initialForm = {
  name: "",
  dosageInstruction: "",
  frequencyPerDay: 1,
  reminderTime: "08:00",
  startDate: new Date().toISOString().slice(0, 10),
  endDate: ""
};

const initialAuthForm = {
  username: "",
  password: ""
};

const statItems = [
  { key: "total", label: "Total Logged Doses", icon: "💊", tone: "total" },
  { key: "onTime", label: "On-Time Doses", icon: "✅", tone: "on-time" },
  { key: "missed", label: "Missed Doses", icon: "⚠️", tone: "missed" },
  { key: "adherence", label: "Adherence", icon: "📈", tone: "adherence" }
];

async function api(path, token, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    ...options
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || "Request failed");
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

export default function App() {
  const [token, setToken] = useState(localStorage.getItem(TOKEN_KEY) || "");
  const [username, setUsername] = useState(localStorage.getItem(USER_KEY) || "");
  const [authForm, setAuthForm] = useState(initialAuthForm);
  const [authMode, setAuthMode] = useState("login");

  const [form, setForm] = useState(initialForm);
  const [medicines, setMedicines] = useState([]);
  const [logs, setLogs] = useState([]);
  const [summary, setSummary] = useState({
    totalDoses: 0,
    takenOnTime: 0,
    takenLate: 0,
    missed: 0,
    adherenceRate: 0,
    onTimeRate: 0,
    medicineBreakdown: []
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [toasts, setToasts] = useState([]);

  const adherenceLabel = useMemo(() => {
    if (summary.adherenceRate >= 85) return "Great";
    if (summary.adherenceRate >= 60) return "Good";
    return "Needs Improvement";
  }, [summary.adherenceRate]);

  function formatReminderTime(timeValue) {
    if (!timeValue) return "--";
    const normalized = timeValue.length === 5 ? `${timeValue}:00` : timeValue;
    const date = new Date(`1970-01-01T${normalized}`);
    if (Number.isNaN(date.getTime())) return timeValue;
    return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
  }

  function formatDate(value) {
    if (!value) return "--";
    return new Date(value).toLocaleDateString([], { day: "2-digit", month: "short", year: "numeric" });
  }

  function addToast(message, type = "success") {
    const id = Date.now() + Math.random();
    setToasts((prev) => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((toast) => toast.id !== id));
    }, 2600);
  }

  async function refreshAll() {
    if (!token) {
      return;
    }

    setLoading(true);
    setError("");
    try {
      const [medicineData, logData, summaryData] = await Promise.all([
        api("/medicines", token),
        api("/adherence/logs", token),
        api("/adherence/summary", token)
      ]);
      setMedicines(medicineData);
      setLogs(logData);
      setSummary(summaryData);
    } catch (err) {
      setError(err.message || "Unable to load data.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (token) {
      refreshAll();
    }
  }, [token]);

  function updateAuthForm(event) {
    const { name, value } = event.target;
    setAuthForm((prev) => ({ ...prev, [name]: value }));
  }

  async function handleAuthSubmit(event) {
    event.preventDefault();
    setError("");

    try {
      const path = authMode === "login" ? "/auth/login" : "/auth/register";
      const data = await api(path, null, {
        method: "POST",
        body: JSON.stringify(authForm)
      });

      localStorage.setItem(TOKEN_KEY, data.token);
      localStorage.setItem(USER_KEY, data.username);
      setToken(data.token);
      setUsername(data.username);
      setAuthForm(initialAuthForm);
      addToast(authMode === "login" ? "Logged in successfully" : "Account created successfully");
    } catch (err) {
      setError(err.message || "Authentication failed.");
    }
  }

  function logout() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setToken("");
    setUsername("");
    setMedicines([]);
    setLogs([]);
    setSummary({
      totalDoses: 0,
      takenOnTime: 0,
      takenLate: 0,
      missed: 0,
      adherenceRate: 0,
      onTimeRate: 0,
      medicineBreakdown: []
    });
    addToast("Logged out", "neutral");
  }

  function updateForm(event) {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  async function handleAddMedicine(event) {
    event.preventDefault();
    setError("");

    try {
      await api("/medicines", token, {
        method: "POST",
        body: JSON.stringify({
          ...form,
          frequencyPerDay: Number(form.frequencyPerDay),
          endDate: form.endDate || null
        })
      });
      setForm(initialForm);
      addToast("Medicine saved successfully");
      await refreshAll();
    } catch (err) {
      setError(err.message || "Could not add medicine.");
    }
  }

  async function markTaken(medicineId) {
    try {
      await api("/adherence/take", token, {
        method: "POST",
        body: JSON.stringify({ medicineId })
      });
      addToast("Dose marked as taken");
      await refreshAll();
    } catch (err) {
      setError(err.message || "Could not mark dose as taken.");
    }
  }

  async function markMissed(medicineId) {
    try {
      await api("/adherence/miss", token, {
        method: "POST",
        body: JSON.stringify({ medicineId })
      });
      addToast("Dose marked as missed", "warn");
      await refreshAll();
    } catch (err) {
      setError(err.message || "Could not mark dose as missed.");
    }
  }

  async function deleteMedicine(medicineId) {
    try {
      await api(`/medicines/${medicineId}`, token, { method: "DELETE" });
      addToast("Medicine deleted", "neutral");
      await refreshAll();
    } catch (err) {
      setError(err.message || "Could not delete medicine.");
    }
  }

  if (!token) {
    return (
      <div className="page">
        <header className="hero">
          <h1>Medicine Reminder</h1>
          <p>Login to manage medicine schedule, dosage, and adherence dashboard.</p>
        </header>

        <div className="toast-stack">
          {toasts.map((toast) => (
            <div key={toast.id} className={`toast ${toast.type}`}>{toast.message}</div>
          ))}
        </div>

        {error && <div className="alert">{error}</div>}

        <section className="card auth-card">
          <h2>{authMode === "login" ? "Sign In" : "Create Account"}</h2>
          <form onSubmit={handleAuthSubmit} className="grid-form">
            <label>
              Username
              <input
                name="username"
                value={authForm.username}
                onChange={updateAuthForm}
                minLength={3}
                required
              />
            </label>
            <label>
              Password
              <input
                type="password"
                name="password"
                value={authForm.password}
                onChange={updateAuthForm}
                minLength={6}
                required
              />
            </label>
            <button type="submit" className="btn primary">
              {authMode === "login" ? "Login" : "Register"}
            </button>
          </form>
          <button
            className="btn subtle switch-auth"
            onClick={() => setAuthMode((prev) => (prev === "login" ? "register" : "login"))}
          >
            {authMode === "login" ? "Need an account? Register" : "Already have an account? Login"}
          </button>
        </section>
      </div>
    );
  }

  return (
    <div className="page">
      <header className="hero">
        <h1>Medicine Reminder</h1>
        <p>Track dosage schedule and monitor adherence in one dashboard.</p>
        <div className="hero-user">
          <span>Welcome, {username}</span>
          <button className="btn subtle" onClick={logout}>Logout</button>
        </div>
      </header>

      <div className="toast-stack">
        {toasts.map((toast) => (
          <div key={toast.id} className={`toast ${toast.type}`}>{toast.message}</div>
        ))}
      </div>

      {error && <div className="alert">{error}</div>}

      <section className="card form-card">
        <h2>Add Medicine</h2>
        <form onSubmit={handleAddMedicine} className="grid-form">
          <label>
            Medicine Name
            <input name="name" value={form.name} onChange={updateForm} required />
          </label>
          <label>
            Dosage Details
            <input
              name="dosageInstruction"
              value={form.dosageInstruction}
              onChange={updateForm}
              placeholder="e.g. 1 tablet after food"
              required
            />
          </label>
          <label>
            Times per Day
            <input
              type="number"
              name="frequencyPerDay"
              min="1"
              max="6"
              value={form.frequencyPerDay}
              onChange={updateForm}
              required
            />
          </label>
          <label>
            Reminder Time
            <input type="time" name="reminderTime" value={form.reminderTime} onChange={updateForm} required />
          </label>
          <label>
            Start Date
            <input type="date" name="startDate" value={form.startDate} onChange={updateForm} required />
          </label>
          <label>
            End Date (Optional)
            <input type="date" name="endDate" value={form.endDate} onChange={updateForm} />
          </label>
          <button type="submit" className="btn primary">Save Medicine</button>
        </form>
      </section>

      <section className="dashboard-grid">
        {statItems.map((item) => {
          const valueMap = {
            total: summary.totalDoses,
            onTime: summary.takenOnTime,
            missed: summary.missed,
            adherence: `${summary.adherenceRate.toFixed(1)}%`
          };

          return (
            <div key={item.key} className={`card metric metric-${item.tone}`}>
              <div className="metric-top">
                <span className="metric-icon" aria-hidden="true">{item.icon}</span>
                <span className="metric-label">{item.label}</span>
              </div>
              <strong>{valueMap[item.key]}</strong>
              {item.key === "adherence" ? (
                <>
                  <small>{adherenceLabel}</small>
                  <div className="progress-bar" role="progressbar" aria-valuenow={summary.adherenceRate} aria-valuemin="0" aria-valuemax="100">
                    <div className="progress-fill" style={{ width: `${Math.min(100, Math.max(0, summary.adherenceRate))}%` }} />
                  </div>
                </>
              ) : null}
            </div>
          );
        })}
      </section>

      <section className="card">
        <h2>Medicine Schedule</h2>
        {loading ? <p>Loading...</p> : null}
        {!loading && medicines.length === 0 ? <p>No medicines added yet.</p> : null}
        <div className="medicine-list">
          {medicines.map((medicine) => (
            <article key={medicine.id} className="medicine-item">
              <div className="medicine-main">
                <h3>{medicine.name}</h3>
                <p className="medicine-note">{medicine.dosageInstruction}</p>
                <div className="medicine-meta">
                  <span><strong>Times per day:</strong> {medicine.frequencyPerDay}</span>
                  <span><strong>Reminder:</strong> {formatReminderTime(medicine.reminderTime)}</span>
                  <span><strong>Start date:</strong> {formatDate(medicine.startDate)}</span>
                  <span><strong>End date:</strong> {medicine.endDate ? formatDate(medicine.endDate) : "Not set"}</span>
                </div>
              </div>
              <div className="row-actions">
                <button className="btn success" onClick={() => markTaken(medicine.id)}>
                  Mark Taken
                </button>
                <button className="btn warn" onClick={() => markMissed(medicine.id)}>
                  Mark Missed
                </button>
                <button className="btn subtle" onClick={() => deleteMedicine(medicine.id)}>
                  Delete
                </button>
              </div>
            </article>
          ))}
        </div>
      </section>

      <section className="card">
        <h2>Adherence by Medicine</h2>
        {summary.medicineBreakdown?.length ? (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Medicine</th>
                  <th>Total</th>
                  <th>On Time</th>
                  <th>Late</th>
                  <th>Missed</th>
                  <th>Adherence %</th>
                </tr>
              </thead>
              <tbody>
                {summary.medicineBreakdown.map((item) => (
                  <tr key={item.medicineId}>
                    <td>{item.medicineName}</td>
                    <td>{item.total}</td>
                    <td>{item.onTime}</td>
                    <td>{item.late}</td>
                    <td>{item.missed}</td>
                    <td>{item.adherenceRate.toFixed(1)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p>No adherence data yet.</p>
        )}
      </section>

      <section className="card">
        <h2>Recent Dose Logs</h2>
        {!logs.length ? (
          <p>No logs yet.</p>
        ) : (
          <ul className="logs">
            {logs.slice(0, 8).map((log) => (
              <li key={log.id}>
                <span className="log-main">{log.medicineName}</span>
                <span className="log-date">{new Date(log.scheduledAt).toLocaleString([], { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" })}</span>
                <span className={`badge ${log.status.toLowerCase()}`}>{log.status}</span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
