INSERT INTO user_roles (id, name)
VALUES
    (1, 'USER'),
    (2, 'ADMIN')
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name;

INSERT INTO projects (description, name, type, max_step, frontend_required)
SELECT '게시판 CRUD, 댓글, 검증/예외 처리를 단계별 hidden test로 구현하는 Spring Boot 백엔드 프로젝트입니다.', 'mmcafe', 'BE', 5, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM projects WHERE name = 'mmcafe'
);

UPDATE projects
SET description = '게시판 CRUD, 댓글, 검증/예외 처리를 단계별 hidden test로 구현하는 Spring Boot 백엔드 프로젝트입니다.',
    type = 'BE',
    max_step = 5,
    frontend_required = TRUE
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
ON CONFLICT ON CONSTRAINT uk_steps_project_step DO UPDATE
SET content = EXCLUDED.content;

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
ON CONFLICT ON CONSTRAINT uk_testcodes_project_step_method DO UPDATE
SET description = EXCLUDED.description;

INSERT INTO mail_categories (name)
SELECT category.name
FROM (
    SELECT 'Spring' AS name
    UNION ALL
    SELECT 'JavaScript'
    UNION ALL
    SELECT 'Database'
    UNION ALL
    SELECT 'Architecture'
) category
WHERE NOT EXISTS (
    SELECT 1 FROM mail_categories existing WHERE existing.name = category.name
);

INSERT INTO mail_questions
    (category_id, title, content, model_answer, difficulty, question_type, created_at)
SELECT c.id, q.title, q.content, q.model_answer, q.difficulty, q.question_type, CURRENT_TIMESTAMP
FROM mail_categories c
JOIN (
    SELECT 'Spring' AS category_name,
           'Spring Boot 앱이 시작되지 않는 코드의 문제를 찾아 수정하세요.' AS title,
           '자동 설정과 컴포넌트 스캔이 적용되지 않는 상황에서 어떤 애노테이션이 필요한지 설명하세요.' AS content,
           '@SpringBootApplication은 자동 설정과 컴포넌트 스캔을 활성화합니다. 애플리케이션 시작 클래스에 이 애노테이션을 추가해야 Controller, Service, Repository Bean이 정상 등록됩니다.' AS model_answer,
           'EASY' AS difficulty,
           'BUG_FIX' AS question_type
    UNION ALL
    SELECT 'JavaScript',
           '비동기 함수가 항상 undefined를 반환하는 이유를 고치세요.',
           'fetch Promise 체인 내부에서만 값을 반환하고 바깥 async 함수에서 반환하지 않는 코드의 문제를 설명하세요.',
           'Promise 체인을 return하거나 await로 응답을 받은 뒤 결과를 return해야 합니다. 내부 then의 return은 바깥 함수 반환값이 아닙니다.',
           'NORMAL',
           'BUG_FIX'
    UNION ALL
    SELECT 'Database',
           'SQL JOIN 결과가 중복되는 이유를 설명하고 고치세요.',
           '1:N 관계에서 단순 JOIN을 했을 때 사용자 행이 여러 번 반복되는 이유와 최신 행만 가져오는 방법을 설명하세요.',
           '1:N 관계를 JOIN하면 N쪽 행 수만큼 결과가 늘어납니다. 사용자별 최신 행만 필요하면 집계 서브쿼리나 윈도우 함수를 사용해 대상 행을 먼저 제한해야 합니다.',
           'NORMAL',
           'CONCEPT'
    UNION ALL
    SELECT 'Architecture',
           '서비스 계층을 분리해야 하는 이유를 설명하세요.',
           'Controller에 비즈니스 로직과 외부 연동 로직이 몰렸을 때 어떤 문제가 생기는지 설명하세요.',
           'Controller는 요청/응답 변환에 집중하고 비즈니스 규칙은 Service로 분리해야 테스트, 재사용, 트랜잭션 경계 관리가 쉬워집니다.',
           'EASY',
           'DESIGN'
) q ON q.category_name = c.name
WHERE NOT EXISTS (
    SELECT 1 FROM mail_questions existing WHERE existing.title = q.title
);

UPDATE mail_questions
SET content = 'The function below is used on a problem-solving page. It should return the fetched user name, but callers keep receiving undefined. Explain the bug and fix the code.',
    code_template = 'async function loadUserName(userId) {
  fetch(`/api/users/${userId}`)
    .then(response => response.json())
    .then(user => {
      return user.name;
    });
}

const name = await loadUserName(7);
console.log(name);',
    model_answer = 'The outer async function does not return the Promise chain. Return the fetch chain or use await, then return user.name from the outer function. Example: const response = await fetch(...); const user = await response.json(); return user.name;',
    difficulty = 'NORMAL',
    question_type = 'BUG_FIX'
WHERE title = '비동기 함수가 항상 undefined를 반환하는 이유를 고쳐보세요.'
   OR title LIKE '%undefined%';

UPDATE mail_questions
SET content = 'Review the controller code. The endpoint works, but the controller owns too much business logic. Point out the problem and suggest a cleaner service-layer design.',
    code_template = '@RestController
@RequiredArgsConstructor
public class OrderController {
    private final OrderRepository orderRepository;
    private final CouponRepository couponRepository;

    @PostMapping("/orders")
    public OrderResponse create(@RequestBody OrderRequest request) {
        Coupon coupon = couponRepository.findByCode(request.couponCode())
            .orElseThrow();
        int discountedPrice = request.price() - coupon.discountAmount();
        Order order = orderRepository.save(new Order(request.userId(), discountedPrice));
        return new OrderResponse(order.getId(), order.getPrice());
    }
}',
    model_answer = 'The controller mixes HTTP handling, repository access, discount policy, and persistence. Move order creation into an OrderService, keep the controller responsible for request/response mapping, and put validation and transaction boundaries in the service.',
    difficulty = 'NORMAL',
    question_type = 'CODE_REVIEW'
WHERE title LIKE '%서비스 계층%'
   OR title LIKE '%Controller%';

INSERT INTO mail_questions
    (category_id, title, content, code_template, model_answer, difficulty, question_type, created_at)
SELECT c.id,
       q.title,
       q.content,
       q.code_template,
       q.model_answer,
       q.difficulty,
       q.question_type,
       CURRENT_TIMESTAMP
FROM mail_categories c
JOIN (
    SELECT 'JavaScript' AS category_name,
           'Array map 결과가 비어 보이는 버그를 고쳐보세요.' AS title,
           'The code should return only active user names. Find the bug and submit a corrected version.' AS content,
           'function activeNames(users) {
  const names = [];
  users.map(user => {
    if (user.active) {
      names.push(user.name);
    }
  });
}

console.log(activeNames([
  { name: "Ari", active: true },
  { name: "Bo", active: false }
]));' AS code_template,
           'Return the computed value. Prefer users.filter(user => user.active).map(user => user.name), or return names after the loop. The original function returns undefined.' AS model_answer,
           'EASY' AS difficulty,
           'BUG_FIX' AS question_type
    UNION ALL
    SELECT 'Spring',
           'Spring Service 트랜잭션 누락 문제를 찾아보세요.',
           'The service saves an article and then saves tags. If tag saving fails, the article still remains. Explain why and fix the transaction boundary.',
           '@Service
@RequiredArgsConstructor
public class ArticleService {
    private final ArticleRepository articleRepository;
    private final TagRepository tagRepository;

    public Long create(CreateArticleRequest request) {
        Article article = articleRepository.save(new Article(request.title(), request.content()));
        request.tags().forEach(tag -> tagRepository.save(new Tag(article, tag)));
        return article.getId();
    }
}',
           'Add @Transactional to the service method so article and tags are committed or rolled back together. Keep transaction boundaries in the service layer, not the controller.',
           'NORMAL',
           'BUG_FIX'
    UNION ALL
    SELECT 'Database',
           'N+1 쿼리가 발생하는 JPA 코드를 개선해보세요.',
           'This code prints each order member name. In production it creates too many SQL queries. Explain the cause and propose a fix.',
           'List<Order> orders = orderRepository.findAll();

for (Order order : orders) {
    System.out.println(order.getMember().getName());
}',
           'Accessing lazy member for each order can cause N+1 selects. Use fetch join, EntityGraph, batch size, or a dedicated DTO query depending on the screen requirement.',
           'NORMAL',
           'CODE_REVIEW'
) q ON q.category_name = c.name
WHERE NOT EXISTS (
    SELECT 1 FROM mail_questions existing WHERE existing.title = q.title
);

INSERT INTO projects (description, name, type, max_step, frontend_required)
SELECT 'JPA 연관관계 매핑, 조회 API, N+1 문제 해결, 컬렉션 Fetch Join 페이징 문제, 페이징 최적화, DTO Projection을 단계별 hidden test로 학습하는 Spring Boot 백엔드 프로젝트입니다.',
       'jpa-lab',
       'BE',
       7,
       FALSE
    WHERE NOT EXISTS (
    SELECT 1 FROM projects WHERE name = 'jpa-lab'
);

UPDATE projects
SET description = 'JPA 연관관계 매핑, 조회 API, N+1 문제 해결, 컬렉션 Fetch Join 페이징 문제, 페이징 최적화, DTO Projection을 단계별 hidden test로 학습하는 Spring Boot 백엔드 프로젝트입니다.',
    type = 'BE',
    max_step = 7,
    frontend_required = FALSE
WHERE name = 'jpa-lab';

INSERT INTO steps (project_id, step, content)
SELECT p.id, s.step, s.content
FROM projects p
         JOIN (
    SELECT 1 AS step,
           'Member와 Post 엔티티를 만들고 게시글 작성자 관계를 매핑합니다. Member는 id, email, nickname을 가져야 하며, Post는 id, title, content, member, createdAt, updatedAt을 가져야 합니다. Post와 Member는 N:1 관계이고 Post.member는 LAZY로딩이어야 합니다.' AS content
    UNION ALL
    SELECT 2,
           'Comment 엔티티를 추가하고 Post, Member와의 연관관계를 매핑합니다. Comment는 id, content, post, member, createdAt을 가져야 하며, Comment.post와 Comment.member는 각각 N:1 LAZY 관계여야 합니다. Post는 comments 컬렉션을 가질 수 있어야 합니다.'
    UNION ALL
    SELECT 3,
           '게시글 단건 조회 API를 구현합니다. GET /api/posts/{postId}는 게시글 id, title, content, 작성자 nickname, 댓글 목록을 DTO로 반환해야 합니다. Entity를 Controller 응답으로 직접 노출하면 안 되며, 존재하지 않는 게시글은 404 Not Found를 반환해야 합니다.'
    UNION ALL
    SELECT 4,
           '게시글 목록 조회와 페이징을 구현합니다. GET /api/posts?page={page}&size={size}는 content, page, size, totalElements를 포함한 응답을 반환해야 합니다. 목록은 createdAt 기준 최신순으로 정렬되어야 하며, 각 게시글의 postId, title, 작성자 nickname, commentCount를 제공해야 합니다. 댓글 목록 전체를 목록 API에서 응답하거나 과도하게 로딩하지 않도록 주의합니다.'
    UNION ALL
    SELECT 5,
           '게시글 목록에서 작성자 정보를 조회할 때 발생할 수 있는 N+1 문제를 해결합니다. 목록 조회 시 작성자 nickname을 함께 응답하면서도 작성자 조회 쿼리가 게시글 수만큼 반복되지 않도록 Fetch Join 또는 EntityGraph를 적용해야 합니다. 이 단계에서는 N+1 해결에 집중하며, 다음 단계에서 컬렉션 Fetch Join과 페이징 문제를 다룹니다.'
    UNION ALL
    SELECT 6,
           '컬렉션 Fetch Join과 페이징을 함께 사용할 때 발생하는 문제를 해결합니다. 목록 조회는 요청한 page size를 정확히 유지해야 하며 댓글 컬렉션을 Fetch Join으로 함께 가져와 메모리 페이징이 발생하는 구조를 피해야 합니다. 목록 API는 게시글 요약과 commentCount만 반환하고, 댓글 목록은 상세 API에서 조회되도록 책임을 분리해야 합니다.'
    UNION ALL
    SELECT 7,
           '게시글 목록 조회에 DTO Projection을 적용합니다. Entity 조회 후 변환이 아니라 필요한 필드만 select하는 조회 전용 DTO 기반 구조로 구현해야 합니다. 응답에는 postId, title, authorNickname, commentCount가 포함되어야 하며 Entity를 직접 노출하면 안 됩니다.'
) s ON TRUE
WHERE p.name = 'jpa-lab'
    ON CONFLICT ON CONSTRAINT uk_steps_project_step DO UPDATE
                                                           SET content = EXCLUDED.content;

INSERT INTO testcodes (project_id, step, method_name, description)
SELECT p.id, t.step, t.method_name, t.description
FROM projects p
         JOIN (
    SELECT 1 AS step,
           'postHasLazyManyToOneMemberAndAuditFields' AS method_name,
           'Member와 Post를 저장한 뒤 Post의 title, 작성자 nickname, createdAt, updatedAt을 조회할 수 있어야 합니다. Post.member는 @ManyToOne 관계이고 FetchType.LAZY로 매핑되어야 합니다.' AS description
    UNION ALL
    SELECT 2,
           'commentRelationsAreMappedAndPersistable',
           'Comment를 Post와 Member에 연결해 저장할 수 있어야 합니다. Post.comments는 1:N 관계로 매핑되어야 하고, Comment.post와 Comment.member는 각각 N:1 LAZY 관계여야 합니다.'
    UNION ALL
    SELECT 3,
           'getPostDetailReturnsDtoWithComments',
           'GET /api/posts/{postId} 호출 시 게시글 id, title, content, 작성자 nickname, 댓글 content, 댓글 작성자 nickname이 DTO 응답으로 반환되어야 합니다. Entity를 직접 응답으로 노출하면 안 됩니다.'
    UNION ALL
    SELECT 3,
           'missingPostReturnsNotFound',
           '존재하지 않는 게시글 id로 GET /api/posts/{postId}를 호출했을 때 404 Not Found를 반환해야 합니다.'
    UNION ALL
    SELECT 4,
           'listPostsUsesPagingLatestOrderAndCommentCount',
           '여러 게시글과 댓글을 저장한 뒤 GET /api/posts?page=0&size=2를 호출하면 content 크기, page, size, totalElements가 맞아야 합니다. 게시글은 createdAt 기준 최신순이어야 하며 commentCount가 실제 댓글 수와 일치해야 합니다.'
    UNION ALL
    SELECT 5,
           'listPostsDoesNotRepeatAuthorQueries',
           '게시글 목록 조회 시 작성자 nickname을 함께 응답하되 작성자 조회 쿼리가 게시글 수만큼 반복되면 안 됩니다. Fetch Join 또는 EntityGraph 등으로 작성자 N+1 문제를 해결했는지 Hibernate Statistics 기반으로 확인합니다.'
    UNION ALL
    SELECT 6,
           'listPostsKeepsPageSizeAndDoesNotExposeComments',
           '목록 API는 요청한 page size를 정확히 유지해야 하며 댓글 목록을 응답에 직접 포함하면 안 됩니다. 컬렉션 Fetch Join과 Pageable을 함께 사용해 메모리 페이징이 발생하는 구조를 피해야 합니다.'
    UNION ALL
    SELECT 6,
           'detailApiStillReturnsComments',
           '페이징 최적화 이후에도 상세 API GET /api/posts/{postId}는 댓글 목록을 정상 반환해야 합니다. 목록 조회와 상세 조회의 책임이 분리되어 있어야 합니다.'
    UNION ALL
    SELECT 7,
           'listPostsUsesDtoProjection',
           '게시글 목록 조회는 DTO Projection 기반으로 필요한 필드만 조회해야 합니다. 응답에는 postId, title, authorNickname, commentCount가 포함되어야 하며 Entity를 직접 노출하면 안 됩니다.'
) t ON TRUE
WHERE p.name = 'jpa-lab'
    ON CONFLICT ON CONSTRAINT uk_testcodes_project_step_method DO UPDATE
                                                                      SET description = EXCLUDED.description;


INSERT INTO projects (description, name, type, max_step, frontend_required)
SELECT 'Spring Security 기반 URL 접근 제어, 회원가입/로그인, JWT Access Token 발급, JWT 인증 필터, Refresh Token 재발급을 단계별 hidden test로 학습하는 Spring Boot 백엔드 프로젝트입니다.',
       'security-lab',
       'BE',
       4,
       FALSE
    WHERE NOT EXISTS (
    SELECT 1 FROM projects WHERE name = 'security-lab'
);

UPDATE projects
SET description = 'Spring Security 기반 URL 접근 제어, 회원가입/로그인, JWT Access Token 발급, JWT 인증 필터, Refresh Token 재발급을 단계별 hidden test로 학습하는 Spring Boot 백엔드 프로젝트입니다.',
    type = 'BE',
    max_step = 4,
    frontend_required = FALSE
WHERE name = 'security-lab';

INSERT INTO steps (project_id, step, content)
SELECT p.id, s.step, s.content
FROM projects p
         JOIN (
    SELECT 1 AS step,
           'Spring Security 설정을 통해 URL별 접근 권한을 분리합니다. GET /api/public/ping은 인증 없이 접근 가능해야 하고, GET /api/private/ping은 인증이 필요해야 하며, GET /api/admin/ping은 관리자 권한이 필요해야 합니다. 이 단계에서는 회원가입, 로그인, JWT 발급은 구현하지 않습니다.' AS content
    UNION ALL
    SELECT 2,
           '회원가입과 로그인을 구현하고 로그인 성공 시 JWT Access Token을 발급합니다. POST /api/auth/signup은 User를 저장해야 하며 비밀번호는 PasswordEncoder로 암호화해야 합니다. POST /api/auth/login은 올바른 이메일과 비밀번호로 로그인하면 accessToken을 반환해야 하고, 잘못된 비밀번호는 401 Unauthorized를 반환해야 합니다. Access Token에는 사용자를 식별할 수 있는 subject 또는 userId와 type=ACCESS claim이 포함되어야 합니다.'
    UNION ALL
    SELECT 3,
           'JWT 인증 필터를 구현하고 현재 인증 사용자 조회 API를 추가합니다. Authorization 헤더의 Bearer Access Token을 읽어 유효성을 검증한 뒤 SecurityContext에 인증 정보를 저장해야 합니다. GET /api/me는 인증된 사용자만 접근 가능해야 하며 현재 사용자의 email과 nickname을 반환해야 합니다. 토큰 없음, 잘못된 토큰, 만료된 토큰은 401 Unauthorized를 반환해야 합니다.'
    UNION ALL
    SELECT 4,
           'Refresh Token 발급과 Access Token 재발급을 구현합니다. 로그인 성공 시 accessToken과 refreshToken을 모두 반환해야 하며 Access Token에는 type=ACCESS, Refresh Token에는 type=REFRESH claim이 포함되어야 합니다. POST /api/auth/refresh는 유효한 Refresh Token으로 새 Access Token을 발급해야 합니다. Access Token으로 refresh API를 호출하거나 Refresh Token으로 보호 API를 호출하면 실패해야 합니다.'
) s ON TRUE
WHERE p.name = 'security-lab'
    ON CONFLICT ON CONSTRAINT uk_steps_project_step DO UPDATE
                                                           SET content = EXCLUDED.content;

INSERT INTO testcodes (project_id, step, method_name, description)
SELECT p.id, t.step, t.method_name, t.description
FROM projects p
         JOIN (
    SELECT 1 AS step,
           'publicPingIsOpenWithoutAuthentication' AS method_name,
           '토큰 없이 GET /api/public/ping을 호출했을 때 200 OK와 public pong 응답이 반환되어야 합니다.' AS description
    UNION ALL
    SELECT 1,
           'privatePingRequiresAuthentication',
           '토큰 없이 GET /api/private/ping을 호출했을 때 401 Unauthorized가 반환되어야 합니다.'
    UNION ALL
    SELECT 1,
           'adminPingRequiresAdminRole',
           'GET /api/admin/ping은 관리자 권한이 필요해야 합니다. 인증 없이 호출하면 4xx 응답이어야 하며 ADMIN 권한 사용자는 접근할 수 있어야 합니다.'
    UNION ALL
    SELECT 2,
           'signupStoresUserWithEncodedPassword',
           'POST /api/auth/signup 호출 시 User가 저장되어야 하며 비밀번호는 평문이 아니라 PasswordEncoder와 매칭되는 해시로 저장되어야 합니다.'
    UNION ALL
    SELECT 2,
           'loginReturnsAccessTokenWithSubjectAndType',
           '회원가입 후 POST /api/auth/login을 호출하면 accessToken이 반환되어야 합니다. Access Token은 테스트용 고정 Secret으로 파싱 가능해야 하며 subject와 type=ACCESS claim을 포함해야 합니다.'
    UNION ALL
    SELECT 2,
           'loginWithWrongPasswordReturnsUnauthorized',
           '잘못된 비밀번호로 POST /api/auth/login을 호출하면 401 Unauthorized가 반환되어야 합니다.'
    UNION ALL
    SELECT 3,
           'meRequiresToken',
           '토큰 없이 GET /api/me를 호출했을 때 401 Unauthorized가 반환되어야 합니다.'
    UNION ALL
    SELECT 3,
           'meReturnsAuthenticatedUser',
           '유효한 Access Token으로 GET /api/me를 호출하면 200 OK와 현재 로그인 사용자의 email, nickname이 반환되어야 합니다.'
    UNION ALL
    SELECT 3,
           'meRejectsInvalidToken',
           '잘못된 JWT로 GET /api/me를 호출하면 401 Unauthorized가 반환되어야 합니다.'
    UNION ALL
    SELECT 4,
           'loginReturnsAccessTokenAndRefreshToken',
           '로그인 성공 시 응답에 accessToken과 refreshToken이 모두 포함되어야 합니다.'
    UNION ALL
    SELECT 4,
           'refreshTokenIssuesNewAccessToken',
           '유효한 Refresh Token으로 POST /api/auth/refresh를 호출하면 새로운 accessToken이 반환되어야 합니다.'
    UNION ALL
    SELECT 4,
           'accessTokenCannotRefresh',
           'Access Token을 refreshToken 값으로 사용해 POST /api/auth/refresh를 호출하면 401 Unauthorized가 반환되어야 합니다.'
    UNION ALL
    SELECT 4,
           'refreshTokenCannotAccessMe',
           'Refresh Token으로 GET /api/me 같은 보호 API를 호출하면 401 Unauthorized가 반환되어야 합니다.'
    UNION ALL
    SELECT 4,
           'invalidRefreshTokenReturnsUnauthorized',
           '잘못된 Refresh Token으로 POST /api/auth/refresh를 호출하면 401 Unauthorized가 반환되어야 합니다.'
) t ON TRUE
WHERE p.name = 'security-lab'
    ON CONFLICT ON CONSTRAINT uk_testcodes_project_step_method DO UPDATE
                                                                      SET description = EXCLUDED.description;


INSERT INTO projects (description, name, type, max_step, frontend_required)
SELECT '상품 CRUD, 이미지 업로드, 상품 대표 이미지 연결, 이미지 검증/조회 URL 제공, 이미지 수정/삭제 및 파일 정리를 단계별 hidden test로 학습하는 Spring Boot 쇼핑몰 백엔드 프로젝트입니다.',
       'market',
       'BE',
       5,
       TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM projects WHERE name = 'market'
);

UPDATE projects
SET description = '상품 CRUD, 이미지 업로드, 상품 대표 이미지 연결, 이미지 검증/조회 URL 제공, 이미지 수정/삭제 및 파일 정리를 단계별 hidden test로 학습하는 Spring Boot 쇼핑몰 백엔드 프로젝트입니다.',
    type = 'BE',
    max_step = 5,
    frontend_required = TRUE
WHERE name = 'market';

INSERT INTO steps (project_id, step, content)
SELECT p.id, s.step, s.content
FROM projects p
         JOIN (
    SELECT 1 AS step,
           '상품 CRUD API를 구현합니다. Product는 id, name, price, description 필드를 가지며, POST /api/products로 상품을 등록하고, GET /api/products/{productId}로 단건 조회하고, GET /api/products로 목록 조
           회하고, PUT /api/products/{productId}로 수정하고, DELETE /api/products/{productId}로 삭제할 수 있어야 합니다. 요청값 검증과 존재하지 않는 상품 조회 시 404 응답을 처리해야 합니다.' AS content
    UNION ALL
    SELECT 2,
           '상품과 연결하지 않는 단일 이미지 업로드 API를 구현합니다. POST /api/images는 multipart/form-data의 image 파일을 받아 UUID 기반 저장 파일명으로 로컬 upload-dir에 저장해야 합니다. 응답에는
           originalFileName, storedFileName, fileSize, contentType이 포함되어야 하며 빈 파일 업로드 시 400 Bad Request를 반환해야 합니다.'
    UNION ALL
    SELECT 3,
           '상품 등록 시 대표 이미지를 함께 저장하는 API를 구현합니다. POST /api/products/with-image는 product JSON part와 image MultipartFile part를 함께 받아 Product를 DB에 저장하고 이미지 파일은 로컬에 저
           장하며 ProductImage 메타데이터를 DB에 저장해야 합니다. 상품 응답과 GET /api/products/{productId} 응답에는 /images/{storedFileName} 형식의 imageUrl이 포함되어야 합니다.'
    UNION ALL
    SELECT 4,
           '이미지 업로드 검증과 이미지 조회 URL을 구현합니다. jpg, jpeg, png, webp 확장자와 image/jpeg, image/png, image/webp Content-Type만 허용하고 최대 파일 크기는 5MB로 제한해야 합니다. 검증 실패 시 400
           Bad Request를 반환해야 하며, WebMvcConfigurer를 이용해 /images/** 요청이 upload-dir의 파일을 응답하도록 정적 리소스 매핑을 구현해야 합니다.'
    UNION ALL
    SELECT 5,
           '상품 대표 이미지 수정과 상품 삭제 시 이미지 파일 정리를 구현합니다. PUT /api/products/{productId}/image는 새 대표 이미지를 검증하고 저장한 뒤 기존 이미지 파일을 삭제하고 ProductImage 메타데이터를
           갱신해야 합니다. DELETE /api/products/{productId}는 Product와 ProductImage를 삭제하고 실제 이미지 파일도 함께 삭제해야 합니다.'
) s ON TRUE
WHERE p.name = 'market'
ON CONFLICT ON CONSTRAINT uk_steps_project_step DO UPDATE
    SET content = EXCLUDED.content;

INSERT INTO testcodes (project_id, step, method_name, description)
SELECT p.id, t.step, t.method_name, t.description
FROM projects p
         JOIN (
    SELECT 1 AS step,
           'createProductAndReadProduct' AS method_name,
           'POST /api/products로 상품을 등록하면 201 Created와 상품 응답이 반환되어야 하며, 등록된 상품은 GET /api/products/{productId}로 조회할 수 있어야 합니다.' AS description
    UNION ALL
    SELECT 1,
           'readProductsReturnsProductList',
           '여러 상품을 등록한 뒤 GET /api/products를 호출하면 상품 목록이 배열로 반환되어야 합니다.'
    UNION ALL
    SELECT 1,
           'updateProduct',
           'PUT /api/products/{productId}로 상품명, 가격, 설명을 수정하면 수정된 상품 응답이 반환되어야 합니다.'
    UNION ALL
    SELECT 1,
           'deleteProduct',
           'DELETE /api/products/{productId} 호출 시 204 No Content가 반환되고 이후 해당 상품 조회는 404를 반환해야 합니다.'
    UNION ALL
    SELECT 1,
           'createInvalidProductFails',
           '상품명, 가격, 설명 검증에 실패하는 요청은 400 Bad Request를 반환해야 합니다.'

    UNION ALL
    SELECT 2,
           'uploadImage',
           'POST /api/images로 이미지 파일을 업로드하면 originalFileName, storedFileName, fileSize, contentType이 반환되고 실제 파일이 upload-dir에 저장되어야 합니다.'
    UNION ALL
    SELECT 2,
           'uploadEmptyImageFails',
           '빈 이미지 파일을 POST /api/images로 업로드하면 400 Bad Request와 오류 메시지가 반환되어야 합니다.'

    UNION ALL
    SELECT 3,
           'createProductWithImageAndReadProduct',
           'POST /api/products/with-image로 상품 JSON과 이미지 파일을 함께 등록하면 Product와 ProductImage가 DB에 저장되고 이미지 파일도 실제 디렉터리에 저장되어야 하며, 상품 조회 응답에 imageUrl이 포함되어
           야 합니다.'
    UNION ALL
    SELECT 3,
           'readMissingProductFails',
           '존재하지 않는 상품을 GET /api/products/{productId}로 조회하면 404 Not Found가 반환되어야 합니다.'
    UNION ALL
    SELECT 3,
           'uploadEmptyImageFails',
           '상품과 이미지 함께 등록 시 빈 이미지 파일을 업로드하면 400 Bad Request가 반환되어야 합니다.'
    UNION ALL
    SELECT 3,
           'invalidProductFails',
           '상품과 이미지 함께 등록 시 상품명 또는 가격 검증에 실패하면 400 Bad Request가 반환되어야 합니다.'

    UNION ALL
    SELECT 4,
           'uploadPngImageAndReadImageUrl',
           'png 이미지로 상품을 등록하면 성공해야 하며 응답 imageUrl은 /images/로 시작하고 GET /images/{storedFileName} 요청 시 200 OK가 반환되어야 합니다.'
    UNION ALL
    SELECT 4,
           'uploadJpgImage',
           'jpg 이미지 업로드가 성공해야 합니다.'
    UNION ALL
    SELECT 4,
           'uploadJpegImage',
           'jpeg 이미지 업로드가 성공해야 합니다.'
    UNION ALL
    SELECT 4,
           'uploadWebpImage',
           'webp 이미지 업로드가 성공해야 합니다.'
    UNION ALL
    SELECT 4,
           'uploadTxtFileFails',
           '허용되지 않는 txt 확장자 파일 업로드 시 400 Bad Request가 반환되어야 합니다.'
    UNION ALL
    SELECT 4,
           'uploadWrongContentTypeFails',
           '허용되지 않는 Content-Type 파일 업로드 시 400 Bad Request가 반환되어야 합니다.'
    UNION ALL
    SELECT 4,
           'uploadTooLargeImageFails',
           '5MB를 초과하는 이미지 파일 업로드 시 400 Bad Request가 반환되어야 합니다.'
    UNION ALL
    SELECT 4,
           'uploadEmptyImageFails',
           '빈 이미지 파일 업로드 시 400 Bad Request가 반환되어야 합니다.'

    UNION ALL
    SELECT 5,
           'updateProductImage',
           'PUT /api/products/{productId}/image로 대표 이미지를 수정하면 새 이미지 파일이 저장되고 기존 이미지 파일은 삭제되며 ProductImage의 storedFileName과 imageUrl이 변경되어야 합니다.'
    UNION ALL
    SELECT 5,
           'updateMissingProductImageFails',
           '존재하지 않는 상품의 대표 이미지를 수정하려 하면 404 Not Found가 반환되어야 합니다.'
    UNION ALL
    SELECT 5,
           'updateInvalidImageFails',
           '허용되지 않는 확장자의 이미지로 대표 이미지 수정을 요청하면 400 Bad Request가 반환되어야 합니다.'
    UNION ALL
    SELECT 5,
           'deleteProductDeletesImageDataAndFile',
           'DELETE /api/products/{productId} 호출 시 상품과 ProductImage 데이터가 삭제되고 실제 이미지 파일도 삭제되어야 합니다.'
    UNION ALL
    SELECT 5,
           'deleteMissingProductFails',
           '존재하지 않는 상품을 삭제하려 하면 404 Not Found가 반환되어야 합니다.'
) t ON TRUE
WHERE p.name = 'market'
ON CONFLICT ON CONSTRAINT uk_testcodes_project_step_method DO UPDATE
    SET description = EXCLUDED.description;