# Testing Policy

## 필수 테스트

- ArchUnit 구조 테스트
- 유스케이스 단위 테스트
- Web layer slice test
- Agent client contract test

## 구조 테스트 예시

- domain 패키지는 adapter 패키지를 참조하지 않는다.
- application 패키지는 adapter 구현체를 참조하지 않는다.
- controller는 use case만 호출한다.
- outbound agent adapter는 HTTP 클라이언트 구현 책임만 가진다.

## 품질 기준

- 새 레이어나 패키지를 추가하면 구조 테스트도 함께 추가한다.
- 공개 API DTO가 바뀌면 관련 문서와 테스트를 함께 수정한다.
- workflow 상태 전이는 테스트로 증명한다.
