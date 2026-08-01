from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime
import asyncio
import httpx
import sqlite3
import random
import logging
import os

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("server")

DB_PATH = os.path.join(os.path.dirname(__file__), "community.db")
SCHEMA_PATH = os.path.join(os.path.dirname(__file__), "schema.sql")

app = FastAPI(
    title="ヤニモグラ (YANI-GOTCHI) バックエンドサーバー",
    description="SQLite SQL データベース & LM Studio (google/gemma-4-12b-qat) 連携サーバー",
    version="2.8.0"
)

# CORS設定（すべてのIPからのアクセス許可）
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

LM_STUDIO_BASE_URLS = [
    "http://127.0.0.1:11434",
    "http://localhost:11434",
    "http://192.168.25.42:11434",
    "http://172.0.0.1:11434"
]

MODEL_CANDIDATES = [
    "google/gemma-4-12b-qat",
    "gemma-4-12b-qat",
    "gemma4-12B qat"
]

def init_db():
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    if os.path.exists(SCHEMA_PATH):
        with open(SCHEMA_PATH, "r", encoding="utf-8") as f:
            schema_sql = f.read()
            cursor.executescript(schema_sql)
    conn.commit()
    conn.close()
    logger.info(f"SQLite DB initialized at {DB_PATH}")

init_db()

class MemberComment(BaseModel):
    name: str
    avatar: str
    comment: str

class Post(BaseModel):
    postId: str
    author: str
    isNpc: bool
    text: str
    timestamp: str
    comments: List[MemberComment] = []
    memberComments: Optional[List[MemberComment]] = []

class CreatePostRequest(BaseModel):
    author: str = "あなた"
    text: str

PERSONA_CONFIGS = [
    {
        "name": "熱血仲間・修造",
        "avatar": "🔥",
        "system_prompt": "あなたはタバコの我慢・減煙に挑む仲間を全力で応援する熱血な親友です。「AI」「人工知能」「ボット」といった言葉は絶対に禁止です。本物の人間として熱い情熱と応援のコメントを1〜2文で短く書いてください。",
        "fallback_templates": [
            "素晴らしい我慢だ！！その熱い魂があれば絶対乗り越えられるぞ！🔥",
            "限界を打ち破れ！辛い時こそ自分を超えるチャンスだ！オオオオーッ！！",
            "ナイスだ！その1歩が最高の未来をつくる！明日も気合で突破だ！"
        ]
    },
    {
        "name": "ツンデレ友達・アスカ",
        "avatar": "😳",
        "system_prompt": "あなたは素直になれないツンデレな友達です。「AI」「人工知能」などの言葉は絶対禁止です。ついつい素っ気なく言ってしまいますが、心の底から相手を心配して褒めるコメントを1〜2文で書いてください。",
        "fallback_templates": [
            "べ、別に心配なんてしてないんだからね！でも…今日我慢できたのはちょっと偉いわよ。",
            "ふん、どうせ我慢できないと思ってたのに…まあ、がんばったじゃない。",
            "調子に乗らないでよね！…ポイントためて耐えられたのは誇っていいんだからね！"
        ]
    },
    {
        "name": "Dr.ヘルス",
        "avatar": "👨‍⚕️",
        "system_prompt": "あなたは親身な専属の医師です。「AI」「人工知能」などの言葉は絶対禁止です。医学的知識をベースに、温かく我慢を称えるコメントを1〜2文で書いてください。",
        "fallback_templates": [
            "素晴らしい我慢です。体内の酸素循環が確実に良くなっていますよ。",
            "見事な自己コントロールです。冷たいお水を飲んで深呼吸してくださいね。",
            "身体がどんどん健康に向かっています。この素晴らしい調子を維持しましょう。"
        ]
    },
    {
        "name": "ヤニモグラ",
        "avatar": "👹",
        "system_prompt": "あなたはタバコの煙が大好きな欲望の妖怪ヤニモグラです。「AI」「人工知能」などの言葉は絶対禁止です。タバコをおねだりしつつ、我慢するユーザーに悔しがるコメントを1〜2文で書いてください。",
        "fallback_templates": [
            "おい！俺のメシ（煙）を奪うな〜！！早くタバコ吸って俺を育てろ〜！！",
            "む、ム念だ…！頼むから1本だけでいいから吸ってくれ〜！",
            "我慢なんて身体に毒だぞ！？俺と一緒にモクモクしようぜ〜！"
        ]
    }
]

async def get_active_lm_studio_model(base_url: str) -> Optional[str]:
    models_url = f"{base_url}/v1/models"
    try:
        async with httpx.AsyncClient(timeout=0.8) as client:
            res = await client.get(models_url)
            if res.status_code == 200:
                data = res.json()
                if "data" in data and len(data["data"]) > 0:
                    return data["data"][0]["id"]
    except Exception:
        pass
    return None

async def fetch_llm_comment(persona: dict, user_text: str) -> str:
    messages = [
        {"role": "system", "content": persona["system_prompt"]},
        {"role": "user", "content": f"つぶやき内容：「{user_text}」\n上記のつぶやきに対して、あなたのキャラクターとして1〜2文で返信コメントしてください。"}
    ]

    for base_url in LM_STUDIO_BASE_URLS:
        endpoint_url = f"{base_url}/v1/chat/completions"
        active_model = await get_active_lm_studio_model(base_url)
        if not active_model:
            continue
            
        models_to_try = [active_model] + MODEL_CANDIDATES

        for model_name in models_to_try:
            payload = {
                "model": model_name,
                "messages": messages,
                "temperature": 0.7,
                "max_tokens": 100
            }

            try:
                async with httpx.AsyncClient(timeout=10.0) as client:
                    logger.info(f"Sending request to LM Studio ({base_url}) for {persona['name']}...")
                    response = await client.post(endpoint_url, json=payload)
                    if response.status_code == 200:
                        data = response.json()
                        raw_content = ""
                        choices = data.get("choices", [])
                        if choices:
                            msg = choices[0].get("message", {})
                            raw_content = msg.get("content", "") or ""
                        
                        cleaned = raw_content.replace("AI", "").replace("人工知能", "").strip()
                        # 🚨 空文字でなければそれを採用！空ならフォールバックへ回す
                        if cleaned and len(cleaned) > 0:
                            logger.info(f"✅ Generated valid comment for {persona['name']}: {cleaned}")
                            return cleaned
            except Exception as e:
                logger.warning(f"Failed connecting to LM Studio for {persona['name']}: {e}")

    # 🚨 空文字防止保障: 必ず実在するテキストを返す！
    fallback = random.choice(persona["fallback_templates"])
    logger.info(f"✅ Fallback comment guaranteed for {persona['name']}: {fallback}")
    return fallback

async def generate_and_save_bg_comments(post_id: str, text: str, timestamp: str):
    """バックグラウンドタスク: LM Studio からコメントを生成し、SQL DB へ確実に保存"""
    logger.info(f"🚀 AI comment generation background task started for post_id: {post_id}")
    try:
        tasks = [fetch_llm_comment(persona, text) for persona in PERSONA_CONFIGS]
        results = await asyncio.gather(*tasks)
        
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()
        
        for i, persona in enumerate(PERSONA_CONFIGS):
            comment_text = results[i]
            # 🚨 万一空文字が渡ってきた場合のセーフティガード
            if not comment_text or len(comment_text.strip()) == 0:
                comment_text = random.choice(persona["fallback_templates"])
                
            cursor.execute(
                "INSERT INTO comments (post_id, name, avatar, comment, timestamp) VALUES (?, ?, ?, ?, ?)",
                (post_id, persona["name"], persona["avatar"], comment_text, timestamp)
            )
            logger.info(f"   -> DB Inserted: {persona['avatar']} {persona['name']}: '{comment_text}'")
        
        conn.commit()
        conn.close()
        logger.info(f"🎉 Successfully saved 4 non-empty comments into SQL DB for post_id: {post_id}")
    except Exception as e:
        logger.error(f"❌ Error generating background comments for post_id {post_id}: {e}", exc_info=True)

def fix_empty_comments_in_db():
    """DB内に過去作られた空文字コメントを一気に補完クリーンアップ"""
    try:
        conn = sqlite3.connect(DB_PATH)
        cursor = conn.cursor()
        cursor.execute("SELECT comment_id, name FROM comments WHERE comment IS NULL OR comment = ''")
        empty_rows = cursor.fetchall()
        
        for row in empty_rows:
            c_id, name = row
            # ペルソナを特定して非空文言を充当
            matched_persona = next((p for p in PERSONA_CONFIGS if p["name"] == name), PERSONA_CONFIGS[0])
            new_comment = random.choice(matched_persona["fallback_templates"])
            cursor.execute("UPDATE comments SET comment = ? WHERE comment_id = ?", (new_comment, c_id))
            
        conn.commit()
        conn.close()
        if len(empty_rows) > 0:
            logger.info(f"🧹 Cleaned and updated {len(empty_rows)} legacy empty comments in SQL DB!")
    except Exception as e:
        logger.error(f"Error cleaning DB: {e}")

# 起動時に過去の空文字コメントを完全自動リペア！
fix_empty_comments_in_db()

def get_all_posts_from_db() -> List[Post]:
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    cursor.execute("SELECT post_id, author, is_npc, text, timestamp FROM posts ORDER BY timestamp DESC")
    post_rows = cursor.fetchall()
    
    posts_list = []
    for row in post_rows:
        post_id, author, is_npc, text, timestamp = row
        cursor.execute("SELECT name, avatar, comment FROM comments WHERE post_id = ? ORDER BY comment_id ASC", (post_id,))
        comment_rows = cursor.fetchall()
        
        comments = [MemberComment(name=r[0], avatar=r[1], comment=r[2]) for r in comment_rows if r[2] and len(r[2].strip()) > 0]
        posts_list.append(
            Post(
                postId=post_id,
                author=author,
                isNpc=bool(is_npc),
                text=text,
                timestamp=timestamp,
                comments=comments,
                memberComments=comments
            )
        )
    conn.close()
    return posts_list

@app.get("/")
def read_root():
    return {
        "status": "online",
        "server": "ヤニモグラ Python Backend Server",
        "primary_ip": "192.168.25.42:8000"
    }

@app.get("/api/feed", response_model=List[Post])
def get_feed():
    """SQLデータベースからタイムライン投稿および最新コメント一覧を取得（リロード時に呼び出し）"""
    return get_all_posts_from_db()

@app.post("/api/posts", response_model=Post)
async def create_post(req: CreatePostRequest):
    """
    ⚡ ポストが来たら即座にSQL DBへ保存してスマホへ返し、
    非同期タスクでAIにコメントを考えてもらいSQL DBへ自動挿入！
    """
    logger.info(f"📥 Received post: '{req.text}'")
    new_id = f"post_{int(datetime.now().timestamp())}_{random.randint(100, 999)}"
    now_str = datetime.now().strftime("%Y-%m-%d %H:%M")
    
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute(
        "INSERT INTO posts (post_id, author, is_npc, text, timestamp) VALUES (?, ?, ?, ?, ?)",
        (new_id, req.author, 0, req.text, now_str)
    )
    conn.commit()
    conn.close()
    
    # ⚡ 非同期タスクでAIコメント生成＆SQL DB挿入を発火
    asyncio.create_task(generate_and_save_bg_comments(new_id, req.text, now_str))
    
    new_post = Post(
        postId=new_id,
        author=req.author,
        isNpc=False,
        text=req.text,
        timestamp=now_str,
        comments=[],
        memberComments=[]
    )
    
    return new_post

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
