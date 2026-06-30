package com.example.taskai.knowledge.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.taskai.common.exception.BusinessException;
import com.example.taskai.common.result.ApiResponse;
import com.example.taskai.config.AuthInterceptor;
import com.example.taskai.knowledge.dto.KnowledgeIndexedRequest;
import com.example.taskai.knowledge.service.AiKnowledgeService;
import com.example.taskai.knowledge.vo.KnowledgeDocumentVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-knowledge")
public class AiKnowledgeController {

    private final AiKnowledgeService aiKnowledgeService;

    public AiKnowledgeController(AiKnowledgeService aiKnowledgeService) {
        this.aiKnowledgeService = aiKnowledgeService;
    }

    @PostMapping("/sync/member/{userId}/resume")
    public ApiResponse<Void> syncResume(@PathVariable Long userId,
                                         HttpServletRequest request) {
        String role = getRole(request);
        if ("MEMBER".equals(role)) {
            Long currentUserId = aiKnowledgeService.resolveUserId(getUsername(request));
            if (!currentUserId.equals(userId)) {
                throw new BusinessException(403, "只能同步自己的简历", HttpStatus.FORBIDDEN);
            }
        }
        aiKnowledgeService.syncMemberResume(userId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/sync/tasks/{taskId}/result")
    public ApiResponse<Void> syncTaskResult(@PathVariable Long taskId,
                                             HttpServletRequest request) {
        String role = getRole(request);
        if ("MEMBER".equals(role)) {
            throw new BusinessException(403, "无权限操作", HttpStatus.FORBIDDEN);
        }
        aiKnowledgeService.syncTaskResult(taskId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/documents")
    public ApiResponse<IPage<KnowledgeDocumentVO>> list(
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer indexed,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(aiKnowledgeService.listDocuments(sourceType, keyword, indexed, page, size));
    }

    @GetMapping("/documents/{id}")
    public ApiResponse<KnowledgeDocumentVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(aiKnowledgeService.getDocument(id));
    }

    @PatchMapping("/documents/{id}/indexed")
    public ApiResponse<Void> updateIndexed(@PathVariable Long id,
                                            @Valid @RequestBody KnowledgeIndexedRequest request,
                                            HttpServletRequest httpRequest) {
        String role = getRole(httpRequest);
        if ("MEMBER".equals(role)) {
            throw new BusinessException(403, "无权限操作", HttpStatus.FORBIDDEN);
        }
        aiKnowledgeService.updateIndexed(id, request.indexed());
        return ApiResponse.ok(null);
    }

    private String getRole(HttpServletRequest request) {
        return (String) request.getAttribute(AuthInterceptor.ATTR_ROLE);
    }

    private String getUsername(HttpServletRequest request) {
        return (String) request.getAttribute(AuthInterceptor.ATTR_USERNAME);
    }
}
