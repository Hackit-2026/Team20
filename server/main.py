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
    description="SQLite SQL データベース & LM Studio (gemma4-12B qat) 連携サーバー",
    version="2.2.0"
)

# CORS設定（すべてのIPからのアクセス許可）
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# LM Studio 接続URLリスト（写真のアドレス 192.168.25.42 を最優先設定）
LM_STUDIO_URLS = [
    "http://192.168.25.42:11434/v1/chat/completions",
    "http://172.0.0.1:11434/v1/chat/completions",
    "http://127.0.0.1:11434/v1/chat/completions",
    "http://localhost:11434/v1/chat/completions"
]
MODEL_NAME = "gemma4-12B qat"

# --- SQLite データベース初期化関数 ---
def init_db():
    """SQLテーブルを自動生成・初期化"""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    if os.path.exists(SCHEMA_PATH):
        with open(SCHEMA_PATH, "r", encoding="utf-8") as f:
            schema_sql = f.read()
            cursor.executescript(schema_sql)
    conn.commit()
    conn.close()
    logger.info(f"SQLite DB initialized at {DB_PATH}")

# サーバー起動時にSQL DBを初期化
init_db()

# --- データモデル定義 ---
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
    comments: List[MemberComment]

class CreatePostRequest(BaseModel):
    author: str = "あなた"
    text: str

# --- 4人のキャラクター設定 ---
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
            "調子に乗らないでよね！…でも、耐えられたのは誇っていいんだからね！"
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

NPC_USERS = [
    "減煙挑戦中のタカシ",
    "禁煙3日目のサクラ",
    "ヤニモグラ育成中のケンジ",
    "タバコ我慢中のユウキ"
]

NPC_POST_TEMPLATES = [
    "今日はついに1本も吸わずに過ごせた！我慢できた！",
    "食後のタバコ我慢するのめちゃくちゃ辛い…耐えろ俺…！",
    "今日で我慢3日目！手が寂しいけど耐えてる！",
    "仕事の合間の1本をグッと堪えてお茶飲んだ！"
]

async def fetch_llm_comment(persona: dict, user_text: str) -> str:
    """LM Studio へ実リクエストを送信（優先IP: 192.168.25.42:11434）してコメント生成"""
    payload = {
        "model": MODEL_NAME,
        "messages": [
            {"role": "system", "content": persona["system_prompt"]},
            {"role": "user", "content": f"つぶやき内容：「{user_text}」\n上記のつぶやきに対して、あなたのキャラクターとして1〜2文で返信コメントしてください。"}
        ],
        "temperature": 0.7,
        "max_tokens": 100
    }

    for url in LM_STUDIO_URLS:
        try:
            async with httpx.AsyncClient(timeout=8.0) as client:
                logger.info(f"Trying to connect to LM Studio at {url}...")
                response = await client.post(url, json=payload)
                if response.status_code == 200:
                    data = response.json()
                    content = data["choices"][0]["message"]["content"].strip()
                    content = content.replace("AI", "").replace("人工知能", "").strip()
                    logger.info(f"Successfully generated comment via {url}")
                    return content
        except Exception as e:
            logger.debug(f"Connection to {url} failed: {e}")

    await asyncio.sleep(random.uniform(0.5, 1.2))
    return random.choice(persona["fallback_templates"])

async def generate_all_comments(user_text: str) -> List[MemberComment]:
    """4人のキャラクターから並行してリアルタイムコメントを生成"""
    tasks = [fetch_llm_comment(persona, user_text) for persona in PERSONA_CONFIGS]
    results = await asyncio.gather(*tasks)
    
    comments = []
    for i, persona in enumerate(PERSONA_CONFIGS):
        comments.append(
            MemberComment(
                name=persona["name"],
                avatar=persona["avatar"],
                comment=results[i]
            )
        )
    return comments

# --- SQL DB 操作関数 ---

def get_all_posts_from_db() -> List[Post]:
    """SQLデータベースからすべての投稿とコメントを取得"""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    cursor.execute("SELECT post_id, author, is_npc, text, timestamp FROM posts ORDER BY timestamp DESC")
    post_rows = cursor.fetchall()
    
    posts_list = []
    for row in post_rows:
        post_id, author, is_npc, text, timestamp = row
        cursor.execute("SELECT name, avatar, comment FROM comments WHERE post_id = ? ORDER BY comment_id ASC", (post_id,))
        comment_rows = cursor.fetchall()
        
        comments = [MemberComment(name=r[0], avatar=r[1], comment=r[2]) for r in comment_rows]
        posts_list.append(
            Post(
                postId=post_id,
                author=author,
                isNpc=bool(is_npc),
                text=text,
                timestamp=timestamp,
                comments=comments
            )
        )
    conn.close()
    return posts_list

def save_post_to_db(post: Post):
    """SQLデータベースに新規投稿とコメントを永続保存"""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    cursor.execute(
        "INSERT INTO posts (post_id, author, is_npc, text, timestamp) VALUES (?, ?, ?, ?, ?)",
        (post.postId, post.author, 1 if post.isNpc else 0, post.text, post.timestamp)
    )
    
    for comment in post.comments:
        cursor.execute(
            "INSERT INTO comments (post_id, name, avatar, comment, timestamp) VALUES (?, ?, ?, ?, ?)",
            (post.postId, comment.name, comment.avatar, comment.comment, post.timestamp)
        )
    
    conn.commit()
    conn.close()
    logger.info(f"Post {post.postId} saved to SQL Database.")

# --- エンドポイント ---

@app.get("/")
def read_root():
    return {
        "status": "online",
        "server": "ヤニモグラ Python Backend Server",
        "primary_ip": "192.168.25.42:8000",
        "lm_studio_target": "http://192.168.25.42:11434/v1/chat/completions",
        "model": MODEL_NAME
    }

@app.get("/api/feed", response_model=List[Post])
def get_feed():
    """SQLデータベースからタイムライン投稿およびコメント一覧を取得"""
    return get_all_posts_from_db()

@app.post("/api/posts", response_model=Post)
async def create_post(req: CreatePostRequest):
    """つぶやき投稿を受け取りSQL保存 ＆ LM Studio実推論でコメント生成"""
    new_id = f"post_{int(datetime.now().timestamp())}_{random.randint(100, 999)}"
    now_str = datetime.now().strftime("%Y-%m-%d %H:%M")
    
    comments = await generate_all_comments(req.text)
    
    new_post = Post(
        postId=new_id,
        author=req.author,
        isNpc=False,
        text=req.text,
        timestamp=now_str,
        comments=comments
    )
    
    save_post_to_db(new_post)
    return new_post

@app.post("/api/cron/bot-post", response_model=Post)
async def generate_npc_bot_post():
    """NPCからの定期投稿を生成しSQLデータベースへ永続保存"""
    author = random.choice(NPC_USERS)
    text = random.choice(NPC_POST_TEMPLATES)
    new_id = f"post_npc_{int(datetime.now().timestamp())}_{random.randint(100, 999)}"
    now_str = datetime.now().strftime("%Y-%m-%d %H:%M")
    
    comments = await generate_all_comments(text)
    
    npc_post = Post(
        postId=new_id,
        author=author,
        isNpc=True,
        text=text,
        timestamp=now_str,
        comments=comments
    )
    
    save_post_to_db(npc_post)
    return npc_post

if __name__ == "__main__":
    import uvicorn
    # 0.0.0.0 でホストすることにより 192.168.25.42 や localhost からアクセス可能
    uvicorn.run(app, host="0.0.0.0", port=8000)
