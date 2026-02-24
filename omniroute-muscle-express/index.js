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

const OFFSET = Math.max(0, PORT - 4001);
const CPU_MULTIPLIER = 1 + OFFSET * 0.25;
const NETWORK_DELAY = OFFSET * 50;

const activeMuscles = new Map();
const MAX_WORKERS = 5;
const INACTIVITY_LIMIT_MS = 10 * 60 * 1000;

app.use(cors());
app.use(express.json());

if (PORT === 4000) {
  const cleanupIdleWorkers = () => {
    const now = Date.now();
    for (const [port, worker] of activeMuscles.entries()) {
      if (now - worker.lastActive > INACTIVITY_LIMIT_MS) {
        unregister(BRAIN_URL, port).finally(() => {
          worker.process.kill("SIGINT");
          activeMuscles.delete(port);
        });
      }
    }
  };

  setInterval(cleanupIdleWorkers, 60000);

  app.post("/spawn/:targetPort", async (req, res) => {
    const targetPort = req.params.targetPort;
    if (activeMuscles.size >= MAX_WORKERS) {
      return res.status(400).json({ error: "Because of limited resources cannot add test server" });
    }
    if (activeMuscles.has(targetPort)) {
      return res.status(400).json({ error: "Server already running" });
    }

    try {
      await register(BRAIN_URL, targetPort);
      const child = spawn("node", ["index.js", targetPort], { stdio: "inherit" });
      activeMuscles.set(targetPort, { process: child, lastActive: Date.now() });
      child.on("exit", () => activeMuscles.delete(targetPort));

      await new Promise(r => setTimeout(r, 1500));
      res.json({ message: `Server ${targetPort} active.` });
    } catch {
      res.status(500).json({ error: "Sync failed with Brain." });
    }
  });

  app.post("/activity/:port", (req, res) => {
    const { port } = req.params;
    if (activeMuscles.has(port)) {
      activeMuscles.get(port).lastActive = Date.now();
      return res.sendStatus(200);
    }
    res.sendStatus(404);
  });

  app.delete("/kill/:targetPort", async (req, res) => {
    const { targetPort } = req.params;
    const worker = activeMuscles.get(targetPort);
    if (worker) {
      try {
        await unregister(BRAIN_URL, targetPort);
        const exitPromise = new Promise(resolve => worker.process.on("exit", resolve));
        worker.process.kill("SIGINT");
        await exitPromise;
        res.json({ message: `Server ${targetPort} removed.` });
      } catch {
        res.status(500).json({ error: "Brain sync failed." });
      }
    } else {
      res.status(404).json({ error: "Server not found" });
    }
  });

  app.delete("/kill-all", async (req, res) => {
    console.log("🛑 NUCLEAR SHUTDOWN: Purging all active workers...");

    try {
      const activePorts = Array.from(activeMuscles.keys());

      const killPromises = activePorts.map(async (port) => {
        const worker = activeMuscles.get(port);
        if (worker) {
          const exitPromise = new Promise(resolve => {
            worker.process.on("exit", resolve);
          });

          worker.process.kill("SIGINT");
          return exitPromise;
        }
      });

      await Promise.all(killPromises);
      activeMuscles.clear();

      console.log("✅ All workers terminated successfully.");
      res.json({ message: "All workers purged from system." });
    } catch (err) {
      console.error("❌ Failed to purge all workers:", err);
      res.status(500).json({ error: "Bulk kill operation failed." });
    }
  });

  app.listen(PORT, () => console.log(`🖥️  MASTER Active on ${PORT}`));
} else {
  app.use(requestTracker);

  app.get("/health", (req, res) => {
    osUtils.cpuUsage((v) => {
      const activeTasks = getActiveCount();
      const simulatedCpu = (v * 100 * CPU_MULTIPLIER) + (activeTasks * 20);
      res.json({
        port: PORT,
        cpu: simulatedCpu.toFixed(2),
        activeTasks,
        multiplier: CPU_MULTIPLIER,
        networkDelay: NETWORK_DELAY,
        status: "UP",
      });
    });
  });

  app.post("/execute", (req, res) => handleTask(req, res, NETWORK_DELAY));

  app.listen(PORT, () => console.log(`🚀 Worker ${PORT} Online`));

  process.on("SIGINT", async () => {
    process.exit(0);
  });
}