package com.example.taskai.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.taskai.knowledge.entity.AiKnowledgeDocument;
import com.example.taskai.knowledge.vo.KnowledgeDocumentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AiKnowledgeDocumentMapper extends BaseMapper<AiKnowledgeDocument> {

    @Select("""
        <script>
        SELECT id, user_id AS userId, source_type AS sourceType, source_id AS sourceId,
               title, content, indexed, created_at AS createdAt, updated_at AS updatedAt
        FROM ai_knowledge_document
        <where>
            <if test="sourceType != null and sourceType != ''">
                AND source_type = #{sourceType}
            </if>
            <if test="keyword != null and keyword != ''">
                AND (title LIKE CONCAT('%', #{keyword}, '%')
                     OR content LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="indexed != null">
                AND indexed = #{indexed}
            </if>
        </where>
        ORDER BY id DESC
        </script>
        """)
    IPage<KnowledgeDocumentVO> selectDocumentPage(IPage<KnowledgeDocumentVO> page,
                                                   @Param("sourceType") String sourceType,
                                                   @Param("keyword") String keyword,
                                                   @Param("indexed") Integer indexed);
}
