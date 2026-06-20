INSERT INTO user_roles (id, name)
SELECT 1, 'USER'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM user_roles)
UNION ALL
SELECT 2, 'ADMIN'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM user_roles);

INSERT INTO projects (description, name, type, max_step)
SELECT '게시판 CRUD를 단계별 테스트를 통해 REST API로 구현합니다.', 'mmcafe', 'BE', 5
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM projects
);

UPDATE projects
SET max_step = 5
WHERE max_step IS NULL;

INSERT INTO steps (project_id, step, content)
SELECT p.id, s.step, s.content
FROM projects p
JOIN (
    SELECT 1 AS step, 'Step 1 requirements for the mmcafe project.' AS content
    UNION ALL
    SELECT 2, 'Step 2 requirements for the mmcafe project.'
    UNION ALL
    SELECT 3, 'Step 3 requirements for the mmcafe project.'
    UNION ALL
    SELECT 4, 'Step 4 requirements for the mmcafe project.'
    UNION ALL
    SELECT 5, 'Step 5 requirements for the mmcafe project.'
) s ON TRUE
WHERE p.name = 'mmcafe'
  AND NOT EXISTS (
      SELECT 1
      FROM steps existing
      WHERE existing.project_id = p.id
        AND existing.step = s.step
  );
