USE mindspace;

-- 测试账号：test@mindspace.com / 123456（BCrypt）
INSERT INTO sys_user (username, email, password)
VALUES ('test', 'test@mindspace.com', '$2a$10$4M2zkYuNiunD/yxYxapR/eLcxACDdN.rU7KrWs8VlpQoWQzS.xxYq');

-- 默认知识库
INSERT INTO knowledge_base (user_id, name, description)
VALUES (1, '我的知识库', '注册时自动创建的默认知识库');

-- 默认「未分类」文件夹
INSERT INTO folder (user_id, knowledge_base_id, parent_id, name, is_default)
VALUES (1, 1, 0, '未分类', 1);
