package org.udg.trackdev.spring.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.udg.trackdev.spring.controller.exceptions.ServiceException;
import org.udg.trackdev.spring.entity.*;
import org.udg.trackdev.spring.repository.CourseYearRepository;

@Service
public class CourseYearService extends BaseService<CourseYear, CourseYearRepository> {

    @Autowired
    CourseService courseService;

    @Autowired
    InviteCourseBuilder courseInviteBuilder;

    @Autowired
    UserService userService;

    @Transactional
    public CourseYear createCourseYear(Long courseId, Integer startYear, String loggedInUserId) {
        Course course = courseService.getCourse(courseId);
        checkCanManageCourse(course, loggedInUserId);
        CourseYear courseYear = new CourseYear(startYear);
        courseYear.setCourse(course);
        course.addCourseYear(courseYear);
        return courseYear;
    }

    public void deleteCourseYear(Long yearId, String loggedInUserId) {
        CourseYear courseYear = get(yearId);
        checkCanManageCourseYear(courseYear, loggedInUserId);
        repo.delete(courseYear);
    }

    @Transactional
    public Invite createInvite(String email, Long yearId, String ownerId) {
        CourseYear courseYear = get(yearId);
        Invite invite = courseInviteBuilder.Build(email, ownerId, courseYear);
        return invite;
    }

    @Transactional
    public void removeStudent(Long yearId, String username, String loggedInUserId) {
        CourseYear courseYear = get(yearId);
        checkCanManageCourseYear(courseYear, loggedInUserId);
        User user = userService.getByUsername(username);
        for(Group group : courseYear.getGroups()) {
            if(group.isMember(user)) {
                group.removeMember(user);
                user.removeFromGroup(group);
            }
        }
        courseYear.removeStudent(user);
        user.removeFromCourseYear(courseYear);
    }

    private void checkCanManageCourseYear(CourseYear courseYear, String userId) {
        checkCanManageCourse(courseYear.getCourse(), userId);
    }

    private void checkCanManageCourse(Course course, String userId) {
        if(!courseService.canManageCourse(course, userId)) {
            throw new ServiceException("User cannot manage this course");
        }
    }
}