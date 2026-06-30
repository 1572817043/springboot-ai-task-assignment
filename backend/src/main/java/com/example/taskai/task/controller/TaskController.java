package com.example.taskai.task.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.taskai.common.exception.BusinessException;
import com.example.taskai.common.result.ApiResponse;
import com.example.taskai.config.AuthInterceptor;
import com.example.taskai.task.dto.TaskAssignRequest;
import com.example.taskai.task.dto.TaskCreateRequest;
import com.example.taskai.task.dto.TaskResultReviewRequest;
import com.example.taskai.task.dto.TaskResultSubmitRequest;
import com.example.taskai.task.dto.TaskStatusRequest;
import com.example.taskai.task.dto.TaskUpdateRequest;
import com.example.taskai.task.service.TaskService;
import com.example.taskai.task.vo.TaskDetailVO;
import com.example.taskai.task.vo.TaskListItemVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ApiResponse<IPage<TaskListItemVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        String role = getRole(request);
        Long userId = taskService.resolveUserId(getUsername(request));
        return ApiResponse.ok(taskService.listTasks(keyword, projectId, status, priority,
            assigneeId, page, size, role, userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<TaskDetailVO> detail(@PathVariable Long id,
                                             HttpServletRequest request) {
        String role = getRole(request);
        Long userId = taskService.resolveUserId(getUsername(request));
        return ApiResponse.ok(taskService.getTaskDetail(id, role, userId));
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody TaskCreateRequest request,
                                     HttpServletRequest httpRequest) {
        String role = getRole(httpRequest);
        if ("MEMBER".equals(role)) {
            throw new BusinessException(403, "无权限创建任务", HttpStatus.FORBIDDEN);
        }
        Long userId = taskService.resolveUserId(getUsername(httpRequest));
        taskService.createTask(request, userId);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id,
                                     @Valid @RequestBody TaskUpdateRequest request,
                                     HttpServletRequest httpRequest) {
        String role = getRole(httpRequest);
        if ("MEMBER".equals(role)) {
            throw new BusinessException(403, "无权限编辑任务", HttpStatus.FORBIDDEN);
        }
        Long userId = taskService.resolveUserId(getUsername(httpRequest));
        taskService.updateTask(id, request, role, userId);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                           @Valid @RequestBody TaskStatusRequest request,
                                           HttpServletRequest httpRequest) {
        String role = getRole(httpRequest);
        Long userId = taskService.resolveUserId(getUsername(httpRequest));
        taskService.updateTaskStatus(id, request.status(), request.remark(), role, userId);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/{id}/assignee")
    public ApiResponse<Void> assign(@PathVariable Long id,
                                     @Valid @RequestBody TaskAssignRequest request,
                                     HttpServletRequest httpRequest) {
        String role = getRole(httpRequest);
        if ("MEMBER".equals(role)) {
            throw new BusinessException(403, "无权限分配任务", HttpStatus.FORBIDDEN);
        }
        Long userId = taskService.resolveUserId(getUsername(httpRequest));
        taskService.assignTask(id, request, role, userId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/result")
    public ApiResponse<Void> submitResult(@PathVariable Long id,
                                           @Valid @RequestBody TaskResultSubmitRequest request,
                                           HttpServletRequest httpRequest) {
        String role = getRole(httpRequest);
        Long userId = taskService.resolveUserId(getUsername(httpRequest));
        taskService.submitResult(id, request, role, userId);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/{id}/result/review")
    public ApiResponse<Void> reviewResult(@PathVariable Long id,
                                           @Valid @RequestBody TaskResultReviewRequest request,
                                           HttpServletRequest httpRequest) {
        String role = getRole(httpRequest);
        if ("MEMBER".equals(role)) {
            throw new BusinessException(403, "无权限验收任务", HttpStatus.FORBIDDEN);
        }
        Long userId = taskService.resolveUserId(getUsername(httpRequest));
        taskService.reviewResult(id, request, role, userId);
        return ApiResponse.ok(null);
    }

    private String getRole(HttpServletRequest request) {
        return (String) request.getAttribute(AuthInterceptor.ATTR_ROLE);
    }

    private String getUsername(HttpServletRequest request) {
        return (String) request.getAttribute(AuthInterceptor.ATTR_USERNAME);
    }
}
