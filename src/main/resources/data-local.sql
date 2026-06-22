INSERT INTO user_roles (id, name)
VALUES
    (1, 'USER'),
    (2, 'ADMIN')
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name;

INSERT INTO projects (description, name, type, max_step)
SELECT '게시판 CRUD, 댓글, 검증/예외 처리를 단계별 hidden test로 구현하는 Spring Boot 백엔드 프로젝트입니다.', 'mmcafe', 'BE', 5
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
