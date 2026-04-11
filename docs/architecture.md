# Spring Orchestrator Architecture

## 목적

이 프로젝트는 여러 에이전트 모듈을 조합해 하나의 워크플로를 완성하는 상위 오케스트레이터다.

## 권장 패키지 구조

```text
com.momently.orchestrator
├── domain
├── application
├── adapter
│   ├── in
│   │   └── web
│   └── out
│       ├── agent
│       └── persistence
└── config
```

## 계층 책임

### domain

- Workflow
- WorkflowStatus
- 도메인 규칙

### application

- 유스케이스
- 상태 전이
- 오케스트레이션 로직

### adapter.in.web

- REST API
- HATEOAS 응답 구성

### adapter.out.agent

- FastAPI 에이전트 호출
- request/response DTO 매핑

### adapter.out.persistence

- DB 저장
- 산출물 경로 저장

## 아키텍처 테스트 권장 항목

- domain -> adapter 의존 금지
- application -> adapter 구현 의존 금지
- web adapter -> application use case만 사용
- outbound agent adapter는 외부 HTTP 구현만 포함

## 향후 확장

- Gradle 멀티모듈 분리
- `architecture-test` 별도 모듈
- agent client 모듈 분리
