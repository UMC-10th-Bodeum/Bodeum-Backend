# AI CHAT API 명세 정리

> 현재 Desktop 보듬 프로젝트의 AI CHAT mock controller/DTO 구조와, Notion API 명세 템플릿을 기준으로 정리한 문서입니다.
>
> 현재 코드는 실제 JWT, DB, AI/RAG 연동 전 단계이며, 명세 기반 mock 응답을 반환합니다.

## 1. 추천 질문 칩 조회 (초기진입)

### Notion 속성

| 항목 | 값 |
| --- | --- |
| Method | GET |
| Endpoint | `/api/ai/suggested-questions` |
| 인증필요 | Yes |
| FE 담당자 | 비어 있음 |
| 백엔드 구현 | 시작 전 |
| 프론트 퍼블리싱 | 시작 전 |
| 통신 | 시작 전 |

### API 설명

- AI 챗봇 화면 초기 진입 시 사용자에게 보여줄 추천 질문 5개를 조회합니다.
- 로그인 사용자의 자녀 정보인 `child_disability`, `birth`, `region`을 기반으로 동적 생성하는 API입니다.
- 자녀 프로필이 등록되어 있지 않은 경우 일반 인기 질문으로 대체합니다.

### 백엔드 구현사항

- 현재 프로젝트에서는 `AiChatController.getSuggestedQuestions()`가 실행됩니다.
- 현재는 DB/JWT/AI 연동 없이 고정 추천 질문 5개를 mock으로 반환합니다.
- 추후 구현 시 JWT로 현재 유저를 식별하고, 자녀 프로필 정보를 조회한 뒤 추천 질문을 생성해야 합니다.

### RequestHeader

```json
{
  "Authorization": "Bearer {accessToken}"
}
```

### PathVariable

```json
(없음)
```

### RequestParam

```json
(없음)
```

### RequestBody

```json
(없음)
```

### Response

- 200 OK

```json
{
  "isSuccess": true,
  "code": "COMMON200_1",
  "message": "성공으로 요청을 처리했습니다.",
  "result": {
    "questions": [
      "아이가 밤마다 자주 깨요. 어떻게 해야 하나요?",
      "아이의 식습관을 어떻게 잡아주면 좋을까요?",
      "어린이집 적응을 도와주는 방법이 궁금해요.",
      "훈육할 때 어디까지 단호하게 말해야 하나요?",
      "아이와 애착을 높이는 놀이를 추천해 주세요."
    ]
  }
}
```

### 예외 케이스

- 401 Unauthorized

```json
{
  "isSuccess": false,
  "code": "COMMON401_1",
  "message": "인증되지 않았습니다.",
  "result": null
}
```

- 500 Internal Server Error

```json
{
  "isSuccess": false,
  "code": "COMMON500_1",
  "message": "서버 에러입니다.",
  "result": null
}
```

---

## 2. 대화 이력 조회

### Notion 속성

| 항목 | 값 |
| --- | --- |
| Method | GET |
| Endpoint | `/api/ai/chat-room/messages` |
| 인증필요 | Yes |
| FE 담당자 | 비어 있음 |
| 백엔드 구현 | 시작 전 |
| 프론트 퍼블리싱 | 시작 전 |
| 통신 | 시작 전 |

### API 설명

- 현재 로그인한 사용자의 AI 대화 이력을 조회합니다.
- 커서 기반 무한스크롤 방식으로 이전 대화를 불러옵니다.
- 대화 이력이 없는 경우 `messages`는 빈 배열로 반환합니다.

### 백엔드 구현사항

- 현재 프로젝트에서는 `AiChatController.getChatMessages(Long cursor, int size)`가 실행됩니다.
- 현재는 실제 DB 조회 없이 USER 메시지 1개와 AI 메시지 1개를 mock으로 반환합니다.
- 추후 구현 시 JWT로 현재 유저를 식별하고, 해당 유저의 `ai_chat_room`과 `ai_message`를 cursor 기반으로 조회해야 합니다.
- AI 메시지에는 출처 정보 `responseSource`와 기존 피드백 정보 `feedback`이 포함될 수 있습니다.

### RequestHeader

```json
{
  "Authorization": "Bearer {accessToken}"
}
```

### PathVariable

```json
(없음)
```

### RequestParam

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| cursor | Long | X | 마지막으로 조회한 메시지 ID |
| size | Integer | X | 조회할 메시지 개수, 기본값 20 |

```json
{
  "cursor": 20,
  "size": 20
}
```

### RequestBody

```json
(없음)
```

### Response

- 200 OK

```json
{
  "isSuccess": true,
  "code": "COMMON200_1",
  "message": "성공으로 요청을 처리했습니다.",
  "result": {
    "messages": [
      {
        "aiMessageId": 1,
        "senderType": "USER",
        "content": "아이가 밤마다 자주 깨요.",
        "createdAt": "2026-07-03T15:30:00"
      },
      {
        "aiMessageId": 2,
        "senderType": "AI",
        "content": "수면 환경과 루틴을 먼저 점검해보는 것이 좋아요.",
        "createdAt": "2026-07-03T15:30:03",
        "responseSource": {
          "sourceTitle": "영유아 수면 가이드",
          "sourceUrl": "https://example.com/sleep-guide"
        },
        "feedback": {
          "feedbackType": "HELPFUL"
        }
      }
    ],
    "hasNext": false,
    "nextCursor": null
  }
}
```

### 예외 케이스

- 401 Unauthorized

```json
{
  "isSuccess": false,
  "code": "COMMON401_1",
  "message": "인증되지 않았습니다.",
  "result": null
}
```

- 500 Internal Server Error

```json
{
  "isSuccess": false,
  "code": "COMMON500_1",
  "message": "서버 에러입니다.",
  "result": null
}
```

---

## 3. 내 채팅방 조회 또는 생성

### Notion 속성

| 항목 | 값 |
| --- | --- |
| Method | GET |
| Endpoint | `/api/ai/chat-room` |
| 인증필요 | Yes |
| FE 담당자 | 비어 있음 |
| 백엔드 구현 | 시작 전 |
| 프론트 퍼블리싱 | 시작 전 |
| 통신 | 시작 전 |

### API 설명

- 현재 로그인한 사용자의 AI 채팅방을 조회합니다.
- 채팅방이 없으면 새로 생성해서 반환합니다.
- `ai_chat_room`은 `UNIQUE(user_id)` 구조이므로 사용자당 AI 채팅방은 1개만 존재합니다.
- `isFirstVisit`이 `true`이면 프론트에서 초기 인사말과 추천 질문 5개를 노출합니다.

### 백엔드 구현사항

- 현재 프로젝트에서는 `AiChatController.getOrCreateChatRoom()`이 실행됩니다.
- 현재는 실제 DB 조회/생성 없이 mock 채팅방 정보를 반환합니다.
- 추후 구현 시 JWT로 현재 유저를 식별하고, `ai_chat_room`을 조회한 뒤 없으면 생성해야 합니다.
- 첫 방문 여부는 해당 채팅방의 메시지 존재 여부 등을 기준으로 판단해야 합니다.

### RequestHeader

```json
{
  "Authorization": "Bearer {accessToken}"
}
```

### PathVariable

```json
(없음)
```

### RequestParam

```json
(없음)
```

### RequestBody

```json
(없음)
```

### Response

- 200 OK

```json
{
  "isSuccess": true,
  "code": "COMMON200_1",
  "message": "성공으로 요청을 처리했습니다.",
  "result": {
    "aiChatRoomId": 1,
    "createdAt": "2026-07-03T15:30:00",
    "isFirstVisit": true
  }
}
```

### 예외 케이스

- 401 Unauthorized

```json
{
  "isSuccess": false,
  "code": "COMMON401_1",
  "message": "인증되지 않았습니다.",
  "result": null
}
```

- 500 Internal Server Error

```json
{
  "isSuccess": false,
  "code": "COMMON500_1",
  "message": "서버 에러입니다.",
  "result": null
}
```

---

## 4. 질문 전송 및 AI 응답 생성

### Notion 속성

| 항목 | 값 |
| --- | --- |
| Method | POST |
| Endpoint | `/api/ai/chat-room/messages` |
| 인증필요 | Yes |
| FE 담당자 | 비어 있음 |
| 백엔드 구현 | 시작 전 |
| 프론트 퍼블리싱 | 시작 전 |
| 통신 | 시작 전 |

### API 설명

- 사용자가 입력한 질문을 AI 챗봇에 전송합니다.
- 사용자 메시지와 AI 응답 메시지가 함께 생성되어 반환됩니다.
- 내부적으로 의도분류, RAG 검색, 응답 생성, 출처 추출, 후속질문 3개 생성이 수행됩니다.

### 백엔드 구현사항

- 현재 프로젝트에서는 `AiChatController.createChatMessage(AiChatMessageCreateReqDTO request)`가 실행됩니다.
- 요청 body는 `AiChatMessageCreateReqDTO`로 받습니다.
- `content`는 필수이며 최대 500자까지 허용됩니다.
- 현재는 실제 AI/RAG 호출 없이 mock `userMessage`, `aiMessage`, `followUpQuestions`를 반환합니다.
- 추후 구현 시 사용자 메시지 저장, AI 응답 저장, 출처 저장, 후속질문 생성까지 처리해야 합니다.

### RequestHeader

```json
{
  "Authorization": "Bearer {accessToken}"
}
```

### PathVariable

```json
(없음)
```

### RequestParam

```json
(없음)
```

### RequestBody

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| content | String | O | 사용자 질문 내용, 1자 이상 500자 이하 |

```json
{
  "content": "아이가 밤마다 자주 깨요."
}
```

### Response

- 200 OK

```json
{
  "isSuccess": true,
  "code": "COMMON200_1",
  "message": "성공으로 요청을 처리했습니다.",
  "result": {
    "userMessage": {
      "aiMessageId": 11,
      "content": "아이가 밤마다 자주 깨요.",
      "createdAt": "2026-07-03T15:30:00"
    },
    "aiMessage": {
      "aiMessageId": 12,
      "content": "아이의 상황을 조금 더 살펴본 뒤, 수면 루틴과 환경을 함께 점검해보세요.",
      "createdAt": "2026-07-03T15:30:03",
      "responseSource": [
        {
          "sourceTitle": "영유아 수면 가이드",
          "sourceUrl": "https://example.com/sleep-guide",
          "infoId": 1
        }
      ]
    },
    "followUpQuestions": [
      "수면 루틴은 어떻게 만들어야 하나요?",
      "밤중 각성이 잦을 때 확인할 점은 무엇인가요?",
      "낮잠 시간이 밤잠에 영향을 줄 수 있나요?"
    ]
  }
}
```

### 예외 케이스

- 400 Bad Request

**공백/빈 문자열이거나 500자를 초과한 경우**

```json
{
  "isSuccess": false,
  "code": "COMMON400_1",
  "message": "잘못된 요청입니다.",
  "result": null
}
```

- 401 Unauthorized

```json
{
  "isSuccess": false,
  "code": "COMMON401_1",
  "message": "인증되지 않았습니다.",
  "result": null
}
```

- 503 Service Unavailable

**AI 응답 생성 실패 또는 타임아웃**

```json
{
  "isSuccess": false,
  "code": "AI_RESPONSE_FAILED",
  "message": "AI 응답 생성에 실패했습니다.",
  "result": null
}
```

- 500 Internal Server Error

```json
{
  "isSuccess": false,
  "code": "COMMON500_1",
  "message": "서버 에러입니다.",
  "result": null
}
```

---

## 5. AI 챗봇 이용동의 여부 조회

### Notion 속성

| 항목 | 값 |
| --- | --- |
| Method | GET |
| Endpoint | `/api/users/me/ai-terms-status` |
| 인증필요 | Yes |
| FE 담당자 | 비어 있음 |
| 백엔드 구현 | 시작 전 |
| 프론트 퍼블리싱 | 시작 전 |
| 통신 | 시작 전 |

### API 설명

- 현재 로그인한 사용자가 AI 챗봇 이용동의를 했는지 조회합니다.
- true이면 프론트에서 사전동의 모달을 다시 노출하지 않습니다.
- 이 API는 유저의 동의 상태를 다루므로 `/users/me` 경로를 유지합니다.

### 백엔드 구현사항

- 현재 프로젝트에서는 `AiChatController.getAiTermsStatus()`가 실행됩니다.
- 현재는 DB 조회 없이 mock으로 `aiTermsAgreed=true`를 반환합니다.
- 추후 구현 시 JWT로 현재 유저를 식별하고 `user_agreement.ai_terms_agreed` 값을 조회해야 합니다.

### RequestHeader

```json
{
  "Authorization": "Bearer {accessToken}"
}
```

### PathVariable

```json
(없음)
```

### RequestParam

```json
(없음)
```

### RequestBody

```json
(없음)
```

### Response

- 200 OK

```json
{
  "isSuccess": true,
  "code": "COMMON200_1",
  "message": "성공으로 요청을 처리했습니다.",
  "result": {
    "aiTermsAgreed": true
  }
}
```

### 예외 케이스

- 401 Unauthorized

```json
{
  "isSuccess": false,
  "code": "COMMON401_1",
  "message": "인증되지 않았습니다.",
  "result": null
}
```

- 500 Internal Server Error

```json
{
  "isSuccess": false,
  "code": "COMMON500_1",
  "message": "서버 에러입니다.",
  "result": null
}
```

---

## 6. AI 챗봇 이용동의 등록

### Notion 속성

| 항목 | 값 |
| --- | --- |
| Method | POST |
| Endpoint | `/api/users/me/ai-terms` |
| 인증필요 | Yes |
| FE 담당자 | 비어 있음 |
| 백엔드 구현 | 시작 전 |
| 프론트 퍼블리싱 | 시작 전 |
| 통신 | 시작 전 |

### API 설명

- 현재 로그인한 사용자의 AI 챗봇 이용동의 값을 `true`로 등록합니다.
- 기본값 `false`에서 `true`로 전환하는 API입니다.
- 이 API는 유저의 동의 상태를 다루므로 `/users/me` 경로를 유지합니다.

### 백엔드 구현사항

- 현재 프로젝트에서는 `AiChatController.agreeAiTerms(AiTermsAgreeReqDTO request)`가 실행됩니다.
- 요청 body는 `AiTermsAgreeReqDTO`로 받습니다.
- `aiTermsAgreed`는 반드시 `true`여야 하며, `@AssertTrue`로 검증합니다.
- 현재는 DB 업데이트 없이 현재 시간을 `agreedAt`으로 반환합니다.
- 추후 구현 시 JWT로 현재 유저를 식별하고 `user_agreement.ai_terms_agreed` 값을 true로 업데이트해야 합니다.

### RequestHeader

```json
{
  "Authorization": "Bearer {accessToken}"
}
```

### PathVariable

```json
(없음)
```

### RequestParam

```json
(없음)
```

### RequestBody

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| aiTermsAgreed | Boolean | O | AI 챗봇 이용동의 여부, 반드시 true |

```json
{
  "aiTermsAgreed": true
}
```

### Response

- 200 OK

```json
{
  "isSuccess": true,
  "code": "COMMON200_1",
  "message": "성공으로 요청을 처리했습니다.",
  "result": {
    "agreedAt": "2026-07-03T15:30:00"
  }
}
```

### 예외 케이스

- 400 Bad Request

**`aiTermsAgreed`가 false이거나 요청값이 올바르지 않은 경우**

```json
{
  "isSuccess": false,
  "code": "COMMON400_1",
  "message": "잘못된 요청입니다.",
  "result": null
}
```

- 401 Unauthorized

```json
{
  "isSuccess": false,
  "code": "COMMON401_1",
  "message": "인증되지 않았습니다.",
  "result": null
}
```

- 500 Internal Server Error

```json
{
  "isSuccess": false,
  "code": "COMMON500_1",
  "message": "서버 에러입니다.",
  "result": null
}
```

---

## 7. AI 메시지 피드백 등록 (정보가 틀려요, 사유 포함)

### Notion 속성

| 항목 | 값 |
| --- | --- |
| Method | POST |
| Endpoint | `/api/ai/messages/{aiMessageId}/feedback` |
| 인증필요 | Yes |
| FE 담당자 | 비어 있음 |
| 백엔드 구현 | 시작 전 |
| 프론트 퍼블리싱 | 시작 전 |
| 통신 | 시작 전 |

### API 설명

- AI 메시지에 대해 "정보가 틀려요" 피드백을 등록합니다.
- `feedbackType`은 `INCORRECT`입니다.
- `INCORRECT` 피드백은 사유를 1개 이상 선택해야 합니다.
- 피드백 등록 완료 후 프론트에서는 제출 완료 토스트를 노출합니다.

### 백엔드 구현사항

- 현재 프로젝트에서는 `AiChatController.createMessageFeedback(Long aiMessageId, AiMessageFeedbackCreateReqDTO request)`가 실행됩니다.
- 요청 body는 `AiMessageFeedbackCreateReqDTO`로 받습니다.
- `feedbackType=INCORRECT`인데 `reasons`가 없거나 빈 배열이면 `BAD_REQUEST`를 발생시킵니다.
- 현재는 DB 저장 없이 mock `aiFeedbackId`, `feedbackType`, `reasons`를 반환합니다.
- 추후 구현 시 `ai_feedback` 1건과 `ai_feedback_reason` N건을 함께 저장해야 합니다.
- `ai_feedback`은 `UNIQUE(ai_message_id)`로 관리하여 메시지당 피드백 1개만 허용해야 합니다.

### RequestHeader

```json
{
  "Authorization": "Bearer {accessToken}"
}
```

### PathVariable

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| aiMessageId | Long | O | 피드백을 등록할 AI 메시지 ID |

```json
{
  "aiMessageId": 12
}
```

### RequestParam

```json
(없음)
```

### RequestBody

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| feedbackType | String | O | 피드백 유형, `INCORRECT` |
| reasons | String[] | O | 틀렸다고 판단한 사유 목록 |

```json
{
  "feedbackType": "INCORRECT",
  "reasons": [
    "TIME",
    "ELIGIBILITY"
  ]
}
```

### Response

- 200 OK

```json
{
  "isSuccess": true,
  "code": "COMMON200_1",
  "message": "성공으로 요청을 처리했습니다.",
  "result": {
    "aiFeedbackId": 1,
    "feedbackType": "INCORRECT",
    "reasons": [
      "TIME",
      "ELIGIBILITY"
    ]
  }
}
```

### 예외 케이스

- 400 Bad Request

**사유를 선택하지 않은 경우**

```json
{
  "isSuccess": false,
  "code": "COMMON400_1",
  "message": "잘못된 요청입니다.",
  "result": null
}
```

- 401 Unauthorized

```json
{
  "isSuccess": false,
  "code": "COMMON401_1",
  "message": "인증되지 않았습니다.",
  "result": null
}
```

- 404 Not Found

**피드백을 등록할 AI 메시지를 찾을 수 없는 경우**

```json
{
  "isSuccess": false,
  "code": "COMMON404_1",
  "message": "해당 리소스를 찾을 수 없습니다.",
  "result": null
}
```

- 409 Conflict

**이미 피드백한 메시지인 경우**

```json
{
  "isSuccess": false,
  "code": "ALREADY_FEEDBACK",
  "message": "이미 피드백한 메시지입니다.",
  "result": null
}
```

- 500 Internal Server Error

```json
{
  "isSuccess": false,
  "code": "COMMON500_1",
  "message": "서버 에러입니다.",
  "result": null
}
```

---

## 8. AI 메시지 피드백 등록 (도움되었어요)

### Notion 속성

| 항목 | 값 |
| --- | --- |
| Method | POST |
| Endpoint | `/api/ai/messages/{aiMessageId}/feedback` |
| 인증필요 | Yes |
| FE 담당자 | 비어 있음 |
| 백엔드 구현 | 시작 전 |
| 프론트 퍼블리싱 | 시작 전 |
| 통신 | 시작 전 |

### API 설명

- AI 메시지에 대해 "도움되었어요" 피드백을 등록합니다.
- `feedbackType`은 `HELPFUL`입니다.
- 이 경우 별도 사유는 전송하지 않습니다.
- 7번 API와 URL 및 컨트롤러 메서드는 같고, `feedbackType` 값으로 동작을 구분합니다.

### 백엔드 구현사항

- 현재 프로젝트에서는 `AiChatController.createMessageFeedback(Long aiMessageId, AiMessageFeedbackCreateReqDTO request)`가 실행됩니다.
- 요청 body는 `AiMessageFeedbackCreateReqDTO`로 받습니다.
- `feedbackType=HELPFUL`인 경우 `reasons`는 필요하지 않습니다.
- 현재 응답 DTO에는 `@JsonInclude(JsonInclude.Include.NON_NULL)`이 적용되어 있어 `reasons`가 null이면 응답에서 제외됩니다.
- 추후 구현 시 `ai_feedback`에 `HELPFUL` 피드백을 저장해야 합니다.
- `ai_feedback`은 `UNIQUE(ai_message_id)`로 관리하여 메시지당 피드백 1개만 허용해야 합니다.

### RequestHeader

```json
{
  "Authorization": "Bearer {accessToken}"
}
```

### PathVariable

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| aiMessageId | Long | O | 피드백을 등록할 AI 메시지 ID |

```json
{
  "aiMessageId": 12
}
```

### RequestParam

```json
(없음)
```

### RequestBody

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| feedbackType | String | O | 피드백 유형, `HELPFUL` |

```json
{
  "feedbackType": "HELPFUL"
}
```

### Response

- 200 OK

```json
{
  "isSuccess": true,
  "code": "COMMON200_1",
  "message": "성공으로 요청을 처리했습니다.",
  "result": {
    "aiFeedbackId": 1,
    "feedbackType": "HELPFUL"
  }
}
```

### 예외 케이스

- 400 Bad Request

```json
{
  "isSuccess": false,
  "code": "COMMON400_1",
  "message": "잘못된 요청입니다.",
  "result": null
}
```

- 401 Unauthorized

```json
{
  "isSuccess": false,
  "code": "COMMON401_1",
  "message": "인증되지 않았습니다.",
  "result": null
}
```

- 404 Not Found

**피드백을 등록할 AI 메시지를 찾을 수 없는 경우**

```json
{
  "isSuccess": false,
  "code": "COMMON404_1",
  "message": "해당 리소스를 찾을 수 없습니다.",
  "result": null
}
```

- 409 Conflict

**이미 피드백한 메시지인 경우**

```json
{
  "isSuccess": false,
  "code": "ALREADY_FEEDBACK",
  "message": "이미 피드백한 메시지입니다.",
  "result": null
}
```

- 500 Internal Server Error

```json
{
  "isSuccess": false,
  "code": "COMMON500_1",
  "message": "서버 에러입니다.",
  "result": null
}
```
