# Spring Orchestrator

여러 FastAPI 에이전트를 순서대로 호출하고 전체 워크플로 상태를 관리하는 Spring 오케스트레이터 프로젝트입니다.

## 역할

- 에이전트 호출 순서 결정
- 워크플로 상태 관리
- 재시도 및 실패 처리
- 결과 저장
- 외부 공개 API 제공

## 목표 구조

```text
spring_orchestrator/
├── AGENT.md
├── docs/
└── src/
    ├── main/java
    └── test/java
```

## 핵심 원칙

- 헥사고날 아키텍처
- 구조를 테스트로 증명
- JavaDoc 우선
- 외부 API는 HATEOAS 고려
- 에이전트 호출은 outbound adapter로 한정

## 자바 버전

- 기준 버전: `Java 25 LTS`

## 현재 상태

- `WorkflowStateMachine`, `WorkflowRunner`, `WorkflowService`가 구현되어 있다.
- 웹 계층은 `WorkflowController` 기준으로 생성/조회/실행 API가 있다.
- 기본 저장소는 `memory` 프로필의 `InMemoryWorkflowRepository`다.
- `memory` 프로필에서는 실제 에이전트 HTTP 연동 전까지 stub outbound adapter로 실행 흐름을 검증한다.
- `local-photo-info` 프로필에서는 `LocalPhotoInfoPipelineAdapter`가 CLI 기반 사진 정보 추출 파이프라인을 실행한다.
- `memory` 외 프로필에서는 `PhotoGroupingAgentClient`가 FastAPI 그룹화 에이전트를 HTTP로 호출한다.
- `postgres` 프로필용 JPA persistence adapter가 추가되어 있다.
- 테스트와 JaCoCo 커버리지 검증이 통과한다.
- 현재 라인 커버리지는 `90% 이상`을 유지하도록 Gradle 검증이 걸려 있다.

## 표준 테스트 명령

로컬 환경에 따라 Gradle 네이티브 로딩 문제가 있을 수 있어서, 현재는 아래 명령을 표준으로 사용한다.

```bash
env GRADLE_USER_HOME=.gradle-home GRADLE_OPTS='-Dorg.gradle.native=false' gradle test jacocoTestReport jacocoTestCoverageVerification
```

## 프로필

- 기본 프로필: `memory`
- PostgreSQL 연동 확인용 프로필: `postgres`

## 다른 PC에서 바로 시작하는 순서

1. `../Agent.md` 읽기
2. `./AGENT.md` 읽기
3. `../docs/orchestrator-design.md` 읽기
4. `../docs/adr/003-id-strategy.md`, `../docs/adr/004-database-strategy.md` 읽기
5. 위의 표준 테스트 명령으로 현재 상태 검증
6. [../docs/next-session-handoff.md](../docs/next-session-handoff.md) 기준으로 다음 작업 진행

## 참고 문서

- 오케스트레이터 설계: [../docs/orchestrator-design.md](../docs/orchestrator-design.md)
- 전역 규칙: [../Agent.md](../Agent.md)
- 다음 세션 인수인계: [../docs/next-session-handoff.md](../docs/next-session-handoff.md)
