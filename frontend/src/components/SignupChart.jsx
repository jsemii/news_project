import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";

// [무엇을 받아서] 일별 가입 통계 목록(data: { date, githubCount, googleCount }[],
//              항상 14개).
// [무엇을 하고] 날짜별 가입자 수를 provider(github/google)별로 누적 막대로
//              그립니다. 다른 차트와 달리 색이 provider 정체성을 나타내므로(이
//              대시보드에서 파이 차트 다음으로 색이 의미를 갖는 두 번째 차트),
//              범례를 함께 보여주고 기존 카테고리 팔레트의 앞 두 색(--chart-cat-1/2)을
//              재사용합니다(새 색상 토큰을 따로 만들지 않음). 가입자 수가 보통
//              하루 0~2명 수준으로 적어서, 별도 라인 2개보다 누적 막대가 "그날 총
//              가입자 수"와 "provider 구성"을 한 번에 더 잘 보여줍니다.
// [무엇을 돌려주는지] 누적 막대 차트(JSX).
export default function SignupChart({ data }) {
  return (
    <ResponsiveContainer width="100%" height="100%">
      <BarChart data={data}>
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
        <Legend wrapperStyle={{ color: "var(--text)", fontSize: 12 }} />
        <Bar dataKey="githubCount" name="GitHub" stackId="signups" fill="var(--chart-cat-1)" radius={[0, 0, 0, 0]} />
        <Bar dataKey="googleCount" name="Google" stackId="signups" fill="var(--chart-cat-2)" radius={[4, 4, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  );
}
