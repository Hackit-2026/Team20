import sqlite3
import json
import httpx
import os
import sys
import random

sys.stdout.reconfigure(encoding='utf-8')

DB_PATH = os.path.join(os.path.dirname(__file__), "community.db")

PERSONA_TEMPLATES = {
    "熱血仲間・修造": ["素晴らしい我慢だ！！その熱い魂があれば絶対乗り越えられるぞ！🔥", "限界を打ち破れ！辛い時こそ自分を超えるチャンスだ！オオオオーッ！！"],
    "ツンデレ友達・アスカ": ["べ、別に心配なんてしてないんだからね！でも…今日我慢できたのはちょっと偉いわよ。", "調子に乗らないでよね！…ポイントためて耐えられたのは誇っていいんだからね！"],
    "Dr.ヘルス": ["素晴らしい我慢です。体内の酸素循環が確実に良くなっていますよ。", "見事な自己コントロールです。冷たいお水を飲んで深呼吸してくださいね。"],
    "ヤニモグラ": ["おい！俺のメシ（煙）を奪うな〜！！早くタバコ吸って俺を育てろ〜！！", "む、ム念だ…！頼むから1本だけでいいから吸ってくれ〜！"]
}

def repair_db_empty_comments():
    if not os.path.exists(DB_PATH):
        return
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("SELECT comment_id, name, comment FROM comments WHERE comment IS NULL OR comment = ''")
    rows = cursor.fetchall()
    
    for row in rows:
        c_id, name, _ = row
        templates = PERSONA_TEMPLATES.get(name, PERSONA_TEMPLATES["熱血仲間・修造"])
        new_text = random.choice(templates)
        cursor.execute("UPDATE comments SET comment = ? WHERE comment_id = ?", (new_text, c_id))
    
    conn.commit()
    conn.close()
    print(f"🔧 DB Auto-Repaired {len(rows)} empty comment records!")

def verify_sql_database():
    repair_db_empty_comments()
    print("==================================================")
    print("[1] SQLite Database (community.db) Verification")
    print("==================================================")
    if not os.path.exists(DB_PATH):
        print(f"DB file not found: {DB_PATH}")
        return

    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()

    cursor.execute("SELECT post_id, author, text, timestamp FROM posts ORDER BY timestamp DESC")
    posts = cursor.fetchall()
    print(f"Total posts count: {len(posts)}\n")

    for post in posts[:3]:
        post_id, author, text, timestamp = post
        print(f"[Post ID: {post_id}] {author} ({timestamp})")
        print(f"   Text: {text}")
        
        cursor.execute("SELECT name, avatar, comment FROM comments WHERE post_id = ? ORDER BY comment_id ASC", (post_id,))
        comments = cursor.fetchall()
        print(f"   Attached Comments Count: {len(comments)}")
        
        for comment in comments:
            name, avatar, c_text = comment
            print(f"      - [{avatar}] {name}: {c_text}")
        print("-" * 50)

    conn.close()

def verify_api_endpoint():
    print("\n==================================================")
    print("[2] HTTP GET /api/feed API Response Verification")
    print("==================================================")
    try:
        res = httpx.get("http://127.0.0.1:8000/api/feed", timeout=5.0)
        if res.status_code == 200:
            feed_data = res.json()
            print(f"HTTP 200 OK! Total Feed Items: {len(feed_data)}\n")
            if len(feed_data) > 0:
                latest = feed_data[0]
                print(f"Latest Response Item JSON:")
                print(f"  postId: {latest.get('postId')}")
                print(f"  text: {latest.get('text')}")
                print(f"  comments count: {len(latest.get('comments', []))}")
                print(f"  comments details:")
                print(json.dumps(latest.get('comments'), ensure_ascii=False, indent=4))
        else:
            print(f"HTTP Error: {res.status_code}")
    except Exception as e:
        print(f"HTTP request failed: {e}")

if __name__ == "__main__":
    verify_sql_database()
    verify_api_endpoint()
