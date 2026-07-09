import { useMemo, useRef, useState } from "react";

export interface ChartPoint {
    x: number; // epoch ms
    y: number; // price
    detail?: string; // e.g. which variants share this price level, for the tooltip
}

export interface ChartSeries {
    key: string;
    label: string;
    color: string;
    points: ChartPoint[];
}

interface Props {
    series: ChartSeries[];
    formatY: (v: number) => string;
    formatX: (v: number) => string;
}

const WIDTH = 800;
const HEIGHT = 300;
const PAD_LEFT = 56;
const PAD_RIGHT = 16;
const PAD_TOP = 16;
const PAD_BOTTOM = 28;

export function LineChart({ series, formatY, formatX }: Props) {
    const [hoverIdx, setHoverIdx] = useState<number | null>(null);
    const [showTable, setShowTable] = useState(false);
    const svgRef = useRef<SVGSVGElement>(null);

    const nonEmpty = series.filter((s) => s.points.length > 0);

    const { xMin, xMax, yMin, yMax, allX } = useMemo(() => {
        const xs = nonEmpty.flatMap((s) => s.points.map((p) => p.x));
        const ys = nonEmpty.flatMap((s) => s.points.map((p) => p.y));
        const yLo = Math.min(...ys);
        const yHi = Math.max(...ys);
        const yPad = (yHi - yLo) * 0.1 || Math.max(1, yHi * 0.1);
        const sortedX = Array.from(new Set(xs)).sort((a, b) => a - b);
        return {
            xMin: Math.min(...xs),
            xMax: Math.max(...xs),
            yMin: Math.max(0, yLo - yPad),
            yMax: yHi + yPad,
            allX: sortedX,
        };
    }, [nonEmpty]);

    if (nonEmpty.length === 0) {
        return <div className="chart-empty">No observations yet for this watch target.</div>;
    }

    const plotW = WIDTH - PAD_LEFT - PAD_RIGHT;
    const plotH = HEIGHT - PAD_TOP - PAD_BOTTOM;

    const scaleX = (x: number) =>
        PAD_LEFT + (xMax === xMin ? plotW / 2 : ((x - xMin) / (xMax - xMin)) * plotW);
    const scaleY = (y: number) =>
        PAD_TOP + plotH - (yMax === yMin ? plotH / 2 : ((y - yMin) / (yMax - yMin)) * plotH);

    const yTicks = 4;
    const yTickValues = Array.from({ length: yTicks + 1 }, (_, i) => yMin + ((yMax - yMin) * i) / yTicks);

    const xTickCount = Math.min(5, allX.length);
    const xTickValues = Array.from({ length: xTickCount }, (_, i) =>
        allX[Math.round((i * (allX.length - 1)) / Math.max(1, xTickCount - 1))]
    );

    const directLabel = nonEmpty.length <= 4;

    function handleMove(e: React.MouseEvent<SVGSVGElement>) {
        const svg = svgRef.current;
        if (!svg) return;
        const rect = svg.getBoundingClientRect();
        const relX = ((e.clientX - rect.left) / rect.width) * WIDTH;
        const dataX = xMin + ((relX - PAD_LEFT) / plotW) * (xMax - xMin);
        let nearest = 0;
        let bestDist = Infinity;
        allX.forEach((x, i) => {
            const d = Math.abs(x - dataX);
            if (d < bestDist) {
                bestDist = d;
                nearest = i;
            }
        });
        setHoverIdx(nearest);
    }

    const hoverX = hoverIdx !== null ? allX[hoverIdx] : null;

    return (
        <div className="chart-wrap">
            {nonEmpty.length >= 2 && (
                <div className="chart-legend">
                    {nonEmpty.map((s) => (
                        <span key={s.key} className="legend-item">
                            <span className="legend-swatch" style={{ background: s.color }} />
                            {s.label}
                        </span>
                    ))}
                </div>
            )}
            <svg
                ref={svgRef}
                viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
                className="chart-svg"
                onMouseMove={handleMove}
                onMouseLeave={() => setHoverIdx(null)}
            >
                {yTickValues.map((v, i) => (
                    <g key={i}>
                        <line
                            x1={PAD_LEFT}
                            x2={WIDTH - PAD_RIGHT}
                            y1={scaleY(v)}
                            y2={scaleY(v)}
                            className="chart-gridline"
                        />
                        <text x={PAD_LEFT - 8} y={scaleY(v) + 4} textAnchor="end" className="chart-axis-label">
                            {formatY(v)}
                        </text>
                    </g>
                ))}
                {xTickValues.map((v, i) => (
                    <text key={i} x={scaleX(v)} y={HEIGHT - 8} textAnchor="middle" className="chart-axis-label">
                        {formatX(v)}
                    </text>
                ))}
                <line
                    x1={PAD_LEFT}
                    x2={WIDTH - PAD_RIGHT}
                    y1={HEIGHT - PAD_BOTTOM}
                    y2={HEIGHT - PAD_BOTTOM}
                    className="chart-baseline"
                />

                {hoverX !== null && (
                    <line
                        x1={scaleX(hoverX)}
                        x2={scaleX(hoverX)}
                        y1={PAD_TOP}
                        y2={HEIGHT - PAD_BOTTOM}
                        className="chart-crosshair"
                    />
                )}

                {nonEmpty.map((s) => {
                    const path = s.points
                        .map((p, i) => `${i === 0 ? "M" : "L"}${scaleX(p.x)},${scaleY(p.y)}`)
                        .join(" ");
                    const last = s.points[s.points.length - 1];
                    return (
                        <g key={s.key}>
                            <path d={path} className="chart-line" stroke={s.color} fill="none" />
                            <circle cx={scaleX(last.x)} cy={scaleY(last.y)} r={4} fill={s.color} />
                            {directLabel && (
                                <text
                                    x={scaleX(last.x) + 6}
                                    y={scaleY(last.y) + 4}
                                    className="chart-direct-label"
                                    fill={s.color}
                                >
                                    {s.label} · {formatY(last.y)}
                                </text>
                            )}
                        </g>
                    );
                })}
            </svg>

            {hoverIdx !== null && (
                <div className="chart-tooltip">
                    <div className="chart-tooltip-title">{formatX(allX[hoverIdx])}</div>
                    {nonEmpty.map((s) => {
                        const point = s.points.find((p) => p.x === allX[hoverIdx]);
                        if (!point) return null;
                        return (
                            <div key={s.key} className="chart-tooltip-row">
                                <span className="legend-swatch" style={{ background: s.color }} />
                                <div>
                                    <div>
                                        {s.label}: {formatY(point.y)}
                                    </div>
                                    {point.detail && <div className="chart-tooltip-detail">{point.detail}</div>}
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}

            <button className="table-toggle" onClick={() => setShowTable((v) => !v)}>
                {showTable ? "Hide table view" : "View as table"}
            </button>

            {showTable && (
                <table className="chart-table">
                    <thead>
                        <tr>
                            <th>When</th>
                            {nonEmpty.map((s) => (
                                <th key={s.key}>{s.label}</th>
                            ))}
                        </tr>
                    </thead>
                    <tbody>
                        {allX
                            .slice()
                            .reverse()
                            .map((x) => (
                                <tr key={x}>
                                    <td>{formatX(x)}</td>
                                    {nonEmpty.map((s) => {
                                        const point = s.points.find((p) => p.x === x);
                                        if (!point) return <td key={s.key}>—</td>;
                                        return (
                                            <td key={s.key}>
                                                {formatY(point.y)}
                                                {point.detail && (
                                                    <span className="table-detail"> ({point.detail})</span>
                                                )}
                                            </td>
                                        );
                                    })}
                                </tr>
                            ))}
                    </tbody>
                </table>
            )}
        </div>
    );
}
