# 🫂 보듬 (Bodum) Backend

> 보듬 백엔드 프로젝트의 개발·협업 컨벤션 문서입니다.  
> 모든 팀원은 작업 시작 전 이 문서를 확인하고, 새로운 규칙이 필요하면 팀 논의 후 문서를 함께 갱신합니다.

---

## 🛠 기술 스택

| 구분 | 기술 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.0 |
| Build Tool | Gradle |
| ORM | Spring Data JPA / Hibernate |
| Database | MySQL |
| API Documentation | Swagger / Springdoc OpenAPI |
| Test | JUnit 5, Spring Boot Test |
| Version Control | Git, GitHub |

---

# 1. Git 컨벤션

## 1.1 브랜치 전략

보듬은 `main`, `develop`, 작업 브랜치를 사용하는 **간소화된 Git Flow**를 사용합니다.

```text
main ← develop ← feat/*
```

| 브랜치 | 역할 | 규칙 |
|---|---|---|
| `main` | 배포 가능한 안정 버전 | 직접 push 금지, PR로만 병합 |
| `develop` | 기능 통합 및 개발 기준 브랜치 | 작업 브랜치는 반드시 여기서 분기 |
| `feat/*` | 새로운 기능 개발 | 기능 단위로 생성 |
| `fix/*` | 일반 버그 수정 | 버그 단위로 생성 |
| `refactor/*` | 기능 변경 없는 구조 개선 | 리팩터링 범위가 명확해야 함 |
| `chore/*` | 설정, 빌드, 문서, 의존성 등 기타 작업 | 기능 로직 변경은 포함하지 않음 |
| `hotfix/*` | 운영 환경 긴급 장애 수정 | 필요한 경우에만 생성 |

### 기본 작업 흐름

1. 작업 전 GitHub Issue를 생성하거나 담당 이슈를 확인합니다.
2. 최신 `develop` 브랜치에서 작업 브랜치를 생성합니다.
3. 기능 구현, 테스트, 자체 검토를 진행합니다.
4. 커밋 컨벤션에 맞춰 커밋합니다.
5. 원격 저장소에 push한 뒤 `develop`을 대상으로 Pull Request를 생성합니다.
6. 코드 리뷰와 CI 확인이 끝난 뒤 병합합니다.
7. 병합된 작업 브랜치는 삭제합니다.

```bash
# 1. develop 최신화
git checkout develop
git pull origin develop

# 2. 작업 브랜치 생성
git checkout -b feat/#12-profile-update

# 3. 작업 후 커밋
git add .
git commit -m "feat: #12 프로필 수정 API 구현"

# 4. 원격 저장소에 브랜치 등록
git push -u origin feat/#12-profile-update
```

> 작업 시작 전에 `develop`을 반드시 최신 상태로 맞춥니다.  
> 다른 사람의 브랜치에서 직접 작업하지 않고, 본인 작업 브랜치에서만 작업합니다.

---

## 1.2 브랜치 네이밍 규칙

### 형식

```text
<type>/#<issue-number>-<short-description>
```

### 예시

```text
feat/#12-profile-update
feat/#24-consent-status-api
fix/#31-token-expiration-error
refactor/#42-user-service-separation
chore/#5-swagger-setting
hotfix/#52-login-failure
```

### 작성 규칙

- 브랜치 이름은 영문 소문자와 하이픈(`-`)을 사용합니다.
- 기능 설명은 짧고 구체적으로 작성합니다.
- 이슈가 존재하는 작업은 이슈 번호를 반드시 포함합니다.
- 개인 이름, 날짜, 의미 없는 단어(`test`, `new`, `final`)는 브랜치명에 사용하지 않습니다.
- 하나의 브랜치는 하나의 기능 또는 하나의 수정 목적만 가집니다.

---

## 1.3 커밋 메시지 컨벤션

### 형식

```text
<type>: #<issue-number> <변경 내용>
```

필요한 경우 본문을 추가합니다.

```text
<type>: #<issue-number> <변경 내용>

<변경 이유, 주의 사항, 구현 범위>
```

### 커밋 타입

| Type | 사용 상황 | 예시 |
|---|---|---|
| `feat` | 새로운 기능 추가 | `feat: #12 프로필 수정 API 구현` |
| `fix` | 버그 수정 | `fix: #18 닉네임 중복 검증 오류 수정` |
| `refactor` | 기능 변화 없는 코드 구조 개선 | `refactor: #21 사용자 조회 로직 분리` |
| `test` | 테스트 코드 추가 또는 수정 | `test: #24 사용자 서비스 단위 테스트 추가` |
| `docs` | README, API 문서 등 문서 수정 | `docs: #3 컨벤션 문서 보완` |
| `style` | 공백, 포맷, import 등 코드 스타일 수정 | `style: #27 import 정리` |
| `chore` | 빌드, 설정, 의존성, CI 등 기타 작업 | `chore: #30 Gradle 의존성 변경` |
| `remove` | 불필요한 코드 또는 파일 삭제 | `remove: #31 미사용 DTO 삭제` |
| `hotfix` | 운영 긴급 수정 | `hotfix: #35 로그인 오류 긴급 수정` |
| `merge` | 브랜치 병합 기록이 필요한 경우 | `merge: develop 최신 변경사항 반영` |

### 좋은 커밋 메시지 예시

```text
feat: #12 프로필 수정 API 구현
fix: #18 약관 동의 여부 조회 시 null 응답 수정
refactor: #21 UserService 조회 책임 분리
test: #24 사용자 정보 수정 실패 케이스 추가
docs: #3 README 브랜치 규칙 보완
```

### 피해야 할 커밋 메시지

```text
수정
fix
작업함
api 변경
final
진짜 최종
```

### 커밋 작성 원칙

- 한 커밋에는 하나의 논리적인 변경만 포함합니다.
- 기능 구현, 리팩터링, 포맷 변경을 가능한 한 한 커밋에 섞지 않습니다.
- 다른 기능과 관련 없는 파일 수정은 별도 커밋으로 분리합니다.
- 커밋하기 전 디버그 코드, 로그, 개인 설정 파일이 포함되지 않았는지 확인합니다.
- 커밋 메시지만 읽어도 변경 목적을 이해할 수 있어야 합니다.

---

## 1.4 Git 주석(Comment) 컨벤션

과제 안내의 “깃 코멘트 컨벤션”이 **커밋 메시지 규칙**을 의미하는 경우는 위 `1.3 커밋 메시지 컨벤션`을 따릅니다.  
코드 내부 주석 규칙도 아래 기준으로 통일합니다.

### 주석을 작성하는 경우

- 비즈니스 정책이나 예외적인 처리 이유가 코드만으로 드러나지 않는 경우
- 외부 API 제약, DB 설계 제약, 보안상 주의 사항을 설명해야 하는 경우
- 임시 우회 로직 또는 향후 개선이 필요한 경우

### 주석을 작성하지 않는 경우

- 코드가 이미 설명하는 단순 동작을 반복하는 경우
- 변수명이나 메서드명으로 충분히 표현 가능한 경우
- 구현 과정을 나열하는 경우

```java
// 좋지 않은 예시: 코드를 그대로 읽어 준다.
// 사용자 정보를 조회한다.
User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

// 좋은 예시: 코드만으로 알기 어려운 정책을 설명한다.
// 닉네임 변경은 사용자당 하루 1회만 허용한다.
validateNicknameChangePeriod(user);
```

### TODO 규칙

`TODO`에는 이슈 번호와 작업 이유를 함께 작성합니다.

```java
// TODO(#42): 약관 버전별 동의 이력을 별도 테이블로 분리한다.
// TODO(#57): 외부 인증 API 장애 시 재시도 정책을 추가한다.
```

- 이슈 번호 없는 TODO는 남기지 않습니다.
- 해결된 TODO는 즉시 제거합니다.
- 장기적으로 남을 기술 부채는 GitHub Issue로 등록합니다.

---

# 2. 코드 스타일 컨벤션

## 2.1 기본 Java 스타일

- Java 21 문법을 기준으로 작성합니다.
- 들여쓰기는 공백 4칸을 사용하며 탭 문자는 사용하지 않습니다.
- 한 줄 길이는 가급적 120자 이내로 유지합니다.
- 중괄호는 K&R 스타일을 사용합니다.
- 클래스, 메서드, 조건문 사이에는 의미 단위로 빈 줄을 사용합니다.
- 불필요한 와일드카드 import는 사용하지 않습니다.
- IDE 자동 포맷을 적용한 상태로 커밋합니다.

```java
public UserProfileResponse getMyProfile(Long userId) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

    return UserProfileResponse.from(user);
}
```

---

## 2.2 네이밍 컨벤션

### 클래스, 인터페이스, DTO

| 구분 | 패턴 | 예시 |
|---|---|---|
| Controller | `{Domain}Controller` | `UserController` |
| Service | `{Domain}Service` | `UserService` |
| Service 구현체 | 기본적으로 별도 구현체를 만들지 않음 | `UserService` |
| Repository | `{Domain}Repository` | `UserRepository` |
| Entity | 단수 명사 | `User`, `Consent` |
| Request DTO | `{Action}{Domain}Request` | `UpdateProfileRequest` |
| Response DTO | `{Domain}{Action}Response` | `UserProfileResponse` |
| 예외 | `{Domain}Exception` | `UserException` |
| 에러 코드 | `{Domain}ErrorCode` | `UserErrorCode` |
| 설정 클래스 | `{Feature}Config` | `SwaggerConfig` |

> 특별한 다중 구현이 필요한 경우가 아니라면 `UserServiceImpl`처럼 단일 구현체를 위한 `Impl` 클래스를 만들지 않습니다.

### 메서드와 변수

- 메서드와 변수는 `camelCase`를 사용합니다.
- 클래스, enum, record는 `PascalCase`를 사용합니다.
- 상수는 `UPPER_SNAKE_CASE`를 사용합니다.
- boolean 변수는 상태가 드러나도록 `is`, `has`, `can`, `should`로 시작합니다.
- 컬렉션은 복수형 명사로 작성합니다.

```java
private static final int MAX_NICKNAME_LENGTH = 20;

private boolean isAgreed;
private boolean hasRequiredConsent;
private List<Consent> consents;
```

### 조회 메서드 구분

| 접두어 | 의미 | 예시 |
|---|---|---|
| `get` | 반드시 존재해야 하며, 없으면 예외 발생 | `getUser(userId)` |
| `find` | 없을 수 있는 결과를 반환 | `findByEmail(email)` |
| `exists` | 존재 여부만 조회 | `existsByNickname(nickname)` |
| `create` | 새 객체 또는 리소스 생성 | `createUser(request)` |
| `update` | 기존 상태 변경 | `updateProfile(userId, request)` |
| `delete` | 삭제 또는 삭제 상태 변경 | `deleteUser(userId)` |
| `validate` | 입력값·정책 검증 | `validateNickname(nickname)` |

---

## 2.3 패키지 구조

보듬은 도메인 중심 패키지 구조를 사용합니다.

```text
com.bodum
├── domain
│   ├── user
│   │   ├── controller
│   │   ├── dto
│   │   │   ├── request
│   │   │   └── response
│   │   ├── entity
│   │   ├── repository
│   │   ├── service
│   │   └── exception
│   │
│   ├── consent
│   ├── auth
│   └── ...
│
├── global
│   ├── auth
│   ├── common
│   ├── config
│   ├── exception
│   ├── response
│   └── infrastructure
│
└── BodumApplication.java
```

### 패키지 역할

| 패키지 | 역할 |
|---|---|
| `controller` | HTTP 요청 수신, 입력 검증, 응답 반환 |
| `dto.request` | 클라이언트 요청 데이터 |
| `dto.response` | 클라이언트 응답 데이터 |
| `entity` | JPA 엔티티와 도메인 상태 변경 메서드 |
| `repository` | DB 접근과 조회 쿼리 |
| `service` | 비즈니스 로직, 트랜잭션 처리 |
| `exception` | 도메인 예외와 에러 코드 |
| `global` | 여러 도메인에서 공통으로 사용하는 기능 |

---

## 2.4 Controller 규칙

- Controller는 요청 수신, DTO 변환, 응답 반환만 담당합니다.
- 비즈니스 로직과 DB 접근 로직을 Controller에 작성하지 않습니다.
- URL은 소문자와 복수형 명사를 사용합니다.
- 인증된 현재 사용자를 기준으로 하는 API는 `/users/me`를 사용합니다.
- 요청 검증은 `@Valid`와 Bean Validation 어노테이션으로 처리합니다.
- 응답은 팀 공통 응답 형식을 사용합니다.

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(userService.getMyProfile(userId));
    }

    @PatchMapping("/me")
    public ApiResponse<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ApiResponse.success(userService.updateProfile(userId, request));
    }
}
```

---

## 2.5 Service 규칙

- Service는 도메인 비즈니스 로직을 담당합니다.
- 조회 전용 메서드에는 `@Transactional(readOnly = true)`를 사용합니다.
- 상태 변경이 필요한 메서드에만 `@Transactional`을 사용합니다.
- 하나의 메서드는 가능한 한 하나의 유스케이스만 처리합니다.
- Controller DTO를 Entity로 무분별하게 넘기지 않고, 필요한 값만 명확하게 사용합니다.

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserProfileResponse getMyProfile(Long userId) {
        User user = getUser(userId);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getUser(userId);
        user.updateNickname(request.nickname());

        return UserProfileResponse.from(user);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    }
}
```

---

## 2.6 Entity 규칙

- Entity에는 `@Setter`를 사용하지 않습니다.
- 상태 변경은 의미가 드러나는 도메인 메서드로 처리합니다.
- 기본 생성자는 `protected`로 제한합니다.
- 연관관계는 기본적으로 지연 로딩(`LAZY`)을 사용합니다.
- `equals`, `hashCode`, `toString` 자동 생성 시 연관관계가 포함되지 않도록 주의합니다.
- Entity는 API 응답으로 직접 반환하지 않습니다.
- DB 컬럼명은 `snake_case`, Java 필드명은 `camelCase`를 사용합니다.

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String nickname;

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }
}
```

---

## 2.7 DTO 규칙

- Request DTO와 Response DTO는 반드시 분리합니다.
- Entity를 Controller의 Request/Response로 직접 사용하지 않습니다.
- DTO는 가능하면 `record`를 사용합니다.
- 요청 DTO에는 검증 어노테이션을 명시합니다.
- Response DTO 변환은 `from()`, 여러 값 조합이 필요하면 `of()`를 사용합니다.

```java
public record UpdateProfileRequest(
        @NotBlank(message = "닉네임은 비어 있을 수 없습니다.")
        @Size(max = 20, message = "닉네임은 20자 이하로 입력해야 합니다.")
        String nickname
) {
}

public record UserProfileResponse(
        Long userId,
        String email,
        String nickname
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname()
        );
    }
}
```

---

## 2.8 예외 처리 규칙

- 예외는 도메인별 ErrorCode enum으로 관리합니다.
- Service에서 비즈니스 예외를 발생시킵니다.
- 전역 예외 처리는 `@RestControllerAdvice`에서 담당합니다.
- 클라이언트에게 내부 구현, SQL, 민감 정보를 노출하지 않습니다.
- 입력값 검증 오류와 비즈니스 오류를 구분합니다.

### 응답 예시

```json
{
  "success": false,
  "code": "USER_NOT_FOUND",
  "message": "사용자를 찾을 수 없습니다."
}
```

```java
@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements BaseErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "DUPLICATE_NICKNAME", "이미 사용 중인 닉네임입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
```

---

# 3. DB 및 Spring Boot 설정 규칙

## 3.1 DB 규칙

- 테이블명과 컬럼명은 `snake_case`를 사용합니다.
- 기본 PK는 `BIGINT`와 Auto Increment 전략을 사용합니다.
- 생성/수정 시각은 `created_at`, `updated_at`으로 통일합니다.
- 삭제 이력이 필요한 경우에만 논리 삭제를 검토합니다.
- 조회 조건, 정렬, 조인에 자주 사용하는 컬럼은 인덱스를 검토합니다.
- 공용 DB에 대해 `ddl-auto: update`를 사용하지 않습니다.
- 공용 환경에서는 `ddl-auto: validate`를 기본으로 합니다.

## 3.2 환경 설정 규칙

```text
src/main/resources
├── application.yml
├── application-local.yml
├── application-dev.yml
└── application-prod.yml
```

- DB 비밀번호, JWT Secret, 외부 API Key 등 민감 정보는 Git에 커밋하지 않습니다.
- 로컬 환경 값은 `.env` 또는 개인 `application-local.yml`로 관리합니다.
- 운영 환경 값은 배포 환경의 Secret 또는 환경 변수로 주입합니다.
- `application-local.yml`과 `.env`는 `.gitignore`에 등록합니다.

---

# 4. Pull Request 머지 규칙

## 4.1 PR 생성 전 체크리스트

PR을 만들기 전 아래 항목을 확인합니다.

- [ ] 최신 `develop` 브랜치를 반영했습니다.
- [ ] 기능 요구사항을 모두 구현했습니다.
- [ ] 로컬에서 정상 동작을 확인했습니다.
- [ ] 관련 테스트를 추가하거나 기존 테스트를 통과했습니다.
- [ ] 불필요한 로그, 디버그 코드, 주석을 제거했습니다.
- [ ] 민감 정보와 개인 IDE 설정 파일이 포함되지 않았습니다.
- [ ] Swagger 문서 또는 API 명세 변경 사항을 확인했습니다.
- [ ] 커밋 메시지와 브랜치명이 컨벤션을 따릅니다.

## 4.2 PR 제목 형식

```text
[Type] 작업 제목
```

### 예시

```text
[Feat] 프로필 수정 API 구현
[Fix] 약관 동의 조회 오류 수정
[Refactor] 사용자 조회 로직 분리
[Docs] 백엔드 컨벤션 문서 보완
```

## 4.3 PR 본문 템플릿

```markdown
## 관련 이슈
- Closes #이슈번호

## 작업 내용
- 구현하거나 수정한 내용을 작성합니다.
- API, DB, 예외 처리 변경 사항이 있다면 함께 작성합니다.

## 테스트
- [ ] 단위 테스트
- [ ] 통합 테스트
- [ ] 로컬 API 테스트

## 스크린샷 / Swagger
- 필요한 경우 첨부합니다.

## 리뷰 포인트
- 리뷰어가 집중해서 봐야 하는 부분을 작성합니다.

## 참고 사항
- 배포, DB 마이그레이션, 프론트 연동 등 추가 공유 사항을 작성합니다.
```

## 4.4 머지 조건

- `main` 브랜치에는 직접 push하지 않습니다.
- 모든 작업 브랜치는 `develop`을 대상으로 PR을 생성합니다.
- 최소 **1명 이상의 승인(Approve)** 후 병합합니다.
- CI 또는 테스트가 실패한 PR은 병합하지 않습니다.
- 해결되지 않은 변경 요청(Request changes)이 있으면 병합하지 않습니다.
- 충돌 해결 후 기능에 영향이 있을 경우 다시 테스트합니다.
- 큰 기능은 작은 단위의 PR로 나누는 것을 우선합니다.
- 병합 방식은 기본적으로 **Squash and merge**를 사용합니다.
- 병합 후 원격 작업 브랜치는 삭제합니다.

### `develop` → `main` 배포 PR 규칙

- 배포 PR에는 포함 기능, 확인된 변경 사항, 배포 시 주의 사항을 작성합니다.
- DB 스키마 변경이 있다면 마이그레이션 순서와 롤백 가능 여부를 함께 기록합니다.
- 배포 전 주요 API의 스모크 테스트 항목을 확인합니다.

---

# 5. 코드 리뷰 규칙

## 5.1 리뷰어 확인 항목

리뷰어는 아래 항목을 중심으로 PR을 확인합니다.

| 구분 | 확인 내용 |
|---|---|
| 요구사항 | 이슈의 기능 요구사항을 충족하는지 |
| 설계 | 책임 분리, 도메인 구조, 계층 구조가 적절한지 |
| 안정성 | null 처리, 예외 처리, 권한 검증이 충분한지 |
| 데이터 | DB 조회 방식, N+1 가능성, 트랜잭션 범위가 적절한지 |
| API | URL, HTTP Method, 상태 코드, DTO 구조가 일관적인지 |
| 테스트 | 성공·실패 케이스와 회귀 방지 테스트가 있는지 |
| 가독성 | 네이밍, 메서드 길이, 중복 코드, 주석이 적절한지 |
| 보안 | Secret 노출, 개인정보 로그, 인증/인가 누락이 없는지 |

## 5.2 리뷰 코멘트 작성 규칙

리뷰 코멘트는 사람이 아니라 코드에 초점을 맞춰 작성합니다.

### 코멘트 접두어

| 접두어 | 의미 | 예시 |
|---|---|---|
| `must:` | 병합 전 반드시 수정해야 하는 사항 | `must: 권한 검증이 없어 다른 사용자의 데이터가 조회될 수 있습니다.` |
| `suggestion:` | 개선을 제안하는 사항 | `suggestion: 이 로직은 별도 메서드로 추출하면 읽기 쉬울 것 같습니다.` |
| `question:` | 의도를 확인하고 싶은 사항 | `question: 이 경우에도 예외 대신 빈 목록을 반환하는 정책인가요?` |
| `nit:` | 사소한 스타일 또는 표현 제안 | `nit: 변수명을 consentStatus로 바꾸면 의미가 더 명확합니다.` |
| `praise:` | 좋은 구현이나 공유할 만한 점 | `praise: 예외 케이스까지 테스트한 점이 좋습니다.` |

### 좋은 리뷰 예시

```text
must: 현재 userId를 요청값으로 받아서 다른 사용자의 프로필 수정이 가능합니다.
인증 컨텍스트의 사용자 ID를 사용하도록 변경이 필요합니다.

suggestion: 닉네임 중복 검증과 변경 가능 기간 검증을 분리하면
각 정책의 책임이 더 명확해질 것 같습니다.

question: 약관 동의 이력을 수정하는 대신 최신 상태만 저장하는 이유가 있을까요?
```

### 피해야 할 리뷰 예시

```text
이상함
다시 짜세요
별로네요
왜 이렇게 했어요?
```

## 5.3 작성자 대응 규칙

- 리뷰 의견을 확인하면 수정 또는 답변을 남깁니다.
- 수정한 경우 해당 스레드에 변경 내용을 간단히 작성합니다.
- 동의하기 어려운 의견은 근거와 대안을 제시하며 논의합니다.
- 리뷰 반영 후 기능 영향이 큰 수정이 있었다면 다시 리뷰를 요청합니다.
- 승인 후에도 코드가 크게 변경되었다면 이전 승인을 그대로 사용하지 않고 재검토를 요청합니다.

---

# 6. 테스트 규칙

- 새로운 Service 로직은 단위 테스트 작성을 우선합니다.
- 인증/인가, 입력 검증, 예외 처리가 핵심인 API는 통합 테스트를 검토합니다.
- 버그 수정에는 재발 방지 테스트를 함께 추가합니다.
- 성공 케이스뿐 아니라 실패 케이스도 확인합니다.
- PR 생성 전 전체 테스트를 실행합니다.

```bash
./gradlew test
```

---

# 7. 최종 제출 체크리스트

- [x] Git 컨벤션을 정의했는가?
- [x] 코드 스타일 컨벤션을 정의했는가?
- [x] Git 커밋 및 코드 주석 컨벤션을 정의했는가?
- [x] PR 머지 규칙을 정의했는가?
- [x] 코드리뷰 규칙을 정의했는가?

---

## 📌 문서 관리 원칙

- 컨벤션은 강제가 목적이 아니라 협업 비용을 줄이기 위한 약속입니다.
- 실제 개발 과정에서 불편하거나 모호한 규칙이 발견되면 팀 논의 후 수정합니다.
- 규칙 변경 시 README와 PR 템플릿, Issue 템플릿도 함께 갱신합니다.
