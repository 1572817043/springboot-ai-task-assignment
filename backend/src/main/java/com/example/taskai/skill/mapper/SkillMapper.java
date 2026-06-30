package com.example.taskai.skill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.taskai.skill.entity.Skill;
import com.example.taskai.skill.vo.SkillListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SkillMapper extends BaseMapper<Skill> {

    @Select("""
        <script>
        SELECT id, skill_name AS skillName, category, description, created_at AS createdAt, updated_at AS updatedAt
        FROM skill
        <where>
            <if test="keyword != null and keyword != ''">
                AND (skill_name LIKE CONCAT('%', #{keyword}, '%')
                     OR category LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="category != null and category != ''">
                AND category = #{category}
            </if>
        </where>
        ORDER BY id DESC
        </script>
        """)
    IPage<SkillListItemVO> selectSkillPage(IPage<SkillListItemVO> page,
                                            @Param("keyword") String keyword,
                                            @Param("category") String category);
}
