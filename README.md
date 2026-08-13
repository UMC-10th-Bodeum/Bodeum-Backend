# 🫂 보듬 (Bodeum)

> 장애아동과 보호자를 위해 맞춤형 복지·의료·교육 정보, 지역 소식, 경험 기반 커뮤니티, 근거 기반 AI 안내를 통합 제공하는 복지 지원 플랫폼

<br>

## 🔗 링크

| 구분 | 링크 |
|---|---|
| 서비스 URL | https://bodeum.site |
| API 문서 (Swagger) | https://api.bodeum.site/swagger-ui/index.html |

<br>

## 🛠 기술 스택

### Backend

![Java](https://img.shields.io/badge/Java_21-007396?style=flat&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_4.1.0-6DB33F?style=flat&logo=springboot&logoColor=white)
![JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=flat&logo=spring&logoColor=white)
![QueryDSL](https://img.shields.io/badge/QueryDSL-0769AD?style=flat&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=flat&logo=springsecurity&logoColor=white)
![OAuth 2.0](https://img.shields.io/badge/OAuth_2.0-EB5424?style=flat&logo=auth0&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=flat&logo=jsonwebtokens&logoColor=white)
![Spring AI](https://img.shields.io/badge/Spring_AI_2.0.0-6DB33F?style=flat&logo=spring&logoColor=white)
![OpenAI API](https://img.shields.io/badge/OpenAI_API-412991?style=flat&logo=openai&logoColor=white)

### Data

![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-FF4438?style=flat&logo=redis&logoColor=white)
![ChromaDB](https://img.shields.io/badge/ChromaDB-FF6446?style=flat&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=flat&logo=flyway&logoColor=white)

### Infrastructure

![AWS](https://img.shields.io/badge/AWS-232F3E?style=flat&logo=amazonwebservices&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=flat&logo=githubactions&logoColor=white)

<br>

## 🏗 시스템 아키텍처

<img width="4848" height="3381" alt="인프라 최종본" src="https://github.com/user-attachments/assets/69e0e934-f8dc-46e2-a58c-3a8b842a0264" />

<br>

## 📊 ERD

<img width="4476" height="1982" alt="ERD 최종본" src="https://github.com/user-attachments/assets/82c3f2a7-fb01-446e-875f-237c0d16f21f" />

<br>

## ✨ 핵심 기능

| 도메인 | 핵심 기능 |
|---|---|
| AUTH | 소셜 로그인(Naver·Kakao), 온보딩, 프로필 관리, 회원탈퇴 |
| HOME | 맞춤형 추천 소식, 인기·최신글 미리보기, 카테고리별 정보 건수 |
| INFO | 복지·의료·교육 정보 조회, 후기·스크랩, 카카오지도 연동 |
| AI CHAT | RAG 기반 AI 답변, 대화 이력 조회, AI 메시지 피드백 |
| COMM | 게시글·댓글·공감, 카테고리별 게시판 |
| NEWS | 활동소식·지역소식 목록·상세 조회, 검색 |
| MYP | 스크랩·게시글·댓글 관리, 뱃지·포인트 시스템 |

<br>

## 🌿 브랜치 전략

```
main ← develop ← feat/#{이슈번호}-{기능명}
```

| 브랜치 | 용도 |
|---|---|
| `main` | 운영 배포 브랜치. 직접 push 금지 |
| `develop` | 통합 개발 브랜치. 기능 브랜치가 병합되는 대상 |
| `feat/#이슈번호-기능명` | 새로운 기능 개발 |
| `fix/#이슈번호-기능명` | 버그 수정 |
| `hotfix/#이슈번호-내용` | 운영 환경 긴급 수정 |

<br>

## ✍️ 커밋 컨벤션

```
[type]: #이슈번호 작업 내용
```

| 타입 | 설명 |
|---|---|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 기능 변경 없이 코드 구조 개선 |
| `chore` | 빌드, 설정, 의존성 등 기타 작업 |
| `docs` | 문서 수정 |
| `test` | 테스트 코드 추가 또는 수정 |
| `hotfix` | 운영 환경 긴급 수정 |

<br>

## 👥 팀원

| <img src="https://github.com/GyeungUk.png" width=100> | <img src="https://github.com/dppfls.png" width=100> | <img src="https://github.com/kimjinho255.png" width=100> |
|:---:|:---:|:---:|
| [김경욱](https://github.com/GyeungUk) | [김예린](https://github.com/dppfls) | [김진호](https://github.com/kimjinho255) |
| **Backend** | **Backend** | **Backend** |

| <img src="https://github.com/slwnt31.png" width=100> | <img src="https://github.com/sangwoolee12.png" width=100> | <img src="https://github.com/dlwjddhks.png" width=100> |
|:---:|:---:|:---:|
| [백수진](https://github.com/slwnt31) | [이상우](https://github.com/sangwoolee12) | [이정완](https://github.com/dlwjddhks) |
| **Backend** | **Backend** | **Backend** |
