import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // 개발 서버(보통 5173 포트)가 "/api"로 시작하는 요청을 백엔드(8080 포트)로
    // 그대로 전달해줍니다. 이게 없으면 프론트(5173)에서 백엔드(8080)로 직접 fetch할 때
    // 포트가 달라서 브라우저의 CORS(다른 출처 간 요청 제한) 정책에 막히는데, 백엔드에
    // CORS 설정을 새로 추가하는 대신 Vite에 이미 내장된 프록시 기능만으로 해결합니다
    // (새 라이브러리 불필요).
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
