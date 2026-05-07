![CI](https://github.com/nano-mm/sb09-MoNew-team01/actions/workflows/ci.yml/badge.svg) [![Code Coverage](https://codecov.io/gh/nano-mm/sb09-MoNew-team01/branch/develop/graph/badge.svg)](https://codecov.io/gh/nano-mm/sb09-MoNew-team01) ![CD](https://github.com/nano-mm/sb09-MoNew-team01/actions/workflows/cd.yml/badge.svg)

# 📰 모뉴 (MoNew)

> 흩어진 뉴스를 한 곳에, 관심 있는 주제만 모아보세요!

**모뉴(MoNew)** 는 다양한 뉴스 출처를 통합하여 관심사 기반으로 뉴스를 저장하는 **뉴스 통합 관리 플랫폼**입니다.
관심 있는 주제의 기사가 등록되면 실시간 알림을 받고, 댓글과 좋아요를 통해 다른 사용자와 의견을 나눌 수 있는 소셜 기능도 함께 제공합니다.

<br>

##  목차

1. [핵심 기능](#핵심-기능)
2. [기술 스택](#기술-스택)
3. [시스템 아키텍처](#시스템-아키텍처)
4. [파일 구조](#팀원-소개)
5. [API 명세](#api-명세)
6. [팀원 소개](#팀원-소개)

---

##  핵심 기능

###  사용자 관리
- 회원가입 / 로그인 / 정보 수정 / 회원 탈퇴
- 유효성 검사 및 커스텀 예외 처리

###  관심사 관리
- 관심사 등록 / 수정 / 삭제 / 목록 조회
- 관심사 구독으로 맞춤형 뉴스 피드 제공

###  뉴스 기사 관리
- 다양한 뉴스 소스에서 기사 자동 수집
- 커서 기반 페이지네이션으로 뉴스 목록 조회
- 뉴스 삭제
- AWS S3 외부 저장소에 뉴스 백업 및 복구

###  댓글 관리
- 댓글 등록 / 수정 / 삭제 / 목록 조회
- 좋아요 / 좋아요 취소
- 좋아요 순 / 최신 순 커서 기반 페이지네이션

###  알림 관리
- 관심사 기사 등록 시 실시간 알림
- 내 댓글에 좋아요 발생 시 알림
- 알림 생성 / 목록 조회 / 읽음 처리 / 삭제

###  활동 내역 관리
- 댓글 작성, 좋아요 등 사용자 활동 이력 자동 스냅샷 관리

<br>

---

##  기술 스택

### Backend
| 분류 | 기술                          |
|------|-----------------------------|
| Language | Java 17  |
| Framework | Spring Boot 3.5  |
| ORM | Spring Data JPA / Hibernate |
| Security | Spring Security |
| Batch | Spring Batch  |
| DB (Primary) | PostgreSQL |
| DB (External) | MongoDB  |
| Build Tool | Gradle|
| Test | JUnit5, Mockito  |

### Infra
| 분류 | 기술 |
|------|------|
| CI/CD | GitHub Actions |
| Container | Docker |
| Cloud | AWS EC2|


<br>

---

##  시스템 아키텍처

본 프로젝트는 2차 리팩터링을 통해 **Hexagonal Architecture(Ports & Adapters)** 구조를 적용했습니다.

```
Client
  │ HTTP
  ▼
adapter.in.web
  │ depends on
  ▼
application.port.in
  │ implemented by
  ▼
application.service
  │ depends on
  ▼
application.port.out
  ▲ implemented by
  │
adapter.out.persistence / adapter.out.mongo / adapter.out.news / adapter.out.storage
  │
  ├─ PostgreSQL
  ├─ MongoDB
  ├─ External News API / RSS
  └─ AWS S3 / Local Storage
```

### 의존성 방향

```
adapter.in.web → application.port.in ← application.service → application.port.out ← adapter.out.*
```

Application 계층은 adapter 구현체에 직접 의존하지 않고, port 인터페이스에만 의존합니다.

<br>

---

## 파일구조
```
monew
┣ src
┃ ┣ main
┃ ┃ ┣ java
┃ ┃ ┃ ┗ com.monew
┃ ┃ ┃ ┃ ┣ adapter
┃ ┃ ┃ ┃ ┃ ┣ in
┃ ┃ ┃ ┃ ┃ ┃ ┗ web             // REST Controller, inbound adapter
┃ ┃ ┃ ┃ ┃ ┗ out
┃ ┃ ┃ ┃ ┃ ┃ ┣ mongo           // MongoDB read model adapter
┃ ┃ ┃ ┃ ┃ ┃ ┣ news            // 외부 뉴스 API/RSS adapter
┃ ┃ ┃ ┃ ┃ ┃ ┗ storage         // S3/Local storage adapter
┃ ┃ ┃ ┃ ┣ application
┃ ┃ ┃ ┃ ┃ ┣ port
┃ ┃ ┃ ┃ ┃ ┃ ┣ in              // inbound use case port
┃ ┃ ┃ ┃ ┃ ┃ ┗ out             // outbound persistence/news/storage port
┃ ┃ ┃ ┃ ┃ ┗ service           // use case 구현체
┃ ┃ ┃ ┃ ┣ domain
┃ ┃ ┃ ┃ ┃ ┗ model             // 도메인/JPA 모델
┃ ┃ ┃ ┃ ┣ dto                 // 요청/응답 DTO
┃ ┃ ┃ ┃ ┣ exception           // 커스텀 예외 + 핸들러
┃ ┃ ┃ ┃ ┣ mapper              // MapStruct
┃ ┃ ┃ ┃ ┣ config              // Security, JPA, Mongo, Batch 설정
┃ ┃ ┃ ┃ ┣ scheduler           // 배치/스케줄러
┃ ┃ ┃ ┃ ┣ listener            // 이벤트 리스너
┃ ┃ ┃ ┃ ┣ util
┃ ┃ ┃ ┃ ┗ MonewApplication.java
┃ ┃ ┗ resources
┃ ┃ ┃ ┣ application.yml
┃ ┃ ┃ ┣ application-dev.yml
┃ ┃ ┃ ┣ application-prod.yml
┃ ┃ ┃ ┗ logback-spring.xml
┃ ┗ test
┃ ┃ ┗ java
┃ ┃ ┃ ┗ com.monew
┃ ┃ ┃ ┃ ┣ unit                # 1. 단위 테스트 (가장 높은 비중)
┃ ┃ ┃ ┃ ┃ ┣ controller        # WebMvcTest를 통한 입력값 검증 테스트
┃ ┃ ┃ ┃ ┃ ┣ service           # Mockito를 활용한 비즈니스 로직 테스트
┃ ┃ ┃ ┃ ┃ ┣ repository        # DataJpaTest (QueryDSL 포함)
┃ ┃ ┃ ┃ ┃ ┗ domain            # 엔티티 메서드 및 비즈니스 규칙 테스트
┃ ┃ ┃ ┃ ┣ integration         # 2. 통합 테스트 (핵심 시나리오)
┃ ┃ ┃ ┃ ┃ ┗ UserActivityFlowTest # 회원가입 -> 기사조회 -> 댓글작성 흐름
┃ ┃ ┃ ┃ ┣ support             # 3. 테스트 보조 도구 (중요!)
┃ ┃ ┃ ┃ ┃ ┣ fixture           # 테스트 데이터 생성기 (UserFixture 등)
┃ ┃ ┃ ┃ ┃ ┣ config            # 테스트 전용 Bean 설정
┃ ┃ ┃ ┃ ┃ ┗ DatabaseCleaner   # 매 테스트 후 DB 초기화 유틸
┃ ┃ ┃ ┃ ┗ arch                # 4. 아키텍처 테스트 (선택 사항)
┃ ┃ ┃ ┃   ┗ DependencyTest    # 패키지 간 의존성 규칙 준수 확인
┣ jacoco.gradle               # 5. 테스트 커버리지 측정 설정 파일
┣ build.gradle                # JaCoCo 플러그인 추가
┣ docker-compose.yml
┣ Dockerfile
┣ .env
┗ README.md
```
---

##  API 명세

###  사용자 (Users)

| Method | URI | 설명 | 인증 |
|--------|-----|------|:----:|
| `POST` | `/api/users` | 회원가입 | ❌ |
| `POST` | `/api/users/login` | 로그인 | ❌ |
| `PATCH` | `/api/users/{userId}` | 정보 수정 | ❌ |
| `DELETE` | `/api/users/{userId}` | 회원 탈퇴 (논리삭제) | ❌ |
| `DELETE` | `/api/users/{userId}/hard` | 회원 탈퇴 (물리삭제) | ❌ |

---

###  관심사 (Interests)

| Method | URI | 설명 | 인증 |
|--------|-----|------|:----:|
| `POST` | `/api/interests` | 관심사 등록 | ❌ |
| `GET` | `/api/interests` | 관심사 목록 조회 | ✅ |
| `PATCH` | `/api/interests/{interestId}` | 관심사 수정 | ❌ |
| `DELETE` | `/api/interests/{interestId}` | 관심사 삭제 | ❌ |
| `POST` | `/api/interests/{interestId}/subscriptions` | 구독 | ✅ |
| `DELETE` | `/api/interests/{interestId}/subscriptions` | 구독 취소 | ✅ |

---

###  뉴스 기사 (Articles)

| Method | URI | 설명 | 인증 |
|--------|-----|------|:----:|
| `GET` | `/api/articles` | 뉴스 목록 조회 | ✅ |
| `GET` | `/api/articles/sources` | 출처 조회 | ❌ |
| `GET` | `/api/articles/restore` | 기사 복구 | ❌ |
| `DELETE` | `/api/articles/{articleId}` | 뉴스 삭제 (논리삭제) | ❌ |
| `DELETE` | `/api/articles/{articleId}/hard` | 뉴스 삭제 (물리삭제) | ❌ |

---

###  댓글 (Comments)

| Method | URI | 설명 | 인증 |
|--------|-----|------|:----:|
| `POST` | `/api/comments` | 댓글 등록 | ❌ |
| `GET` | `/api/comments` | 댓글 목록 조회 | 선택 |
| `PATCH` | `/api/comments/{commentId}` | 댓글 수정 | ✅ (본인) |
| `DELETE` | `/api/comments/{commentId}` | 댓글 삭제 (논리삭제) | ❌ |
| `DELETE` | `/api/comments/{commentId}/hard` | 댓글 삭제 (물리삭제) | ❌ |
| `POST` | `/api/comments/{commentId}/comment-likes` | 좋아요 | ✅ |
| `DELETE` | `/api/comments/{commentId}/comment-likes` | 좋아요 취소 | ✅ |

---

###  알림 (Notifications)

| Method | URI | 설명 | 인증 |
|--------|-----|------|:----:|
| `POST` | `/api/notifications` | 알림 생성 | ✅ |
| `GET` | `/api/notifications` | 알림 목록 조회 | ✅ |
| `PATCH` | `/api/notifications/{notificationId}` | 알림 읽음 처리 | ✅ |
| `DELETE` | `/api/notifications/{notificationId}` | 알림 삭제 | ✅ |

---

###  활동 내역 (User Activity)

| Method | URI | 설명 | 인증 |
|--------|-----|------|:----:|
| `GET` | `/api/user-activities/{userId}` | 활동 내역 조회 | ❌ |

<br>

---

## 팀원 소개

| 이름     | 담당 기능           | GitHub |
|----------|--------------------|--------|
| 나은비   | 알림, 인프라 구축   | [@nano-mm](https://github.com/nano-mm) |
| 이진용   | 사용자, 활동 내역 관리 | [@alpha-lens](https://github.com/alpha-lens) |
| 한성재   | 뉴스 기사 관리       | [@seonghj](https://github.com/seonghj) |
| 임혜민   | 관심사 관리         | [@hyemin-L](https://github.com/hyemin-L) |
| 이용일   | 댓글 관리           | [@lyi980403-arch](https://github.com/lyi980403-arch) |

---

<p style="text-align:center">
  <sub>© 2026 MoNew Team Doraemong House. All rights reserved.</sub>
</p>
