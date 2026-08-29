import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";

// [무엇을 받아서] 산업별 스크랩 건수 목록(data: { industry, count }[], GET
//              /api/scraps/industries 응답 그대로 — 건수 내림차순으로 이미
//              정렬돼 있음).
// [무엇을 하고] 산업을 세로축(카테고리), 건수를 가로축(수치)에 두는 가로 막대
//              차트로 그립니다. 축/라벨로 이미 산업이 구분되는 단일 지표라
//              (관리자 대시보드의 다른 단일 계열 차트와 같은 원칙) 색은
//              --accent 하나만 씁니다 — 산업마다 다른 색을 쓰는 건 정체성을
//              중복으로 인코딩하는 것이라 오히려 불필요합니다(파이 차트처럼
//              색 자체가 식별 수단인 경우와 다름).
// [무엇을 돌려주는지] 가로 막대 차트(JSX).
export default function ScrapIndustryChart({ data }) {
  return (
    <ResponsiveContainer width="100%" height="100%">
      <BarChart data={data} layout="vertical" margin={{ left: 12 }}>
        <CartesianGrid stroke="var(--border)" strokeOpacity={0.6} horizontal={false} />
        <XAxis type="number" allowDecimals={false} tick={{ fill: "var(--text)", fontSize: 11 }} />
        <YAxis
          type="category"
          dataKey="industry"
          width={100}
          tick={{ fill: "var(--text)", fontSize: 12 }}
        />
        <Tooltip
          contentStyle={{ background: "var(--surface)", border: "1px solid var(--border)", color: "var(--text)" }}
        />
        <Bar dataKey="count" fill="var(--accent)" radius={[0, 4, 4, 0]} />
      </BarChart>
    </ResponsiveContainer>
  );
}
