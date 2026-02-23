import { getActiveCount } from "../middleware/monitor.js";

export const handleTask = (req, res) => {
  const taskID = Math.floor(Math.random() * 9000) + 1000;
  const port = req.app.get("port");

  console.log(
    `[Worker ${port}] 📥 Task #${taskID} started. Active: ${getActiveCount()}`,
  );

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
