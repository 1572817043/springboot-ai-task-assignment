package com.example.taskai.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.taskai.common.exception.BusinessException;
import com.example.taskai.knowledge.entity.AiKnowledgeDocument;
import com.example.taskai.knowledge.mapper.AiKnowledgeDocumentMapper;
import com.example.taskai.knowledge.service.AiKnowledgeService;
import com.example.taskai.member.entity.MemberProfile;
import com.example.taskai.member.mapper.MemberProfileMapper;
import com.example.taskai.task.entity.Task;
import com.example.taskai.task.entity.TaskResult;
import com.example.taskai.task.mapper.TaskMapper;
import com.example.taskai.task.mapper.TaskResultMapper;
import com.example.taskai.user.entity.SysUser;
import com.example.taskai.user.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiKnowledgeServiceTests {

    @Mock
    private AiKnowledgeDocumentMapper documentMapper;
    @Mock
    private MemberProfileMapper memberProfileMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private TaskMapper taskMapper;
    @Mock
    private TaskResultMapper taskResultMapper;

    @InjectMocks
    private AiKnowledgeService aiKnowledgeService;

    // ========== syncMemberResume ==========

    @Test
    void syncResumeThrowsWhenUserNotFound() {
        when(sysUserMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> aiKnowledgeService.syncMemberResume(99L));
        assertEquals(404, ex.getCode());
        assertEquals("用户不存在", ex.getMessage());
    }

    @Test
    void syncResumeThrowsWhenNoResumeData() {
        SysUser user = new SysUser();
        user.setId(3L);
        user.setRealName("成员用户");

        when(sysUserMapper.selectById(3L)).thenReturn(user);
        when(memberProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> aiKnowledgeService.syncMemberResume(3L));
        assertEquals(400, ex.getCode());
        assertEquals("该成员暂无简历数据", ex.getMessage());
    }

    @Test
    void syncResumeInsertsNewDocument() {
        SysUser user = new SysUser();
        user.setId(3L);
        user.setRealName("成员用户");

        MemberProfile profile = new MemberProfile();
        profile.setResumeText("简历内容");
        profile.setExperienceSummary("经验总结");

        when(sysUserMapper.selectById(3L)).thenReturn(user);
        when(memberProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(profile);
        when(documentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        aiKnowledgeService.syncMemberResume(3L);

        ArgumentCaptor<AiKnowledgeDocument> captor = ArgumentCaptor.forClass(AiKnowledgeDocument.class);
        verify(documentMapper).insert(captor.capture());
        AiKnowledgeDocument doc = captor.getValue();
        assertEquals("RESUME", doc.getSourceType());
        assertEquals(3L, doc.getSourceId());
        assertEquals(3L, doc.getUserId());
        assertEquals("成员用户 简历画像", doc.getTitle());
        assertEquals("简历内容\n\n经验总结", doc.getContent());
        assertEquals(0, doc.getIndexed());
    }

    @Test
    void syncResumeUpdatesExistingDocument() {
        SysUser user = new SysUser();
        user.setId(3L);
        user.setRealName("成员用户");

        MemberProfile profile = new MemberProfile();
        profile.setResumeText("新简历");
        profile.setExperienceSummary("新经验");

        AiKnowledgeDocument existing = new AiKnowledgeDocument();
        existing.setId(10L);
        existing.setSourceType("RESUME");
        existing.setSourceId(3L);
        existing.setIndexed(1);

        when(sysUserMapper.selectById(3L)).thenReturn(user);
        when(memberProfileMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(profile);
        when(documentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        aiKnowledgeService.syncMemberResume(3L);

        verify(documentMapper).updateById(existing);
        assertEquals("成员用户 简历画像", existing.getTitle());
        assertEquals("新简历\n\n新经验", existing.getContent());
        assertEquals(0, existing.getIndexed());
        verify(documentMapper, never()).insert(any(AiKnowledgeDocument.class));
    }

    // ========== syncTaskResult ==========

    @Test
    void syncTaskResultThrowsWhenTaskNotFound() {
        when(taskMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> aiKnowledgeService.syncTaskResult(99L));
        assertEquals(404, ex.getCode());
        assertEquals("任务不存在", ex.getMessage());
    }

    @Test
    void syncTaskResultThrowsWhenNoResult() {
        Task task = new Task();
        task.setId(1L);
        task.setTitle("开发登录功能");

        when(taskMapper.selectById(1L)).thenReturn(task);
        when(taskResultMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> aiKnowledgeService.syncTaskResult(1L));
        assertEquals(400, ex.getCode());
        assertEquals("该任务暂无提交成果", ex.getMessage());
    }

    @Test
    void syncTaskResultInsertsNewDocument() {
        Task task = new Task();
        task.setId(1L);
        task.setTitle("开发登录功能");

        TaskResult result = new TaskResult();
        result.setId(5L);
        result.setTaskId(1L);
        result.setUserId(3L);
        result.setResultSummary("已完成开发");
        result.setResultUrl("http://example.com/result");

        when(taskMapper.selectById(1L)).thenReturn(task);
        when(taskResultMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(result);
        when(documentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        aiKnowledgeService.syncTaskResult(1L);

        ArgumentCaptor<AiKnowledgeDocument> captor = ArgumentCaptor.forClass(AiKnowledgeDocument.class);
        verify(documentMapper).insert(captor.capture());
        AiKnowledgeDocument doc = captor.getValue();
        assertEquals("TASK_RESULT", doc.getSourceType());
        assertEquals(5L, doc.getSourceId());
        assertEquals(3L, doc.getUserId());
        assertEquals("开发登录功能 成果", doc.getTitle());
        assertEquals("已完成开发\n链接：http://example.com/result", doc.getContent());
        assertEquals(0, doc.getIndexed());
    }

    @Test
    void syncTaskResultUpdatesExistingDocument() {
        Task task = new Task();
        task.setId(1L);
        task.setTitle("开发登录功能");

        TaskResult result = new TaskResult();
        result.setId(5L);
        result.setTaskId(1L);
        result.setUserId(3L);
        result.setResultSummary("更新后的成果");

        AiKnowledgeDocument existing = new AiKnowledgeDocument();
        existing.setId(20L);
        existing.setSourceType("TASK_RESULT");
        existing.setSourceId(5L);
        existing.setIndexed(1);

        when(taskMapper.selectById(1L)).thenReturn(task);
        when(taskResultMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(result);
        when(documentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        aiKnowledgeService.syncTaskResult(1L);

        verify(documentMapper).updateById(existing);
        assertEquals("开发登录功能 成果", existing.getTitle());
        assertEquals("更新后的成果", existing.getContent());
        assertEquals(0, existing.getIndexed());
        verify(documentMapper, never()).insert(any(AiKnowledgeDocument.class));
    }

    // ========== updateIndexed ==========

    @Test
    void updateIndexedThrowsWhenDocumentNotFound() {
        when(documentMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> aiKnowledgeService.updateIndexed(99L, 1));
        assertEquals(404, ex.getCode());
        assertEquals("文档不存在", ex.getMessage());
    }

    @Test
    void updateIndexedUpdatesDocument() {
        AiKnowledgeDocument doc = new AiKnowledgeDocument();
        doc.setId(1L);
        doc.setIndexed(0);

        when(documentMapper.selectById(1L)).thenReturn(doc);

        aiKnowledgeService.updateIndexed(1L, 1);

        verify(documentMapper).updateById(doc);
        assertEquals(1, doc.getIndexed());
    }
}
