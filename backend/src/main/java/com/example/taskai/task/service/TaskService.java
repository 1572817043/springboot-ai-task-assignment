package com.example.taskai.task.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.taskai.common.exception.BusinessException;
import com.example.taskai.project.entity.Project;
import com.example.taskai.project.entity.ProjectMember;
import com.example.taskai.project.mapper.ProjectMapper;
import com.example.taskai.project.mapper.ProjectMemberMapper;
import com.example.taskai.skill.entity.Skill;
import com.example.taskai.skill.entity.TaskRequiredSkill;
import com.example.taskai.skill.mapper.SkillMapper;
import com.example.taskai.skill.mapper.TaskRequiredSkillMapper;
import com.example.taskai.task.dto.TaskAssignRequest;
import com.example.taskai.task.dto.TaskCreateRequest;
import com.example.taskai.task.dto.TaskRequiredSkillRequest;
import com.example.taskai.task.dto.TaskResultReviewRequest;
import com.example.taskai.task.dto.TaskResultSubmitRequest;
import com.example.taskai.task.dto.TaskUpdateRequest;
import com.example.taskai.task.entity.Task;
import com.example.taskai.task.entity.TaskAssignment;
import com.example.taskai.task.entity.TaskResult;
import com.example.taskai.task.entity.TaskStatusLog;
import com.example.taskai.task.mapper.TaskAssignmentMapper;
import com.example.taskai.task.mapper.TaskMapper;
import com.example.taskai.task.mapper.TaskResultMapper;
import com.example.taskai.task.mapper.TaskStatusLogMapper;
import com.example.taskai.task.vo.TaskDetailVO;
import com.example.taskai.task.vo.TaskListItemVO;
import com.example.taskai.task.vo.TaskRequiredSkillVO;
import com.example.taskai.task.vo.TaskResultVO;
import com.example.taskai.task.vo.TaskStatusLogVO;
import com.example.taskai.user.entity.SysUser;
import com.example.taskai.user.mapper.SysUserMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private final TaskMapper taskMapper;
    private final TaskAssignmentMapper taskAssignmentMapper;
    private final TaskResultMapper taskResultMapper;
    private final TaskStatusLogMapper taskStatusLogMapper;
    private final TaskRequiredSkillMapper taskRequiredSkillMapper;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final SysUserMapper sysUserMapper;
    private final SkillMapper skillMapper;

    public TaskService(TaskMapper taskMapper,
                       TaskAssignmentMapper taskAssignmentMapper,
                       TaskResultMapper taskResultMapper,
                       TaskStatusLogMapper taskStatusLogMapper,
                       TaskRequiredSkillMapper taskRequiredSkillMapper,
                       ProjectMapper projectMapper,
                       ProjectMemberMapper projectMemberMapper,
                       SysUserMapper sysUserMapper,
                       SkillMapper skillMapper) {
        this.taskMapper = taskMapper;
        this.taskAssignmentMapper = taskAssignmentMapper;
        this.taskResultMapper = taskResultMapper;
        this.taskStatusLogMapper = taskStatusLogMapper;
        this.taskRequiredSkillMapper = taskRequiredSkillMapper;
        this.projectMapper = projectMapper;
        this.projectMemberMapper = projectMemberMapper;
        this.sysUserMapper = sysUserMapper;
        this.skillMapper = skillMapper;
    }

    public IPage<TaskListItemVO> listTasks(String keyword, Long projectId, String status,
                                            String priority, Long assigneeId,
                                            int page, int size,
                                            String role, Long currentUserId) {
        Page<TaskListItemVO> pageParam = new Page<>(page, size);

        if ("MEMBER".equals(role)) {
            return taskMapper.selectTaskPage(pageParam, keyword, projectId, status, priority,
                currentUserId, null);
        }

        if ("MANAGER".equals(role)) {
            List<Long> projectIds = getManagedOrJoinedProjectIds(currentUserId);
            if (projectIds.isEmpty()) {
                pageParam.setRecords(Collections.emptyList());
                return pageParam;
            }
            return taskMapper.selectTaskPage(pageParam, keyword, projectId, status, priority,
                assigneeId, projectIds);
        }

        return taskMapper.selectTaskPage(pageParam, keyword, projectId, status, priority,
            assigneeId, null);
    }

    public TaskDetailVO getTaskDetail(Long taskId, String role, Long currentUserId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在", HttpStatus.NOT_FOUND);
        }

        checkTaskViewAccess(task, role, currentUserId);
        return toDetailVO(task);
    }

    @Transactional
    public void createTask(TaskCreateRequest request, Long currentUserId) {
        Project project = projectMapper.selectById(request.projectId());
        if (project == null) {
            throw new BusinessException(400, "项目不存在", HttpStatus.BAD_REQUEST);
        }

        Task task = new Task();
        task.setProjectId(request.projectId());
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriority(request.priority() != null ? request.priority() : "MEDIUM");
        task.setStatus("UNASSIGNED");
        task.setCreatorId(currentUserId);
        task.setDeadline(request.deadline());
        task.setEstimatedHours(request.estimatedHours());
        taskMapper.insert(task);

        if (request.requiredSkills() != null) {
            for (TaskRequiredSkillRequest skill : request.requiredSkills()) {
                TaskRequiredSkill trs = new TaskRequiredSkill();
                trs.setTaskId(task.getId());
                trs.setSkillId(skill.skillId());
                trs.setWeight(skill.weight() != null ? skill.weight() : BigDecimal.ONE);
                taskRequiredSkillMapper.insert(trs);
            }
        }

        insertStatusLog(task.getId(), null, "UNASSIGNED", currentUserId, null);
    }

    public void updateTask(Long taskId, TaskUpdateRequest request, String role, Long currentUserId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在", HttpStatus.NOT_FOUND);
        }

        checkProjectManagerAccess(task.getProjectId(), role, currentUserId);

        if (request.title() != null) {
            task.setTitle(request.title());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        if (request.deadline() != null) {
            task.setDeadline(request.deadline());
        }
        if (request.estimatedHours() != null) {
            task.setEstimatedHours(request.estimatedHours());
        }
        taskMapper.updateById(task);

        if (request.requiredSkills() != null) {
            taskRequiredSkillMapper.delete(
                new LambdaQueryWrapper<TaskRequiredSkill>().eq(TaskRequiredSkill::getTaskId, taskId)
            );
            for (TaskRequiredSkillRequest skill : request.requiredSkills()) {
                TaskRequiredSkill trs = new TaskRequiredSkill();
                trs.setTaskId(taskId);
                trs.setSkillId(skill.skillId());
                trs.setWeight(skill.weight() != null ? skill.weight() : BigDecimal.ONE);
                taskRequiredSkillMapper.insert(trs);
            }
        }
    }

    public void updateTaskStatus(Long taskId, String newStatus, String remark,
                                  String role, Long currentUserId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在", HttpStatus.NOT_FOUND);
        }

        if ("MEMBER".equals(role)) {
            if (!currentUserId.equals(task.getAssigneeId())) {
                throw new BusinessException(403, "无权限修改此任务状态", HttpStatus.FORBIDDEN);
            }
            if (!"IN_PROGRESS".equals(newStatus) && !"WAIT_REVIEW".equals(newStatus)) {
                throw new BusinessException(400, "成员只能将任务改为进行中或待验收", HttpStatus.BAD_REQUEST);
            }
        } else {
            checkProjectManagerAccess(task.getProjectId(), role, currentUserId);
        }

        String oldStatus = task.getStatus();
        task.setStatus(newStatus);
        taskMapper.updateById(task);
        insertStatusLog(taskId, oldStatus, newStatus, currentUserId, remark);
    }

    public void assignTask(Long taskId, TaskAssignRequest request, String role, Long currentUserId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在", HttpStatus.NOT_FOUND);
        }

        checkProjectManagerAccess(task.getProjectId(), role, currentUserId);

        SysUser assignee = sysUserMapper.selectById(request.assigneeId());
        if (assignee == null) {
            throw new BusinessException(400, "用户不存在", HttpStatus.BAD_REQUEST);
        }

        Long memberCount = projectMemberMapper.selectCount(
            new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, task.getProjectId())
                .eq(ProjectMember::getUserId, request.assigneeId())
        );
        if (memberCount == 0) {
            throw new BusinessException(400, "该用户不是项目成员", HttpStatus.BAD_REQUEST);
        }

        String oldStatus = task.getStatus();
        task.setAssigneeId(request.assigneeId());
        if ("UNASSIGNED".equals(oldStatus)) {
            task.setStatus("TODO");
        }
        taskMapper.updateById(task);

        TaskAssignment assignment = new TaskAssignment();
        assignment.setTaskId(taskId);
        assignment.setAssigneeId(request.assigneeId());
        assignment.setAssignedBy(currentUserId);
        assignment.setSource("MANUAL");
        assignment.setReason(request.reason());
        taskAssignmentMapper.insert(assignment);

        insertStatusLog(taskId, oldStatus, task.getStatus(), currentUserId, null);
    }

    @Transactional
    public void assignTaskByAi(Long taskId, Long assigneeId, Long operatorId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在", HttpStatus.NOT_FOUND);
        }

        String oldStatus = task.getStatus();
        task.setAssigneeId(assigneeId);
        if ("UNASSIGNED".equals(oldStatus)) {
            task.setStatus("TODO");
        }
        taskMapper.updateById(task);

        TaskAssignment assignment = new TaskAssignment();
        assignment.setTaskId(taskId);
        assignment.setAssigneeId(assigneeId);
        assignment.setAssignedBy(operatorId);
        assignment.setSource("AI");
        taskAssignmentMapper.insert(assignment);

        insertStatusLog(taskId, oldStatus, task.getStatus(), operatorId, "AI 推荐分配");
    }

    @Transactional
    public void submitResult(Long taskId, TaskResultSubmitRequest request,
                              String role, Long currentUserId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在", HttpStatus.NOT_FOUND);
        }

        if ("MEMBER".equals(role) && !currentUserId.equals(task.getAssigneeId())) {
            throw new BusinessException(403, "无权限提交此任务成果", HttpStatus.FORBIDDEN);
        }

        TaskResult result = new TaskResult();
        result.setTaskId(taskId);
        result.setUserId(currentUserId);
        result.setResultSummary(request.resultSummary());
        result.setResultUrl(request.resultUrl());
        result.setReviewStatus("PENDING");
        taskResultMapper.insert(result);

        String oldStatus = task.getStatus();
        task.setStatus("WAIT_REVIEW");
        taskMapper.updateById(task);
        insertStatusLog(taskId, oldStatus, "WAIT_REVIEW", currentUserId, null);
    }

    public void reviewResult(Long taskId, TaskResultReviewRequest request,
                              String role, Long currentUserId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在", HttpStatus.NOT_FOUND);
        }

        checkProjectManagerAccess(task.getProjectId(), role, currentUserId);

        TaskResult result = taskResultMapper.selectOne(
            new LambdaQueryWrapper<TaskResult>()
                .eq(TaskResult::getTaskId, taskId)
                .orderByDesc(TaskResult::getId)
                .last("LIMIT 1")
        );
        if (result == null) {
            throw new BusinessException(400, "该任务没有提交记录", HttpStatus.BAD_REQUEST);
        }

        result.setReviewStatus(request.reviewStatus());
        result.setReviewComment(request.reviewComment());
        result.setReviewedAt(java.time.LocalDateTime.now());
        taskResultMapper.updateById(result);

        String oldStatus = task.getStatus();
        String newStatus;
        if ("APPROVED".equals(request.reviewStatus())) {
            newStatus = "COMPLETED";
        } else {
            newStatus = "IN_PROGRESS";
        }
        task.setStatus(newStatus);
        taskMapper.updateById(task);
        insertStatusLog(taskId, oldStatus, newStatus, currentUserId, request.reviewComment());
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

    private void checkTaskViewAccess(Task task, String role, Long currentUserId) {
        if ("ADMIN".equals(role)) {
            return;
        }
        if ("MEMBER".equals(role)) {
            if (!currentUserId.equals(task.getAssigneeId())) {
                throw new BusinessException(403, "无权限查看此任务", HttpStatus.FORBIDDEN);
            }
            return;
        }
        if ("MANAGER".equals(role)) {
            List<Long> projectIds = getManagedOrJoinedProjectIds(currentUserId);
            if (!projectIds.contains(task.getProjectId())) {
                throw new BusinessException(403, "无权限查看此任务", HttpStatus.FORBIDDEN);
            }
        }
    }

    private void checkProjectManagerAccess(Long projectId, String role, Long currentUserId) {
        if ("ADMIN".equals(role)) {
            return;
        }
        if ("MANAGER".equals(role)) {
            Project project = projectMapper.selectById(projectId);
            if (project != null && project.getManagerId().equals(currentUserId)) {
                return;
            }
            throw new BusinessException(403, "无权限操作此任务", HttpStatus.FORBIDDEN);
        }
        throw new BusinessException(403, "无权限操作此任务", HttpStatus.FORBIDDEN);
    }

    private List<Long> getManagedOrJoinedProjectIds(Long userId) {
        List<Project> managed = projectMapper.selectList(
            new LambdaQueryWrapper<Project>().eq(Project::getManagerId, userId)
        );
        List<ProjectMember> joined = projectMemberMapper.selectList(
            new LambdaQueryWrapper<ProjectMember>().eq(ProjectMember::getUserId, userId)
        );
        List<Long> ids = new ArrayList<>();
        managed.forEach(p -> ids.add(p.getId()));
        joined.forEach(pm -> {
            if (!ids.contains(pm.getProjectId())) {
                ids.add(pm.getProjectId());
            }
        });
        return ids;
    }

    private void insertStatusLog(Long taskId, String oldStatus, String newStatus,
                                  Long operatorId, String remark) {
        TaskStatusLog log = new TaskStatusLog();
        log.setTaskId(taskId);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        taskStatusLogMapper.insert(log);
    }

    private TaskDetailVO toDetailVO(Task task) {
        SysUser creator = sysUserMapper.selectById(task.getCreatorId());
        SysUser assignee = task.getAssigneeId() != null ? sysUserMapper.selectById(task.getAssigneeId()) : null;
        Project project = projectMapper.selectById(task.getProjectId());

        List<TaskRequiredSkillVO> skills = getRequiredSkills(task.getId());
        List<TaskStatusLogVO> logs = taskStatusLogMapper.selectLogsByTaskId(task.getId());
        TaskResultVO latestResult = getLatestResult(task.getId());

        return new TaskDetailVO(
            task.getId(),
            task.getProjectId(),
            project != null ? project.getProjectName() : null,
            task.getTitle(),
            task.getDescription(),
            task.getPriority(),
            task.getStatus(),
            task.getCreatorId(),
            creator != null ? creator.getRealName() : null,
            task.getAssigneeId(),
            assignee != null ? assignee.getRealName() : null,
            task.getDeadline(),
            task.getEstimatedHours(),
            task.getCreatedAt(),
            task.getUpdatedAt(),
            skills,
            logs,
            latestResult
        );
    }

    private List<TaskRequiredSkillVO> getRequiredSkills(Long taskId) {
        List<TaskRequiredSkill> trsList = taskRequiredSkillMapper.selectList(
            new LambdaQueryWrapper<TaskRequiredSkill>().eq(TaskRequiredSkill::getTaskId, taskId)
        );
        if (trsList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> skillIds = trsList.stream().map(TaskRequiredSkill::getSkillId).toList();
        Map<Long, Skill> skillMap = skillMapper.selectBatchIds(skillIds).stream()
            .collect(Collectors.toMap(Skill::getId, s -> s));

        return trsList.stream().map(trs -> {
            Skill skill = skillMap.get(trs.getSkillId());
            return new TaskRequiredSkillVO(
                trs.getSkillId(),
                skill != null ? skill.getSkillName() : null,
                skill != null ? skill.getCategory() : null,
                trs.getWeight()
            );
        }).toList();
    }

    private TaskResultVO getLatestResult(Long taskId) {
        TaskResult result = taskResultMapper.selectOne(
            new LambdaQueryWrapper<TaskResult>()
                .eq(TaskResult::getTaskId, taskId)
                .orderByDesc(TaskResult::getId)
                .last("LIMIT 1")
        );
        if (result == null) {
            return null;
        }
        return new TaskResultVO(
            result.getResultSummary(),
            result.getResultUrl(),
            result.getReviewStatus(),
            result.getReviewComment(),
            result.getSubmittedAt(),
            result.getReviewedAt()
        );
    }
}
