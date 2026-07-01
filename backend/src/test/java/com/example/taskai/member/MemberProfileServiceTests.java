package com.example.taskai.member;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.taskai.member.service.MemberProfileService;
import com.example.taskai.member.vo.MemberProfileListItemVO;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MemberProfileServiceTests {

    @Autowired
    private MemberProfileService memberProfileService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS user_skill");
        jdbcTemplate.execute("DROP TABLE IF EXISTS member_profile");
        jdbcTemplate.execute("DROP TABLE IF EXISTS sys_user_role");
        jdbcTemplate.execute("DROP TABLE IF EXISTS skill");
        jdbcTemplate.execute("DROP TABLE IF EXISTS sys_role");
        jdbcTemplate.execute("DROP TABLE IF EXISTS sys_user");

        jdbcTemplate.execute("""
            CREATE TABLE sys_user (
              id BIGINT PRIMARY KEY,
              username VARCHAR(50) NOT NULL,
              password_hash VARCHAR(255),
              real_name VARCHAR(50) NOT NULL,
              email VARCHAR(100),
              phone VARCHAR(30),
              avatar_url VARCHAR(500),
              status VARCHAR(20),
              created_at DATETIME,
              updated_at DATETIME
            )
            """);
        jdbcTemplate.execute("""
            CREATE TABLE sys_role (
              id BIGINT PRIMARY KEY,
              role_code VARCHAR(50) NOT NULL,
              role_name VARCHAR(50) NOT NULL,
              description VARCHAR(255)
            )
            """);
        jdbcTemplate.execute("""
            CREATE TABLE sys_user_role (
              id BIGINT PRIMARY KEY,
              user_id BIGINT NOT NULL,
              role_id BIGINT NOT NULL
            )
            """);
        jdbcTemplate.execute("""
            CREATE TABLE member_profile (
              id BIGINT PRIMARY KEY,
              user_id BIGINT NOT NULL,
              resume_text TEXT,
              experience_summary TEXT,
              current_workload INT,
              completed_task_count INT,
              overdue_task_count INT,
              task_completion_rate DECIMAL(5,2),
              overdue_rate DECIMAL(5,2),
              created_at DATETIME,
              updated_at DATETIME
            )
            """);
        jdbcTemplate.execute("""
            CREATE TABLE skill (
              id BIGINT PRIMARY KEY,
              skill_name VARCHAR(80) NOT NULL,
              category VARCHAR(80),
              description VARCHAR(255),
              created_at DATETIME,
              updated_at DATETIME
            )
            """);
        jdbcTemplate.execute("""
            CREATE TABLE user_skill (
              id BIGINT PRIMARY KEY,
              user_id BIGINT NOT NULL,
              skill_id BIGINT NOT NULL,
              level INT,
              years DECIMAL(4,1),
              description VARCHAR(255),
              created_at DATETIME,
              updated_at DATETIME
            )
            """);

        jdbcTemplate.update("""
            INSERT INTO sys_user (id, username, password_hash, real_name, email, status)
            VALUES (3, 'member', 'member123', '成员用户', 'member@example.com', 'ENABLED')
            """);
        jdbcTemplate.update("""
            INSERT INTO sys_role (id, role_code, role_name)
            VALUES (3, 'MEMBER', '成员')
            """);
        jdbcTemplate.update("""
            INSERT INTO sys_user_role (id, user_id, role_id)
            VALUES (1, 3, 3)
            """);
        jdbcTemplate.update("""
            INSERT INTO member_profile (
              id, user_id, current_workload, completed_task_count,
              overdue_task_count, task_completion_rate, overdue_rate
            )
            VALUES (1, 3, 2, 5, 1, 0.80, 0.20)
            """);
        jdbcTemplate.update("""
            INSERT INTO skill (id, skill_name, category)
            VALUES (1, 'Spring Boot', '后端')
            """);
        jdbcTemplate.update("""
            INSERT INTO user_skill (id, user_id, skill_id, level, years)
            VALUES (1, 3, 1, 4, 2.5)
            """);
    }

    @Test
    void listProfilesReturnsMemberRowsWithSkillNames() {
        IPage<MemberProfileListItemVO> page = memberProfileService.listProfiles(null, null, 1, 10);

        assertEquals(1, page.getTotal());
        assertEquals(1, page.getPages());
        assertEquals(1, page.getRecords().size());
        MemberProfileListItemVO item = page.getRecords().getFirst();
        assertEquals(3L, item.userId());
        assertEquals("成员用户", item.realName());
        assertEquals(new BigDecimal("0.80"), item.taskCompletionRate());
        assertEquals(1, item.skills().size());
        assertEquals("Spring Boot", item.skills().getFirst());
    }

    @Test
    void updateProfileSavesPhoneOnUserRecord() {
        memberProfileService.updateProfile(3L, new com.example.taskai.member.dto.MemberProfileUpdateRequest(
            "13900000000",
            "新简历",
            "新经验",
            null,
            null,
            null,
            null,
            null
        ));

        String phone = jdbcTemplate.queryForObject(
            "SELECT phone FROM sys_user WHERE id = 3",
            String.class
        );
        assertEquals("13900000000", phone);
    }
}
