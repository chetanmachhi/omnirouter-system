import axios from "axios";

export const handleTask = async (req, res, networkDelay) => {
  const port = process.argv[2];
  const taskID = Math.floor(Math.random() * 10000);

  const MANAGER_BASE = process.env.MANAGER_URL || "http://localhost:4000";
  await axios.post(`${MANAGER_BASE}/activity/${port}`).catch(() => { });

  const totalTime = networkDelay + 30000;

  setTimeout(() => {
    res.json({
      success: true,
      processedBy: port,
      simulatedLag: `${networkDelay}ms`,
      workTime: "30000ms"
    });
  }, totalTime);
};