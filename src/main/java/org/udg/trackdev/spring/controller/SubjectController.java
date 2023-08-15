package org.udg.trackdev.spring.controller;

import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.udg.trackdev.spring.configuration.UserType;
import org.udg.trackdev.spring.entity.Subject;
import org.udg.trackdev.spring.entity.Courses;
import org.udg.trackdev.spring.entity.User;
import org.udg.trackdev.spring.model.IdObjectLong;
import org.udg.trackdev.spring.entity.views.EntityLevelViews;
import org.udg.trackdev.spring.service.AccessChecker;
import org.udg.trackdev.spring.service.SubjectService;
import org.udg.trackdev.spring.service.CourseService;
import org.udg.trackdev.spring.service.UserService;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping(path = "/subjects")
public class SubjectController extends CrudController<Subject, SubjectService> {

    @Autowired
    CourseService courseService;

    @Autowired
    AccessChecker accessChecker;

    @Autowired
    UserService userService;

    @GetMapping
    @JsonView(EntityLevelViews.CourseComplete.class)
    public List<Subject> search(Principal principal, @RequestParam(value = "search", required = false) String search) {
        String userId = super.getUserId(principal);
        User user = userService.get(userId);
        if (user.isUserType(UserType.ADMIN)){
            return super.search(search);
        }
        else {
            String refinedSearch = super.scopedSearch("ownerId:" + userId, search);
            return super.search(refinedSearch);
        }
    }

    @GetMapping(path = "/{id}")
    @JsonView(EntityLevelViews.CourseComplete.class)
    public Subject getSubject(Principal principal, @PathVariable("id") Long id) {
        Subject subject = service.getSubject(id);
        String userId = super.getUserId(principal);
        accessChecker.checkCanViewCourse(subject, userId);
        return subject;
    }

    @PostMapping
    public IdObjectLong createSubject(Principal principal, @Valid @RequestBody NewSubject subjectRequest) {
        String userId = super.getUserId(principal);
        Subject createdSubject = service.createSubject(subjectRequest.name, subjectRequest.acronym, userId);

        return new IdObjectLong(createdSubject.getId());
    }

    @PutMapping(path = "/{id}")
    @JsonView(EntityLevelViews.CourseComplete.class)
    public Subject editSubject(Principal principal,
                               @PathVariable("id") Long id,
                               @Valid @RequestBody EditSubject subjectRequest) {
        String userId = super.getUserId(principal);
        Subject modifiedSubject = service.editSubjectDetails(id, subjectRequest.name, userId);

        return modifiedSubject;
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Void> deleteSubject(Principal principal, @PathVariable("id") Long id) {
        String userId = super.getUserId(principal);
        service.deleteSubject(id, userId);

        return okNoContent();
    }

    @PostMapping(path = "/{subjectId}/courses")
    public IdObjectLong createCourse(Principal principal,
                                     @PathVariable("subjectId") Long subjectId,
                                     @Valid @RequestBody NewCourse courseRequest) {
        String userId = super.getUserId(principal);
        Courses createdCourse = courseService.createCourse(subjectId, courseRequest.startYear, userId);
        return new IdObjectLong(createdCourse.getId());
    }

    @DeleteMapping(path = "/courses/{courseId}")
    public ResponseEntity deleteCourse(Principal principal,
                                       @PathVariable("courseId") Long courseId) {
        String userId = super.getUserId(principal);
        courseService.deleteCourse(courseId, userId);
        return okNoContent();
    }

    static class NewSubject {
        @NotBlank
        @Size(max = Subject.NAME_LENGTH)
        public String name;
        public String acronym;
    }

    static class EditSubject {
        @NotBlank
        @Size(max = Subject.NAME_LENGTH)
        public String name;
    }

    static class NewCourse {
        @Min(value = 2020)
        @Max(value = 3000)
        public Integer startYear;
    }
}