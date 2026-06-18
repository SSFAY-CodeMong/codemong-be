INSERT INTO user_roles (id, name)
SELECT 1, 'USER'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM user_roles)
UNION ALL
SELECT 2, 'ADMIN'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM user_roles);

INSERT INTO projects (description, name, type)
SELECT '게시판 CRUD와 단계별 테스트를 통해 REST API 구현을 연습합니다.', 'mmcafe', 'BE'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM projects
);
