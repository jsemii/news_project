import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";

// [무엇을 받아서] 직무별 평균 점수 통계 목록(data: { job, avgScore }[]).
// [무엇을 하고] 직무별 평균 importance_score를 막대 차트로 그립니다. 단일
//              지표라 색은 --accent 하나로 통일합니다.
// [무엇을 돌려주는지] 막대 차트(JSX).
export default function JobScoreChart({ data }) {
  return (
    <ResponsiveContainer width="100%" height="100%">
      <BarChart data={data}>
        <CartesianGrid stroke="var(--border)" strokeOpacity={0.6} vertical={false} />
        <XAxis dataKey="job" tick={{ fill: "var(--text)", fontSize: 11 }} />
        <YAxis tick={{ fill: "var(--text)", fontSize: 11 }} />
        <Tooltip
          contentStyle={{ background: "var(--surface)", border: "1px solid var(--border)", color: "var(--text)" }}
          formatter={(value) => value.toFixed(2)}
        />
        <Bar dataKey="avgScore" fill="var(--accent)" radius={[4, 4, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  );
}
