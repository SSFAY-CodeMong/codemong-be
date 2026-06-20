INSERT INTO user_roles (id, name)
SELECT 1, 'USER'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM user_roles WHERE id = 1)
UNION ALL
SELECT 2, 'ADMIN'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM user_roles WHERE id = 2);

INSERT INTO projects (description, name, type, max_step)
SELECT '게시판 CRUD, 댓글, 검증/예외 처리를 단계별 hidden test로 구현하는 Spring Boot 백엔드 프로젝트입니다.', 'mmcafe', 'BE', 5
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM projects WHERE name = 'mmcafe'
);

UPDATE projects
SET description = '게시판 CRUD, 댓글, 검증/예외 처리를 단계별 hidden test로 구현하는 Spring Boot 백엔드 프로젝트입니다.',
    type = 'BE',
    max_step = 5
WHERE name = 'mmcafe';

UPDATE projects
SET max_step = 5
WHERE max_step IS NULL;

INSERT INTO steps (project_id, step, content)
SELECT p.id, s.step, s.content
FROM projects p
JOIN (
    SELECT 1 AS step, '게시글 생성과 단건 조회를 구현합니다. POST /boards는 title/content를 받아 201 Created와 id, title, content, createdAt을 반환해야 하고, GET /boards/{id}는 존재하는 게시글을 200 OK로 조회해야 합니다. 존재하지 않는 게시글 조회는 404 Not Found를 반환해야 합니다.' AS content
    UNION ALL
    SELECT 2, '게시글 목록 조회와 페이징을 구현합니다. GET /boards?page={page}&size={size}는 Page 응답 형태로 content 배열을 반환해야 하며, 최신 게시글이 먼저 오도록 createdAt 기준 내림차순 정렬되어야 합니다.'
    UNION ALL
    SELECT 3, '게시글 수정과 삭제를 구현합니다. PUT /boards/{id}는 title/content를 수정하고 수정된 게시글을 반환해야 하며, DELETE /boards/{id}는 204 No Content를 반환해야 합니다. 수정/삭제/조회 대상이 없으면 404 Not Found를 반환해야 합니다.'
    UNION ALL
    SELECT 4, '댓글 생성, 목록 조회, 삭제를 구현합니다. POST /boards/{id}/comments는 존재하는 게시글에 댓글을 생성하고 201 Created를 반환해야 하며, GET /boards/{id}/comments는 해당 게시글 댓글 목록을 반환해야 합니다. DELETE /boards/comments/{id}는 댓글을 삭제하고 204 No Content를 반환해야 합니다. 없는 게시글에 댓글을 달면 404 Not Found를 반환해야 합니다.'
    UNION ALL
    SELECT 5, '요청 값 검증과 공통 예외 응답을 구현합니다. 게시글 title/content와 댓글 content가 비어 있으면 400 Bad Request를 반환해야 하며, 존재하지 않는 게시글/댓글 요청은 404 Not Found와 status 값을 포함한 JSON 오류 응답을 반환해야 합니다.'
) s ON TRUE
WHERE p.name = 'mmcafe'
ON DUPLICATE KEY UPDATE content = VALUES(content);

INSERT INTO testcodes (project_id, step, method_name, description)
SELECT p.id, t.step, t.method_name, t.description
FROM projects p
JOIN (
    SELECT 1 AS step,
           'createBoardAndReadBoard' AS method_name,
           'POST /boards로 게시글을 생성했을 때 201 Created, 숫자 id, 요청한 title/content, createdAt이 응답되어야 합니다. 이어서 GET /boards/{id}로 같은 게시글을 조회했을 때 200 OK와 저장된 title이 반환되어야 합니다.' AS description
    UNION ALL
    SELECT 1,
           'readMissingBoardFails',
           '존재하지 않는 게시글 id로 GET /boards/{id}를 호출했을 때 404 Not Found를 반환해야 합니다. Optional 처리나 예외 매핑이 누락되면 이 테스트가 실패합니다.'
    UNION ALL
    SELECT 2,
           'listBoardsWithLatestOrderAndPaging',
           '여러 게시글을 생성한 뒤 GET /boards?page=0&size=2, page=1&size=2를 호출했을 때 Page 응답의 content 크기와 순서가 맞아야 합니다. 최신순 정렬이 아니거나 Page 형태가 아니면 실패합니다.'
    UNION ALL
    SELECT 3,
           'updateDeleteAndMissingCases',
           '게시글 수정, 수정 후 조회, 삭제, 삭제 후 404, 없는 게시글 수정/삭제 404를 모두 검증합니다. PUT /boards/{id}, DELETE /boards/{id}의 상태 코드와 영속성 반영을 확인하세요.'
    UNION ALL
    SELECT 4,
           'commentFlow',
           '게시글에 댓글을 생성하고 목록에서 확인한 뒤 댓글을 삭제하면 목록이 비어야 합니다. POST /boards/{id}/comments, GET /boards/{id}/comments, DELETE /boards/comments/{id} 흐름을 검증합니다.'
    UNION ALL
    SELECT 4,
           'commentOnMissingBoardFails',
           '존재하지 않는 게시글에 POST /boards/{id}/comments를 호출했을 때 404 Not Found를 반환해야 합니다. 댓글 생성 전에 게시글 존재 여부를 확인해야 합니다.'
    UNION ALL
    SELECT 5,
           'validationAndExceptionStatus',
           '게시글 title/content 또는 댓글 content가 빈 값이면 400 Bad Request가 나와야 하고, 없는 게시글/댓글 요청은 404와 status 값을 포함한 JSON 오류 응답을 반환해야 합니다.'
) t ON TRUE
WHERE p.name = 'mmcafe'
ON DUPLICATE KEY UPDATE description = VALUES(description);
