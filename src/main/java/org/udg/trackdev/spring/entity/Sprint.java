package org.udg.trackdev.spring.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.lang.NonNull;
import org.udg.trackdev.spring.controller.exceptions.EntityException;
import org.udg.trackdev.spring.entity.sprintchanges.*;
import org.udg.trackdev.spring.entity.views.EntityLevelViews;
import org.udg.trackdev.spring.serializer.JsonDateSerializer;
import org.udg.trackdev.spring.service.Global;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

@Entity
@Table(name = "sprints")
public class Sprint extends BaseEntityLong {

    //-- ATTRIBUTES

    public static final int NAME_LENGTH = 50;

    @NonNull
    private String name;

    @JsonView(EntityLevelViews.Basic.class)
    @JsonSerialize(using = JsonDateSerializer.class)
    private Date startDate;

    @JsonView(EntityLevelViews.Basic.class)
    @JsonSerialize(using = JsonDateSerializer.class)
    private Date endDate;

    @Column(name = "`status`")
    private SprintStatus status;

    @ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private Collection<Task> activeTasks = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "projectId")
    private Project project;

    @OneToMany(mappedBy = "entity", cascade = CascadeType.ALL)
    private Collection<SprintChange> sprintChanges;

    //--- CONSTRUCTOR

    public Sprint() {}

    public Sprint(String name) {
        this.name = name;
        this.status = SprintStatus.DRAFT;
    }

    //--- GETTERS AND SETTERS

    @NonNull
    @JsonView(EntityLevelViews.Basic.class)
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name, User modifier) {
        this.name = name;
        this.sprintChanges.add(new SprintNameChange(modifier, this, name));
    }

    @JsonView(EntityLevelViews.Basic.class)
    @JsonFormat(pattern = Global.SIMPLE_LOCALDATE_FORMAT)
    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate, User modifier) {
        Date oldValue = this.startDate;
        this.startDate = startDate;
        if(oldValue != null) {
            sprintChanges.add(new SprintStartDateChange(modifier, this, startDate));
        }
    }

    @JsonView(EntityLevelViews.Basic.class)
    @JsonFormat(pattern = Global.SIMPLE_LOCALDATE_FORMAT)
    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate, User modifier) {
        Date oldValue = this.endDate;
        this.endDate = endDate;
        if(oldValue != null) {
            sprintChanges.add(new SprintEndDateChange(modifier, this, endDate));
        }
    }

    @JsonView(EntityLevelViews.Basic.class)
    public SprintStatus getStatus() { return this.status; }

    public void setStatus(SprintStatus status, User modifier) {
        if(status == SprintStatus.CLOSED && !areAllTasksClosed()) {
            throw new EntityException("Cannot close sprint with open tasks");
        }
        if(status == SprintStatus.ACTIVE) {
            for(Task task : this.activeTasks) {
                if(task.getStatus() == TaskStatus.CREATED) {
                    task.setStatus(TaskStatus.TODO, modifier);
                }
            }
        }
        this.status = status;
        this.sprintChanges.add(new SprintStatusChange(modifier, this, status));
    }

    @JsonView(EntityLevelViews.Basic.class)
    public Collection<Task> getActiveTasks() {
        return this.activeTasks;
    }

    public void setActiveTasks(Collection<Task> tasks) {
        this.activeTasks = tasks;
    }

    public Project getProject() {
        return this.project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    //--- METHODS

    public void addTask(Task task, User modifier) {
        this.activeTasks.add(task);
        this.sprintChanges.add(new SprintTaskAdded(modifier, this, task));
        if(this.status == SprintStatus.ACTIVE && task.getStatus() == TaskStatus.CREATED) {
            task.setStatus(TaskStatus.TODO, modifier);
        }
    }

    public void removeTask(Task task, User modifier) {
        this.activeTasks.remove(task);
        this.sprintChanges.add(new SprintTaskRemoved(modifier, this, task));
    }

    private boolean areAllTasksClosed() {
        boolean allClosed = this.activeTasks.stream().allMatch(
                t -> t.getStatus() == TaskStatus.DONE || t.getStatus() == TaskStatus.DELETED);
        return allClosed;
    }
}
