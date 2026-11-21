USE mindspace;

-- MySQL 8.4 的 ngram 解析器适合中文全文检索。
-- 只需要执行一次；重复执行会提示索引已存在。
ALTER TABLE document
    ADD FULLTEXT INDEX ft_document_search (original_name, extracted_text, summary)
    WITH PARSER ngram;
