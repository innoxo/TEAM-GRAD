# GPT 분석과 Firebase 연동을 모두 포함한 메인 백엔드 스크립트.
# 안드로이드 앱이 올린 원본 데이터를 분석하여 결과를 다시 Firebase에 저장합니다.


import firebase_admin
from firebase_admin import credentials, db
import openai
from datetime import date, datetime
import json
import pytz
import re
import os # 환경변수용
from dotenv import load_dotenv

# .env 파일에서 환경 변수를 로드
load_dotenv()

FIREBASE_URL = "https://apptracker-26831-default-rtdb.firebaseio.com/"
TODAY = date.today().strftime('%Y-%m-%d')
KST = pytz.timezone('Asia/Seoul')

# Firebase 및 OpenAI 인증 (환경 변수 사용)  --> 이거 진행해야 함
try:
    firebase_creds_json = os.environ.get("GOOGLE_CREDENTIALS_JSON")
    service_account_key = json.loads(firebase_creds_json)
    openai.api_key = os.environ.get("OPENAI_API_KEY")

    if not firebase_admin._apps:
        cred = credentials.Certificate(service_account_key)
        firebase_admin.initialize_app(cred, {"databaseURL": FIREBASE_URL})

    ref = db.reference()
    print(f"Firebase 연결 완료 (오늘 날짜: {TODAY})")
except Exception as e:
    print(f"인증 정보 로드 실패: 환경 변수를 확인해주세요. ({e})")
    exit()

# 카테고리별 고정 설정값
CATEGORY_SCORES = {"공부": 100, "정보수집": 30, "생산": 50, "SNS": 20, "엔터테인먼트": 5, "기타": 0}

# 유틸리티 및 GPT
def log_point_transaction(uid, points, reason):
    if points == 0: return
    log_entry = {"points": points, "reason": reason, "timestamp": datetime.now(KST).isoformat()}
    ref.child(f"pointsLog/{uid}").push(log_entry)
    print(f"  - 포인트 로그 기록: {uid}, {points}점 ({reason})")

def get_gpt_analysis(usage_data_for_prompt):
    """GPT API를 호출하여 앱 사용 내역을 분석하고 JSON 결과를 반환합니다."""
    prompt = (
        "다음은 사용자의 오늘 앱 사용 요약입니다(분 단위).\n"
        "이 활동들을 종합하여 오늘 하루를 대표하는 하나의 요약 문장을 만들어주세요.\n"
        "그리고 각 앱을 [공부, 정보수집, 생산, SNS, 엔터테인먼트, 기타] 중 하나로 분류하고, 카테고리별 총 사용 시간을 계산해주세요.\n"
        "응답은 반드시 다음 JSON 형식으로만 해주세요:\n"
        "{\"summary\": \"오늘의 요약 문장\", \"categoryMinutes\": {\"공부\": 120, \"SNS\": 30, ...}}\n\n"
        "데이터:\n"
        + "\n".join(usage_data_for_prompt)
    )
    try:
        res = openai.ChatCompletion.create(
            model="gpt-4o-mini",
            messages=[{"role": "user", "content": prompt}],
            response_format={"type": "json_object"}
        )
        content = res["choices"][0]["message"]["content"]
        return json.loads(content)
    except Exception as e:
        print(f"GPT 요청 실패: {e}")
        return {"summary": "활동 요약 생성에 실패했습니다.", "categoryMinutes": {}}

# 퀘스트 처리
def process_user_quests(uid, daily_app_usage_minutes):
    """QuestItem의 퀘스트를 처리하고 보상 포인트를 계산합니다."""
    quests_ref = ref.child(f"quests/{uid}")
    user_quests = quests_ref.get()
    if not user_quests: return 0

    now = datetime.now(KST)
    total_quest_points = 0

    for quest_id, quest_data in user_quests.items():
        if quest_data.get("completed", False): continue
        try:
            deadline_str = f"{quest_data['deadlineDate']} {quest_data['deadlineTime']}"
            deadline = KST.localize(datetime.strptime(deadline_str, "%Y-%m-%d %H:%M:%S"))
        except (KeyError, ValueError): continue
        
        if now < deadline: continue

        package_name = quest_data.get("packageName")
        target_minutes = int(quest_data.get("targetMinutes", 0))
        actual_minutes = int(daily_app_usage_minutes.get(package_name, 0))

        if actual_minutes >= target_minutes:
            reward = (target_minutes // 30) * 50
            total_quest_points += reward
            quests_ref.child(quest_id).update({"completed": True})
            log_point_transaction(uid, reward, f"퀘스트 성공: {quest_data.get('appName')}")
            print(f"퀘스트 성공! '{quest_data.get('appName')}' (+{reward}점)")
    
    return total_quest_points

# 데이터 분석 및 업데이트
def analyze_and_update_all():
    all_usage_stats = ref.child(f"usageStats/{TODAY}").get()
    if not all_usage_stats:
        print("분석할 사용 기록이 없습니다.")
        return

    for uid, apps in all_usage_stats.items():
        print(f"--- 사용자 {uid} 분석 시작 ---")
        current_points = ref.child(f"users/{uid}/points").get() or 0
        
        # 데이터 가공 파트
        daily_app_usage_minutes = {pkg: round(data.get("usedTimeMillis", 0) / 60000) for pkg, data in apps.items()}
        usage_for_prompt = [f"- {pkg}: {mins}분" for pkg, mins in daily_app_usage_minutes.items()]

        # GPT 분석 수행 파트
        analysis_result = get_gpt_analysis(usage_for_prompt)
        category_minutes = analysis_result.get("categoryMinutes", {})
        
        # 활동 점수 계산 파트
        activity_score = sum(CATEGORY_SCORES.get(cat, 0) for cat, mins in category_minutes.items() if mins > 0)
        
        # 퀘스트 처리 파트
        quest_points = process_user_quests(uid, daily_app_usage_minutes)

        # 최종 포인트 합산 및 저장 파트
        total_score_today = activity_score + quest_points
        final_total_points = current_points + total_score_today
        ref.child(f"users/{uid}/points").set(final_total_points)
        if activity_score > 0: log_point_transaction(uid, activity_score, "일일 활동 보상")
        print(f"\n{uid}에게 총 {total_score_today}점 적립 완료 (누적: {final_total_points}점)")

        # 대시보드 데이터 저장
        dashboard_data = {
            "dailySummary": analysis_result.get("summary", "요약 없음"),
            "categoryMinutes": category_minutes,
            "totalMinutes": sum(daily_app_usage_minutes.values())
        }
        ref.child(f"dashboard/{uid}/{TODAY}").set(dashboard_data)
        print(f"{uid}의 대시보드 데이터 저장 완료")

def update_ranking_data():
    """전체 사용자의 포인트를 기준으로 /ranking 데이터를 생성/업데이트합니다."""
    print("\n>> 전체 사용자 랭킹 업데이트 중...")
    users_data = ref.child("users").get()
    if not users_data: return

    ranking = [{"uid": uid, "name": data.get("name", "익명"), "points": data.get("points", 0)}
               for uid, data in users_data.items()]

    sorted_ranking = sorted(ranking, key=lambda x: x["points"], reverse=True)
    ref.child("ranking").set(sorted_ranking)
    print("랭킹 데이터 Firebase 업데이트 완료\n")
    
    print("--- 실시간 사용자 랭킹 ---")
    for i, user in enumerate(sorted_ranking[:10], 1):
        print(f"{i}위 - {user['name']}({user['uid']}) : {user['points']}점")

# 메인 실행 블록
if __name__ == "__main__":
    analyze_and_update_all()
    update_ranking_data()
    print("\n 모든 작업이 완료되었습니다.")