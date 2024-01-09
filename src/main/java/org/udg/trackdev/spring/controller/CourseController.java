package org.udg.trackdev.spring.controller;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.udg.trackdev.spring.configuration.UserType;
import org.udg.trackdev.spring.entity.Course;
import org.udg.trackdev.spring.entity.Project;
import org.udg.trackdev.spring.entity.User;
import org.udg.trackdev.spring.entity.views.EntityLevelViews;
import org.udg.trackdev.spring.model.IdObjectLong;
import org.udg.trackdev.spring.service.AccessChecker;
import org.udg.trackdev.spring.service.CourseService;
import org.udg.trackdev.spring.service.ProjectService;
import org.udg.trackdev.spring.service.UserService;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "4. Courses")
@RestController
@RequestMapping(path = "/courses")
public class CourseController extends BaseController {

    @Autowired
    CourseService service;

    @Autowired
    ProjectService projectService;

    @Autowired
    UserService userService;

    @Autowired
    AccessChecker accessChecker;

    @Operation(summary = "Get all courses", description = "Get all courses")
    @GetMapping
    @JsonView(EntityLevelViews.CourseComplete.class)
    public Collection<Course> getCourses(Principal principal) {
        String userId = super.getUserId(principal);
        User user = userService.get(userId);
        if (user.isUserType(UserType.ADMIN))
            return service.getAll();
        else
            throw new IllegalArgumentException("Unknown user role: " + user.getRoles());
    }

    @Operation(summary = "Get specific course", description = "Get specific course")
    @GetMapping(path = "/{courseId}")
    @JsonView(EntityLevelViews.CourseComplete.class)
    public Course getCourse(Principal principal, @PathVariable("courseId") Long courseId) {
        String userId = super.getUserId(principal);
        Course course = service.get(courseId);
        accessChecker.checkCanViewCourse(course, userId);
        return course;
    }

    @Operation(summary = "Edit specific course", description = "Edit specific course")
    @PatchMapping(path = "/{courseId}")
    @JsonView(EntityLevelViews.CourseComplete.class)
    public Course editCourse(Principal principal,
                             @PathVariable("courseId") Long courseId,
                             @Valid @RequestBody EditCourse courseRequest) {
        String userId = super.getUserId(principal);
        return service.editCourse(courseId, courseRequest.startYear, courseRequest.subjectId,
                courseRequest.githubOrganization, userId);
    }

    @Operation(summary = "Delete specific course", description = "Delete specific course")
    @DeleteMapping(path = "/{courseId}")
    public ResponseEntity<Void> deleteCourse(Principal principal,
                                       @PathVariable("courseId") Long courseId) {
        String userId = super.getUserId(principal);
        service.deleteCourse(courseId, userId);
        return okNoContent();
    }

    @Operation(summary = "Get projects enrolled to specific course", description = "Get projects enrolled to specific course")
    @GetMapping(path = "/{courseId}/projects")
    @JsonView(EntityLevelViews.Basic.class)
    public Collection<Project> getProjects(Principal principal,
                                           @PathVariable("courseId") Long courseId) {
        String userId = super.getUserId(principal);
        Course course = service.get(courseId);
        Collection<Project> projects;
        if(accessChecker.canViewCourseAllProjects(course, userId)) {
            projects = course.getProjects();
        } else {
            projects = course.getProjects().stream()
                    .filter(group -> group.isMember(userId))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        return projects;
    }

    @Operation(summary = "Create project enrolled to specific course", description = "Create project enrolled to specific course")
    @PostMapping(path = "/{courseId}/projects")
    public IdObjectLong createProject(Principal principal,
                                      @PathVariable("courseId") Long courseId,
                                      @Valid @RequestBody NewProject projectRequest) {
        String userId = super.getUserId(principal);
        Project createdProject = projectService.createProject(projectRequest.name, projectRequest.members, courseId, userId);
        return new IdObjectLong(createdProject.getId());
    }


    static class NewProject {
        @NotBlank
        @Size(max = Project.NAME_LENGTH)
        public String name;
        public Collection<String> members;
    }

    static class EditCourse {
        @Min(value = 2020)
        @Max(value = 3000)
        public Integer startYear;
        public Long subjectId;
        public String githubOrganization;
    }
}