const express = require("express");
const os = require("os-utils");
const app = express();

const PORT = 4001;

// The Telemetry Endpoint: Java Brain calls this to check "Load"
app.get("/health", (req, res) => {
  os.cpuUsage((v) => {
    res.json({
      id: `worker-${PORT}`,
      status: "UP",
      cpu: (v * 100).toFixed(2),
      mem: os.freeCommandMem().toFixed(2),
      port: PORT,
    });
  });
});

// The Execution Endpoint: Where tasks are sent
app.post("/execute", express.json(), (req, res) => {
  console.log(`[Worker ${PORT}] Received task: ${req.body.taskName}`);
  setTimeout(() => {
    res.json({ success: true, processedBy: PORT });
  }, 100);
});

app.listen(PORT, () => console.log(`🚀 Muscle Active on Port ${PORT}`));
