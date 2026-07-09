import { DatabaseSync } from "node:sqlite";
import path from "node:path";
import fs from "node:fs";

// Default assumes `npm run dev` is launched from viewer/, sitting next to the repo's
// ./data/dealtracker.db (see DealTracker/CLAUDE.md "Build & run"). Override with
// DEALTRACKER_DB_PATH to point at a copy scp'd down from Mack instead (see
// docs/data-viewer-plan.md) — this viewer never writes, so pointing it at a live file
// mounted elsewhere is also safe.
const DEFAULT_DB_PATH = path.resolve(process.cwd(), "../data/dealtracker.db");

let db: DatabaseSync | null = null;

export function getDb(): DatabaseSync {
    if (db) return db;

    const dbPath = getDbPath();

    if (!fs.existsSync(dbPath)) {
        throw new Error(
            `No SQLite file found at ${dbPath}. Run "mvn spring-boot:run" from the repo root at least once to create it, or set DEALTRACKER_DB_PATH to a copy pulled down from Mack.`
        );
    }

    // node:sqlite ships with Node itself (stable since Node 22.5) - no native addon build
    // step, unlike better-sqlite3, so there's nothing to compile against whatever Node
    // version happens to be on PATH.
    db = new DatabaseSync(dbPath, { readOnly: true });
    return db;
}

export function getDbPath(): string {
    return process.env.DEALTRACKER_DB_PATH
        ? path.resolve(process.env.DEALTRACKER_DB_PATH)
        : DEFAULT_DB_PATH;
}
