package com.example.taskai.project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.taskai.common.exception.BusinessException;
import com.example.taskai.common.result.ApiResponse;
import com.example.taskai.config.AuthInterceptor;
import com.example.taskai.project.dto.ProjectCreateRequest;
import com.example.taskai.project.dto.ProjectMemberAddRequest;
import com.example.taskai.project.dto.ProjectStatusRequest;
import com.example.taskai.project.dto.ProjectUpdateRequest;
import com.example.taskai.project.service.ProjectService;
import com.example.taskai.project.vo.ProjectDetailVO;
import com.example.taskai.project.vo.ProjectListItemVO;
import com.example.taskai.project.vo.ProjectMemberVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ApiResponse<IPage<ProjectListItemVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        String role = getRole(request);
        Long userId = resolveUserId(request, role);
        return ApiResponse.ok(projectService.listProjects(keyword, status, page, size, role, userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectDetailVO> detail(@PathVariable Long id,
                                                HttpServletRequest request) {
        String role = getRole(request);
        Long userId = resolveUserId(request, role);
        return ApiResponse.ok(projectService.getProjectDetail(id, role, userId));
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody ProjectCreateRequest request,
                                     HttpServletRequest httpRequest) {
        String role = getRole(httpRequest);
        if ("MEMBER".equals(role)) {
            throw new BusinessException(403, "无权限创建项目", HttpStatus.FORBIDDEN);
        }
        Long userId = resolveUserId(httpRequest, role);
        projectService.createProject(request, userId);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id,
                                     @Valid @RequestBody ProjectUpdateRequest request,
                                     HttpServletRequest httpRequest) {
        String role = getRole(httpRequest);
        if ("MEMBER".equals(role)) {
            throw new BusinessException(403, "无权限编辑项目", HttpStatus.FORBIDDEN);
        }
        Long userId = resolveUserId(httpRequest, role);
        projectService.updateProject(id, request, role, userId);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                           @Valid @RequestBody ProjectStatusRequest request,
                                           HttpServletRequest httpRequest) {
        String role = getRole(httpRequest);
        if ("MEMBER".equals(role)) {
            throw new BusinessException(403, "无权限修改项目状态", HttpStatus.FORBIDDEN);
        }
        Long userId = resolveUserId(httpRequest, role);
        projectService.updateProjectStatus(id, request.status(), role, userId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/members")
    public ApiResponse<List<ProjectMemberVO>> listMembers(@PathVariable Long id,
                                                           HttpServletRequest request) {
        String role = getRole(request);
        Long userId = resolveUserId(request, role);
        return ApiResponse.ok(projectService.listMembers(id, role, userId));
    }

    @PostMapping("/{id}/members")
    public ApiResponse<Void> addMember(@PathVariable Long id,
                                        @Valid @RequestBody ProjectMemberAddRequest request,
                                        HttpServletRequest httpRequest) {
        String role = getRole(httpRequest);
        if ("MEMBER".equals(role)) {
            throw new BusinessException(403, "无权限操作项目成员", HttpStatus.FORBIDDEN);
        }
        Long userId = resolveUserId(httpRequest, role);
        projectService.addMember(id, request, role, userId);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ApiResponse<Void> removeMember(@PathVariable Long id,
                                           @PathVariable Long userId,
                                           HttpServletRequest httpRequest) {
        String role = getRole(httpRequest);
        if ("MEMBER".equals(role)) {
            throw new BusinessException(403, "无权限操作项目成员", HttpStatus.FORBIDDEN);
        }
        Long currentUserId = resolveUserId(httpRequest, role);
        projectService.removeMember(id, userId, role, currentUserId);
        return ApiResponse.ok(null);
    }

    private String getRole(HttpServletRequest request) {
        return (String) request.getAttribute(AuthInterceptor.ATTR_ROLE);
    }

    private Long resolveUserId(HttpServletRequest request, String role) {
        String username = (String) request.getAttribute(AuthInterceptor.ATTR_USERNAME);
        return projectService.resolveUserId(username);
    }
}
