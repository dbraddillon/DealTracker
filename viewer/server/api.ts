import type { IncomingMessage, ServerResponse } from "node:http";
import { getDb, getDbPath } from "./db";
import type {
    HistoryPoint,
    NotificationEventRow,
    ObservationRow,
    TriggerRuleSummary,
    VariantStatus,
    WatchTargetOverview,
} from "../src/types";

type Handler = (url: URL, res: ServerResponse) => void;

const routes: Record<string, Handler> = {
    "/api/meta": handleMeta,
    "/api/overview": handleOverview,
    "/api/history": handleHistory,
    "/api/notifications": handleNotifications,
    "/api/observations": handleObservations,
};

export function apiMiddleware(req: IncomingMessage, res: ServerResponse, next: () => void): void {
    if (!req.url || !req.url.startsWith("/api/")) {
        next();
        return;
    }

    const url = new URL(req.url, "http://localhost");
    const handler = routes[url.pathname];
    if (!handler) {
        sendJson(res, 404, { error: `No such endpoint: ${url.pathname}` });
        return;
    }

    try {
        handler(url, res);
    } catch (err) {
        sendJson(res, 503, { error: err instanceof Error ? err.message : String(err) });
    }
}

function sendJson(res: ServerResponse, status: number, body: unknown): void {
    res.statusCode = status;
    res.setHeader("Content-Type", "application/json");
    res.end(JSON.stringify(body));
}

function toBool(value: number): boolean {
    return value !== 0;
}

function handleMeta(_url: URL, res: ServerResponse): void {
    // Touch the db so a missing-file error surfaces here rather than as a silent empty page.
    getDb();
    sendJson(res, 200, { dbPath: getDbPath() });
}

function handleOverview(_url: URL, res: ServerResponse): void {
    const db = getDb();

    const targets = db
        .prepare(
            `SELECT id, name, category, brand, active FROM watch_target ORDER BY name`
        )
        .all() as { id: number; name: string; category: string | null; brand: string | null; active: number }[];

    const ruleRows = db
        .prepare(
            `SELECT id, watch_target_id, rule_type, threshold_value, cooldown_hours, active
             FROM trigger_rule ORDER BY id`
        )
        .all() as {
        id: number;
        watch_target_id: number;
        rule_type: string;
        threshold_value: number | null;
        cooldown_hours: number;
        active: number;
    }[];

    // Latest observation per (listing, variant title) — one row per flavor/variant currently seen.
    const variantRows = db
        .prepare(
            `SELECT o.listing_id AS listingId, l.watch_target_id AS watchTargetId, l.url AS listingUrl,
                    l.variant_label AS variantLabel, l.currency AS currency,
                    o.title AS title, o.observed_at AS observedAt, o.price AS price,
                    o.sale_price AS salePrice, o.availability AS availability
             FROM observation o
             JOIN listing l ON l.id = o.listing_id
             JOIN (
                 SELECT listing_id, COALESCE(title, '') AS title_key, MAX(observed_at) AS max_observed_at
                 FROM observation
                 GROUP BY listing_id, title_key
             ) latest ON latest.listing_id = o.listing_id
                      AND COALESCE(o.title, '') = latest.title_key
                      AND o.observed_at = latest.max_observed_at`
        )
        .all() as {
        listingId: number;
        watchTargetId: number;
        listingUrl: string;
        variantLabel: string | null;
        currency: string;
        title: string | null;
        observedAt: string;
        price: number | null;
        salePrice: number | null;
        availability: string | null;
    }[];

    const rulesByTarget = new Map<number, TriggerRuleSummary[]>();
    for (const r of ruleRows) {
        const list = rulesByTarget.get(r.watch_target_id) ?? [];
        list.push({
            id: r.id,
            ruleType: r.rule_type,
            thresholdValue: r.threshold_value,
            cooldownHours: r.cooldown_hours,
            active: toBool(r.active),
        });
        rulesByTarget.set(r.watch_target_id, list);
    }

    const variantsByTarget = new Map<number, VariantStatus[]>();
    for (const v of variantRows) {
        const effectivePrice = v.salePrice ?? v.price;
        const activePriceBelowThresholds = (rulesByTarget.get(v.watchTargetId) ?? [])
            .filter((r) => r.active && r.ruleType === "price_below" && r.thresholdValue !== null)
            .map((r) => r.thresholdValue as number);
        const underThreshold =
            effectivePrice !== null && activePriceBelowThresholds.some((t) => effectivePrice < t);

        const list = variantsByTarget.get(v.watchTargetId) ?? [];
        list.push({
            listingId: v.listingId,
            listingUrl: v.listingUrl,
            variantLabel: v.variantLabel,
            title: v.title,
            currency: v.currency,
            observedAt: v.observedAt,
            price: v.price,
            salePrice: v.salePrice,
            effectivePrice,
            availability: v.availability,
            underThreshold,
        });
        variantsByTarget.set(v.watchTargetId, list);
    }

    const overview: WatchTargetOverview[] = targets.map((t) => ({
        id: t.id,
        name: t.name,
        category: t.category,
        brand: t.brand,
        active: toBool(t.active),
        rules: rulesByTarget.get(t.id) ?? [],
        variants: (variantsByTarget.get(t.id) ?? []).sort((a, b) =>
            (a.title ?? "").localeCompare(b.title ?? "")
        ),
    }));

    sendJson(res, 200, overview);
}

function handleHistory(url: URL, res: ServerResponse): void {
    const watchTargetId = Number(url.searchParams.get("watchTargetId"));
    if (!watchTargetId) {
        sendJson(res, 400, { error: "watchTargetId query param is required" });
        return;
    }

    const db = getDb();
    const rows = db
        .prepare(
            `SELECT o.listing_id AS listingId, o.title AS title, o.observed_at AS observedAt,
                    o.price AS price, o.sale_price AS salePrice, o.availability AS availability
             FROM observation o
             JOIN listing l ON l.id = o.listing_id
             WHERE l.watch_target_id = ?
             ORDER BY o.observed_at ASC`
        )
        .all(watchTargetId) as {
        listingId: number;
        title: string | null;
        observedAt: string;
        price: number | null;
        salePrice: number | null;
        availability: string | null;
    }[];

    const points: HistoryPoint[] = rows.map((r) => ({
        listingId: r.listingId,
        title: r.title,
        observedAt: r.observedAt,
        price: r.price,
        salePrice: r.salePrice,
        effectivePrice: r.salePrice ?? r.price,
        availability: r.availability,
    }));

    sendJson(res, 200, points);
}

function handleNotifications(url: URL, res: ServerResponse): void {
    const limit = Number(url.searchParams.get("limit") ?? "50");
    const db = getDb();

    const rows = db
        .prepare(
            `SELECT n.id AS id, n.watch_target_id AS watchTargetId, w.name AS watchTargetName,
                    n.listing_id AS listingId, r.rule_type AS ruleType, r.threshold_value AS thresholdValue,
                    o.price AS price, o.sale_price AS salePrice, n.sent_at AS sentAt
             FROM notification_event n
             JOIN watch_target w ON w.id = n.watch_target_id
             JOIN trigger_rule r ON r.id = n.trigger_rule_id
             LEFT JOIN observation o ON o.id = n.observation_id
             ORDER BY n.sent_at DESC
             LIMIT ?`
        )
        .all(limit) as {
        id: number;
        watchTargetId: number;
        watchTargetName: string;
        listingId: number | null;
        ruleType: string;
        thresholdValue: number | null;
        price: number | null;
        salePrice: number | null;
        sentAt: string;
    }[];

    const events: NotificationEventRow[] = rows.map((r) => ({
        id: r.id,
        watchTargetId: r.watchTargetId,
        watchTargetName: r.watchTargetName,
        listingId: r.listingId,
        ruleType: r.ruleType,
        thresholdValue: r.thresholdValue,
        observationEffectivePrice: r.salePrice ?? r.price,
        sentAt: r.sentAt,
    }));

    sendJson(res, 200, events);
}

function handleObservations(url: URL, res: ServerResponse): void {
    const listingId = Number(url.searchParams.get("listingId"));
    const limit = Number(url.searchParams.get("limit") ?? "200");
    if (!listingId) {
        sendJson(res, 400, { error: "listingId query param is required" });
        return;
    }

    const db = getDb();
    const rows = db
        .prepare(
            `SELECT id, listing_id AS listingId, observed_at AS observedAt, title, availability,
                    price, sale_price AS salePrice, member_price AS memberPrice,
                    subscribe_price AS subscribePrice, quantity_value AS quantityValue,
                    quantity_unit AS quantityUnit, promo_text AS promoText
             FROM observation
             WHERE listing_id = ?
             ORDER BY observed_at DESC
             LIMIT ?`
        )
        .all(listingId, limit) as unknown as ObservationRow[];

    sendJson(res, 200, rows);
}
