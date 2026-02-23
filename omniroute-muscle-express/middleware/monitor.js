let activeRequests = 0;

export const requestTracker = (req, res, next) => {
  activeRequests++;

  const cleanup = () => {
    activeRequests--;
    res.removeListener("finish", cleanup);
    res.removeListener("close", cleanup);
  };

  res.on("finish", cleanup);
  res.on("close", cleanup);

  next();
};

export const getActiveCount = () => activeRequests;
