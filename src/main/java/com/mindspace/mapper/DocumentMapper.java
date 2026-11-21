package com.mindspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mindspace.dto.DocumentSearchDTO;
import com.mindspace.entity.Document;
import com.mindspace.vo.DocumentSearchVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文档表 Mapper。
 *
 * 这里手写两个全文检索方法：
 * 1. countSearch：查命中总数，给分页算 total；
 * 2. search：查当前页数据，带正文片段。
 *
 * 注意：@Select 里如果出现 <script>，MyBatis 会按 XML 解析。
 * 因此 SQL 字符串里只能放 SQL 和动态标签，
 * 不能把带 <、>、& 的注释写进字符串，否则会 XML 解析失败。
 */
public interface DocumentMapper extends BaseMapper<Document> {

    /*
     * 公共 FROM + WHERE。
     *
     * 这里用 Java 文本块写多行 SQL；它会被拼进下面的 <script> 里。
     * 为了不被 XML 解析器误判：
     * 1. 字符串里不写 SQL 注释；
     * 2. <if> 是 MyBatis 动态标签，不是 SQL 注释；
     * 3. 比较运算符写成 &gt;= 和 &lt;=。
     *
     * likeFallback：
     * MySQL ngram 对个别英文词（例如 Java）可能不命中，
     * 所以 Service 先走 MATCH；如果总数为 0 且关键词含英文字母，
     * 再用 LOCATE 做一次精确包含兜底。
     */
    String SEARCH_FROM_WHERE = """
            FROM document d
            WHERE d.deleted = 0
              AND d.user_id = #{userId}
              AND d.knowledge_base_id = #{query.knowledgeBaseId}
              <if test="query.folderId != null and query.folderId != 0">
                  AND d.folder_id = #{query.folderId}
              </if>
              <if test="query.fileType != null and query.fileType != ''">
                  AND d.file_type = #{query.fileType}
              </if>
              <if test="query.processStatus != null and query.processStatus != ''">
                  AND d.process_status = #{query.processStatus}
              </if>
              <if test="query.aiCategory != null and query.aiCategory != ''">
                  AND d.ai_category = #{query.aiCategory}
              </if>
              <if test="query.startTime != null">
                  AND d.create_time &gt;= #{query.startTime}
              </if>
              <if test="query.endTime != null">
                  AND d.create_time &lt;= #{query.endTime}
              </if>
              <if test="query.keyword != null and query.keyword != ''">
                  <choose>
                      <when test="likeFallback">
                          AND (
                              LOCATE(#{query.keyword}, d.original_name) > 0
                              OR LOCATE(#{query.keyword}, d.extracted_text) > 0
                          )
                      </when>
                      <otherwise>
                          AND MATCH(d.original_name, d.extracted_text, d.summary)
                              AGAINST (#{query.keyword} IN NATURAL LANGUAGE MODE)
                      </otherwise>
                  </choose>
              </if>
            """;

    /**
     * 查命中总数。
     */
    @Select("<script>SELECT COUNT(*) " + SEARCH_FROM_WHERE + "</script>")
    long countSearch(@Param("userId") Long userId,
                     @Param("query") DocumentSearchDTO query,
                     @Param("likeFallback") boolean likeFallback);

    /**
     * 查当前页数据。
     *
     * 1. SELECT 只取列表需要的字段，不返回整篇 extracted_text；
     * 2. CASE 生成 textSnippet：关键词附近 220 个字符；
     * 3. WHERE 复用 SEARCH_FROM_WHERE；
     * 4. 有关键词且 sort=relevance 时按 MATCH 分数排序，否则按时间；
     * 5. 手动 LIMIT 分页，避免分页插件解析 MATCH ... AGAINST。
     */
    @Select("<script>"
            + "SELECT d.id, d.knowledge_base_id, d.folder_id, d.original_name, "
            + "d.file_type, d.file_size, d.process_status, d.ai_category, d.create_time, "
            + "CASE "
            + "  WHEN #{query.keyword} IS NULL OR #{query.keyword} = '' THEN LEFT(d.extracted_text, 200) "
            + "  WHEN LOCATE(#{query.keyword}, d.extracted_text) > 0 THEN "
            + "    SUBSTRING(d.extracted_text, "
            + "      GREATEST(1, LOCATE(#{query.keyword}, d.extracted_text) - 80), 220) "
            + "  ELSE LEFT(d.extracted_text, 200) "
            + "END AS text_snippet "
            + SEARCH_FROM_WHERE
            + "<choose>"
            + "  <when test=\"query.keyword != null and query.keyword != '' "
            + "      and query.sort == 'relevance' and !likeFallback\">"
            + "    ORDER BY MATCH(d.original_name, d.extracted_text, d.summary) "
            + "      AGAINST (#{query.keyword} IN NATURAL LANGUAGE MODE) DESC, "
            + "      d.create_time DESC, d.id DESC"
            + "  </when>"
            + "  <otherwise>"
            + "    ORDER BY d.create_time DESC, d.id DESC"
            + "  </otherwise>"
            + "</choose>"
            + "LIMIT #{offset}, #{size}"
            + "</script>")
    List<DocumentSearchVO> search(@Param("userId") Long userId,
                                  @Param("query") DocumentSearchDTO query,
                                  @Param("offset") long offset,
                                  @Param("size") long size,
                                  @Param("likeFallback") boolean likeFallback);
}
