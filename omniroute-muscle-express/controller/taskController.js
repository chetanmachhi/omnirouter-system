import axios from "axios";

export const handleTask = (req, res) => {
  const taskID = Math.floor(Math.random() * 9000) + 1000;
  const port = req.app.get("port") || process.argv[2];

  const MANAGER_BASE = process.env.MANAGER_URL || "http://localhost:4000";

  axios.post(`${MANAGER_BASE}/activity/${port}`).catch(() => {
    console.error(`[Worker ${port}] ⚠️ Manager notification failed at ${MANAGER_BASE}`);
  });

  console.log(`[Worker ${port}] 📥 Task #${taskID} started.`);

  setTimeout(() => {
    console.log(`[Worker ${port}] ✅ Task #${taskID} finished.`);
    res.json({
      success: true,
      taskID,
      port,
      message: "Task completed after 10s",
    });
  }, 10000);
};