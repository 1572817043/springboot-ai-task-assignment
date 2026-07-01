package com.example.taskai.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.taskai.common.exception.BusinessException;
import com.example.taskai.project.dto.ProjectCreateRequest;
import com.example.taskai.project.dto.ProjectUpdateRequest;
import com.example.taskai.project.entity.Project;
import com.example.taskai.project.entity.ProjectMember;
import com.example.taskai.project.mapper.ProjectMapper;
import com.example.taskai.project.mapper.ProjectMemberMapper;
import com.example.taskai.project.service.ProjectService;
import com.example.taskai.user.entity.SysUser;
import com.example.taskai.user.mapper.SysUserMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTests {

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private ProjectMemberMapper projectMemberMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void managerCreatesProjectForSelfEvenWhenPayloadContainsAnotherManager() {
        SysUser manager = new SysUser();
        manager.setId(2L);
        when(sysUserMapper.selectById(2L)).thenReturn(manager);
        when(projectMapper.insert(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(10L);
            return 1;
        });

        ProjectCreateRequest request = new ProjectCreateRequest(
            "新项目",
            "描述",
            99L,
            "NOT_STARTED",
            LocalDate.now(),
            LocalDate.now().plusDays(30)
        );

        projectService.createProject(request, "MANAGER", 2L);

        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectMapper).insert(projectCaptor.capture());
        assertEquals(2L, projectCaptor.getValue().getManagerId());

        ArgumentCaptor<ProjectMember> memberCaptor = ArgumentCaptor.forClass(ProjectMember.class);
        verify(projectMemberMapper).insert(memberCaptor.capture());
        assertEquals(2L, memberCaptor.getValue().getUserId());
    }

    @Test
    void managerCannotTransferProjectToAnotherManager() {
        Project project = new Project();
        project.setId(1L);
        project.setManagerId(2L);
        when(projectMapper.selectById(1L)).thenReturn(project);

        ProjectUpdateRequest request = new ProjectUpdateRequest(
            null,
            null,
            3L,
            null,
            null,
            null
        );

        BusinessException ex = assertThrows(BusinessException.class,
            () -> projectService.updateProject(1L, request, "MANAGER", 2L));
        assertEquals(403, ex.getCode());
    }
}
