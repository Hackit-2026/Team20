from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime
import asyncio
import httpx
import random
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("server")

app = FastAPI(
    title="ヤニモグラ (YANI-GOTCHI) バックエンドサーバー",
    description="コミュニティタイムライン & LM Studio(gemma4-12B qat) 連携バックエンド",
    version="2.0.0"
)

# CORS設定（Androidエミュレータ / 実機からの接続許可）
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# LM Studio 接続設定
LM_STUDIO_URLS = [
    "http://172.0.0.1:11434/v1/chat/completions",
    "http://127.0.0.1:11434/v1/chat/completions",
    "http://localhost:11434/v1/chat/completions"
]
MODEL_NAME = "gemma4-12B qat"

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

# --- 4人のキャラクター設定（※「AI」という表現は一切排除） ---
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

# メモリ内タイムラインDB
posts_db: List[Post] = []

async def fetch_llm_comment(persona: dict, user_text: str) -> str:
    """LM Studio (172.0.0.1:11434 / 127.0.0.1:11434 / gemma4-12B qat) へ実リクエストを送信してコメント生成"""
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
                logger.info(f"Connecting to LM Studio ({url}) with model {MODEL_NAME}...")
                response = await client.post(url, json=payload)
                if response.status_code == 200:
                    data = response.json()
                    content = data["choices"][0]["message"]["content"].strip()
                    # 「AI」という単語が含まれていた場合は除去
                    content = content.replace("AI", "").replace("人工知能", "").strip()
                    logger.info(f"Received LLM response for {persona['name']}: {content}")
                    return content
        except Exception as e:
            logger.warning(f"LM Studio connection to {url} failed: {e}")

    # LM Studioがオフラインの際のフォールバック（自然な実推論遅延を再現）
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

# --- エンドポイント ---

@app.get("/")
def read_root():
    return {
        "status": "online",
        "server": "ヤニモグラ Python Backend (LM Studio Direct Integration)",
        "lm_studio_model": MODEL_NAME,
        "target_ip": "172.0.0.1:11434 / 127.0.0.1:11434"
    }

@app.get("/api/feed", response_model=List[Post])
def get_feed():
    """タイムライン投稿および全メンバーのコメント一覧取得"""
    if not posts_db:
        # 初回起動時にデフォルト投稿を挿入
        posts_db.append(
            Post(
                postId="post_init_1",
                author="減煙挑戦中のタカシ",
                isNpc=True,
                text="今日で我慢3日目！手が寂しいけど耐えてる！",
                timestamp="2026-08-01 15:30",
                comments=[
                    MemberComment(name="熱血仲間・修造", avatar="🔥", comment="3日突破はお前の勝利だ！その情熱を燃やし続けろ！！"),
                    MemberComment(name="ツンデレ友達・アスカ", avatar="😳", comment="ふ、ふん！3日くらいで喜ばないでよね！…応援してるけど。"),
                    MemberComment(name="Dr.ヘルス", avatar="👨‍⚕️", comment="3日目は体内のニコチンが抜ける大事な節目です。素晴らしい！"),
                    MemberComment(name="ヤニモグラ", avatar="👹", comment="うがぁ〜！タバコを吸え〜！俺を餓死させる気か〜！")
                ]
            )
        )
    return posts_db

@app.post("/api/posts", response_model=Post)
async def create_post(req: CreatePostRequest):
    """ユーザーがつぶやきを投稿 -> LM Studio(gemma4-12B qat)経由で4人のメンバーが実遅延付きで同時コメント"""
    new_id = f"post_{len(posts_db) + 1}_{int(datetime.now().timestamp())}"
    now_str = datetime.now().strftime("%Y-%m-%d %H:%M")
    
    # リアルタイムLLM生成
    comments = await generate_all_comments(req.text)
    
    new_post = Post(
        postId=new_id,
        author=req.author,
        isNpc=False,
        text=req.text,
        timestamp=now_str,
        comments=comments
    )
    
    posts_db.insert(0, new_post)
    return new_post

@app.post("/api/cron/bot-post", response_model=Post)
async def generate_npc_bot_post():
    """コミュニティメンバー（NPC）からのつぶやき投稿自動生成"""
    author = random.choice(NPC_USERS)
    text = random.choice(NPC_POST_TEMPLATES)
    new_id = f"post_npc_{len(posts_db) + 1}"
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
    
    posts_db.insert(0, npc_post)
    return npc_post

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
