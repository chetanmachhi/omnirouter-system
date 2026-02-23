import express from "express";
import osUtils from "os-utils";
import { register, unregister } from "./controller/registry.js";
import { requestTracker, getActiveCount } from "./middleware/monitor.js";
import { handleTask } from "./controller/taskController.js";

const app = express();
const PORT = parseInt(process.argv[2]) || 4001;
const BRAIN_URL = process.env.BRAIN_URL || "http://localhost:8080";

app.set("port", PORT);
app.use(express.json());
app.use(requestTracker);

app.get("/health", (req, res) => {
  osUtils.cpuUsage((v) => {
    const activeTasks = getActiveCount();
    const simulatedCpu = v * 100 + activeTasks * 15;
    const currentDelay = activeTasks * 200;

    res.json({
      port: PORT,
      cpu: simulatedCpu.toFixed(2),
      activeTasks,
      baseDelay: currentDelay,
      status: "UP",
    });
  });
});

app.post("/execute", handleTask);

app.listen(PORT, async () => {
  console.log(`🚀 Worker ${PORT} Active. Load accumulation: 10s/task.`);
  await register(BRAIN_URL, PORT);
});

process.on("SIGINT", async () => {
  await unregister(BRAIN_URL, PORT);
  process.exit();
});
