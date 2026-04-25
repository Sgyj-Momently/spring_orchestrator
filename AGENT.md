# Spring Orchestrator Rules

이 문서는 `spring_orchestrator` 프로젝트 내부에서 따르는 전용 개발 규칙을 정의한다.
루트의 [Agent.md](../Agent.md)를 상속하며, 스프링/자바 프로젝트에서는 이 문서의 규칙을 더 강하게 적용한다.

## 0. 자바 버전 기준

- 이 프로젝트의 기준 자바 버전은 `Java 25 LTS`이다.
- 빌드, 테스트, 로컬 실행, CI 환경은 가능하면 같은 메이저 버전으로 맞춘다.
- 버전 변경 시 `build.gradle`, 문서, CI 설정을 함께 수정한다.

## 1. 아키텍처 원칙

- 이 프로젝트는 헥사고날 아키텍처를 기본 구조로 사용한다.
- 도메인 계층은 스프링 프레임워크와 외부 기술에 의존하지 않는다.
- 애플리케이션 계층은 유스케이스와 오케스트레이션 책임만 가진다.
- 어댑터 계층은 inbound와 outbound 역할을 명확히 구분한다.
- 컨트롤러는 요청을 유스케이스로 전달하고 응답으로 변환하는 역할만 수행한다.
- 외부 에이전트 호출은 outbound adapter를 통해서만 수행한다.
- 특정 에이전트의 API 세부사항이 도메인이나 애플리케이션 계층으로 새어 나오지 않게 한다.

## 2. 패키지/모듈 경계

- 기본 패키지 구조는 아래를 따른다.
- `domain`
- `application`
- `adapter.in.web`
- `adapter.out.agent`
- `adapter.out.persistence`
- `config`
- 처음에는 단일 스프링 프로젝트로 시작할 수 있지만, 패키지 경계는 멀티모듈 수준으로 엄격히 유지한다.
- 필요해지면 아래와 같은 Gradle 멀티모듈로 확장할 수 있게 설계한다.
- `orchestrator-domain`
- `orchestrator-application`
- `orchestrator-adapter-in-web`
- `orchestrator-adapter-out-agent`
- `orchestrator-architecture-test`

## 3. 구조를 테스트로 증명

- 헥사고날 원칙은 말로만 유지하지 않고 테스트 코드로 항상 증명한다.
- ArchUnit 테스트를 반드시 둔다.
- 최소 검증 규칙은 아래를 포함한다.
- `domain`은 `application`, `adapter`, `config`를 참조하지 않는다.
- `application`은 `adapter.in`, `adapter.out` 구현체를 직접 참조하지 않는다.
- `adapter.in.web`는 `application` 유스케이스만 호출한다.
- `adapter.out.agent`는 외부 API 호출 구현 책임만 가진다.
- 모듈 경계 위반은 테스트 실패로 처리한다.

## 4. 테스트 원칙

- 유스케이스는 단위 테스트를 기본으로 한다.
- 아키텍처 규칙은 ArchUnit 테스트로 검증한다.
- 웹 계층은 slice test 또는 통합 테스트로 검증한다.
- 외부 에이전트 호출은 mock 또는 contract test로 검증한다.
- 구조 테스트가 없는 새로운 레이어 추가는 허용하지 않는다.

## 5. JavaDoc 원칙

- public class, public interface, public method에는 JavaDoc을 작성한다.
- 외부 API DTO, command, response, use case, port에는 JavaDoc을 필수로 작성한다.
- JavaDoc은 “무엇을 하는가”와 “왜 존재하는가”를 드러내야 한다.
- 도메인 개념과 상태 전이는 JavaDoc으로 의미를 분명히 남긴다.
- private helper는 무조건 JavaDoc을 강제하지 않지만, 복잡한 로직은 짧은 주석 또는 명확한 이름으로 설명한다.
- JavaDoc은 한 줄 요약으로 끝내지 않는다. 최소한 책임, 계층상 존재 이유, 주요 입력/출력 의미를 함께 설명한다.
- adapter JavaDoc에는 어떤 외부 시스템/프로필/실행 방식을 감싸는지, 실패가 어떤 예외나 상태로 전파되는지 적는다.
- port JavaDoc에는 application 계층이 외부 구현을 몰라도 되게 하는 경계라는 점과 구현체가 지켜야 할 계약을 적는다.
- 설정 properties JavaDoc에는 공개 API로 받지 않는 내부 설정이라는 점, 기본값 또는 운영에서 바꿔야 하는 값을 적는다.
- record JavaDoc의 `@param`에는 단순 타입 설명이 아니라 후속 단계에서 어떻게 쓰이는 값인지 적는다.
- override method라도 외부 호출 지점이거나 부작용이 있으면 JavaDoc을 다시 작성해 구현체별 동작과 실패 조건을 명시한다.
- JavaDoc이 코드와 달라졌다고 느껴지면 코드 변경과 같은 커밋에서 반드시 같이 갱신한다.

## 6. HATEOAS 원칙

- 외부 공개 REST API는 가능하면 HATEOAS를 적용한다.
- 특히 workflow 생성/조회/재시도/취소 API에는 링크 관계를 명시하는 방향을 우선 고려한다.
- 내부 에이전트 간 호출 API는 HATEOAS보다 간결한 계약을 우선할 수 있다.
- 외부 API와 내부 API의 설계 기준을 혼동하지 않는다.

## 7. API 원칙

- 공개 API는 도메인 의도만 표현한다.
- 인프라 설정, 내부 모델 주소, timeout 같은 값은 공개 요청 계약에 노출하지 않는다.
- enum, 식별자, 명시적 전략 값을 우선 사용한다.
- entity를 직접 외부로 노출하지 않고 DTO로 변환한다.
- request/response DTO는 계약 관점에서 안정적으로 유지한다.

## 7-1. ID 전략 원칙

- 외부 공개 식별자와 워크플로 식별자의 기본 전략은 `UUIDv7`이다.
- 분산 환경에서도 중앙 조정 없이 충돌 가능성을 사실상 무시할 수 있는 방향을 우선한다.
- 숫자형 내부 키가 정말 필요한 경우에만 성능 검증을 근거로 `Snowflake` 계열 전략을 별도 검토한다.
- 공개 API 계약, 링크, 아티팩트 참조에는 내부 전용 증가형 키를 직접 노출하지 않는다.

## 7-2. DB 전략 원칙

- 기본 운영 데이터 저장소는 `PostgreSQL`을 기준으로 설계한다.
- 워크플로 메타데이터와 상태 기록은 관계형 DB에 저장한다.
- 큰 JSON 산출물과 문서 결과물은 DB 본문에 과도하게 적재하지 않고 artifact 저장소로 분리한다.
- 캐시, 분산 락, 단기 상태 공유가 필요할 때만 `Redis`를 보조 계층으로 추가한다.
- 높은 트래픽과 수평 확장을 기본 가정으로 두고 멱등성, 재시도, 중복 방지 전략을 함께 설계한다.

## 8. 오케스트레이션 원칙

- 전체 작업 순서와 상태 머신은 Spring이 관리한다.
- 에이전트는 다음 단계를 결정하지 않는다.
- 실패한 단계는 기록되어야 하며, 이전 성공 산출물은 유지한다.
- workflow 상태 전이는 코드와 테스트에서 모두 확인 가능해야 한다.

## 9. 권장 코드 구조

- `WorkflowController`
- `WorkflowService`
- `WorkflowStateMachine`
- `WorkflowRepository`
- `PhotoInfoAgentClient`
- `PhotoGroupingAgentClient`
- `HeroPhotoAgentClient`

## 10. 금지 사항

- 컨트롤러에서 비즈니스 로직 직접 구현 금지
- 도메인 계층에서 스프링 애너테이션 사용 금지
- 외부 에이전트 HTTP 호출을 application/domain에서 직접 수행 금지
- 구조 테스트 없이 패키지 경계 변경 금지

## 11. Git Flow 적용

- 이 프로젝트의 기능 작업은 `main`에서 직접 진행하지 않는다.
- 스프링 오케스트레이터 관련 개발은 기본적으로 `feature/orchestrator-*` 브랜치에서 시작한다.
- 운영 장애 대응이 아닌 이상 `hotfix/*`를 남용하지 않는다.
- PR 전에는 최소 `gradle test jacocoTestReport jacocoTestCoverageVerification` 기준의 검증 상태를 확인한다.
- 문서에서 참조하는 전역 규칙은 루트의 [Agent.md](../Agent.md)를 기준으로 유지한다.
