package org.udg.trackdev.spring.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.udg.trackdev.spring.controller.exceptions.ServiceException;
import org.udg.trackdev.spring.entity.*;
import org.udg.trackdev.spring.entity.taskchanges.*;
import org.udg.trackdev.spring.model.MergePatchTask;
import org.udg.trackdev.spring.repository.TaskRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TaskService extends BaseServiceLong<Task, TaskRepository> {

    @Autowired
    ProjectService projectService;

    @Autowired
    UserService userService;

    @Autowired
    TaskChangeService taskChangeService;

    @Autowired
    SprintService sprintService;

    @Autowired
    CommentService commentService;

    @Autowired
    AccessChecker accessChecker;

    @Transactional
    public Task createTask(Long projectId, String name, String userId) {
        Project project = projectService.get(projectId);
        User user = userService.get(userId);
        accessChecker.checkCanViewProject(project, userId);
        Task task = new Task(name, user);
        task.setProject(project);
        project.addTask(task);
        this.repo.save(task);

        return task;
    }

    @Transactional
    public Task createSubTask(Long taskId, String name, String userId) {
        Task parentTask = this.get(taskId);
        User user = userService.get(userId);
        accessChecker.checkCanViewProject(parentTask.getProject(), userId);
        Task subtask = new Task(name, user);
        subtask.setProject(parentTask.getProject());
        subtask.setParentTask(parentTask);
        parentTask.addChildTask(subtask);
        this.repo.save(subtask);

        return subtask;
    }

    @Transactional
    public Task editTask(Long id, MergePatchTask editTask, String userId) {
        Task task = get(id);
        User user = userService.get(userId);
        accessChecker.checkCanViewProject(task.getProject(), userId);

        List<TaskChange> changes = new ArrayList<>();
        if(editTask.name != null) {
            String oldName = task.getName();
            String name = editTask.name.orElseThrow(
                    () -> new ServiceException("Not possible to set name to null"));
            task.setName(name);
            changes.add(new TaskNameChange(user, task, oldName, name));
        }
        if(editTask.assignee != null) {
            User assigneeUser = null;
            String oldValue = task.getAssignee() != null ? task.getAssignee().getUsername() : null;
            if(editTask.assignee.isPresent()) {
                assigneeUser = userService.getByUsername(editTask.assignee.get());
                if(!task.getProject().isMember(assigneeUser)) {
                    throw new ServiceException("Assignee is not in the list of possible assignees");
                }
                task.setAssignee(assigneeUser);
            } else {
                task.setAssignee(null);
            }
            changes.add(new TaskAssigneeChange(user, task, oldValue, task.getAssignee().getUsername()));
        }
        if(editTask.estimationPoints != null) {
            Integer oldPoints = task.getEstimationPoints();
            Integer points = editTask.estimationPoints.orElse(null);
            task.setEstimationPoints(points);
            changes.add(new TaskEstimationPointsChange(user, task, oldPoints, points));
        }
        if(editTask.status != null) {
            TaskStatus status = editTask.status.orElseThrow(
                    () -> new ServiceException("Not possible to set status to null"));
            task.setStatus(status, user);
        }
        if(editTask.rank != null) {
            Integer newRank = editTask.rank.orElseThrow(
                    () -> new ServiceException("Not possible to set rank to null"));
            Long backlogId = 1L; // TODO
            Integer currentRank = task.getRank();
            if(newRank != currentRank) {
                Collection<TaskChange> otherChanges = updateOtherTasksRank(backlogId, user, newRank, currentRank);
                task.setRank(newRank);
                changes.add(new TaskRankChange(user, task, newRank));
                changes.addAll(otherChanges);
            }
        }
        /**if(editTask.activeSprint != null) {
            if(editTask.rank == null || !editTask.rank.isPresent()) {
                throw new ServiceException("Is not allowed to change sprint without specifying rank");
            }
            Sprint newSprint = null;
            if(editTask.activeSprint.isPresent()) {
                Sprint oldSprint = task.getActiveSprints();
                if(oldSprint != null) {
                    oldSprint.removeTask(task, user);
                }
                Long newSprintId = editTask.activeSprint.get();
                newSprint = sprintService.get(newSprintId);
                task.setActiveSprint(newSprint);
                newSprint.addTask(task, user);
            } else {
                Sprint oldSprint = task.getActiveSprint();
                task.setActiveSprint(null);
                oldSprint.removeTask(task, user);
            }

            changes.add(new TaskActiveSprintsChange(user, task, newSprint));
        }**/
        if(editTask.activeSprints != null){
            Collection<Long> sprintsIds = editTask.activeSprints.orElseThrow(
                    () -> new ServiceException("Not possible to set activeSprints to null"));
            String oldValues = task.getActiveSprints().stream().map(Sprint::getName).collect(Collectors.joining(","));
            Collection<Sprint> sprints = sprintService.getSpritnsByIds(sprintsIds);
            String newValues = sprints.stream().map(Sprint::getName).collect(Collectors.joining(","));
            task.setActiveSprints(sprints);
            sprints.stream().forEach(sprint -> sprint.addTask(task, user));
            changes.add(new TaskActiveSprintsChange(user, task, oldValues, newValues));
        }
        if (editTask.comment != null) {
            Comment comment = editTask.comment.orElseThrow(
                    () -> new ServiceException("Not possible to set discussion to null"));
            task.addComment(commentService.addComment(comment.getContent(), userService.get(userId), task));
        }
        repo.save(task);
        for(TaskChange change: changes) {
            taskChangeService.store(change);
        }
        return task;
    }


    public Collection<Comment> getComments(Long taskId) {
        return commentService.getComments(taskId);
    }

    private Collection<TaskChange> updateOtherTasksRank(Long backlogId, User user, Integer newRank, Integer currentRank) {
        List<TaskChange> changes = new ArrayList<>();
        List<Task> tasks = new ArrayList<>();
        int increase = 0;
        if(newRank > currentRank) {
            //tasks = repo().findAllBetweenRanksOfBacklog(backlogId, currentRank +1, newRank);
            increase = -1;
        } else if (newRank < currentRank){
            //tasks = repo().findAllBetweenRanksOfBacklog(backlogId, newRank, currentRank -1);
            increase = 1;
        }
        for(Task otherTask : tasks) {
            Integer otherNewRank = otherTask.getRank() + increase;
            otherTask.setRank(otherNewRank);
            changes.add(new TaskRankChange(user, otherTask, otherNewRank));
        }

        return changes;
    }
}
