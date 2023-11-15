package org.udg.trackdev.spring.controller;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.udg.trackdev.spring.entity.Project;
import org.udg.trackdev.spring.entity.Sprint;
import org.udg.trackdev.spring.entity.Task;
import org.udg.trackdev.spring.entity.views.EntityLevelViews;
import org.udg.trackdev.spring.service.AccessChecker;
import org.udg.trackdev.spring.service.ProjectService;
import org.udg.trackdev.spring.service.UserService;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.security.Principal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "5. Projects")
@RestController
@RequestMapping("/projects")
public class ProjectController extends BaseController {
    @Autowired
    ProjectService service;

    @Autowired
    UserService userService;

    @Autowired
    AccessChecker accessChecker;

    //TODO: Get enrolled projects
    @GetMapping
    @JsonView(EntityLevelViews.Basic.class)
    public Collection<Project> getProjects(Principal principal) {
        String userId = super.getUserId(principal);
        accessChecker.checkCanViewAllProjects(userId);
        return service.findAll();
    }

    @GetMapping(path = "/{projectId}")
    @JsonView(EntityLevelViews.ProjectComplete.class)
    public Project getProject(Principal principal, @PathVariable(name = "projectId") Long projectId) {
        String userId = super.getUserId(principal);
        Project project = service.get(projectId);
        accessChecker.checkCanViewProject(project, userId);
        userService.setCurrentProject(userService.get(userId), project);
        return project;
    }

    @PatchMapping(path = "/{projectId}")
    @JsonView(EntityLevelViews.Basic.class)
    public Project editProject(Principal principal,
                               @PathVariable(name = "projectId") Long projectId,
                               @Valid @RequestBody EditProject projectRequest) {
        String userId = super.getUserId(principal);
        Project modifiedProject = service.editProject(projectId, projectRequest.name, projectRequest.members, userId);
        return modifiedProject;
    }

    @DeleteMapping(path = "/{projectId}")
    @JsonView(EntityLevelViews.Basic.class)
    public ResponseEntity deleteProject(Principal principal,
                                        @PathVariable(name = "projectId") Long projectId) {
        String userId = super.getUserId(principal);
        service.deleteProject(projectId, userId);
        return okNoContent();
    }

    @GetMapping(path = "/{projectId}/tasks")
    @JsonView(EntityLevelViews.Basic.class)
    public Collection<Task> getProjectTasks(Principal principal,
                                            @PathVariable(name = "projectId") Long projectId) {
        String userId = super.getUserId(principal);
        Project project = service.get(projectId);
        accessChecker.checkCanViewProject(project, userId);
        return service.getProjectTasks(project);
    }

    @PostMapping(path = "/{projectId}/sprints")
    @JsonView(EntityLevelViews.Basic.class)
    public ResponseEntity createProjectSprint(Principal principal,
                                       @PathVariable(name = "projectId") Long projectId,
                                       @Valid @RequestBody CreateSprint sprintRequest) {
        String userId = super.getUserId(principal);
        Project project = service.get(projectId);
        service.createSprint(project, sprintRequest.name, sprintRequest.startDate, sprintRequest.endDate, userId);
        return okNoContent();
    }

    //TODO: Modificar a Optionals
    static class EditProject {
        @Size(min = 1, max = Project.NAME_LENGTH)
        public String name;
        public Collection<String> members;
    }

    static class CreateSprint {
        @Size(min = 1, max = Sprint.NAME_LENGTH)
        public String name;
        public Date startDate;
        public Date endDate;
    }
}