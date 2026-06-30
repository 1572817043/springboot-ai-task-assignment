package com.example.taskai.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.taskai.auth.dto.LoginRequest;
import com.example.taskai.auth.vo.CurrentUserVO;
import com.example.taskai.auth.vo.LoginResponse;
import com.example.taskai.common.exception.BusinessException;
import com.example.taskai.user.entity.SysRole;
import com.example.taskai.user.entity.SysUser;
import com.example.taskai.user.mapper.SysRoleMapper;
import com.example.taskai.user.mapper.SysUserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

	private final TokenService tokenService;
	private final SysUserMapper sysUserMapper;
	private final SysRoleMapper sysRoleMapper;

	public AuthService(TokenService tokenService, SysUserMapper sysUserMapper, SysRoleMapper sysRoleMapper) {
		this.tokenService = tokenService;
		this.sysUserMapper = sysUserMapper;
		this.sysRoleMapper = sysRoleMapper;
	}

	public LoginResponse login(LoginRequest request) {
		SysUser user = sysUserMapper.selectOne(
			new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.username())
		);
		if (user == null || !user.getPasswordHash().equals(request.password())) {
			throw new BusinessException(401, "账号或密码错误", HttpStatus.UNAUTHORIZED);
		}
		if (!"ENABLED".equals(user.getStatus())) {
			throw new BusinessException(403, "账号已被禁用", HttpStatus.FORBIDDEN);
		}
		SysRole role = sysRoleMapper.selectRoleByUserId(user.getId());
		String roleCode = role != null ? role.getRoleCode() : "MEMBER";
		String token = tokenService.createToken(user.getUsername(), roleCode);
		return new LoginResponse(token, new CurrentUserVO(user.getUsername(), user.getRealName(), roleCode));
	}

	public CurrentUserVO currentUser(String authorization) {
		String bearer = readBearerToken(authorization);
		TokenService.TokenPayload payload = tokenService.parseToken(bearer);
		SysUser user = sysUserMapper.selectOne(
			new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, payload.username())
		);
		if (user == null) {
			throw new BusinessException(401, "未登录或登录已过期", HttpStatus.UNAUTHORIZED);
		}
		if (!"ENABLED".equals(user.getStatus())) {
			throw new BusinessException(403, "账号已被禁用", HttpStatus.FORBIDDEN);
		}
		SysRole role = sysRoleMapper.selectRoleByUserId(user.getId());
		String roleCode = role != null ? role.getRoleCode() : "MEMBER";
		return new CurrentUserVO(user.getUsername(), user.getRealName(), roleCode);
	}

	private String readBearerToken(String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			throw new BusinessException(401, "未登录或登录已过期", HttpStatus.UNAUTHORIZED);
		}
		return authorization.substring("Bearer ".length());
	}
}
