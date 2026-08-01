from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime
import random

app = FastAPI(
    title="ヤニモグラ (YANI-GOTCHI) - Python API Server",
    description="マルチAIコメント生成 & コミュニティタイムライン バックエンドAPI",
    version="1.0.0"
)

# CORS設定（Androidエミュレータ/実機からのアクセス許可）
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- データモデル定義 ---
class AIComment(BaseModel):
    aiName: str
    avatar: str
    comment: str

class Post(BaseModel):
    postId: str
    author: str
    isNpc: bool
    text: str
    timestamp: str
    aiComments: List[AIComment]

class CreatePostRequest(BaseModel):
    author: str = "あなた"
    text: str

# --- マルチAIペルソナ辞書 ---
AI_PERSONAS = [
    {
        "aiName": "熱血コーチAI",
        "avatar": "🔥",
        "templates": [
            "素晴らしい我慢だ！！その熱い魂があれば絶対乗り越えられるぞ！🔥",
            "限界を打ち破れ！辛い時こそ進化のチャンスだ！オオオオーッ！！",
            "ナイスだ！その1歩が未来を変える！明日も気合で突破だ！"
        ]
    },
    {
        "aiName": "ツンデレAI",
        "avatar": "😳",
        "templates": [
            "べ、別に心配なんてしてないんだからね！でも…今日我慢できたのはちょっと偉いわよ。",
            "ふん、どうせ誘惑に負けると思ってたのに…まあ、がんばったじゃない。",
            "調子に乗らないでよね！…でも、我慢できたのは誇っていいんだからね！"
        ]
    },
    {
        "aiName": "ドクターAI",
        "avatar": "👨‍⚕️",
        "templates": [
            "医学的にも素晴らしい成果です。ニコチンの離脱症状は今がピークですが大丈夫です。",
            "素晴らしい自己コントロールです。冷たいお水を飲んで深呼吸してくださいね。",
            "肺機能が確実に回復に向かっています。この調子で身体を労わりましょう。"
        ]
    },
    {
        "aiName": "ヤニモグラ",
        "avatar": "👹",
        "templates": [
            "おい！俺のメシ（煙）を奪うな〜！！早くタバコ吸って俺を肥やせ〜！！",
            "む、ム念だ…ポイントが下がる…！頼むから1本だけでいいから吸ってくれ〜！",
            "我慢なんて身体に毒だぞ！？俺と一緒にヤニ天国へ行こうぜ〜！"
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
    "今日はついに1本も吸わずに過ごせた！奇跡！",
    "食後のタバコ我慢するのめちゃくちゃ辛い…耐えろ俺…！",
    "今日で禁煙3日目！ヤニモグラが禁断症状で暴れてる笑",
    "仕事の合間の1本をグッと堪えてお茶飲んだ！"
]

# メモリ内タイムラインDB（初期ダミーデータ）
posts_db: List[Post] = [
    Post(
        postId="post_init_1",
        author="減煙挑戦中のタカシ",
        isNpc=True,
        text="今日で禁煙3日目！ヤニモグラが禁断症状で暴れてる笑",
        timestamp="2026-08-01 15:30",
        aiComments=[
            AIComment(aiName="熱血コーチAI", avatar="🔥", comment="3日突破はお前の勝利だ！その情熱を燃やし続けろ！！"),
            AIComment(aiName="ツンデレAI", avatar="😳", comment="ふ、ふん！3日くらいで喜ばないでよね！…応援してるけど。"),
            AIComment(aiName="ドクターAI", avatar="👨‍⚕️", comment="3日目は体内のニコチンが完全に抜ける大事な節目です。素晴らしい！"),
            AIComment(aiName="ヤニモグラ", avatar="👹", comment="うがぁ〜！タバコを吸え〜！俺を餓死させる気か〜！")
        ]
    )
]

def generate_multi_ai_comments(user_text: str) -> List[AIComment]:
    """投稿文に対して複数のAIペルソナからの自動応援コメントを一斉生成"""
    comments = []
    for persona in AI_PERSONAS:
        comment_text = random.choice(persona["templates"])
        comments.append(
            AIComment(
                aiName=persona["aiName"],
                avatar=persona["avatar"],
                comment=comment_text
            )
        )
    return comments

# --- API エンドポイント ---

@app.get("/")
def read_root():
    return {"message": "ヤニモグラ (YANI-GOTCHI) Python Backend API Server is running!"}

@app.get("/api/feed", response_model=List[Post])
def get_feed():
    """タイムライン投稿および関連するマルチAIコメント一覧の取得"""
    return posts_db

@app.post("/api/posts", response_model=Post)
def create_post(req: CreatePostRequest):
    """新規つぶやき投稿の受付 ＆ 複数AIペルソナからの自動応援コメント即時生成"""
    new_id = f"post_{len(posts_db) + 1}_{int(datetime.now().timestamp())}"
    now_str = datetime.now().strftime("%Y-%m-%d %H:%M")
    
    ai_comments = generate_multi_ai_comments(req.text)
    
    new_post = Post(
        postId=new_id,
        author=req.author,
        isNpc=False,
        text=req.text,
        timestamp=now_str,
        aiComments=ai_comments
    )
    
    posts_db.insert(0, new_post)
    return new_post

@app.post("/api/cron/bot-post", response_model=Post)
def generate_npc_bot_post():
    """アプリ専用ユーザー（NPC）からの定期投稿の自動生成プロセッサ"""
    author = random.choice(NPC_USERS)
    text = random.choice(NPC_POST_TEMPLATES)
    new_id = f"post_npc_{len(posts_db) + 1}"
    now_str = datetime.now().strftime("%Y-%m-%d %H:%M")
    
    ai_comments = generate_multi_ai_comments(text)
    
    npc_post = Post(
        postId=new_id,
        author=author,
        isNpc=True,
        text=text,
        timestamp=now_str,
        aiComments=ai_comments
    )
    
    posts_db.insert(0, npc_post)
    return npc_post

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
