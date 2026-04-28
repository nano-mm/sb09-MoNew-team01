# 📰 모뉴 (MoNew)

> 흩어진 뉴스를 한 곳에, 관심 있는 주제만 모아보세요!

**모뉴(MoNew)** 는 다양한 뉴스 출처를 통합하여 관심사 기반으로 뉴스를 저장하는 **뉴스 통합 관리 플랫폼**입니다.
관심 있는 주제의 기사가 등록되면 실시간 알림을 받고, 댓글과 좋아요를 통해 다른 사용자와 의견을 나눌 수 있는 소셜 기능도 함께 제공합니다.

<br>

##  목차

1. [핵심 기능](#-핵심-기능)
2. [기술 스택](#-기술-스택)
3. [시스템 아키텍처](#-시스템-아키텍처)
4. [API 명세](#-api-명세)
5. [팀원 소개](#-팀원-소개)

<br>

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
- 뉴스 삭제 및 **MongoDB 외부 저장소 연동** 지원

###  댓글 관리
- 댓글 등록 / 수정 / 삭제 (소프트 딜리트) / 목록 조회
- 좋아요 / 좋아요 취소
- 좋아요 순 / 최신 순 커서 기반 페이지네이션

###  알림 관리
- 관심사 기사 등록 시 실시간 알림
- 내 댓글에 좋아요 발생 시 알림
- 알림 생성 / 목록 조회 / 읽음 처리 / 삭제

###  활동 내역 관리
- 댓글 작성, 좋아요 등 사용자 활동 이력 자동 스냅샷 관리

###  기술 요구 사항
- 입력값 유효성 검사 및 커스텀 예외 처리
- 구조화된 로그 관리
- 테스트 주도 개발 (TDD)
- CI/CD 파이프라인 구축
- Spring Batch를 활용한 뉴스 기사 배치 처리
- MongoDB를 외부 저장소로 활용한 데이터 관리

<br>

---

##  기술 스택

### Backend
| 분류 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.5|
| ORM | Spring Data JPA / Hibernate |
| Security | Spring Security |
| Batch | Spring Batch |
| DB (Primary) | PostgreSQL |
| DB (External) | MongoDB |
| Build Tool | Gradle |
| Test | JUnit5, Mockito |

### Infra
| 분류 | 기술 |
|------|------|
| CI/CD | GitHub Actions |
| Container | Docker |


<br>

---

##  시스템 아키텍처

```
┌─────────────┐
│   Client    │
│  (Browser)  │
└──────┬──────┘
       │ HTTP
       ▼
┌─────────────────────────────────────────────┐
│              Spring Boot Application        │
│                                             │
│  ┌──────────────┐    ┌───────────────────┐  │
│  │  Controller  │───▶│     Service       │  │
│  └──────────────┘    └────────┬──────────┘  │
│                               │             │
│                      ┌────────▼──────────┐  │
│                      │    Repository     │  │
│                      │  (JPA / Mongo)    │  │
│                      └────────┬──────────┘  │
└───────────────────────────────┼─────────────┘
                                │
               ┌────────────────┴────────────────┐
               │                                 │
    ┌──────────▼──────────┐       ┌──────────────▼──────────┐
    │     PostgreSQL      │       │         MongoDB         │
    │   (Primary DB)      │       │   (External Storage)    │
    └─────────────────────┘       └─────────────────────────┘
```

<br>

---

##  API 명세

###  사용자 (Users)

| Method | URI | 설명 | 인증 |
|--------|-----|------|:----:|
| `POST` | `/api/users` | 회원가입 | ❌ |
| `POST` | `/api/users/login` | 로그인 | ❌ |
| `GET` | `/api/users/me` | 내 정보 조회 | ✅ |
| `PATCH` | `/api/users/{userId}` | 정보 수정 | ✅ |
| `DELETE` | `/api/users/{userId}` | 회원 탈퇴 | ✅ |

---

###  관심사 (Interests)

| Method | URI | 설명 | 인증 |
|--------|-----|------|:----:|
| `POST` | `/api/interests` | 관심사 등록 | ✅ |
| `GET` | `/api/interests` | 관심사 목록 조회 | ✅ |
| `PATCH` | `/api/interests/{interestId}` | 관심사 수정 | ✅ |
| `DELETE` | `/api/interests/{interestId}` | 관심사 삭제 | ✅ |
| `POST` | `/api/interests/{interestId}/subscriptions` | 구독 | ✅ |
| `DELETE` | `/api/interests/{interestId}/subscriptions` | 구독 취소 | ✅ |

---

###  뉴스 기사 (Articles)

| Method | URI | 설명 | 인증 |
|--------|-----|------|:----:|
| `GET` | `/api/articles` | 뉴스 목록 조회 | ✅ |
| `DELETE` | `/api/articles/{articleId}` | 뉴스 삭제 | ✅ (ADMIN) |
| `POST` | `/api/articles/backup` | MongoDB 저장 | ✅ (ADMIN) |
| `POST` | `/api/articles/restore` | MongoDB 복원 | ✅ (ADMIN) |

---

###  댓글 (Comments)

| Method | URI | 설명 | 인증 |
|--------|-----|------|:----:|
| `POST` | `/api/comments` | 댓글 등록 | ✅ |
| `GET` | `/api/comments` | 댓글 목록 조회 | 선택 |
| `PATCH` | `/api/comments/{commentId}` | 댓글 수정 | ✅ (본인) |
| `DELETE` | `/api/comments/{commentId}` | 댓글 삭제 (소프트) | ✅ (본인) |
| `DELETE` | `/api/comments/{commentId}/hard` | 댓글 삭제 (하드) | ✅ (ADMIN) |
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
| `GET` | `/api/user-activities/{userId}` | 활동 내역 조회 | ✅ |

<br>

---

##  팀원 소개

| 이름     | 담당 기능          | GitHub |
|---------|----------------|--------|
| 나은비   | 알림,인프라 구축      | [@github](#) |
| 이진용   | 뉴스 기사 관리       | [@github](#) |
| 한성재   | 사용자, 활동 내역 관리  | [@github](#) |
| 임혜민   | 관심사 관리         | [@github](#) |
| 이용일   | 댓글 관리          | [@github](#) |
<br>

---

<p align="center">
  <sub>© 2026 MoNew Team Doraemong House. All rights reserved.</sub>
</p>
