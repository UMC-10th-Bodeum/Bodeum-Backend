# Bodum Backend 작업 지침

이 저장소에서 수행하는 모든 개발, 수정, 리뷰, 테스트, Git 및 문서 작업은
루트의 `README.md`에 정의된 보듬 백엔드 개발·협업 컨벤션을 따라야 한다.
작업을 시작하기 전에 관련 컨벤션을 확인하고, 구현과 검증 결과가 해당 규칙을
충족하는지 작업 종료 전에 다시 점검한다.

## 핵심 원칙

- 기술 기준은 Java 21, Spring Boot 4.1.0, Gradle, Spring Data JPA/Hibernate,
  MySQL, Springdoc OpenAPI, JUnit 5이다.
- 도메인 중심 패키지 구조와 Controller, Service, Entity, DTO, Repository의
  책임 분리를 지킨다.
- Controller에는 비즈니스 로직이나 DB 접근 로직을 두지 않는다.
- 조회 트랜잭션은 `@Transactional(readOnly = true)`, 상태 변경 트랜잭션은
  `@Transactional`을 사용한다.
- Entity에 `@Setter`를 사용하지 않고 의미가 드러나는 도메인 메서드로
  상태를 변경한다. 연관관계는 기본적으로 LAZY를 사용하며 Entity를 API로
  직접 반환하지 않는다.
- Request DTO와 Response DTO를 분리하고, 가능하면 `record`를 사용한다.
  요청 DTO에는 Bean Validation을 적용하며 변환 메서드는 `from()` 또는
  여러 값 조합 시 `of()`를 사용한다.
- 도메인별 ErrorCode와 비즈니스 예외를 사용하며 내부 구현, SQL, Secret,
  개인정보 등 민감 정보를 외부 응답이나 로그에 노출하지 않는다.
- 새 Service 로직에는 단위 테스트를 우선하고, 버그 수정에는 회귀 테스트를
  추가한다. 성공과 실패 케이스를 모두 검토하고 완료 전 전체 테스트를 실행한다.
- 주석은 코드로 드러나지 않는 정책과 제약의 이유를 설명할 때만 작성한다.
  TODO에는 반드시 이슈 번호와 이유를 포함한다.
- 브랜치, 커밋, PR, 리뷰 코멘트와 머지 절차는 `README.md`의 Git/PR/리뷰
  컨벤션을 그대로 따른다. 이슈 번호가 필요한데 제공되지 않았다면 임의로
  만들지 않는다.
- 규칙을 바꿔야 할 때는 임의로 변경하지 말고 팀 논의가 필요함을 사용자에게
  알린다. 승인된 규칙 변경은 README, PR 템플릿, Issue 템플릿에도 함께
  반영해야 한다.

세부 규칙이나 예시가 이 요약과 다르게 해석될 여지가 있으면 `README.md`의
전체 컨벤션을 권위 있는 기준으로 사용한다.
