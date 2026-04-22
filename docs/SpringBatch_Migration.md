Spring Batch 마이그레이션 설치 안내
=================================

이 문서는 Spring Batch를 프로덕션 환경에서 활성화하기 전에 필요한 메타데이터 테이블을 생성하는 방법을 안내합니다.

파일 위치
- Flyway 마이그레이션: `src/main/resources/db/migration/V1__create_spring_batch_tables.sql`
- 참고용 스키마: `src/main/resources/spring-batch/schema-postgresql.sql`

권장 절차
1. 데이터베이스 접속 정보를 준비하세요 (host, port, database, user, password).
2. 운영 DB에서 장애가 발생하지 않도록 유지보수 창을 예약하세요.
3. 아래 중 한 방법으로 스크립트를 적용하세요.

방법 A: Flyway를 이미 프로젝트에서 사용 중인 경우
1) Flyway가 설정되어 있다면 위 `db/migration/V1__create_spring_batch_tables.sql` 파일을 커밋하고 배포 시 Flyway가 자동으로 마이그레이션을 적용합니다.

방법 B: psql 명령으로 직접 적용 (PostgreSQL)
```bash
# 로컬에서 psql을 사용해 적용하는 예시
PGPASSWORD="<password>" psql -h <host> -p <port> -U <user> -d <database> -f src/main/resources/db/migration/V1__create_spring_batch_tables.sql
```

방법 C: 수동으로 SQL 내용을 DB 관리 UI(예: pgAdmin)에 붙여넣어 실행

주의 사항
- 스크립트는 기본적으로 존재하지 않는 테이블을 생성하도록 IF NOT EXISTS를 사용합니다. 이미 스키마가 적용되어 있으면 충돌이 발생하지 않습니다.
- Spring Batch 구성에서 `JobRepository`가 DB를 사용하도록 설정되어 있는지 확인하세요. (application-*.yml에서 datasource와 관련 설정 확인)
- Production DB에 적용하기 전 스테이징 환경에서 먼저 실행해보시기 바랍니다.

추가 제안
- 롤백을 위해 적용 전 스키마 덤프를 받아두세요.
- Flyway를 사용하면 버전 관리 및 롤백이 용이합니다. Flyway 적용이 가능하면 권장합니다.

