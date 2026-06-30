package com.example.taskai.member.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.taskai.common.exception.BusinessException;
import com.example.taskai.common.result.ApiResponse;
import com.example.taskai.config.AuthInterceptor;
import com.example.taskai.member.dto.MemberProfileUpdateRequest;
import com.example.taskai.member.dto.MemberSkillSaveRequest;
import com.example.taskai.member.service.MemberProfileService;
import com.example.taskai.member.vo.MemberProfileDetailVO;
import com.example.taskai.member.vo.MemberProfileListItemVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/member-profiles")
public class MemberProfileController {

    private final MemberProfileService memberProfileService;

    public MemberProfileController(MemberProfileService memberProfileService) {
        this.memberProfileService = memberProfileService;
    }

    @GetMapping
    public ApiResponse<IPage<MemberProfileListItemVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long skillId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(memberProfileService.listProfiles(keyword, skillId, page, size));
    }

    @GetMapping("/{userId}")
    public ApiResponse<MemberProfileDetailVO> detail(@PathVariable Long userId,
                                                      HttpServletRequest request) {
        checkMemberAccess(userId, request);
        return ApiResponse.ok(memberProfileService.getProfileDetail(userId));
    }

    @PutMapping("/{userId}")
    public ApiResponse<Void> update(@PathVariable Long userId,
                                    @Valid @RequestBody MemberProfileUpdateRequest request,
                                    HttpServletRequest httpRequest) {
        checkMemberAccess(userId, httpRequest);
        memberProfileService.updateProfile(userId, request);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{userId}/skills")
    public ApiResponse<Void> saveSkills(@PathVariable Long userId,
                                        @Valid @RequestBody MemberSkillSaveRequest request,
                                        HttpServletRequest httpRequest) {
        checkMemberAccess(userId, httpRequest);
        memberProfileService.saveSkills(userId, request.skills());
        return ApiResponse.ok(null);
    }

    private void checkMemberAccess(Long userId, HttpServletRequest request) {
        String role = (String) request.getAttribute(AuthInterceptor.ATTR_ROLE);
        if ("MEMBER".equals(role)) {
            String username = (String) request.getAttribute(AuthInterceptor.ATTR_USERNAME);
            Long currentUserId = memberProfileService.resolveUserId(username);
            if (!currentUserId.equals(userId)) {
                throw new BusinessException(403, "无权限访问", HttpStatus.FORBIDDEN);
            }
        }
    }
}
