package com.example.taskai.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.taskai.member.entity.MemberProfile;
import com.example.taskai.member.vo.MemberProfileListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MemberProfileMapper extends BaseMapper<MemberProfile> {

    @Select("""
        <script>
        SELECT u.id AS userId, u.username, u.real_name AS realName, u.email,
               IFNULL(mp.current_workload, 0) AS currentWorkload,
               IFNULL(mp.completed_task_count, 0) AS completedTaskCount,
               IFNULL(mp.overdue_task_count, 0) AS overdueTaskCount,
               IFNULL(mp.task_completion_rate, 0) AS taskCompletionRate,
               IFNULL(mp.overdue_rate, 0) AS overdueRate,
               NULL AS skills
        FROM sys_user u
        INNER JOIN sys_user_role ur ON ur.user_id = u.id
        INNER JOIN sys_role r ON r.id = ur.role_id AND r.role_code = 'MEMBER'
        LEFT JOIN member_profile mp ON mp.user_id = u.id
        <where>
            <if test="keyword != null and keyword != ''">
                AND (u.username LIKE CONCAT('%', #{keyword}, '%')
                     OR u.real_name LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="skillId != null">
                AND EXISTS (SELECT 1 FROM user_skill us WHERE us.user_id = u.id AND us.skill_id = #{skillId})
            </if>
        </where>
        ORDER BY u.id DESC
        </script>
        """)
    IPage<MemberProfileListItemVO> selectMemberProfiles(IPage<MemberProfileListItemVO> page,
                                                         @Param("keyword") String keyword,
                                                         @Param("skillId") Long skillId);
}
