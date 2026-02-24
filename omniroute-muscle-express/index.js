import express from "express";
import cors from "cors";
import { spawn } from "child_process";
import osUtils from "os-utils";
import { register, unregister } from "./controller/registry.js";
import { requestTracker, getActiveCount } from "./middleware/monitor.js";
import { handleTask } from "./controller/taskController.js";

const app = express();
const PORT = parseInt(process.argv[2]) || 4000;
const BRAIN_URL = process.env.BRAIN_URL || "http://localhost:8080";

const activeMuscles = new Map();
const MAX_WORKERS = 5;
const INACTIVITY_LIMIT_MS = 10 * 60 * 1000;

app.use(cors());
app.use(express.json());

if (PORT === 4000) {
  // --- MASTER MANAGER LOGIC ---

  const cleanupIdleWorkers = () => {
    const now = Date.now();
    for (const [port, worker] of activeMuscles.entries()) {
      if (now - worker.lastActive > INACTIVITY_LIMIT_MS) {
        console.log(`🧹 Auto-Cleanup: Killing idle Muscle on Port ${port}`);
        worker.process.kill("SIGINT");
        activeMuscles.delete(port);
      }
    }
  };

  setInterval(cleanupIdleWorkers, 60000);

  app.post("/spawn/:targetPort", (req, res) => {
    const targetPort = req.params.targetPort;

    if (activeMuscles.size >= MAX_WORKERS) {
      return res.status(400).json({
        error: "Because of limited resources cannot add test server"
      });
    }

    if (activeMuscles.has(targetPort)) {
      return res.status(400).json({ error: "Worker already running" });
    }

    console.log(`📡 Manager: Spawning Muscle on Port ${targetPort}...`);
    const child = spawn("node", ["index.js", targetPort], { stdio: "inherit" });

    activeMuscles.set(targetPort, {
      process: child,
      lastActive: Date.now()
    });

    child.on("exit", () => activeMuscles.delete(targetPort));
    res.json({ message: `Muscle ${targetPort} booting...` });
  });

  app.post("/activity/:port", (req, res) => {
    const { port } = req.params;
    if (activeMuscles.has(port)) {
      activeMuscles.get(port).lastActive = Date.now();
      return res.sendStatus(200);
    }
    res.sendStatus(404);
  });

  app.post("/kill/:targetPort", (req, res) => {
    const targetPort = req.params.targetPort;
    const worker = activeMuscles.get(targetPort);
    if (worker) {
      worker.process.kill("SIGINT");
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
    await register(BRAIN_URL, PORT);
  });

  process.on("SIGINT", async () => {
    console.log(`🛑 Shutting down Muscle ${PORT}...`);
    await unregister(BRAIN_URL, PORT);
    process.exit(0);
  });
}