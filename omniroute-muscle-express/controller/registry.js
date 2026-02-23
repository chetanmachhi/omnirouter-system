import axios from "axios";

export const register = async (url, port) => {
  try {
    await axios.post(`${url}/api/registry/register?port=${port}`);
    console.log(`✅ Registered at: ${url}`);
  } catch (err) {
    console.warn(`⚠️ Brain offline at ${url}.`);
  }
};

export const unregister = async (url, port) => {
  try {
    await axios.post(`${url}/api/registry/unregister?port=${port}`);
    console.log(`❌ Unregistered.`);
  } catch (err) {
    // Fail silently on exit
  }
};
