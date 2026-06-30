package com.example.taskai.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.taskai.ai.entity.AiRecommendationBatch;
import com.example.taskai.ai.entity.AiRecommendationCandidate;
import com.example.taskai.ai.mapper.AiRecommendationBatchMapper;
import com.example.taskai.ai.mapper.AiRecommendationCandidateMapper;
import com.example.taskai.ai.service.AiAssignmentService;
import com.example.taskai.ai.vo.AiRecommendationResponse;
import com.example.taskai.common.exception.BusinessException;
import com.example.taskai.member.entity.MemberProfile;
import com.example.taskai.member.mapper.MemberProfileMapper;
import com.example.taskai.project.entity.Project;
import com.example.taskai.project.entity.ProjectMember;
import com.example.taskai.project.mapper.ProjectMapper;
import com.example.taskai.project.mapper.ProjectMemberMapper;
import com.example.taskai.skill.entity.TaskRequiredSkill;
import com.example.taskai.skill.mapper.SkillMapper;
import com.example.taskai.skill.mapper.TaskRequiredSkillMapper;
import com.example.taskai.skill.mapper.UserSkillMapper;
import com.example.taskai.task.entity.Task;
import com.example.taskai.task.mapper.TaskMapper;
import com.example.taskai.task.mapper.TaskResultMapper;
import com.example.taskai.task.service.TaskService;
import com.example.taskai.user.entity.SysUser;
import com.example.taskai.user.mapper.SysUserMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiAssignmentServiceTests {

    @Mock
    private AiRecommendationBatchMapper batchMapper;
    @Mock
    private AiRecommendationCandidateMapper candidateMapper;
    @Mock
    private TaskMapper taskMapper;
    @Mock
    private TaskResultMapper taskResultMapper;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private ProjectMemberMapper projectMemberMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private UserSkillMapper userSkillMapper;
    @Mock
    private TaskRequiredSkillMapper taskRequiredSkillMapper;
    @Mock
    private SkillMapper skillMapper;
    @Mock
    private MemberProfileMapper memberProfileMapper;
    @Mock
    private TaskService taskService;

    @InjectMocks
    private AiAssignmentService aiAssignmentService;

    private Task mockTask() {
        Task task = new Task();
        task.setId(1L);
        task.setProjectId(1L);
        task.setTitle("开发登录功能");
        return task;
    }

    private Project mockProject() {
        Project project = new Project();
        project.setId(1L);
        project.setManagerId(2L);
        return project;
    }

    private void stubRecommendData() {
        ProjectMember pm = new ProjectMember();
        pm.setUserId(3L);
        pm.setProjectId(1L);

        SysUser user = new SysUser();
        user.setId(3L);
        user.setRealName("成员用户");
        user.setStatus("ENABLED");

        when(taskMapper.selectById(1L)).thenReturn(mockTask());
        when(projectMemberMapper.selectList(any())).thenReturn(List.of(pm));
        when(sysUserMapper.selectList(any())).thenReturn(List.of(user));
        when(taskRequiredSkillMapper.selectList(any())).thenReturn(List.of());
        when(taskResultMapper.selectCount(any())).thenReturn(0L);
        when(memberProfileMapper.selectOne(any())).thenReturn(null);
    }

    @Test
    void adminCanRecommend() {
        stubRecommendData();

        AiRecommendationResponse response = aiAssignmentService.recommend(1L, "ADMIN", 1L);

        assertNotNull(response);
        assertEquals(1L, response.taskId());
        assertEquals(1, response.candidates().size());
        verify(batchMapper).insert(any(AiRecommendationBatch.class));
    }

    @Test
    void projectManagerCanRecommend() {
        stubRecommendData();
        when(projectMapper.selectById(1L)).thenReturn(mockProject());

        AiRecommendationResponse response = aiAssignmentService.recommend(1L, "MANAGER", 2L);

        assertNotNull(response);
        assertEquals(1L, response.taskId());
    }

    @Test
    void nonProjectManagerCannotRecommend() {
        when(taskMapper.selectById(1L)).thenReturn(mockTask());
        when(projectMapper.selectById(1L)).thenReturn(mockProject());

        BusinessException ex = assertThrows(BusinessException.class,
            () -> aiAssignmentService.recommend(1L, "MANAGER", 99L));
        assertEquals(403, ex.getCode());
    }

    @Test
    void memberCannotRecommend() {
        when(taskMapper.selectById(1L)).thenReturn(mockTask());

        BusinessException ex = assertThrows(BusinessException.class,
            () -> aiAssignmentService.recommend(1L, "MEMBER", 3L));
        assertEquals(403, ex.getCode());
    }

    @Test
    void adminCanAccept() {
        AiRecommendationCandidate candidate = new AiRecommendationCandidate();
        candidate.setId(1L);
        candidate.setBatchId(1L);
        candidate.setCandidateUserId(3L);

        AiRecommendationBatch batch = new AiRecommendationBatch();
        batch.setId(1L);
        batch.setTaskId(1L);

        when(candidateMapper.selectById(1L)).thenReturn(candidate);
        when(batchMapper.selectById(1L)).thenReturn(batch);
        when(taskMapper.selectById(1L)).thenReturn(mockTask());
        when(candidateMapper.selectList(any())).thenReturn(List.of(candidate));

        aiAssignmentService.acceptCandidate(1L, "ADMIN", 1L);

        verify(taskService).assignTaskByAi(1L, 3L, 1L);
    }

    @Test
    void projectManagerCanAccept() {
        AiRecommendationCandidate candidate = new AiRecommendationCandidate();
        candidate.setId(1L);
        candidate.setBatchId(1L);
        candidate.setCandidateUserId(3L);

        AiRecommendationBatch batch = new AiRecommendationBatch();
        batch.setId(1L);
        batch.setTaskId(1L);

        when(candidateMapper.selectById(1L)).thenReturn(candidate);
        when(batchMapper.selectById(1L)).thenReturn(batch);
        when(taskMapper.selectById(1L)).thenReturn(mockTask());
        when(projectMapper.selectById(1L)).thenReturn(mockProject());
        when(candidateMapper.selectList(any())).thenReturn(List.of(candidate));

        aiAssignmentService.acceptCandidate(1L, "MANAGER", 2L);

        verify(taskService).assignTaskByAi(1L, 3L, 2L);
    }

    @Test
    void nonProjectManagerCannotAccept() {
        AiRecommendationCandidate candidate = new AiRecommendationCandidate();
        candidate.setId(1L);
        candidate.setBatchId(1L);
        candidate.setCandidateUserId(3L);

        AiRecommendationBatch batch = new AiRecommendationBatch();
        batch.setId(1L);
        batch.setTaskId(1L);

        when(candidateMapper.selectById(1L)).thenReturn(candidate);
        when(batchMapper.selectById(1L)).thenReturn(batch);
        when(taskMapper.selectById(1L)).thenReturn(mockTask());
        when(projectMapper.selectById(1L)).thenReturn(mockProject());

        BusinessException ex = assertThrows(BusinessException.class,
            () -> aiAssignmentService.acceptCandidate(1L, "MANAGER", 99L));
        assertEquals(403, ex.getCode());
    }

    @Test
    void memberCannotAccept() {
        AiRecommendationCandidate candidate = new AiRecommendationCandidate();
        candidate.setId(1L);
        candidate.setBatchId(1L);
        candidate.setCandidateUserId(3L);

        AiRecommendationBatch batch = new AiRecommendationBatch();
        batch.setId(1L);
        batch.setTaskId(1L);

        when(candidateMapper.selectById(1L)).thenReturn(candidate);
        when(batchMapper.selectById(1L)).thenReturn(batch);
        when(taskMapper.selectById(1L)).thenReturn(mockTask());

        BusinessException ex = assertThrows(BusinessException.class,
            () -> aiAssignmentService.acceptCandidate(1L, "MEMBER", 3L));
        assertEquals(403, ex.getCode());
    }

    @Test
    void acceptCallsAssignTaskByAiWithSourceAi() {
        AiRecommendationCandidate candidate = new AiRecommendationCandidate();
        candidate.setId(1L);
        candidate.setBatchId(1L);
        candidate.setCandidateUserId(3L);

        AiRecommendationBatch batch = new AiRecommendationBatch();
        batch.setId(1L);
        batch.setTaskId(1L);

        when(candidateMapper.selectById(1L)).thenReturn(candidate);
        when(batchMapper.selectById(1L)).thenReturn(batch);
        when(taskMapper.selectById(1L)).thenReturn(mockTask());
        when(candidateMapper.selectList(any())).thenReturn(List.of(candidate));

        aiAssignmentService.acceptCandidate(1L, "ADMIN", 1L);

        verify(taskService).assignTaskByAi(1L, 3L, 1L);
    }
}
