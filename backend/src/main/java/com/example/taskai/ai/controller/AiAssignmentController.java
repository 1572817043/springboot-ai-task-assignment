package com.example.taskai.ai.controller;

import com.example.taskai.ai.service.AiAssignmentService;
import com.example.taskai.ai.vo.AiRecommendationResponse;
import com.example.taskai.common.result.ApiResponse;
import com.example.taskai.config.AuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-assignment")
public class AiAssignmentController {

    private final AiAssignmentService aiAssignmentService;

    public AiAssignmentController(AiAssignmentService aiAssignmentService) {
        this.aiAssignmentService = aiAssignmentService;
    }

    @PostMapping("/tasks/{taskId}/recommend")
    public ApiResponse<AiRecommendationResponse> recommend(@PathVariable Long taskId,
                                                            HttpServletRequest request) {
        String role = getRole(request);
        Long userId = aiAssignmentService.resolveUserId(getUsername(request));
        return ApiResponse.ok(aiAssignmentService.recommend(taskId, role, userId));
    }

    @GetMapping("/tasks/{taskId}/recommendations/latest")
    public ApiResponse<AiRecommendationResponse> latest(@PathVariable Long taskId,
                                                         HttpServletRequest request) {
        String role = getRole(request);
        Long userId = aiAssignmentService.resolveUserId(getUsername(request));
        return ApiResponse.ok(aiAssignmentService.getLatestRecommendation(taskId, role, userId));
    }

    @PatchMapping("/candidates/{candidateId}/accept")
    public ApiResponse<Void> accept(@PathVariable Long candidateId,
                                     HttpServletRequest request) {
        String role = getRole(request);
        Long userId = aiAssignmentService.resolveUserId(getUsername(request));
        aiAssignmentService.acceptCandidate(candidateId, role, userId);
        return ApiResponse.ok(null);
    }

    private String getRole(HttpServletRequest request) {
        return (String) request.getAttribute(AuthInterceptor.ATTR_ROLE);
    }

    private String getUsername(HttpServletRequest request) {
        return (String) request.getAttribute(AuthInterceptor.ATTR_USERNAME);
    }
}
