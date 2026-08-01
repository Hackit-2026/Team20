-- コミュニティタイムライン DB スキーマ定義
CREATE TABLE IF NOT EXISTS posts (
    post_id TEXT PRIMARY KEY,
    author TEXT NOT NULL,
    is_npc INTEGER NOT NULL DEFAULT 0,
    text TEXT NOT NULL,
    timestamp TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS comments (
    comment_id INTEGER PRIMARY KEY AUTOINCREMENT,
    post_id TEXT NOT NULL,
    name TEXT NOT NULL,
    avatar TEXT NOT NULL,
    comment TEXT NOT NULL,
    timestamp TEXT NOT NULL,
    FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE
);

-- 初期サンプル投稿（過去データ）
INSERT OR IGNORE INTO posts (post_id, author, is_npc, text, timestamp) VALUES
('post_init_1', '減煙挑戦中のタカシ', 1, '今日で我慢3日目！手が寂しいけど耐えてる！', '2026-08-01 15:30');

INSERT OR IGNORE INTO comments (post_id, name, avatar, comment, timestamp) VALUES
('post_init_1', '熱血仲間・修造', '🔥', '3日突破はお前の勝利だ！その情熱を燃やし続けろ！！', '2026-08-01 15:31'),
('post_init_1', 'ツンデレ友達・アスカ', '😳', 'ふ、ふん！3日くらいで喜ばないでよね！…応援してるけど。', '2026-08-01 15:31'),
('post_init_1', 'Dr.ヘルス', '👨‍⚕️', '3日目は体内のニコチンが抜ける大事な節目です。素晴らしい！', '2026-08-01 15:32'),
('post_init_1', 'ヤニモグラ', '👹', 'うがぁ〜！タバコを吸え〜！俺を餓死させる気か〜！', '2026-08-01 15:32');
