import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";

// [무엇을 받아서] 필터링 사유별 통계 목록(data: { reason, count }[]).
// [무엇을 하고] 규칙 기반 필터가 뉴스를 걸러낸 사유별 건수를 막대 차트로
//              그립니다. 단일 지표라 색은 --accent 하나로 통일합니다.
//              reason 값(예: "CONTENT_TOO_SHORT")이 길어서 가로로 4개를 나란히
//              쓰면 카드 폭 안에서 서로 겹칠 수 있으므로, 라벨을 -20도 기울이고
//              아래 여백을 넉넉히 줘서 4개 모두 겹치지 않고 읽히게 합니다(건수
//              차이가 커서— 예: TOO_OLD 180건 vs CONTENT_TOO_SHORT 3건 — 가장
//              작은 막대는 원래도 눈에 잘 안 띄는데, 라벨까지 겹치면 그 막대
//              자체가 없는 것처럼 보일 수 있습니다).
// [무엇을 돌려주는지] 막대 차트(JSX).
export default function FilteredReasonChart({ data }) {
  return (
    <ResponsiveContainer width="100%" height="100%">
      <BarChart data={data} margin={{ bottom: 24 }}>
        <CartesianGrid stroke="var(--border)" strokeOpacity={0.6} vertical={false} />
        <XAxis
          dataKey="reason"
          tick={{ fill: "var(--text)", fontSize: 11 }}
          interval={0}
          angle={-20}
          textAnchor="end"
          height={50}
        />
        <YAxis tick={{ fill: "var(--text)", fontSize: 11 }} allowDecimals={false} />
        <Tooltip
          contentStyle={{ background: "var(--surface)", border: "1px solid var(--border)", color: "var(--text)" }}
        />
        <Bar dataKey="count" fill="var(--accent)" radius={[4, 4, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  );
}
