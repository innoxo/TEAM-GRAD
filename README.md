# **Project \- brief**

## **프로젝트 설명**

#### **휴대폰 사용 제어에 어려움을 겪는 사용자들을 위한 AI 분석 기반 커뮤니티 게임형 관리 시스템**

## **Keyword**

#### **스마트폰 사용 제어, 커뮤니티 기반 게임, AI 분석**

## **주요 기능**

* 사용자 맞춤 퀘스트 설정: 원하는 앱을 직접 선택하여 제어하고자 하는 사용 시간을 설정  
* 분석 & 요약 피드백: AI를 통한 앱 사용 로그 분석 및 피드백 제공  
* 포인트 \+ 랭킹 & 추가 퀘스트 추천: 퀘스트 달성도에 따른 포인트 부여로 다른 사용자간 경쟁, 앱 사용 로그 기반 관련 퀘스트 추천  
* 커뮤니티 기능: 유사 퀘스트를 도전하는 다른 유저들과 소통

<br>

# **Play & Focus (프로젝트 상세)**

**2025년도 캡스톤 디자인 프로젝트 (15팀 GRAD)** \> **팀원:** 이재인, 나지원

## **프로젝트 상세 개요**

본 프로젝트는 기존의 강제적인 앱 차단 방식에서 벗어나, **AI 분석**과 **게이미피케이션(퀘스트, 랭킹)** 요소를 결합하여 사용자가 스스로 스마트폰 사용 습관을 개선하도록 돕는 안드로이드 애플리케이션입니다.

### **💡 기능 구현 기술 상세 (Technical Features)**

* **정밀 사용량 측정**: UsageStatsManager의 이벤트를 분석하여 백그라운드 실행을 제외한 **실질적 포그라운드 사용 시간**을 정밀하게 측정 및 시각화 (Pie Chart 사용).  
* **AI 하루 요약**: OpenAI GPT-4o-mini를 활용하여 사용자의 하루 앱 사용 패턴을 정성적으로 분석하고 격려 메시지 제공.  
* **퀘스트 시스템**: 특정 앱 사용 시간 줄이기/늘리기 목표 설정 및 성공/실패 판정 로직 구현.  
* **맞춤 퀘스트 추천**: 앱 내부(On-Device)에서 동작하는 **TF-IDF 및 K-Means 알고리즘**을 통해 사용자 패턴과 유사한 앱 퀘스트 자동 추천.  
* **랭킹 시스템**: Firebase Realtime DB 기반 사용자 간 포인트 경쟁 시스템 구현.

## **기술 스택**

| 구분 | 기술 / 라이브러리 | 설명 |
| :---- | :---- | :---- |
| **Language** | Kotlin | Android 앱 개발 주 언어 |
| **UI** | Jetpack Compose | 최신 선언형 UI 툴킷 사용 (Material3) |
| **Architecture** | MVVM | View, ViewModel, Model 분리 패턴 적용 |
| **Backend / DB** | Firebase Realtime DB | 사용자 정보, 퀘스트, 랭킹 데이터 실시간 동기화 |
| **AI / API** | OpenAI API | 앱 카테고리 분류 및 하루 요약 텍스트 생성 |
| **Algorithm** | Custom TF-IDF & K-Means | 앱 군집화 및 추천 알고리즘 자체 구현 (Kotlin) |
| **Chart** | MPAndroidChart | 앱 사용량 통계 시각화 |

## **프로젝트 구조 (Directory Structure)**

app/src/main/java/com/example/apptracker  
├── ai                  \# AI 알고리즘 엔진 (AppClusteringEngine.kt)  
├── ui                  \# UI 화면 (Dashboard, Quest, Ranking 등)  
├── viewmodel           \# 비즈니스 로직 (UsageViewModel, QuestViewModel 등)  
├── repository          \# 데이터 통신 (QuestRepository)  
├── service             \# 외부 API 통신 (OpenAIService)  
└── model               \# 데이터 모델 (QuestItem, AppUsage 등)

## **설치 및 실행 방법 (How to Build & Run)**

이 프로젝트를 로컬 환경에서 실행하기 위해서는 다음 단계가 필요합니다.

### **1\. 사전 요구 사항**

* **Android Studio**: Koala Feature Drop 이상 권장  
* **JDK**: Version 17 이상  
* **Android SDK**: API Level 26 (Oreo) 이상

### **2\. 프로젝트 클론**

git clone 진행

### **3\. 환경 설정**

보안상의 이유로 API Key와 Firebase 설정 파일은 깃허브에 포함되지 않았습니다. 실행을 위해 다음 파일들을 추가해야 합니다.

**A. Firebase 설정 (google-services.json)**

1. Firebase 콘솔에서 프로젝트 생성 후 Android 앱 추가.  
2. 다운로드 받은 google-services.json 파일을 app/ 폴더 내부에 위치시킵니다.

**B. OpenAI API Key 설정**

1. app/src/main/java/com/example/apptracker/OpenAIService.kt 파일을 엽니다.  
2. apiKey 변수에 유효한 OpenAI API Key를 입력합니다.  
   private val apiKey \= "sk-proj-..." // 본인의 API Key 입력

### **4\. 빌드 및 실행**

1. Android Studio에서 프로젝트를 엽니다 (File \> Open).  
2. Gradle Sync가 완료될 때까지 기다립니다.  
3. 상단 메뉴의 Run \> Run 'app'을 클릭하거나 Shift \+ F10을 누릅니다.  
4. 실행된 에뮬레이터 또는 기기에서 **"다른 앱 위에 그리기"** 및 **"사용 정보 접근 허용"** 권한을 승인해야 정상 작동합니다.

## **데이터 입력 및 테스트 방법**

### **1\. 데이터 수집 (자동)**

* 앱을 최초 실행하고 권한을 허용하면, 안드로이드 시스템(UsageStatsManager)으로부터 **지난 24시간의 앱 사용 기록**을 자동으로 불러옵니다.  
* 별도의 수동 입력 없이 대시보드에서 차트와 요약 메시지를 확인할 수 있습니다.

### **2\. 퀘스트 생성 및 테스트**

1. 메인 화면에서$$퀘스트 만들기$$  
   버튼 클릭.  
2. **"AI 맞춤 추천"** 섹션에서 추천된 앱을 선택하거나, 하단 리스트에서 원하는 앱 선택.  
3. 목표 시간(예: 30분)과 조건(이하/이상) 설정 후 생성.  
4. 실제 해당 앱을 사용(또는 미사용)한 뒤, 시간이 지나면 성공/실패 여부가 자동 판정됩니다.

## **라이브러리 및 오픈소스 출처 (Open Source Attribution)**

본 프로젝트는 다음 오픈소스 라이브러리를 활용하였습니다.

* **Retrofit2 & OkHttp3**: HTTP 통신 및 API 호출  
* **MPAndroidChart**: 데이터 시각화 (Apache License 2.0)  
* **Firebase Android SDK**: 실시간 데이터베이스 연동 (Apache License 2.0)  
* **Google GSON**: JSON 데이터 파싱


# **Ground Rule**

## **📝회의**

* **대면 회의**: 매주 화요일 캡스톤 수업 종료 후 필요시에 진행한다.  
* **비대면 회의**: 추가 토의가 필요할 경우, 즉각적으로 시간을 조율하여 디스코드에서 토의를 진행한다.

###### **추가 회의 시간은 1시간 30분을 넘지 않도록 한다.**

## **📋스터디**

* 각자 프로젝트에 필요할 파트를 공부하고, 대면 회의 시작 전 진행 상황을 리포트 한다.

## **🖥️기타**

* 프로젝트 관련 토의 및 아이디어 공유는 디스코드에 기록한다.  
* 회의 시 서기는 돌아가며 기록한다.  
* 회의 기록은 상대방이 최종 확인한 후 공유 드라이브에 업로드 한다.  
* 이미지 자료 및 제출물은 공유 드라이브에 저장한다.
