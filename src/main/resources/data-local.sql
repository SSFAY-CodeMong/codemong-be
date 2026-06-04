INSERT INTO user_roles (id, name)
SELECT 1, 'USER'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM user_roles)
UNION ALL
SELECT 2, 'ADMIN'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM user_roles);

INSERT INTO projects (description, name, type)
SELECT '게시판', 'mmcafe', 'BE'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM projects
);