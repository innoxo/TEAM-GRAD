# TEAM-GRAD - 앱 사용 기록 분석 프로젝트

## 팀 정보

- 팀명: **GRAD**
- 팀원: 나지원, 이재인

**한 줄 소개:**  
사용자의 앱 사용 기록 데이터 수집, 개별 사용량 로그 분석, 맞춤형 추천 메세지 제공

---

##  GitHub Repository
https://github.com/innoxo/TEAM-GRAD

---

## 📁 코드 구성 설명

| 경로 / 파일명               | 역할 |
|----------------------------|------|
| `apptracker/`              | Android 앱 전체 (앱 사용 기록 수집 및 Firebase 전송) |
| └── `app/`                 | 앱 메인 코드 |
| └── `build.gradle.kts`     | 앱 전체 의존성 및 빌드 설정 |
| `firebase_analysis.ipynb`  | Python 노트북 (GPT + Firebase 분석 백엔드) |
| `README.md`                | 프로젝트 설명서 (이 파일) |
| `Ground_Rule.MD`           | 팀 내부 규칙 정리 문서 |

---

##  사용 기술 스택

- Android Studio (Kotlin 기반)
- Firebase Realtime Database
- OpenAI GPT-4 API
- scikit-learn (TF-IDF, KMeans)
- Google Colab

---

##  전체 동작 흐름 요약

1. Android 앱이 사용자의 앱 실행 기록을 수집하여 `/usageStats/`에 저장  
2. Colab 노트북(`firebase_analysis.ipynb`) 실행  
   - `/usageStats/` → `/logs/` 변환  
   - GPT 기반 활동 요약 및 카테고리 분류  
   - 분류 결과 기반 점수 계산 및 `/users/{uid}/points` 저장  
3. 실시간 사용자 랭킹 출력  

---

##  실행 방법 (분석 노트북)

1. Colab에서 `firebase_analysis.ipynb` 열기
2. Firebase 인증 JSON 업로드
3. OpenAI API Key 입력
4. 셀 순서대로 실행

---

## 📝 예시 로그

```json
"user123": {
  "log1": "PUBG 게임을 2시간 플레이했습니다",
  "log2": "2시간 동안 수학 문제를 풀었습니다",
  "log3": "1시간 동안 한글 파일을 사용하여 문서 제작"
}
```

---

##  주요 기능

- 앱 사용 로그 기반 클러스터링 및 요약
- GPT 기반 카테고리 분류: [공부], [정보수집], [생산], [SNS], [엔터테인먼트], [기타]
- 활동 시간 기반 점수 계산
- 실시간 랭킹 출력 및 시각화 가능

---


