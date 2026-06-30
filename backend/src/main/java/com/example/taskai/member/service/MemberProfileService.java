package com.example.taskai.member.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.taskai.common.exception.BusinessException;
import com.example.taskai.member.dto.MemberProfileUpdateRequest;
import com.example.taskai.member.dto.MemberSkillItemRequest;
import com.example.taskai.member.entity.MemberProfile;
import com.example.taskai.member.mapper.MemberProfileMapper;
import com.example.taskai.member.vo.MemberProfileDetailVO;
import com.example.taskai.member.vo.MemberProfileListItemVO;
import com.example.taskai.member.vo.MemberSkillVO;
import com.example.taskai.skill.entity.Skill;
import com.example.taskai.skill.entity.UserSkill;
import com.example.taskai.skill.mapper.SkillMapper;
import com.example.taskai.skill.mapper.UserSkillMapper;
import com.example.taskai.user.entity.SysUser;
import com.example.taskai.user.mapper.SysUserMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberProfileService {

    private final MemberProfileMapper memberProfileMapper;
    private final SysUserMapper sysUserMapper;
    private final UserSkillMapper userSkillMapper;
    private final SkillMapper skillMapper;

    public MemberProfileService(MemberProfileMapper memberProfileMapper,
                                SysUserMapper sysUserMapper,
                                UserSkillMapper userSkillMapper,
                                SkillMapper skillMapper) {
        this.memberProfileMapper = memberProfileMapper;
        this.sysUserMapper = sysUserMapper;
        this.userSkillMapper = userSkillMapper;
        this.skillMapper = skillMapper;
    }

    public IPage<MemberProfileListItemVO> listProfiles(String keyword, Long skillId, int page, int size) {
        Page<MemberProfileListItemVO> pageParam = new Page<>(page, size);
        IPage<MemberProfileListItemVO> result = memberProfileMapper.selectMemberProfiles(pageParam, keyword, skillId);

        if (result.getRecords().isEmpty()) {
            return result;
        }

        List<Long> userIds = result.getRecords().stream()
            .map(MemberProfileListItemVO::userId)
            .toList();

        List<UserSkill> userSkills = userSkillMapper.selectList(
            new LambdaQueryWrapper<UserSkill>().in(UserSkill::getUserId, userIds)
        );
        if (userSkills.isEmpty()) {
            result.getRecords().forEach(r -> {
                // skills 字段已在 record 中，需要通过反射或重新构造来设置
            });
            return withEmptySkills(result);
        }

        List<Long> skillIds = userSkills.stream().map(UserSkill::getSkillId).distinct().toList();
        Map<Long, String> skillNameMap = skillMapper.selectBatchIds(skillIds).stream()
            .collect(Collectors.toMap(Skill::getId, Skill::getSkillName));

        Map<Long, List<String>> userSkillNames = userSkills.stream()
            .collect(Collectors.groupingBy(
                UserSkill::getUserId,
                Collectors.mapping(us -> skillNameMap.getOrDefault(us.getSkillId(), ""), Collectors.toList())
            ));

        return withSkills(result, userSkillNames);
    }

    public MemberProfileDetailVO getProfileDetail(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在", HttpStatus.NOT_FOUND);
        }

        MemberProfile profile = memberProfileMapper.selectOne(
            new LambdaQueryWrapper<MemberProfile>().eq(MemberProfile::getUserId, userId)
        );

        List<MemberSkillVO> skills = getMemberSkills(userId);

        return new MemberProfileDetailVO(
            user.getId(),
            user.getUsername(),
            user.getRealName(),
            user.getEmail(),
            user.getPhone(),
            profile != null ? profile.getResumeText() : null,
            profile != null ? profile.getExperienceSummary() : null,
            profile != null ? profile.getCurrentWorkload() : 0,
            profile != null ? profile.getCompletedTaskCount() : 0,
            profile != null ? profile.getOverdueTaskCount() : 0,
            profile != null ? profile.getTaskCompletionRate() : null,
            profile != null ? profile.getOverdueRate() : null,
            skills
        );
    }

    public void updateProfile(Long userId, MemberProfileUpdateRequest request) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在", HttpStatus.NOT_FOUND);
        }

        MemberProfile profile = memberProfileMapper.selectOne(
            new LambdaQueryWrapper<MemberProfile>().eq(MemberProfile::getUserId, userId)
        );

        if (profile == null) {
            profile = new MemberProfile();
            profile.setUserId(userId);
            profile.setResumeText(request.resumeText());
            profile.setExperienceSummary(request.experienceSummary());
            profile.setCurrentWorkload(request.currentWorkload() != null ? request.currentWorkload() : 0);
            profile.setCompletedTaskCount(request.completedTaskCount() != null ? request.completedTaskCount() : 0);
            profile.setOverdueTaskCount(request.overdueTaskCount() != null ? request.overdueTaskCount() : 0);
            profile.setTaskCompletionRate(request.taskCompletionRate());
            profile.setOverdueRate(request.overdueRate());
            memberProfileMapper.insert(profile);
        } else {
            if (request.resumeText() != null) {
                profile.setResumeText(request.resumeText());
            }
            if (request.experienceSummary() != null) {
                profile.setExperienceSummary(request.experienceSummary());
            }
            if (request.currentWorkload() != null) {
                profile.setCurrentWorkload(request.currentWorkload());
            }
            if (request.completedTaskCount() != null) {
                profile.setCompletedTaskCount(request.completedTaskCount());
            }
            if (request.overdueTaskCount() != null) {
                profile.setOverdueTaskCount(request.overdueTaskCount());
            }
            if (request.taskCompletionRate() != null) {
                profile.setTaskCompletionRate(request.taskCompletionRate());
            }
            if (request.overdueRate() != null) {
                profile.setOverdueRate(request.overdueRate());
            }
            memberProfileMapper.updateById(profile);
        }
    }

    @Transactional
    public void saveSkills(Long userId, List<MemberSkillItemRequest> skills) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在", HttpStatus.NOT_FOUND);
        }

        List<Long> skillIds = skills.stream().map(MemberSkillItemRequest::skillId).toList();
        List<Skill> existingSkills = skillMapper.selectBatchIds(skillIds);
        if (existingSkills.size() != skillIds.size()) {
            throw new BusinessException(400, "部分技能不存在", HttpStatus.BAD_REQUEST);
        }

        userSkillMapper.delete(
            new LambdaQueryWrapper<UserSkill>().eq(UserSkill::getUserId, userId)
        );

        for (MemberSkillItemRequest item : skills) {
            UserSkill us = new UserSkill();
            us.setUserId(userId);
            us.setSkillId(item.skillId());
            us.setLevel(item.level());
            us.setYears(item.years());
            us.setDescription(item.description());
            userSkillMapper.insert(us);
        }
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

    private List<MemberSkillVO> getMemberSkills(Long userId) {
        List<UserSkill> userSkills = userSkillMapper.selectList(
            new LambdaQueryWrapper<UserSkill>().eq(UserSkill::getUserId, userId)
        );
        if (userSkills.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> skillIds = userSkills.stream().map(UserSkill::getSkillId).toList();
        Map<Long, Skill> skillMap = skillMapper.selectBatchIds(skillIds).stream()
            .collect(Collectors.toMap(Skill::getId, s -> s));

        return userSkills.stream().map(us -> {
            Skill skill = skillMap.get(us.getSkillId());
            return new MemberSkillVO(
                us.getSkillId(),
                skill != null ? skill.getSkillName() : null,
                skill != null ? skill.getCategory() : null,
                us.getLevel(),
                us.getYears(),
                us.getDescription()
            );
        }).toList();
    }

    private IPage<MemberProfileListItemVO> withEmptySkills(IPage<MemberProfileListItemVO> page) {
        List<MemberProfileListItemVO> records = page.getRecords().stream()
            .map(r -> new MemberProfileListItemVO(
                r.userId(), r.username(), r.realName(), r.email(),
                r.currentWorkload(), r.completedTaskCount(), r.overdueTaskCount(),
                r.taskCompletionRate(), r.overdueRate(), Collections.emptyList()
            ))
            .toList();
        page.setRecords(records);
        return page;
    }

    private IPage<MemberProfileListItemVO> withSkills(IPage<MemberProfileListItemVO> page,
                                                       Map<Long, List<String>> userSkillNames) {
        List<MemberProfileListItemVO> records = page.getRecords().stream()
            .map(r -> new MemberProfileListItemVO(
                r.userId(), r.username(), r.realName(), r.email(),
                r.currentWorkload(), r.completedTaskCount(), r.overdueTaskCount(),
                r.taskCompletionRate(), r.overdueRate(),
                userSkillNames.getOrDefault(r.userId(), Collections.emptyList())
            ))
            .toList();
        page.setRecords(records);
        return page;
    }
}
