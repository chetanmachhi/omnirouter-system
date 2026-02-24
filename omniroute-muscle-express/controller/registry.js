import axios from "axios";

export const register = async (url, port) => {
  try {
    await axios.post(`${url}/api/registry/register?port=${port}`);
    console.log(`✅ Brain sync: ${port}`);
  } catch (err) {
    console.warn(`⚠️ Brain offline.`);
    throw err;
  }
};

export const unregister = async (url, port) => {
  try {
    await axios.post(`${url}/api/registry/unregister?port=${port}`);
    console.log(`❌ Brain purged: ${port}`);
  } catch (err) {
    console.warn(`⚠️ Cleanup failed.`);
    throw err;
  }
};