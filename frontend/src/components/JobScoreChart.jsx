import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";

// [무엇을 받아서] 직무별 평균 점수 통계 목록(data: { job, avgScore, sampleCount }[]).
//              avgScore는 importance_score가 0(V3 마이그레이션 이전 레거시
//              기본값)인 행을 제외하고 계산된 값입니다 — 백엔드 StatsMapper.xml
//              참고. sampleCount는 그 평균이 몇 건을 근거로 했는지입니다.
// [무엇을 하고] 직무별 평균 importance_score를 막대 차트로 그립니다. 단일
//              지표라 색은 --accent 하나로 통일합니다. 툴팁에 몇 건을 근거로 한
//              평균인지(sampleCount)도 함께 보여줘서, "1~10점 척도인데 평균이
//              낮아 보인다"는 오해 없이 실제 분석된 건수 기준 평균임을 알 수
//              있게 합니다.
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
          formatter={(value, name, item) => [`${value.toFixed(2)}점 (${item.payload.sampleCount}건 기준)`, "평균 점수"]}
        />
        <Bar dataKey="avgScore" fill="var(--accent)" radius={[4, 4, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  );
}
