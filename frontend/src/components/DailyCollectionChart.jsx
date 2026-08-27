import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";

// [무엇을 받아서] 일별 수집 통계 목록(data: { date, count }[], 항상 14개).
// [무엇을 하고] 날짜별 수집 건수를 라인 차트로 그립니다. 단일 지표라 색이 정체성을
//              나타낼 필요가 없으므로 기존 --accent 색 하나만 씁니다.
// [무엇을 돌려주는지] 라인 차트(JSX).
export default function DailyCollectionChart({ data }) {
  return (
    <ResponsiveContainer width="100%" height="100%">
      <LineChart data={data}>
        <CartesianGrid stroke="var(--border)" strokeOpacity={0.6} vertical={false} />
        <XAxis
          dataKey="date"
          tick={{ fill: "var(--text)", fontSize: 11 }}
          tickFormatter={(date) => date.slice(5)}
        />
        <YAxis tick={{ fill: "var(--text)", fontSize: 11 }} allowDecimals={false} />
        <Tooltip
          contentStyle={{ background: "var(--surface)", border: "1px solid var(--border)", color: "var(--text)" }}
        />
        <Line type="monotone" dataKey="count" stroke="var(--accent)" strokeWidth={2} dot={false} />
      </LineChart>
    </ResponsiveContainer>
  );
}
