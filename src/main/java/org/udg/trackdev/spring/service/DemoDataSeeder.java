package org.udg.trackdev.spring.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.udg.trackdev.spring.configuration.UserType;
import org.udg.trackdev.spring.entity.*;
import org.udg.trackdev.spring.model.MergePatchSprint;
import org.udg.trackdev.spring.model.MergePatchTask;

import java.time.LocalDate;
import java.util.*;

@Component
public class DemoDataSeeder {

    private final Logger logger = LoggerFactory.getLogger(Global.class);
    private final List<Integer> possibleEstimationPoints = Arrays.asList(1, 2, 3, 5, 8, 13, 20, 40, 100);

    @Autowired
    Global global;

    @Autowired
    private UserService userService;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private SprintService sprintService;

    @Autowired
    private TaskService taskService;

    public void seedDemoData() {
        logger.info("Starting populating database ...");
        // users
        User admin = userService.addUserInternal("professor1", "professor1@trackdev.com", global.getPasswordEncoder().encode("123456"), List.of(UserType.ADMIN, UserType.PROFESSOR));
        User student1 = userService.addUserInternal("student1", "student1@trackdev.com", global.getPasswordEncoder().encode("0000"), List.of(UserType.STUDENT));
        User student2 = userService.addUserInternal("student2", "student2@trackdev.com", global.getPasswordEncoder().encode("2222"), List.of(UserType.STUDENT));
        User professor2 = userService.addUserInternal("professor2", "professor2@trackdev.com", global.getPasswordEncoder().encode("123456"), List.of(UserType.PROFESSOR));
        User test_nou = userService.addUserInternal("test_nou", "test_nou@gmail.com", global.getPasswordEncoder().encode("123456"), List.of(UserType.STUDENT));
        User test_random_pass = userService.addUserInternal("test1","test.randompass@gmail.com",global.getPasswordEncoder().encode(RandomStringUtils.randomAscii(8)), List.of(UserType.STUDENT));
        User gerard = userService.addUserInternal("gerard", "gerard.rovellat@gmail.com", global.getPasswordEncoder().encode("admin"), List.of(UserType.ADMIN));

        List<User> enrolledStudents = createDemoStudents();
        enrolledStudents.add(student1);
        enrolledStudents.add(student2);

        // course
        Subject subject = subjectService.createSubject("PDS2024","PDS" ,admin.getId());
        Course course = courseService.createCourse(subject.getId(), 2021, admin.getId());

        // one subject set up
        populateGroup(admin, course, "Movie reviews", Arrays.asList(student1, student2));
        populateGroup(admin, course, "Calendar", enrolledStudents.subList(0,4));
        logger.info("Done populating database");
    }

    private void populateGroup(User admin, Course course, String groupName, List<User> users) {
        List<String> usernames = new ArrayList<>();
        for(User user: users) {
            usernames.add(user.getUsername());
            courseService.addStudent(course.getId(), user.getUsername(), admin.getId());
        }

        Project project = projectService.createProject(groupName, usernames, course.getId(), admin.getId());

        Random random = new Random();
        LocalDate start = LocalDate.of(2021,3,1);
        LocalDate end = start.plusDays(14);
        //populatePastSprint(backlog.getId(), "First iteration", start, end, users, true);
        start = end;
        end = start.plusDays(14);
        //populatePastSprint(backlog.getId(), "Second iteration", start, end, users, true);
        start = end;
        end = start.plusDays(14);
        //populatePastSprint(backlog.getId(), "Third iteration", start, end, users, false);

        for(int i = 0; i <= 15; i++) {
            User reporter = users.get(random.nextInt(users.size()));
            Task task = taskService.createTask(project.getId(), "Lorem ipsum dolor sit amet", reporter.getId());

            if(random.nextBoolean()) {
                MergePatchTask editTask = buildBacklogEditTask(users, random);
                taskService.editTask(task.getId(), editTask, reporter.getId());
            }
            if(i == 0) {
                taskService.createSubTask(task.getId(), "Create endpoint GET /items", reporter.getId());
                taskService.createSubTask(task.getId(), "Show list of items in UI", reporter.getId());
                taskService.createSubTask(task.getId(), "Style items", reporter.getId());
            }
        }
    }

    /**private void populatePastSprint(Long backlogId, String name, LocalDate start, LocalDate end, List<User> users, boolean close) {
        Random random = new Random();
        User sprintCreator = users.get(random.nextInt(users.size()));
        //Sprint sprint = sprintService.create(name, start, end, sprintCreator.getId());

        List<Task> tasks = createTasks(backlogId, 5, users, random);
        User editor = users.get(random.nextInt(users.size()));
        int rank = 1;
        for(Task task : tasks) {
            // Add to sprint
            MergePatchTask change = new MergePatchTask();
            Collection<Long> sprints = new ArrayList<>();
            sprints.add(sprint.getId());
            change.activeSprints = Optional.of(sprints);
            change.rank = Optional.of(rank);
            taskService.editTask(task.getId(), change, editor.getId());

            // Random change
            MergePatchTask editTask = buildBacklogEditTask(users, random);
            taskService.editTask(task.getId(), editTask, editor.getId());

            rank++;
        }
        saveOpenSprint(sprintCreator, sprint);

        if(close) {
            for(Task task : tasks) {
                if(task.getStatus() != TaskStatus.DONE) {
                    MergePatchTask change = new MergePatchTask();
                    change.status = Optional.of(TaskStatus.DONE);
                    taskService.editTask(task.getId(), change, editor.getId());
                }
            }
            saveCloseSprint(sprintCreator, sprint);
        } else {
            for(Task task : tasks) {
                if(random.nextBoolean()) {
                    MergePatchTask change = new MergePatchTask();
                    change.status = Optional.of(getRandomStatus(random));
                    taskService.editTask(task.getId(), change, editor.getId());
                }
            }
        }
    }**/

    private void saveOpenSprint(User sprintCreator, Sprint sprint) {
        MergePatchSprint sprintChange = new MergePatchSprint();
        sprintChange.status = Optional.of(SprintStatus.ACTIVE);
        sprintService.editSprint(sprint.getId(), sprintChange, sprintCreator.getId());
    }

    private void saveCloseSprint(User sprintCreator, Sprint sprint) {
        MergePatchSprint sprintChange = new MergePatchSprint();
        sprintChange.status = Optional.of(SprintStatus.CLOSED);
        sprintService.editSprint(sprint.getId(), sprintChange, sprintCreator.getId());
    }

    private List<Task> createTasks(Long backlogId, int amount, List<User> users, Random random) {
        List<Task> tasks = new ArrayList<>();
        for(int i = 0; i <= amount; i++) {
            User reporter = users.get(random.nextInt(users.size()));
            Task task = taskService.createTask(backlogId, "Lorem ipsum dolor sit amet", reporter.getId());
            tasks.add(task);
        }
        return tasks;
    }

    private MergePatchTask buildBacklogEditTask(List<User> users, Random random) {
        Integer points = possibleEstimationPoints.get(random.nextInt(possibleEstimationPoints.size()));
        User assignee = users.get(random.nextInt(users.size()));

        MergePatchTask editTask = new MergePatchTask();
        editTask.assignee = Optional.of(assignee.getUsername());
        editTask.estimationPoints = Optional.of(points);
        return editTask;
    }

    private TaskStatus getRandomStatus(Random random) {
        TaskStatus status = TaskStatus.CREATED;
        int index = random.nextInt(3);
        switch (index) {
            case 0:
                status = TaskStatus.TODO;
                break;
            case 1:
                status = TaskStatus.INPROGRESS;
                break;
            case 2:
                status = TaskStatus.DONE;
                break;
        }
        return status;
    }

    private List<User> createDemoStudents() {
        List<String> names = Arrays.asList(
            "Blanca", "Said", "Carles", "Ferran", "Joanot", "Hassen", "Malek", "Osman", "Mahomet", "Guillem", "Roc", // mar i cel
            "Hamlet", "Claudius", "Gertrude", "The Ghost", "Polonius", "Ophelia", "Horatio", "Laertes", "Rosencrantz", "Guildenstern", "Fortinbras" // hamlet
        );
        List<User> users = new ArrayList<>();
        for(String name: names) {
            String username = name.toLowerCase(Locale.ROOT).replace(" ", ".");
            String encodedPassword = global.getPasswordEncoder().encode(username + "1234");
            User user = userService.addUserInternal(username, username + "@trackdev.com", encodedPassword, List.of(UserType.STUDENT));
            users.add(user);
        }
        return users;
    }

}
