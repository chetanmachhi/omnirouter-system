import express from "express";
import osUtils from "os-utils";
import os from "os";

const app = express();
const PORT = parseInt(process.argv[2]) || 4001;

// The "Variance Seed" - how far away from the base port is this worker?
const OFFSET = PORT - 4001;

// 1. CPU Variance: Port 4001 = 1x, 4002 = 1.5x, 4003 = 2x, etc.
const CPU_MULTIPLIER = 1 + OFFSET * 0.5;

// 2. Latency Variance: Port 4001 = 0ms, 4002 = 200ms, 4003 = 400ms, etc.
const NETWORK_DELAY = OFFSET * 200;

app.get("/health", (req, res) => {
  osUtils.cpuUsage((v) => {
    const simulatedCpu = v * 100 * CPU_MULTIPLIER;

    res.json({
      id: `worker-${PORT}`,
      status: "UP",
      cpu: simulatedCpu.toFixed(2),
      mem: (os.freemem() / 1024 / 1024).toFixed(2),
      port: PORT,
      multiplier: CPU_MULTIPLIER,
      baseDelay: NETWORK_DELAY,
    });
  });
});

app.post("/execute", express.json(), (req, res) => {
  console.log(
    `[Worker ${PORT}] 🚀 Task Received. Processing with ${NETWORK_DELAY}ms lag...`,
  );

  setTimeout(() => {
    res.json({
      success: true,
      processedBy: PORT,
      simulatedLag: `${NETWORK_DELAY}ms`,
    });
  }, NETWORK_DELAY + 100); // base delay + 100ms processing time
});

app.listen(PORT, () => {
  console.log(
    `🚀 Worker ${PORT} Active | Multiplier: ${CPU_MULTIPLIER}x | Delay: ${NETWORK_DELAY}ms`,
  );
});
