package com.example.taskai.skill.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.taskai.common.result.ApiResponse;
import com.example.taskai.skill.dto.SkillCreateRequest;
import com.example.taskai.skill.dto.SkillUpdateRequest;
import com.example.taskai.skill.service.SkillService;
import com.example.taskai.skill.vo.SkillDetailVO;
import com.example.taskai.skill.vo.SkillListItemVO;
import com.example.taskai.skill.vo.SkillOptionVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping
    public ApiResponse<IPage<SkillListItemVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(skillService.listSkills(keyword, category, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<SkillDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(skillService.getSkillDetail(id));
    }

    @GetMapping("/options")
    public ApiResponse<List<SkillOptionVO>> options() {
        return ApiResponse.ok(skillService.listOptions());
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody SkillCreateRequest request) {
        skillService.createSkill(request);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @RequestBody SkillUpdateRequest request) {
        skillService.updateSkill(id, request);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        skillService.deleteSkill(id);
        return ApiResponse.ok(null);
    }
}
