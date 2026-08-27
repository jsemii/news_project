import { Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip } from "recharts";

// application.yml의 ai.industries와 같은 순서입니다. 산업마다 고정된 색을
// 배정하기 위한 기준 순서로만 씁니다 — API 응답은 건수 내림차순이라 매번 순서가
// 달라질 수 있는데, 색까지 그 순서를 따라가면 "금융이 어떤 날은 파란색, 어떤
// 날은 빨간색"처럼 보여서 혼란스럽습니다. 이 배열의 인덱스로 --chart-cat-1..8을
// 고정 배정하면, 데이터에 없는 산업이 있어도 나머지 색이 앞으로 당겨지지 않습니다.
const INDUSTRY_ORDER = [
  "금융",
  "제조",
  "반도체",
  "플랫폼/IT서비스",
  "유통/커머스",
  "에너지",
  "바이오/헬스케어",
  "공공/정부",
];

// [무엇을 받아서] 산업별 통계 목록(data: { industry, count }[]).
// [무엇을 하고] 각 조각에 INDUSTRY_ORDER 기준 고정 색을 배정한 파이 차트를
//              그립니다. 색만으로 산업을 구분하지 않도록 범례(Legend)를 항상
//              함께 보여줍니다.
// [무엇을 돌려주는지] 파이 차트(JSX).
export default function IndustryPieChart({ data }) {
  return (
    <ResponsiveContainer width="100%" height="100%">
      <PieChart>
        <Pie data={data} dataKey="count" nameKey="industry" outerRadius="70%">
          {data.map((entry) => {
            const colorIndex = INDUSTRY_ORDER.indexOf(entry.industry);
            const colorNumber = colorIndex === -1 ? 1 : (colorIndex % 8) + 1;
            return <Cell key={entry.industry} fill={`var(--chart-cat-${colorNumber})`} />;
          })}
        </Pie>
        <Legend wrapperStyle={{ color: "var(--text)", fontSize: 12 }} />
        <Tooltip
          contentStyle={{ background: "var(--surface)", border: "1px solid var(--border)", color: "var(--text)" }}
        />
      </PieChart>
    </ResponsiveContainer>
  );
}
