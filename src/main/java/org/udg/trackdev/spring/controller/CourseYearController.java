package org.udg.trackdev.spring.controller;

import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.udg.trackdev.spring.controller.exceptions.ControllerException;
import org.udg.trackdev.spring.entity.*;
import org.udg.trackdev.spring.entity.views.PrivacyLevelViews;
import org.udg.trackdev.spring.service.CourseYearService;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.security.Principal;
import java.util.Set;

@RestController
@RequestMapping(path = "/courses/years")
public class CourseYearController extends BaseController {

    @Autowired
    CourseYearService courseYearService;

    @PostMapping(path = "/{yearId}/invites")
    public IdObjectLong createInvite(Principal principal,
                                    @PathVariable("yearId") Long yearId,
                                    @Valid @RequestBody NewCourseInvite inviteRequest) {
        String userId = super.getUserId(principal);
        Invite createdInvite = courseYearService.createInvite(inviteRequest.email, yearId, userId);
        return new IdObjectLong(createdInvite.getId());
    }

    @GetMapping(path = "/{yearId}/students")
    @JsonView(PrivacyLevelViews.Public.class)
    public Set<User> getStudents(Principal principal,
                                 @PathVariable("yearId") Long yearId) {
        String userId = super.getUserId(principal);
        CourseYear courseYear = courseYearService.get(yearId);
        if(!courseYear.getCourse().getOwnerId().equals(userId)) {
            throw new ControllerException("You don't have access to this resource");
        }
        return courseYear.getStudents();
    }

    @DeleteMapping(path = "/{yearId}/students/{userId}")
    public ResponseEntity getStudents(Principal principal,
                                      @PathVariable("yearId") Long yearId,
                                      @PathVariable("userId") String userId) {
        String principalUserId = super.getUserId(principal);
        courseYearService.removeStudent(yearId, userId, principalUserId);
        return okNoContent();
    }

    static class NewCourseInvite {
        @NotNull
        @Email
        @Size(max = User.EMAIL_LENGTH)
        public String email;
    }
}