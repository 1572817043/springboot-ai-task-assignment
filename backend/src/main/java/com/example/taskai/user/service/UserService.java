package com.example.taskai.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.taskai.common.exception.BusinessException;
import com.example.taskai.user.dto.UserCreateRequest;
import com.example.taskai.user.dto.UserPasswordRequest;
import com.example.taskai.user.dto.UserStatusRequest;
import com.example.taskai.user.dto.UserUpdateRequest;
import com.example.taskai.user.entity.SysRole;
import com.example.taskai.user.entity.SysUser;
import com.example.taskai.user.entity.SysUserRole;
import com.example.taskai.user.mapper.SysRoleMapper;
import com.example.taskai.user.mapper.SysUserMapper;
import com.example.taskai.user.mapper.SysUserRoleMapper;
import com.example.taskai.user.vo.UserDetailVO;
import com.example.taskai.user.vo.UserListItemVO;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    public UserService(SysUserMapper sysUserMapper,
                       SysRoleMapper sysRoleMapper,
                       SysUserRoleMapper sysUserRoleMapper) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
    }

    public IPage<UserListItemVO> listUsers(String keyword, String status, int page, int size) {
        Page<UserListItemVO> pageParam = new Page<>(page, size);
        return sysUserMapper.selectUsersWithRole(pageParam, keyword, status);
    }

    public UserDetailVO getUserDetail(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在", HttpStatus.NOT_FOUND);
        }
        SysRole role = sysRoleMapper.selectRoleByUserId(id);
        return new UserDetailVO(
            user.getId(),
            user.getUsername(),
            user.getRealName(),
            user.getEmail(),
            user.getPhone(),
            user.getAvatarUrl(),
            user.getStatus(),
            role != null ? role.getId() : null,
            role != null ? role.getRoleCode() : null,
            role != null ? role.getRoleName() : null,
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    @Transactional
    public void createUser(UserCreateRequest request) {
        Long count = sysUserMapper.selectCount(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.username())
        );
        if (count > 0) {
            throw new BusinessException(400, "用户名已存在", HttpStatus.BAD_REQUEST);
        }

        SysRole role = sysRoleMapper.selectById(request.roleId());
        if (role == null) {
            throw new BusinessException(400, "角色不存在", HttpStatus.BAD_REQUEST);
        }

        SysUser user = new SysUser();
        user.setUsername(request.username());
        user.setPasswordHash(request.password());
        user.setRealName(request.realName());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setStatus(request.status() != null ? request.status() : "ENABLED");
        sysUserMapper.insert(user);

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(request.roleId());
        sysUserRoleMapper.insert(userRole);
    }

    @Transactional
    public void updateUser(Long id, UserUpdateRequest request) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在", HttpStatus.NOT_FOUND);
        }

        if (request.realName() != null) {
            user.setRealName(request.realName());
        }
        if (request.email() != null) {
            user.setEmail(request.email());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.status() != null) {
            user.setStatus(request.status());
        }
        sysUserMapper.updateById(user);

        if (request.roleId() != null) {
            SysRole role = sysRoleMapper.selectById(request.roleId());
            if (role == null) {
                throw new BusinessException(400, "角色不存在", HttpStatus.BAD_REQUEST);
            }
            SysUserRole existing = sysUserRoleMapper.selectOne(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id)
            );
            if (existing != null) {
                existing.setRoleId(request.roleId());
                sysUserRoleMapper.updateById(existing);
            } else {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(id);
                userRole.setRoleId(request.roleId());
                sysUserRoleMapper.insert(userRole);
            }
        }
    }

    public void updateUserStatus(Long id, UserStatusRequest request) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在", HttpStatus.NOT_FOUND);
        }
        user.setStatus(request.status());
        sysUserMapper.updateById(user);
    }

    public void resetPassword(Long id, UserPasswordRequest request) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在", HttpStatus.NOT_FOUND);
        }
        user.setPasswordHash(request.password());
        sysUserMapper.updateById(user);
    }
}
