package com.example.taskai.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.taskai.ai.entity.AiRecommendationBatch;
import com.example.taskai.ai.entity.AiRecommendationCandidate;
import com.example.taskai.ai.mapper.AiRecommendationBatchMapper;
import com.example.taskai.ai.mapper.AiRecommendationCandidateMapper;
import com.example.taskai.ai.vo.AiRecommendationCandidateVO;
import com.example.taskai.ai.vo.AiRecommendationResponse;
import com.example.taskai.common.exception.BusinessException;
import com.example.taskai.member.entity.MemberProfile;
import com.example.taskai.member.mapper.MemberProfileMapper;
import com.example.taskai.project.entity.Project;
import com.example.taskai.project.entity.ProjectMember;
import com.example.taskai.project.mapper.ProjectMapper;
import com.example.taskai.project.mapper.ProjectMemberMapper;
import com.example.taskai.skill.entity.Skill;
import com.example.taskai.skill.entity.TaskRequiredSkill;
import com.example.taskai.skill.entity.UserSkill;
import com.example.taskai.skill.mapper.SkillMapper;
import com.example.taskai.skill.mapper.TaskRequiredSkillMapper;
import com.example.taskai.skill.mapper.UserSkillMapper;
import com.example.taskai.task.entity.Task;
import com.example.taskai.task.entity.TaskResult;
import com.example.taskai.task.mapper.TaskMapper;
import com.example.taskai.task.mapper.TaskResultMapper;
import com.example.taskai.task.service.TaskService;
import com.example.taskai.user.entity.SysUser;
import com.example.taskai.user.mapper.SysUserMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiAssignmentService {

    private final AiRecommendationBatchMapper batchMapper;
    private final AiRecommendationCandidateMapper candidateMapper;
    private final TaskMapper taskMapper;
    private final TaskResultMapper taskResultMapper;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final SysUserMapper sysUserMapper;
    private final UserSkillMapper userSkillMapper;
    private final TaskRequiredSkillMapper taskRequiredSkillMapper;
    private final SkillMapper skillMapper;
    private final MemberProfileMapper memberProfileMapper;
    private final TaskService taskService;

    public AiAssignmentService(AiRecommendationBatchMapper batchMapper,
                               AiRecommendationCandidateMapper candidateMapper,
                               TaskMapper taskMapper,
                               TaskResultMapper taskResultMapper,
                               ProjectMapper projectMapper,
                               ProjectMemberMapper projectMemberMapper,
                               SysUserMapper sysUserMapper,
                               UserSkillMapper userSkillMapper,
                               TaskRequiredSkillMapper taskRequiredSkillMapper,
                               SkillMapper skillMapper,
                               MemberProfileMapper memberProfileMapper,
                               TaskService taskService) {
        this.batchMapper = batchMapper;
        this.candidateMapper = candidateMapper;
        this.taskMapper = taskMapper;
        this.taskResultMapper = taskResultMapper;
        this.projectMapper = projectMapper;
        this.projectMemberMapper = projectMemberMapper;
        this.sysUserMapper = sysUserMapper;
        this.userSkillMapper = userSkillMapper;
        this.taskRequiredSkillMapper = taskRequiredSkillMapper;
        this.skillMapper = skillMapper;
        this.memberProfileMapper = memberProfileMapper;
        this.taskService = taskService;
    }

    @Transactional
    public AiRecommendationResponse recommend(Long taskId, String role, Long currentUserId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在", HttpStatus.NOT_FOUND);
        }

        checkManageAccess(task.getProjectId(), role, currentUserId);

        List<ProjectMember> members = projectMemberMapper.selectList(
            new LambdaQueryWrapper<ProjectMember>().eq(ProjectMember::getProjectId, task.getProjectId())
        );
        if (members.isEmpty()) {
            throw new BusinessException(400, "该项目没有成员，无法推荐", HttpStatus.BAD_REQUEST);
        }

        List<Long> memberUserIds = members.stream().map(ProjectMember::getUserId).toList();
        List<SysUser> users = sysUserMapper.selectList(
            new LambdaQueryWrapper<SysUser>()
                .in(SysUser::getId, memberUserIds)
                .eq(SysUser::getStatus, "ENABLED")
        );
        if (users.isEmpty()) {
            throw new BusinessException(400, "该项目没有可用成员", HttpStatus.BAD_REQUEST);
        }

        List<TaskRequiredSkill> requiredSkills = taskRequiredSkillMapper.selectList(
            new LambdaQueryWrapper<TaskRequiredSkill>().eq(TaskRequiredSkill::getTaskId, taskId)
        );

        List<AiRecommendationCandidate> candidates = new ArrayList<>();
        for (SysUser user : users) {
            BigDecimal skillScore = calcSkillScore(user.getId(), requiredSkills);
            BigDecimal historyScore = calcHistoryScore(user.getId());
            BigDecimal workloadScore = calcWorkloadScore(user.getId());
            BigDecimal completionScore = calcCompletionScore(user.getId());
            BigDecimal deadlineRiskScore = calcDeadlineRiskScore(user.getId(), task.getDeadline());

            BigDecimal totalScore = skillScore.add(historyScore).add(workloadScore)
                .add(completionScore).add(deadlineRiskScore);

            String reason = buildReason(user.getId(), skillScore, historyScore, workloadScore,
                completionScore, requiredSkills);

            AiRecommendationCandidate candidate = new AiRecommendationCandidate();
            candidate.setCandidateUserId(user.getId());
            candidate.setTotalScore(totalScore.setScale(2, RoundingMode.HALF_UP));
            candidate.setSkillScore(skillScore.setScale(2, RoundingMode.HALF_UP));
            candidate.setHistoryScore(historyScore.setScale(2, RoundingMode.HALF_UP));
            candidate.setWorkloadScore(workloadScore.setScale(2, RoundingMode.HALF_UP));
            candidate.setCompletionScore(completionScore.setScale(2, RoundingMode.HALF_UP));
            candidate.setDeadlineRiskScore(deadlineRiskScore.setScale(2, RoundingMode.HALF_UP));
            candidate.setReason(reason);
            candidate.setAccepted(0);
            candidates.add(candidate);
        }

        candidates.sort(Comparator.comparing(AiRecommendationCandidate::getTotalScore).reversed());

        AiRecommendationBatch batch = new AiRecommendationBatch();
        batch.setTaskId(taskId);
        batch.setModelName("rule-engine-v1");
        batch.setStatus("SUCCESS");
        batch.setCreatedBy(currentUserId);
        batchMapper.insert(batch);

        for (int i = 0; i < candidates.size(); i++) {
            AiRecommendationCandidate c = candidates.get(i);
            c.setBatchId(batch.getId());
            c.setRankNo(i + 1);
            candidateMapper.insert(c);
        }

        List<AiRecommendationCandidateVO> vos = new ArrayList<>();
        for (AiRecommendationCandidate c : candidates) {
            SysUser user = sysUserMapper.selectById(c.getCandidateUserId());
            vos.add(new AiRecommendationCandidateVO(
                c.getId(),
                c.getCandidateUserId(),
                user != null ? user.getRealName() : null,
                c.getRankNo(),
                c.getTotalScore(),
                c.getSkillScore(),
                c.getHistoryScore(),
                c.getWorkloadScore(),
                c.getCompletionScore(),
                c.getDeadlineRiskScore(),
                c.getReason(),
                c.getAccepted()
            ));
        }

        return new AiRecommendationResponse(batch.getId(), taskId, vos);
    }

    public AiRecommendationResponse getLatestRecommendation(Long taskId, String role, Long currentUserId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在", HttpStatus.NOT_FOUND);
        }

        if (!"ADMIN".equals(role)) {
            Project project = projectMapper.selectById(task.getProjectId());
            if ("MEMBER".equals(role)) {
                boolean isProjectMember = projectMemberMapper.selectCount(
                    new LambdaQueryWrapper<ProjectMember>()
                        .eq(ProjectMember::getProjectId, task.getProjectId())
                        .eq(ProjectMember::getUserId, currentUserId)
                ) > 0;
                if (!isProjectMember) {
                    throw new BusinessException(403, "无权限查看此推荐", HttpStatus.FORBIDDEN);
                }
            } else if ("MANAGER".equals(role)) {
                if (project == null || !project.getManagerId().equals(currentUserId)) {
                    throw new BusinessException(403, "无权限查看此推荐", HttpStatus.FORBIDDEN);
                }
            }
        }

        AiRecommendationBatch batch = batchMapper.selectOne(
            new LambdaQueryWrapper<AiRecommendationBatch>()
                .eq(AiRecommendationBatch::getTaskId, taskId)
                .orderByDesc(AiRecommendationBatch::getId)
                .last("LIMIT 1")
        );
        if (batch == null) {
            throw new BusinessException(404, "暂无推荐记录", HttpStatus.NOT_FOUND);
        }

        List<AiRecommendationCandidate> candidates = candidateMapper.selectList(
            new LambdaQueryWrapper<AiRecommendationCandidate>()
                .eq(AiRecommendationCandidate::getBatchId, batch.getId())
                .orderByAsc(AiRecommendationCandidate::getRankNo)
        );

        List<AiRecommendationCandidateVO> vos = candidates.stream().map(c -> {
            SysUser user = sysUserMapper.selectById(c.getCandidateUserId());
            return new AiRecommendationCandidateVO(
                c.getId(),
                c.getCandidateUserId(),
                user != null ? user.getRealName() : null,
                c.getRankNo(),
                c.getTotalScore(),
                c.getSkillScore(),
                c.getHistoryScore(),
                c.getWorkloadScore(),
                c.getCompletionScore(),
                c.getDeadlineRiskScore(),
                c.getReason(),
                c.getAccepted()
            );
        }).toList();

        return new AiRecommendationResponse(batch.getId(), taskId, vos);
    }

    @Transactional
    public void acceptCandidate(Long candidateId, String role, Long currentUserId) {
        AiRecommendationCandidate candidate = candidateMapper.selectById(candidateId);
        if (candidate == null) {
            throw new BusinessException(404, "推荐记录不存在", HttpStatus.NOT_FOUND);
        }

        AiRecommendationBatch batch = batchMapper.selectById(candidate.getBatchId());
        if (batch == null) {
            throw new BusinessException(404, "推荐批次不存在", HttpStatus.NOT_FOUND);
        }

        Task task = taskMapper.selectById(batch.getTaskId());
        if (task != null) {
            checkManageAccess(task.getProjectId(), role, currentUserId);
        }

        List<AiRecommendationCandidate> allCandidates = candidateMapper.selectList(
            new LambdaQueryWrapper<AiRecommendationCandidate>()
                .eq(AiRecommendationCandidate::getBatchId, batch.getId())
        );
        for (AiRecommendationCandidate c : allCandidates) {
            c.setAccepted(c.getId().equals(candidateId) ? 1 : 0);
            candidateMapper.updateById(c);
        }

        taskService.assignTaskByAi(batch.getTaskId(), candidate.getCandidateUserId(), currentUserId);
    }

    private void checkManageAccess(Long projectId, String role, Long currentUserId) {
        if ("ADMIN".equals(role)) {
            return;
        }
        if ("MANAGER".equals(role)) {
            Project project = projectMapper.selectById(projectId);
            if (project != null && project.getManagerId().equals(currentUserId)) {
                return;
            }
        }
        throw new BusinessException(403, "无权限操作", HttpStatus.FORBIDDEN);
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

    private BigDecimal calcSkillScore(Long userId, List<TaskRequiredSkill> requiredSkills) {
        if (requiredSkills.isEmpty()) {
            return new BigDecimal("17.50");
        }

        List<UserSkill> userSkills = userSkillMapper.selectList(
            new LambdaQueryWrapper<UserSkill>().eq(UserSkill::getUserId, userId)
        );
        Map<Long, Integer> userSkillLevelMap = userSkills.stream()
            .collect(Collectors.toMap(UserSkill::getSkillId, UserSkill::getLevel));

        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal matchedWeight = BigDecimal.ZERO;

        for (TaskRequiredSkill trs : requiredSkills) {
            BigDecimal weight = trs.getWeight() != null ? trs.getWeight() : BigDecimal.ONE;
            totalWeight = totalWeight.add(weight);

            Integer level = userSkillLevelMap.get(trs.getSkillId());
            if (level != null) {
                BigDecimal levelFactor = BigDecimal.valueOf(level).divide(BigDecimal.valueOf(5), 4, RoundingMode.HALF_UP);
                matchedWeight = matchedWeight.add(weight.multiply(levelFactor));
            }
        }

        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return matchedWeight.divide(totalWeight, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("35"));
    }

    private BigDecimal calcHistoryScore(Long userId) {
        Long completedCount = taskResultMapper.selectCount(
            new LambdaQueryWrapper<TaskResult>().eq(TaskResult::getUserId, userId)
        );
        int count = completedCount.intValue();
        if (count >= 10) {
            return new BigDecimal("25");
        } else if (count >= 5) {
            return new BigDecimal("20");
        } else if (count >= 2) {
            return new BigDecimal("15");
        } else if (count >= 1) {
            return new BigDecimal("10");
        }
        return new BigDecimal("5");
    }

    private BigDecimal calcWorkloadScore(Long userId) {
        MemberProfile profile = memberProfileMapper.selectOne(
            new LambdaQueryWrapper<MemberProfile>().eq(MemberProfile::getUserId, userId)
        );
        int workload = (profile != null && profile.getCurrentWorkload() != null)
            ? profile.getCurrentWorkload() : 0;
        if (workload == 0) {
            return new BigDecimal("20");
        } else if (workload <= 2) {
            return new BigDecimal("15");
        } else if (workload <= 5) {
            return new BigDecimal("10");
        } else if (workload <= 8) {
            return new BigDecimal("5");
        }
        return new BigDecimal("0");
    }

    private BigDecimal calcCompletionScore(Long userId) {
        MemberProfile profile = memberProfileMapper.selectOne(
            new LambdaQueryWrapper<MemberProfile>().eq(MemberProfile::getUserId, userId)
        );
        if (profile == null || profile.getTaskCompletionRate() == null) {
            return new BigDecimal("5");
        }
        BigDecimal rate = profile.getTaskCompletionRate();
        return rate.multiply(new BigDecimal("10")).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
            .min(new BigDecimal("10"));
    }

    private BigDecimal calcDeadlineRiskScore(Long userId, LocalDateTime deadline) {
        if (deadline == null) {
            return new BigDecimal("5");
        }
        long hoursUntilDeadline = Duration.between(LocalDateTime.now(), deadline).toHours();
        MemberProfile profile = memberProfileMapper.selectOne(
            new LambdaQueryWrapper<MemberProfile>().eq(MemberProfile::getUserId, userId)
        );
        int workload = (profile != null && profile.getCurrentWorkload() != null)
            ? profile.getCurrentWorkload() : 0;

        if (hoursUntilDeadline <= 0) {
            return BigDecimal.ZERO;
        } else if (hoursUntilDeadline <= 24 && workload > 3) {
            return new BigDecimal("2");
        } else if (hoursUntilDeadline <= 72 && workload > 5) {
            return new BigDecimal("4");
        } else if (workload <= 2) {
            return new BigDecimal("10");
        } else if (workload <= 5) {
            return new BigDecimal("7");
        }
        return new BigDecimal("5");
    }

    private String buildReason(Long userId, BigDecimal skillScore, BigDecimal historyScore,
                                BigDecimal workloadScore, BigDecimal completionScore,
                                List<TaskRequiredSkill> requiredSkills) {
        StringBuilder sb = new StringBuilder();

        if (skillScore.compareTo(new BigDecimal("25")) >= 0) {
            List<String> matchedSkills = getMatchedSkillNames(userId, requiredSkills);
            if (!matchedSkills.isEmpty()) {
                sb.append("技能匹配度较高，掌握 ").append(String.join("、", matchedSkills));
            } else {
                sb.append("技能匹配度较高");
            }
        } else if (skillScore.compareTo(new BigDecimal("15")) >= 0) {
            sb.append("技能匹配度一般");
        } else {
            sb.append("技能匹配度较低");
        }

        MemberProfile profile = memberProfileMapper.selectOne(
            new LambdaQueryWrapper<MemberProfile>().eq(MemberProfile::getUserId, userId)
        );
        int workload = (profile != null && profile.getCurrentWorkload() != null)
            ? profile.getCurrentWorkload() : 0;
        if (workload <= 2) {
            sb.append("；当前工作量较低");
        } else if (workload >= 6) {
            sb.append("；当前工作量较高");
        }

        if (profile != null && profile.getTaskCompletionRate() != null) {
            sb.append("；历史完成率 ").append(profile.getTaskCompletionRate()).append("%");
        }

        return sb.toString();
    }

    private List<String> getMatchedSkillNames(Long userId, List<TaskRequiredSkill> requiredSkills) {
        List<UserSkill> userSkills = userSkillMapper.selectList(
            new LambdaQueryWrapper<UserSkill>().eq(UserSkill::getUserId, userId)
        );
        Map<Long, Integer> userSkillMap = userSkills.stream()
            .collect(Collectors.toMap(UserSkill::getSkillId, UserSkill::getLevel));

        List<Long> matchedSkillIds = requiredSkills.stream()
            .filter(trs -> userSkillMap.containsKey(trs.getSkillId()))
            .map(TaskRequiredSkill::getSkillId)
            .toList();

        if (matchedSkillIds.isEmpty()) {
            return Collections.emptyList();
        }

        return skillMapper.selectBatchIds(matchedSkillIds).stream()
            .map(Skill::getSkillName)
            .toList();
    }
}
