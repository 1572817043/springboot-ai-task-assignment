package com.example.taskai.skill.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.taskai.common.exception.BusinessException;
import com.example.taskai.skill.dto.SkillCreateRequest;
import com.example.taskai.skill.dto.SkillUpdateRequest;
import com.example.taskai.skill.entity.Skill;
import com.example.taskai.skill.entity.TaskRequiredSkill;
import com.example.taskai.skill.entity.UserSkill;
import com.example.taskai.skill.mapper.SkillMapper;
import com.example.taskai.skill.mapper.TaskRequiredSkillMapper;
import com.example.taskai.skill.mapper.UserSkillMapper;
import com.example.taskai.skill.vo.SkillDetailVO;
import com.example.taskai.skill.vo.SkillListItemVO;
import com.example.taskai.skill.vo.SkillOptionVO;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SkillService {

    private final SkillMapper skillMapper;
    private final UserSkillMapper userSkillMapper;
    private final TaskRequiredSkillMapper taskRequiredSkillMapper;

    public SkillService(SkillMapper skillMapper,
                        UserSkillMapper userSkillMapper,
                        TaskRequiredSkillMapper taskRequiredSkillMapper) {
        this.skillMapper = skillMapper;
        this.userSkillMapper = userSkillMapper;
        this.taskRequiredSkillMapper = taskRequiredSkillMapper;
    }

    public IPage<SkillListItemVO> listSkills(String keyword, String category, int page, int size) {
        Page<SkillListItemVO> pageParam = new Page<>(page, size);
        return skillMapper.selectSkillPage(pageParam, keyword, category);
    }

    public SkillDetailVO getSkillDetail(Long id) {
        Skill skill = skillMapper.selectById(id);
        if (skill == null) {
            throw new BusinessException(404, "技能不存在", HttpStatus.NOT_FOUND);
        }
        return new SkillDetailVO(
            skill.getId(),
            skill.getSkillName(),
            skill.getCategory(),
            skill.getDescription(),
            skill.getCreatedAt(),
            skill.getUpdatedAt()
        );
    }

    public List<SkillOptionVO> listOptions() {
        List<Skill> skills = skillMapper.selectList(
            new LambdaQueryWrapper<Skill>().select(Skill::getId, Skill::getSkillName, Skill::getCategory)
                .orderByAsc(Skill::getId)
        );
        return skills.stream()
            .map(s -> new SkillOptionVO(s.getId(), s.getSkillName(), s.getCategory()))
            .toList();
    }

    public void createSkill(SkillCreateRequest request) {
        Long count = skillMapper.selectCount(
            new LambdaQueryWrapper<Skill>().eq(Skill::getSkillName, request.skillName())
        );
        if (count > 0) {
            throw new BusinessException(400, "技能名称已存在", HttpStatus.BAD_REQUEST);
        }

        Skill skill = new Skill();
        skill.setSkillName(request.skillName());
        skill.setCategory(request.category());
        skill.setDescription(request.description());
        skillMapper.insert(skill);
    }

    public void updateSkill(Long id, SkillUpdateRequest request) {
        Skill skill = skillMapper.selectById(id);
        if (skill == null) {
            throw new BusinessException(404, "技能不存在", HttpStatus.NOT_FOUND);
        }

        if (request.skillName() != null && !request.skillName().equals(skill.getSkillName())) {
            Long count = skillMapper.selectCount(
                new LambdaQueryWrapper<Skill>().eq(Skill::getSkillName, request.skillName())
            );
            if (count > 0) {
                throw new BusinessException(400, "技能名称已存在", HttpStatus.BAD_REQUEST);
            }
            skill.setSkillName(request.skillName());
        }
        if (request.category() != null) {
            skill.setCategory(request.category());
        }
        if (request.description() != null) {
            skill.setDescription(request.description());
        }
        skillMapper.updateById(skill);
    }

    public void deleteSkill(Long id) {
        Skill skill = skillMapper.selectById(id);
        if (skill == null) {
            throw new BusinessException(404, "技能不存在", HttpStatus.NOT_FOUND);
        }

        Long userSkillCount = userSkillMapper.selectCount(
            new LambdaQueryWrapper<UserSkill>().eq(UserSkill::getSkillId, id)
        );
        if (userSkillCount > 0) {
            throw new BusinessException(400, "该技能已被成员使用，无法删除", HttpStatus.BAD_REQUEST);
        }

        Long taskSkillCount = taskRequiredSkillMapper.selectCount(
            new LambdaQueryWrapper<TaskRequiredSkill>().eq(TaskRequiredSkill::getSkillId, id)
        );
        if (taskSkillCount > 0) {
            throw new BusinessException(400, "该技能已被任务引用，无法删除", HttpStatus.BAD_REQUEST);
        }

        skillMapper.deleteById(id);
    }
}
