from html import escape


TABLES = [
    {
        "title": "지역",
        "physical": "regions",
        "x": 50,
        "y": 40,
        "color": "#1f6f8b",
        "rows": [
            ("PK", "지역 ID", "id", "BIGINT", "NOT NULL", "AUTO_INCREMENT", "지역 고유 식별자"),
            ("", "시/도", "region_level_1", "VARCHAR(50)", "NOT NULL", "", "예: 서울특별시"),
            ("", "시/군/구", "region_level_2", "VARCHAR(50)", "NOT NULL", "", "예: 강남구"),
            ("", "전체 지역명", "full_name", "VARCHAR(100)", "NOT NULL", "", "시/도 + 시/군/구"),
        ],
    },
    {
        "title": "사용자",
        "physical": "users",
        "x": 740,
        "y": 40,
        "color": "#155a7a",
        "rows": [
            ("PK", "사용자 ID", "id", "BIGINT", "NOT NULL", "AUTO_INCREMENT", "사용자 고유 식별자"),
            ("", "소셜 로그인 제공자", "provider", "VARCHAR(20)", "NOT NULL", "", "KAKAO, NAVER"),
            ("UK", "소셜 제공자 사용자 ID", "provider_user_id", "VARCHAR(128)", "NOT NULL", "", "소셜 플랫폼 사용자 식별자"),
            ("UK", "인증 주체 UUID", "auth_subject", "VARCHAR(36)", "NOT NULL", "", "서비스 내부 인증용 UUID"),
            ("", "이메일", "email", "VARCHAR(255)", "NULL", "", "소셜 계정 이메일"),
            ("", "닉네임", "nickname", "VARCHAR(100)", "NULL", "", "서비스 내 사용자 닉네임"),
            ("", "프로필 이미지 URL", "profile_image_url", "VARCHAR(512)", "NULL", "", "사용자 프로필 이미지 주소"),
            ("FK", "지역 ID", "region_id", "BIGINT", "NULL", "", "regions.id 참조"),
            ("", "포인트", "point", "INT", "NOT NULL", "DEFAULT 0", "보호자 활동 포인트"),
            ("", "사용자 상태", "status", "VARCHAR(20)", "NOT NULL", "DEFAULT ACTIVE", "ACTIVE, HIDDEN, DELETED"),
            ("", "탈퇴 사유", "withdrawal_reason", "VARCHAR(255)", "NULL", "", "사용자가 입력한 탈퇴 사유"),
            ("", "탈퇴/삭제 시각", "deleted_at", "TIMESTAMP", "NULL", "", "탈퇴 또는 삭제 처리 시각"),
            ("", "생성 시각", "created_at", "TIMESTAMP", "NOT NULL", "CURRENT_TIMESTAMP", "사용자 생성 시각"),
            ("", "수정 시각", "updated_at", "TIMESTAMP", "NOT NULL", "CURRENT_TIMESTAMP", "사용자 정보 수정 시각"),
        ],
    },
    {
        "title": "약관 동의",
        "physical": "user_agreement",
        "x": 1430,
        "y": 40,
        "color": "#2f7d5c",
        "rows": [
            ("PK", "약관 동의 ID", "id", "BIGINT", "NOT NULL", "AUTO_INCREMENT", "약관 동의 고유 식별자"),
            ("FK/UK", "사용자 ID", "user_id", "BIGINT", "NOT NULL", "", "users.id 참조, 사용자당 1개"),
            ("", "서비스 이용약관 동의 여부", "service_terms_agreed", "BOOLEAN", "NOT NULL", "DEFAULT FALSE", "필수 약관"),
            ("", "개인정보처리방침 동의 여부", "privacy_policy_agreed", "BOOLEAN", "NOT NULL", "DEFAULT FALSE", "필수 약관"),
            ("", "AI 챗봇 이용 동의 여부", "ai_terms_agreed", "BOOLEAN", "NOT NULL", "DEFAULT FALSE", "선택 약관"),
            ("", "약관 동의 시각", "agreed_at", "TIMESTAMP", "NULL", "", "약관 동의 완료 시각"),
        ],
    },
    {
        "title": "온보딩",
        "physical": "user_onboarding",
        "x": 1430,
        "y": 430,
        "color": "#6d5a9c",
        "rows": [
            ("PK", "온보딩 ID", "id", "BIGINT", "NOT NULL", "AUTO_INCREMENT", "온보딩 고유 식별자"),
            ("FK/UK", "사용자 ID", "user_id", "BIGINT", "NOT NULL", "", "users.id 참조, 사용자당 1개"),
            ("", "온보딩 건너뛰기 여부", "onboarding_skipped", "BOOLEAN", "NOT NULL", "DEFAULT FALSE", "그만하기/건너뛰기 여부"),
        ],
    },
    {
        "title": "자녀 프로필",
        "physical": "child_profile",
        "x": 740,
        "y": 760,
        "color": "#1f6f8b",
        "rows": [
            ("PK", "자녀 프로필 ID", "id", "BIGINT", "NOT NULL", "AUTO_INCREMENT", "자녀 프로필 고유 식별자"),
            ("FK/UK", "사용자 ID", "user_id", "BIGINT", "NOT NULL", "", "users.id 참조, 사용자당 1개"),
            ("", "자녀 닉네임", "child_nickname", "VARCHAR(20)", "NULL", "", "자녀 표시 이름"),
            ("", "자녀 생년월", "child_birth", "VARCHAR(7)", "NULL", "", "YYYY-MM 형식"),
            ("", "자녀 특징 키워드", "keyword_text", "VARCHAR(100)", "NULL", "", "예: 말이 느림"),
        ],
    },
    {
        "title": "보호자 프로필",
        "physical": "guardian_profile",
        "x": 1430,
        "y": 760,
        "color": "#7a4b87",
        "rows": [
            ("PK", "보호자 프로필 ID", "id", "BIGINT", "NOT NULL", "AUTO_INCREMENT", "보호자 프로필 고유 식별자"),
            ("FK/UK", "사용자 ID", "user_id", "BIGINT", "NOT NULL", "", "users.id 참조, 사용자당 1개"),
            ("", "보호자 닉네임", "guardian_nickname", "VARCHAR(20)", "NULL", "", "커뮤니티 표시 닉네임"),
            ("", "보호자 유형", "guardian_type", "VARCHAR(50)", "NULL", "", "PARENT, GRANDPARENT, SIBLING, OTHER"),
            ("", "커뮤니티 역할 유형", "community_role_type", "VARCHAR(50)", "NULL", "", "INFO_SEEKER, EXPERIENCE_SHARER, WISDOM_HELPER"),
        ],
    },
    {
        "title": "장애/케어 영역",
        "physical": "disability_type",
        "x": 50,
        "y": 760,
        "color": "#2f7d5c",
        "rows": [
            ("PK", "장애/케어 영역 ID", "id", "INT", "NOT NULL", "", "케어 영역 고유 식별자"),
            ("", "장애/케어 영역명", "name", "VARCHAR(50)", "NOT NULL", "", "화면 표시 이름"),
            ("UK", "장애/케어 영역 코드", "code", "VARCHAR(50)", "NOT NULL", "", "시스템 식별 코드"),
        ],
    },
    {
        "title": "자녀 장애/케어 매핑",
        "physical": "child_disability",
        "x": 50,
        "y": 1110,
        "color": "#4e7a8f",
        "rows": [
            ("PK/FK", "자녀 프로필 ID", "child_profile_id", "BIGINT", "NOT NULL", "", "child_profile.id 참조"),
            ("PK/FK", "장애/케어 영역 ID", "disability_type_id", "INT", "NOT NULL", "", "disability_type.id 참조"),
        ],
    },
    {
        "title": "관심사 카테고리",
        "physical": "interest_category",
        "x": 740,
        "y": 1110,
        "color": "#8a6f2a",
        "rows": [
            ("PK", "관심사 카테고리 ID", "id", "INT", "NOT NULL", "", "관심사 고유 식별자"),
            ("", "관심사명", "name", "VARCHAR(50)", "NOT NULL", "", "화면 표시 이름"),
            ("UK", "관심사 코드", "code", "VARCHAR(50)", "NOT NULL", "", "시스템 식별 코드"),
            ("", "정렬 순서", "sort_order", "INT", "NOT NULL", "DEFAULT 0", "표시 정렬 순서"),
        ],
    },
    {
        "title": "사용자 관심사",
        "physical": "user_interest",
        "x": 1430,
        "y": 1110,
        "color": "#8a6f2a",
        "rows": [
            ("PK/FK", "사용자 ID", "user_id", "BIGINT", "NOT NULL", "", "users.id 참조"),
            ("PK/FK", "관심사 카테고리 ID", "interest_category_id", "INT", "NOT NULL", "", "interest_category.id 참조"),
        ],
    },
]

RELATIONS = [
    ((740, 235), (650, 235), (650, 170), (810, 170), "N:1"),
    ((1430, 210), (1360, 210), (1360, 210), (1500, 210), "1:1"),
    ((1430, 530), (1360, 530), (1360, 330), (1500, 330), "1:1"),
    ((1120, 760), (1120, 700), (1120, 700), (1120, 620), "1:1"),
    ((1430, 850), (1360, 850), (1360, 420), (1500, 420), "1:1"),
    ((430, 1110), (430, 1030), (430, 1030), (740, 930), "N:M"),
    ((260, 1110), (260, 1030), (260, 1030), (260, 970), "N:M"),
    ((1430, 1200), (1360, 1200), (1360, 520), (1500, 520), "N:M"),
    ((1430, 1235), (1340, 1235), (1340, 1215), (1500, 1215), "N:M"),
]


def table_height(table):
    return 46 + len(table["rows"]) * 36


def text(x, y, value, size=14, weight="400", fill="#f4f7fb", anchor="start", italic=False):
    style = "font-style:italic;" if italic else ""
    return (
        f'<text x="{x}" y="{y}" fill="{fill}" font-size="{size}" '
        f'font-weight="{weight}" text-anchor="{anchor}" style="{style}">{escape(value)}</text>'
    )


def draw_table(table):
    x = table["x"]
    y = table["y"]
    w = 760
    h = table_height(table)
    rows = []
    rows.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="4" fill="#26353d" stroke="{table["color"]}" stroke-width="3"/>')
    rows.append(f'<rect x="{x}" y="{y}" width="{w}" height="46" rx="4" fill="{table["color"]}"/>')
    rows.append(text(x + 14, y + 30, table["title"], 19, "700"))
    rows.append(text(x + w - 14, y + 30, table["physical"], 18, "700", anchor="end"))

    col_widths = [48, 176, 178, 104, 92, 162]
    col_x = [x]
    for cw in col_widths[:-1]:
        col_x.append(col_x[-1] + cw)

    y0 = y + 46
    for i, row in enumerate(table["rows"]):
        ry = y0 + i * 36
        fill = "#2f414b" if i % 2 == 0 else "#293942"
        if i == 0:
            fill = "#3a82ad"
        rows.append(f'<rect x="{x}" y="{ry}" width="{w}" height="36" fill="{fill}" stroke="#17252d" stroke-width="1"/>')
        cx = x
        for cw in col_widths[:-1]:
            cx += cw
            rows.append(f'<line x1="{cx}" y1="{ry}" x2="{cx}" y2="{ry + 36}" stroke="#17252d" stroke-width="1"/>')

        key, logical, physical, typ, null, opt, comment = row
        key_color = "#f7d34a" if "PK" in key else "#f0a2c7" if "FK" in key else "#b9c6ce"
        rows.append(text(x + 22, ry + 24, key, 11, "700", key_color, anchor="middle"))
        rows.append(text(col_x[1] + 8, ry + 24, logical, 13, "700"))
        rows.append(text(col_x[2] + 8, ry + 24, physical, 13, "600"))
        rows.append(text(col_x[3] + 8, ry + 24, typ, 13, "500"))
        null_color = "#ff6565" if null == "NULL" else "#f4f7fb"
        rows.append(text(col_x[4] + 8, ry + 24, null, 13, "700", null_color))
        rows.append(text(col_x[5] + 8, ry + 24, opt, 12, "500", "#d7dde2", italic=True))
        rows.append(text(col_x[5] + 168, ry + 24, comment, 12, "500", "#f4f7fb"))
    return "\n".join(rows)


def draw_relation(points, label):
    p = " ".join(f"{x},{y}" for x, y in points)
    lx = sum(x for x, _ in points) / len(points)
    ly = sum(y for _, y in points) / len(points)
    return (
        f'<polyline points="{p}" fill="none" stroke="#e8aabe" stroke-width="3" '
        f'stroke-dasharray="8 6" marker-start="url(#dot)" marker-end="url(#crow)"/>'
        f'{text(lx + 8, ly - 8, label, 12, "700", "#ffc1d2")}'
    )


svg_parts = [
    '<svg xmlns="http://www.w3.org/2000/svg" width="2240" height="1440" viewBox="0 0 2240 1440">',
    '<defs>',
    '<marker id="crow" markerWidth="12" markerHeight="12" refX="10" refY="6" orient="auto" markerUnits="strokeWidth">',
    '<path d="M2,2 L10,6 L2,10" fill="none" stroke="#e8aabe" stroke-width="2"/>',
    '</marker>',
    '<marker id="dot" markerWidth="8" markerHeight="8" refX="4" refY="4" markerUnits="strokeWidth">',
    '<circle cx="4" cy="4" r="3" fill="#e8aabe"/>',
    '</marker>',
    '</defs>',
    '<rect width="2240" height="1440" fill="#202020"/>',
    text(50, 24, "보듬 User 중심 논리 ERD - 상세 컬럼 버전", 20, "700", "#f4f7fb"),
]

for relation in RELATIONS:
    svg_parts.append(draw_relation(relation[:-1], relation[-1]))

for table in TABLES:
    svg_parts.append(draw_table(table))

svg_parts.append("</svg>")

with open("docs/erd-output/bodeum-user-erd-detail.svg", "w", encoding="utf-8") as f:
    f.write("\n".join(svg_parts))
