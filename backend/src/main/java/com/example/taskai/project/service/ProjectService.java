package com.example.taskai.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.taskai.common.exception.BusinessException;
import com.example.taskai.project.dto.ProjectCreateRequest;
import com.example.taskai.project.dto.ProjectMemberAddRequest;
import com.example.taskai.project.dto.ProjectUpdateRequest;
import com.example.taskai.project.entity.Project;
import com.example.taskai.project.entity.ProjectMember;
import com.example.taskai.project.mapper.ProjectMapper;
import com.example.taskai.project.mapper.ProjectMemberMapper;
import com.example.taskai.project.vo.ProjectDetailVO;
import com.example.taskai.project.vo.ProjectListItemVO;
import com.example.taskai.project.vo.ProjectMemberVO;
import com.example.taskai.user.entity.SysUser;
import com.example.taskai.user.mapper.SysUserMapper;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final SysUserMapper sysUserMapper;

    public ProjectService(ProjectMapper projectMapper,
                          ProjectMemberMapper projectMemberMapper,
                          SysUserMapper sysUserMapper) {
        this.projectMapper = projectMapper;
        this.projectMemberMapper = projectMemberMapper;
        this.sysUserMapper = sysUserMapper;
    }

    public IPage<ProjectListItemVO> listProjects(String keyword, String status, int page, int size,
                                                  String role, Long currentUserId) {
        Page<ProjectListItemVO> pageParam = new Page<>(page, size);
        Long filterUserId = "ADMIN".equals(role) ? null : currentUserId;
        return projectMapper.selectProjectPage(pageParam, keyword, status, filterUserId);
    }

    public ProjectDetailVO getProjectDetail(Long projectId, String role, Long currentUserId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(404, "项目不存在", HttpStatus.NOT_FOUND);
        }

        if (!"ADMIN".equals(role)) {
            checkProjectAccess(project, currentUserId);
        }

        return toDetailVO(project);
    }

    @Transactional
    public void createProject(ProjectCreateRequest request, String role, Long currentUserId) {
        Long managerId = "ADMIN".equals(role) && request.managerId() != null
            ? request.managerId()
            : currentUserId;
        SysUser manager = sysUserMapper.selectById(managerId);
        if (manager == null) {
            throw new BusinessException(400, "负责人不存在", HttpStatus.BAD_REQUEST);
        }

        Project project = new Project();
        project.setProjectName(request.projectName());
        project.setDescription(request.description());
        project.setManagerId(managerId);
        project.setStatus(request.status() != null ? request.status() : "NOT_STARTED");
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());
        projectMapper.insert(project);

        ProjectMember member = new ProjectMember();
        member.setProjectId(project.getId());
        member.setUserId(managerId);
        member.setProjectRole("负责人");
        projectMemberMapper.insert(member);
    }

    public void updateProject(Long projectId, ProjectUpdateRequest request,
                               String role, Long currentUserId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(404, "项目不存在", HttpStatus.NOT_FOUND);
        }

        if (!"ADMIN".equals(role)) {
            if (!project.getManagerId().equals(currentUserId)) {
                throw new BusinessException(403, "无权限编辑此项目", HttpStatus.FORBIDDEN);
            }
        }

        if (request.projectName() != null) {
            project.setProjectName(request.projectName());
        }
        if (request.description() != null) {
            project.setDescription(request.description());
        }
        if (request.managerId() != null) {
            if (!"ADMIN".equals(role) && !request.managerId().equals(project.getManagerId())) {
                throw new BusinessException(403, "无权限变更项目负责人", HttpStatus.FORBIDDEN);
            }
            SysUser manager = sysUserMapper.selectById(request.managerId());
            if (manager == null) {
                throw new BusinessException(400, "负责人不存在", HttpStatus.BAD_REQUEST);
            }
            project.setManagerId(request.managerId());
        }
        if (request.status() != null) {
            project.setStatus(request.status());
        }
        if (request.startDate() != null) {
            project.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            project.setEndDate(request.endDate());
        }
        projectMapper.updateById(project);
    }

    public void updateProjectStatus(Long projectId, String status,
                                     String role, Long currentUserId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(404, "项目不存在", HttpStatus.NOT_FOUND);
        }

        if (!"ADMIN".equals(role)) {
            if (!project.getManagerId().equals(currentUserId)) {
                throw new BusinessException(403, "无权限修改此项目状态", HttpStatus.FORBIDDEN);
            }
        }

        project.setStatus(status);
        projectMapper.updateById(project);
    }

    public List<ProjectMemberVO> listMembers(Long projectId, String role, Long currentUserId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(404, "项目不存在", HttpStatus.NOT_FOUND);
        }

        if (!"ADMIN".equals(role)) {
            checkProjectAccess(project, currentUserId);
        }

        return projectMemberMapper.selectMembersByProjectId(projectId);
    }

    @Transactional
    public void addMember(Long projectId, ProjectMemberAddRequest request,
                           String role, Long currentUserId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(404, "项目不存在", HttpStatus.NOT_FOUND);
        }

        if (!"ADMIN".equals(role)) {
            if (!project.getManagerId().equals(currentUserId)) {
                throw new BusinessException(403, "无权限操作此项目成员", HttpStatus.FORBIDDEN);
            }
        }

        SysUser user = sysUserMapper.selectById(request.userId());
        if (user == null) {
            throw new BusinessException(400, "用户不存在", HttpStatus.BAD_REQUEST);
        }

        Long count = projectMemberMapper.selectCount(
            new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, request.userId())
        );
        if (count > 0) {
            throw new BusinessException(400, "该用户已是项目成员", HttpStatus.BAD_REQUEST);
        }

        ProjectMember member = new ProjectMember();
        member.setProjectId(projectId);
        member.setUserId(request.userId());
        member.setProjectRole(request.projectRole());
        projectMemberMapper.insert(member);
    }

    public void removeMember(Long projectId, Long userId, String role, Long currentUserId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(404, "项目不存在", HttpStatus.NOT_FOUND);
        }

        if (!"ADMIN".equals(role)) {
            if (!project.getManagerId().equals(currentUserId)) {
                throw new BusinessException(403, "无权限操作此项目成员", HttpStatus.FORBIDDEN);
            }
        }

        projectMemberMapper.delete(
            new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId)
        );
    }

    public Long resolveUserId(String username) {
        SysUser user = sysUserMapper.selectOne(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (user == null) {
            throw new BusinessException(401, "用户不存在", HttpStatus.UNAUTHORIZED);
        }
        return user.getId();
    }

    private void checkProjectAccess(Project project, Long userId) {
        boolean isManager = project.getManagerId().equals(userId);
        if (isManager) {
            return;
        }
        Long memberCount = projectMemberMapper.selectCount(
            new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, project.getId())
                .eq(ProjectMember::getUserId, userId)
        );
        if (memberCount == 0) {
            throw new BusinessException(403, "无权限访问此项目", HttpStatus.FORBIDDEN);
        }
    }

    private ProjectDetailVO toDetailVO(Project project) {
        SysUser manager = sysUserMapper.selectById(project.getManagerId());
        List<ProjectMemberVO> members = projectMemberMapper.selectMembersByProjectId(project.getId());

        return new ProjectDetailVO(
            project.getId(),
            project.getProjectName(),
            project.getDescription(),
            project.getManagerId(),
            manager != null ? manager.getRealName() : null,
            project.getStatus(),
            project.getStartDate(),
            project.getEndDate(),
            project.getCreatedAt(),
            project.getUpdatedAt(),
            members
        );
    }
}
