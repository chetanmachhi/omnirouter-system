import express from "express";
import cors from "cors";
import { spawn } from "child_process";
import osUtils from "os-utils";
import { register, unregister } from "./controller/registry.js";
import { requestTracker, getActiveCount } from "./middleware/monitor.js";
import { handleTask } from "./controller/taskController.js";

const app = express();
// Default to 4000 if no port is provided
const PORT = parseInt(process.argv[2]) || 4000;
const BRAIN_URL = process.env.BRAIN_URL || "http://localhost:8080";
const activeMuscles = new Map();
const MAX_WORKERS = 5;

app.use(cors());
app.use(express.json());

if (PORT === 4000) {
  // --- MASTER MANAGER LOGIC ---
  app.post("/spawn/:targetPort", (req, res) => {
    const targetPort = req.params.targetPort;
    if (activeMuscles.size >= MAX_WORKERS) return res.status(400).json({ error: "Cluster full" });
    if (activeMuscles.has(targetPort)) return res.status(400).json({ error: "Worker already running" });

    // Spawn a new instance of this same file as a worker
    const child = spawn("node", ["index.js", targetPort], { stdio: "inherit" });
    activeMuscles.set(targetPort, child);

    child.on("exit", () => activeMuscles.delete(targetPort));
    res.json({ message: `Muscle ${targetPort} booting...` });
  });

  app.post("/kill/:targetPort", (req, res) => {
    const targetPort = req.params.targetPort;
    const child = activeMuscles.get(targetPort);
    if (child) {
      child.kill("SIGINT"); 
      res.json({ message: `Muscle ${targetPort} terminated.` });
    } else {
      res.status(404).json({ error: "Worker not found" });
    }
  });

  app.listen(PORT, () => console.log(`🖥️  MASTER MANAGER Active on Port ${PORT}`));

} else {
  // --- WORKER MUSCLE LOGIC ---
  app.use(requestTracker);

  app.get("/health", (req, res) => {
    osUtils.cpuUsage((v) => {
      const activeTasks = getActiveCount();
      const simulatedCpu = v * 100 + activeTasks * 15;
      res.json({
        port: PORT,
        cpu: simulatedCpu.toFixed(2),
        activeTasks,
        status: "UP",
      });
    });
  });

  app.post("/execute", handleTask);

  app.listen(PORT, async () => {
    console.log(`🚀 Muscle ${PORT} Online.`);
    // Only workers register with the Spring Boot Brain
    await register(BRAIN_URL, PORT);
  });

  process.on("SIGINT", async () => {
    console.log(`🛑 Shutting down Muscle ${PORT}...`);
    await unregister(BRAIN_URL, PORT);
    process.exit(0);
  });
}