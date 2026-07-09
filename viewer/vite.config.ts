import { defineConfig, type Plugin } from "vite";
import react from "@vitejs/plugin-react";
import { apiMiddleware } from "./server/api";

// Serves the read-only JSON API from inside the same dev server as the React app, so
// "npm run dev" is the whole setup — no second process, no CORS, nothing to launch separately.
function dealTrackerApiPlugin(): Plugin {
    return {
        name: "dealtracker-api",
        configureServer(server) {
            server.middlewares.use(apiMiddleware);
        },
    };
}

export default defineConfig({
    plugins: [react(), dealTrackerApiPlugin()],
});
