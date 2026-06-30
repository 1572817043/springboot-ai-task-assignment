package com.example.taskai.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.taskai.common.result.ApiResponse;
import com.example.taskai.user.entity.SysRole;
import com.example.taskai.user.mapper.SysRoleMapper;
import com.example.taskai.user.vo.RoleOptionVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final SysRoleMapper sysRoleMapper;

    public RoleController(SysRoleMapper sysRoleMapper) {
        this.sysRoleMapper = sysRoleMapper;
    }

    @GetMapping("/options")
    public ApiResponse<List<RoleOptionVO>> options() {
        List<SysRole> roles = sysRoleMapper.selectList(
            new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId)
        );
        List<RoleOptionVO> list = roles.stream()
            .map(r -> new RoleOptionVO(r.getId(), r.getRoleCode(), r.getRoleName()))
            .toList();
        return ApiResponse.ok(list);
    }
}
