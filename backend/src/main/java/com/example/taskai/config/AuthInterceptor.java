package com.example.taskai.config;

import com.example.taskai.auth.service.TokenService;
import com.example.taskai.common.exception.BusinessException;
import com.example.taskai.common.result.ApiResponse;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_USERNAME = "currentUsername";
    public static final String ATTR_ROLE = "currentRole";

    private final TokenService tokenService;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(TokenService tokenService, ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        if (isExcluded(path, method)) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            writeError(response, HttpStatus.UNAUTHORIZED, 401, "未登录或登录已过期");
            return false;
        }

        String token = authorization.substring("Bearer ".length());
        TokenService.TokenPayload payload;
        try {
            payload = tokenService.parseToken(token);
        } catch (BusinessException e) {
            writeError(response, HttpStatus.valueOf(e.getStatus().value()), e.getCode(), e.getMessage());
            return false;
        }

        request.setAttribute(ATTR_USERNAME, payload.username());
        request.setAttribute(ATTR_ROLE, payload.role());

        if (!"ADMIN".equals(payload.role())) {
            if (path.startsWith("/api/users")) {
                writeError(response, HttpStatus.FORBIDDEN, 403, "无权限访问");
                return false;
            }
            if (path.startsWith("/api/skills") && !"GET".equalsIgnoreCase(method)) {
                writeError(response, HttpStatus.FORBIDDEN, 403, "无权限访问");
                return false;
            }
            if ("MEMBER".equals(payload.role()) && "GET".equalsIgnoreCase(method) && "/api/member-profiles".equals(path)) {
                writeError(response, HttpStatus.FORBIDDEN, 403, "无权限访问");
                return false;
            }
        }

        return true;
    }

    private boolean isExcluded(String path, String method) {
        if ("POST".equals(method) && "/api/auth/login".equals(path)) {
            return true;
        }
        if ("GET".equals(method) && "/api/system/health".equals(path)) {
            return true;
        }
        return false;
    }

    private void writeError(HttpServletResponse response, HttpStatus status, int code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new ApiResponse<>(code, message, null));
    }
}
