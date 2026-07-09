import { useEffect, useMemo, useState } from "react";
import { LineChart, type ChartSeries } from "./LineChart";
import type {
    HistoryPoint,
    NotificationEventRow,
    ObservationRow,
    WatchTargetOverview,
} from "./types";

async function fetchJson<T>(url: string): Promise<T> {
    const res = await fetch(url);
    const body = await res.json();
    if (!res.ok) throw new Error(body.error ?? `Request to ${url} failed`);
    return body as T;
}

function formatDate(ms: number): string {
    return new Date(ms).toLocaleString(undefined, {
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit",
    });
}

function currencyFormatter(currency: string) {
    const fmt = new Intl.NumberFormat(undefined, { style: "currency", currency });
    return (v: number) => fmt.format(v);
}

// Watch targets can have a dozen-plus variants (flavors, sizes) that mostly move in lockstep -
// one line per variant turns into overlapping, indistinguishable hues past the categorical
// palette's 8-color safe limit. Instead, collapse to at most two lines - lowest and highest
// effective price seen across variants at each poll - and list which variants sit at that price
// in the tooltip/table detail, so "what's the cheapest it's gotten" reads at a glance without
// losing per-variant fidelity (still fully browsable in the variant cards and raw table below).
function describeGroup(titles: string[]): string {
    const shown = titles.slice(0, 3);
    const rest = titles.length - shown.length;
    return shown.join(", ") + (rest > 0 ? ` +${rest} more` : "");
}

export default function App() {
    const [overview, setOverview] = useState<WatchTargetOverview[] | null>(null);
    const [dbPath, setDbPath] = useState<string | null>(null);
    const [error, setError] = useState<string | null>(null);
    const [selectedId, setSelectedId] = useState<number | null>(null);
    const [history, setHistory] = useState<HistoryPoint[] | null>(null);
    const [notifications, setNotifications] = useState<NotificationEventRow[] | null>(null);
    const [browseListingId, setBrowseListingId] = useState<number | null>(null);
    const [observations, setObservations] = useState<ObservationRow[] | null>(null);

    useEffect(() => {
        fetchJson<{ dbPath: string }>("/api/meta")
            .then((m) => setDbPath(m.dbPath))
            .catch((e) => setError(e.message));

        fetchJson<WatchTargetOverview[]>("/api/overview")
            .then((data) => {
                setOverview(data);
                if (data.length > 0) setSelectedId(data[0].id);
            })
            .catch((e) => setError(e.message));

        fetchJson<NotificationEventRow[]>("/api/notifications?limit=25")
            .then(setNotifications)
            .catch((e) => setError(e.message));
    }, []);

    useEffect(() => {
        if (selectedId === null) return;
        fetchJson<HistoryPoint[]>(`/api/history?watchTargetId=${selectedId}`)
            .then(setHistory)
            .catch((e) => setError(e.message));
        setBrowseListingId(null);
        setObservations(null);
    }, [selectedId]);

    useEffect(() => {
        if (browseListingId === null) return;
        fetchJson<ObservationRow[]>(`/api/observations?listingId=${browseListingId}&limit=200`)
            .then(setObservations)
            .catch((e) => setError(e.message));
    }, [browseListingId]);

    const selected = overview?.find((t) => t.id === selectedId) ?? null;

    const currency = selected?.variants[0]?.currency ?? "USD";
    const fmtY = useMemo(() => currencyFormatter(currency), [currency]);

    const series: ChartSeries[] = useMemo(() => {
        if (!history) return [];

        const byTime = new Map<number, { title: string; price: number }[]>();
        for (const h of history) {
            if (h.effectivePrice === null) continue;
            const x = new Date(h.observedAt).getTime();
            const list = byTime.get(x) ?? [];
            list.push({ title: h.title ?? "(untitled)", price: h.effectivePrice });
            byTime.set(x, list);
        }

        const xs = Array.from(byTime.keys()).sort((a, b) => a - b);
        const lowPoints = [];
        const highPoints = [];
        let anyDifference = false;

        for (const x of xs) {
            const entries = byTime.get(x)!;
            const min = Math.min(...entries.map((e) => e.price));
            const max = Math.max(...entries.map((e) => e.price));
            if (max > min) anyDifference = true;
            lowPoints.push({
                x,
                y: min,
                detail: describeGroup(entries.filter((e) => e.price === min).map((e) => e.title)),
            });
            highPoints.push({
                x,
                y: max,
                detail: describeGroup(entries.filter((e) => e.price === max).map((e) => e.title)),
            });
        }

        if (!anyDifference) {
            return [{ key: "price", label: "Price", color: "var(--series-1)", points: lowPoints }];
        }

        return [
            { key: "low", label: "Lowest price", color: "var(--series-1)", points: lowPoints },
            { key: "high", label: "Highest price", color: "var(--series-2)", points: highPoints },
        ];
    }, [history]);

    const listings = useMemo(() => {
        if (!selected) return [];
        const map = new Map<number, string>();
        for (const v of selected.variants) {
            map.set(v.listingId, v.listingUrl);
        }
        return Array.from(map.entries());
    }, [selected]);

    return (
        <div className="app">
            <header className="app-header">
                <h1>DealTracker Viewer</h1>
                <span className="db-path">{dbPath ?? "…"}</span>
            </header>

            {error && <div className="banner-error">{error}</div>}

            <div className="layout">
                <nav className="target-list">
                    {overview?.map((t) => (
                        <button
                            key={t.id}
                            className={"target-item" + (t.id === selectedId ? " selected" : "")}
                            onClick={() => setSelectedId(t.id)}
                        >
                            <div className="target-name">{t.name}</div>
                            <div className="target-meta">
                                {t.brand ?? "—"} · {t.category ?? "—"}
                            </div>
                        </button>
                    ))}
                </nav>

                <main className="main-panel">
                    {selected && (
                        <>
                            <section className="variant-cards">
                                {selected.variants.map((v) => (
                                    <div className="variant-card" key={`${v.listingId}-${v.title}`}>
                                        <div className="variant-title">{v.title ?? "(untitled)"}</div>
                                        <div className="variant-price">
                                            {v.effectivePrice !== null ? fmtY(v.effectivePrice) : "—"}
                                            {v.salePrice !== null && v.price !== null && (
                                                <span className="variant-was">
                                                    was {fmtY(v.price)}
                                                </span>
                                            )}
                                        </div>
                                        <div className="variant-badges">
                                            {v.underThreshold && <span className="badge badge-good">✓ Under threshold</span>}
                                            {v.availability === "out_of_stock" && (
                                                <span className="badge badge-critical">✕ Out of stock</span>
                                            )}
                                        </div>
                                        <div className="variant-observed">as of {formatDate(new Date(v.observedAt).getTime())}</div>
                                    </div>
                                ))}
                            </section>

                            {selected.rules.length > 0 && (
                                <section className="rules-row">
                                    {selected.rules.map((r) => (
                                        <span key={r.id} className={"rule-chip" + (r.active ? "" : " rule-inactive")}>
                                            {r.ruleType}
                                            {r.thresholdValue !== null ? ` < ${fmtY(r.thresholdValue)}` : ""}
                                        </span>
                                    ))}
                                </section>
                            )}

                            <section className="chart-section">
                                <h2>Price range across variants</h2>
                                <LineChart series={series} formatY={fmtY} formatX={formatDate} />
                            </section>

                            <section className="browse-section">
                                <h2>Raw observations</h2>
                                <select
                                    value={browseListingId ?? ""}
                                    onChange={(e) => setBrowseListingId(Number(e.target.value) || null)}
                                >
                                    <option value="">Pick a listing to browse…</option>
                                    {listings.map(([id, url]) => (
                                        <option key={id} value={id}>
                                            {url}
                                        </option>
                                    ))}
                                </select>

                                {observations && (
                                    <div className="table-scroll">
                                        <table className="raw-table">
                                            <thead>
                                                <tr>
                                                    <th>Observed at</th>
                                                    <th>Title</th>
                                                    <th>Availability</th>
                                                    <th>Price</th>
                                                    <th>Sale</th>
                                                    <th>Member</th>
                                                    <th>Subscribe</th>
                                                    <th>Promo</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {observations.map((o) => (
                                                    <tr key={o.id}>
                                                        <td>{formatDate(new Date(o.observedAt).getTime())}</td>
                                                        <td>{o.title ?? "—"}</td>
                                                        <td>{o.availability ?? "—"}</td>
                                                        <td>{o.price ?? "—"}</td>
                                                        <td>{o.salePrice ?? "—"}</td>
                                                        <td>{o.memberPrice ?? "—"}</td>
                                                        <td>{o.subscribePrice ?? "—"}</td>
                                                        <td>{o.promoText ?? "—"}</td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    </div>
                                )}
                            </section>
                        </>
                    )}

                    <section className="notifications-section">
                        <h2>Recent notifications</h2>
                        {notifications && notifications.length === 0 && <p className="muted">None fired yet.</p>}
                        {notifications && notifications.length > 0 && (
                            <table className="raw-table">
                                <thead>
                                    <tr>
                                        <th>Sent</th>
                                        <th>Watch target</th>
                                        <th>Rule</th>
                                        <th>Threshold</th>
                                        <th>Price at fire</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {notifications.map((n) => (
                                        <tr key={n.id}>
                                            <td>{formatDate(new Date(n.sentAt).getTime())}</td>
                                            <td>{n.watchTargetName}</td>
                                            <td>{n.ruleType}</td>
                                            <td>{n.thresholdValue ?? "—"}</td>
                                            <td>{n.observationEffectivePrice ?? "—"}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        )}
                    </section>
                </main>
            </div>
        </div>
    );
}
