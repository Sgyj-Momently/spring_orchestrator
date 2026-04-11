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

## 참고 문서

- 오케스트레이터 설계: [../docs/orchestrator-design.md](../docs/orchestrator-design.md)
- 전역 규칙: [../Agent.md](../Agent.md)
