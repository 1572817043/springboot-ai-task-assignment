package com.example.taskai.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.taskai.common.exception.BusinessException;
import com.example.taskai.knowledge.entity.AiKnowledgeDocument;
import com.example.taskai.knowledge.mapper.AiKnowledgeDocumentMapper;
import com.example.taskai.knowledge.vo.KnowledgeDocumentVO;
import com.example.taskai.member.entity.MemberProfile;
import com.example.taskai.member.mapper.MemberProfileMapper;
import com.example.taskai.task.entity.Task;
import com.example.taskai.task.entity.TaskResult;
import com.example.taskai.task.mapper.TaskMapper;
import com.example.taskai.task.mapper.TaskResultMapper;
import com.example.taskai.user.entity.SysUser;
import com.example.taskai.user.mapper.SysUserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AiKnowledgeService {

    private final AiKnowledgeDocumentMapper documentMapper;
    private final MemberProfileMapper memberProfileMapper;
    private final SysUserMapper sysUserMapper;
    private final TaskMapper taskMapper;
    private final TaskResultMapper taskResultMapper;

    public AiKnowledgeService(AiKnowledgeDocumentMapper documentMapper,
                               MemberProfileMapper memberProfileMapper,
                               SysUserMapper sysUserMapper,
                               TaskMapper taskMapper,
                               TaskResultMapper taskResultMapper) {
        this.documentMapper = documentMapper;
        this.memberProfileMapper = memberProfileMapper;
        this.sysUserMapper = sysUserMapper;
        this.taskMapper = taskMapper;
        this.taskResultMapper = taskResultMapper;
    }

    public void syncMemberResume(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在", HttpStatus.NOT_FOUND);
        }

        MemberProfile profile = memberProfileMapper.selectOne(
            new LambdaQueryWrapper<MemberProfile>().eq(MemberProfile::getUserId, userId)
        );

        StringBuilder content = new StringBuilder();
        if (profile != null && profile.getResumeText() != null) {
            content.append(profile.getResumeText());
        }
        if (profile != null && profile.getExperienceSummary() != null) {
            if (!content.isEmpty()) {
                content.append("\n\n");
            }
            content.append(profile.getExperienceSummary());
        }

        if (content.isEmpty()) {
            throw new BusinessException(400, "该成员暂无简历数据", HttpStatus.BAD_REQUEST);
        }

        AiKnowledgeDocument existing = documentMapper.selectOne(
            new LambdaQueryWrapper<AiKnowledgeDocument>()
                .eq(AiKnowledgeDocument::getSourceType, "RESUME")
                .eq(AiKnowledgeDocument::getSourceId, userId)
        );

        if (existing != null) {
            existing.setTitle(user.getRealName() + " 简历画像");
            existing.setContent(content.toString());
            existing.setIndexed(0);
            documentMapper.updateById(existing);
        } else {
            AiKnowledgeDocument doc = new AiKnowledgeDocument();
            doc.setUserId(userId);
            doc.setSourceType("RESUME");
            doc.setSourceId(userId);
            doc.setTitle(user.getRealName() + " 简历画像");
            doc.setContent(content.toString());
            doc.setIndexed(0);
            documentMapper.insert(doc);
        }
    }

    public void syncTaskResult(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在", HttpStatus.NOT_FOUND);
        }

        TaskResult result = taskResultMapper.selectOne(
            new LambdaQueryWrapper<TaskResult>()
                .eq(TaskResult::getTaskId, taskId)
                .orderByDesc(TaskResult::getId)
                .last("LIMIT 1")
        );
        if (result == null) {
            throw new BusinessException(400, "该任务暂无提交成果", HttpStatus.BAD_REQUEST);
        }

        StringBuilder content = new StringBuilder();
        if (result.getResultSummary() != null) {
            content.append(result.getResultSummary());
        }
        if (result.getResultUrl() != null) {
            if (!content.isEmpty()) {
                content.append("\n");
            }
            content.append("链接：").append(result.getResultUrl());
        }

        AiKnowledgeDocument existing = documentMapper.selectOne(
            new LambdaQueryWrapper<AiKnowledgeDocument>()
                .eq(AiKnowledgeDocument::getSourceType, "TASK_RESULT")
                .eq(AiKnowledgeDocument::getSourceId, result.getId())
        );

        if (existing != null) {
            existing.setTitle(task.getTitle() + " 成果");
            existing.setContent(content.toString());
            existing.setIndexed(0);
            documentMapper.updateById(existing);
        } else {
            AiKnowledgeDocument doc = new AiKnowledgeDocument();
            doc.setUserId(result.getUserId());
            doc.setSourceType("TASK_RESULT");
            doc.setSourceId(result.getId());
            doc.setTitle(task.getTitle() + " 成果");
            doc.setContent(content.toString());
            doc.setIndexed(0);
            documentMapper.insert(doc);
        }
    }

    public IPage<KnowledgeDocumentVO> listDocuments(String sourceType, String keyword,
                                                     Integer indexed, int page, int size) {
        Page<KnowledgeDocumentVO> pageParam = new Page<>(page, size);
        return documentMapper.selectDocumentPage(pageParam, sourceType, keyword, indexed);
    }

    public KnowledgeDocumentVO getDocument(Long id) {
        AiKnowledgeDocument doc = documentMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(404, "文档不存在", HttpStatus.NOT_FOUND);
        }
        return new KnowledgeDocumentVO(
            doc.getId(), doc.getUserId(), doc.getSourceType(), doc.getSourceId(),
            doc.getTitle(), doc.getContent(), doc.getIndexed(),
            doc.getCreatedAt(), doc.getUpdatedAt()
        );
    }

    public void updateIndexed(Long id, Integer indexed) {
        AiKnowledgeDocument doc = documentMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(404, "文档不存在", HttpStatus.NOT_FOUND);
        }
        doc.setIndexed(indexed);
        documentMapper.updateById(doc);
    }

    public Long resolveUserId(String username) {
        SysUser user = sysUserMapper.selectOne(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (user == null) {
            throw new BusinessException(401, "用户不存在", HttpStatus.UNAUTHORIZED);
        }
        return user.getId();
    }
}
