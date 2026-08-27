import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";

// [무엇을 받아서] 필터링 사유별 통계 목록(data: { reason, count }[]).
// [무엇을 하고] 규칙 기반 필터가 뉴스를 걸러낸 사유별 건수를 막대 차트로
//              그립니다. 단일 지표라 색은 --accent 하나로 통일합니다.
// [무엇을 돌려주는지] 막대 차트(JSX).
export default function FilteredReasonChart({ data }) {
  return (
    <ResponsiveContainer width="100%" height="100%">
      <BarChart data={data}>
        <CartesianGrid stroke="var(--border)" strokeOpacity={0.6} vertical={false} />
        <XAxis dataKey="reason" tick={{ fill: "var(--text)", fontSize: 11 }} />
        <YAxis tick={{ fill: "var(--text)", fontSize: 11 }} allowDecimals={false} />
        <Tooltip
          contentStyle={{ background: "var(--surface)", border: "1px solid var(--border)", color: "var(--text)" }}
        />
        <Bar dataKey="count" fill="var(--accent)" radius={[4, 4, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  );
}
