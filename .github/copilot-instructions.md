# GitHub Copilot Instructions

## 프로젝트 개요
암호화폐 커뮤니티 플랫폼 - Spring Boot 백엔드와 React TypeScript 프론트엔드로 구성

## 코딩 스타일 가이드

### Java/Spring Boot (백엔드)
- **패키지 구조**: `com.coincommunity.backend.*` 형식 유지
- **어노테이션**: `@RequiredArgsConstructor`, `@Slf4j` 사용
- **로깅**: SLF4J를 사용하여 적절한 로그 레벨 적용
- **API 문서**: Swagger/OpenAPI 어노테이션 필수 (`@Operation`, `@ApiResponses`)
- **예외 처리**: 커스텀 예외 클래스 활용
- **트랜잭션**: `@Transactional` 적절히 활용
- **DTO 패턴**: 엔티티 직접 노출 금지, DTO 변환 필수

### TypeScript/React (프론트엔드)
- **컴포넌트**: 함수형 컴포넌트 사용 (`React.FC`)
- **스타일링**: Material-UI (MUI) 컴포넌트 우선 사용
- **타입**: 엄격한 타입 정의, `any` 사용 금지
- **상태 관리**: React hooks (`useState`, `useEffect`) 활용
- **API 호출**: `apiClient` 유틸리티 사용
- **에러 처리**: try-catch 블록과 사용자 친화적 에러 메시지

## 명명 규칙
- **Java**: CamelCase (클래스), camelCase (메서드/변수)
- **TypeScript**: PascalCase (컴포넌트), camelCase (함수/변수)
- **API 엔드포인트**: kebab-case
- **데이터베이스**: snake_case

## 보안 가이드라인
- 사용자 입력 검증 필수
- SQL Injection 방지 (JPA 쿼리 메서드 사용)
- XSS 방지 (입력값 이스케이프)
- 인증/인가 체크 필수

## 성능 최적화
- **백엔드**: 페이징 처리, N+1 쿼리 방지, 적절한 인덱스 사용
- **프론트엔드**: React.memo, useMemo, useCallback 적절히 활용

## 테스트
- 단위 테스트 작성 권장
- API 테스트 시 `/test` 엔드포인트 활용

## 주요 기능
- 암호화폐 가격 정보 조회
- 뉴스 수집 및 표시 (Guardian API)
- 사용자 커뮤니티 (게시글, 댓글)
- 실시간 알림
- 포트폴리오 관리

## 외부 API
- Guardian API (뉴스)
- Upbit API (가격 정보)
- CoinGecko API (가격 정보)

## 데이터베이스
- H2 (개발), PostgreSQL (운영)
- JPA/Hibernate 사용

## 반드시 지켜야 할 규칙
1. 한글 주석과 로그 메시지 사용
2. 모든 public API에 Swagger 문서화
3. 프론트엔드는 반응형 디자인 (모바일 지원)
4. 에러 처리와 로깅 필수
5. 타입 안전성 보장

## 프론트엔드 디자인 가이드라인

### 가독성 (Readability)
- **매직 넘버 명명**: 상수로 대체하여 의미를 명확히 표현
- **구현 세부사항 추상화**: 복잡한 로직을 전용 컴포넌트/HOC로 분리
- **조건부 렌더링 분리**: 서로 다른 조건의 UI/로직을 별도 컴포넌트로 구성
- **복잡한 삼항 연산자 단순화**: if/else 또는 IIFE 사용으로 가독성 향상
- **시선 이동 최소화**: 단순 로직은 인라인 정의로 컨텍스트 스위칭 감소
- **복잡한 조건 명명**: 복잡한 boolean 조건을 의미있는 변수명으로 할당

### 예측 가능성 (Predictability)
- **반환 타입 표준화**: 유사한 함수/훅에 일관된 반환 타입 사용
- **숨겨진 로직 노출**: 함수는 시그니처에서 암시하는 작업만 수행 (단일 책임)
- **고유하고 설명적인 이름**: 모호함을 피하고 구체적인 동작을 명시

### 응집성 (Cohesion)
- **폼 응집성 고려**: 필드별 또는 폼별 응집성 선택 (요구사항에 따라)
- **기능/도메인별 코드 구성**: 코드 타입이 아닌 기능/도메인별 디렉토리 구조
- **매직 넘버와 로직 연관**: 상수를 관련 로직 근처에 정의하거나 명확한 이름 사용

### 결합도 (Coupling)
- **추상화와 결합도 균형**: 성급한 추상화 피하고 낮은 결합도 선호
- **상태 관리 범위 지정**: 넓은 상태 관리를 작고 집중된 훅/컨텍스트로 분할
- **Props Drilling 제거**: 컴포넌트 조합(Composition) 사용으로 불필요한 중간 의존성 제거

### 코드 패턴 예시
```typescript
// 매직 넘버 명명
const ANIMATION_DELAY_MS = 300;

// 복잡한 조건 명명
const isSameCategory = product.categories.some(category => category.id === targetCategory.id);
const isPriceInRange = product.prices.some(price => price >= minPrice && price <= maxPrice);

// 표준화된 반환 타입
type ValidationResult = { ok: true } | { ok: false; reason: string };

// 도메인별 디렉토리 구조
// src/domains/user/components/UserProfileCard.tsx
// src/domains/product/hooks/useProducts.ts
```
