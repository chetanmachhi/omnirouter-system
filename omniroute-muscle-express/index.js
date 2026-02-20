import express from "express";
import osUtils from "os-utils";
import os from "os";

const app = express();
const PORT = process.env.PORT || 4001;

app.get("/health", (req, res) => {
  osUtils.cpuUsage((v) => {
    res.json({
      id: `worker-${PORT}`,
      status: "UP",
      cpu: (v * 100).toFixed(2),
      mem: (os.freemem() / 1024 / 1024).toFixed(2),
      port: PORT,
    });
  });
});

app.post("/execute", express.json(), (req, res) => {
  const delay = Math.floor(Math.random() * 400) + 100;

  console.log(`[Worker ${PORT}] Processing ${req.body.taskName}...`);

  setTimeout(() => {
    res.json({
      success: true,
      processedBy: PORT,
      timeTaken: `${delay}ms`,
    });
  }, delay);
});

app.listen(PORT, () => console.log(`🚀 Muscle Active on Port ${PORT}`));
