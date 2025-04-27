package com.agileteam.sprinttracker.ui;

import com.agileteam.sprinttracker.github.GitHubIntegration;
import com.agileteam.sprinttracker.github.GitHubIntegration.Issue;
import com.agileteam.sprinttracker.manager.SprintManager;
import com.agileteam.sprinttracker.model.Sprint;
import com.agileteam.sprinttracker.model.Task;
import com.agileteam.sprinttracker.model.TeamMember;
import com.agileteam.sprinttracker.model.UserStory;
import com.agileteam.sprinttracker.storage.DataStorage;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Manages the user interface for the Sprint Tracker.
 */
public class UIManager {
    private final Scanner scanner;
    private final SprintManager sprintManager;
    private final DataStorage dataStorage;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    public UIManager(SprintManager sprintManager, DataStorage dataStorage) {
        this.scanner = new Scanner(System.in);
        this.sprintManager = sprintManager;
        this.dataStorage = dataStorage;
    }
    
    /**
     * Displays the main menu and handles user input.
     */
    public void showMainMenu() {
        boolean running = true;
        
        while (running) {
            clearScreen();
            Sprint currentSprint = sprintManager.getCurrentSprint();
            
            System.out.println("====== AGILE TEAM SPRINT TRACKER ======");
            if (currentSprint != null) {
                System.out.println("Current Sprint: " + currentSprint.getName() + 
                        " (" + currentSprint.getCompletionPercentage() + "% complete)");
            } else {
                System.out.println("No active sprint selected");
            }
            
            System.out.println("\nMAIN MENU");
            System.out.println("1. Manage Sprints");
            System.out.println("2. Manage Team Members");
            System.out.println("3. GitHub Integration");
            System.out.println("4. Save Data");
            System.out.println("5. Exit");
            
            System.out.print("\nEnter your choice: ");
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    showSprintMenu();
                    break;
                case "2":
                    showTeamMemberMenu();
                    break;
                case "3":
                    showGitHubMenu();
                    break;
                case "4":
                    saveData();
                    break;
                case "5":
                    running = false;
                    System.out.println("Exiting the application...");
                    break;
                default:
                    System.out.println("Invalid choice. Press Enter to continue...");
                    scanner.nextLine();
                    break;
            }
        }
    }
    
    /**
     * Displays the sprint management menu.
     */
    private void showSprintMenu() {
        boolean running = true;
        
        while (running) {
            clearScreen();
            Sprint currentSprint = sprintManager.getCurrentSprint();
            List<Sprint> allSprints = sprintManager.getAllSprints();
            
            System.out.println("====== SPRINT MANAGEMENT ======");
            if (currentSprint != null) {
                System.out.println("Current Sprint: " + currentSprint.getName());
            } else {
                System.out.println("No active sprint selected");
            }
            
            System.out.println("\nSPRINT MENU");
            System.out.println("1. Create New Sprint");
            System.out.println("2. Select Sprint");
            System.out.println("3. View Sprint Details");
            System.out.println("4. Manage User Stories");
            System.out.println("5. Back to Main Menu");
            
            System.out.print("\nEnter your choice: ");
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    createNewSprint();
                    break;
                case "2":
                    selectSprint();
                    break;
                case "3":
                    viewSprintDetails();
                    break;
                case "4":
                    if (currentSprint != null) {
                        showUserStoryMenu(currentSprint);
                    } else {
                        System.out.println("No sprint selected! Please select a sprint first.");
                        waitForEnter();
                    }
                    break;
                case "5":
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice. Press Enter to continue...");
                    scanner.nextLine();
                    break;
            }
        }
    }
    
    // Sprint management methods
    private void createNewSprint() {
        clearScreen();
        System.out.println("====== CREATE NEW SPRINT ======");
        
        System.out.print("Enter sprint name: ");
        String name = scanner.nextLine().trim();
        
        LocalDate startDate = null;
        while (startDate == null) {
            System.out.print("Enter start date (yyyy-MM-dd): ");
            try {
                startDate = LocalDate.parse(scanner.nextLine().trim(), DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format! Please use yyyy-MM-dd.");
            }
        }
        
        LocalDate endDate = null;
        while (endDate == null) {
            System.out.print("Enter end date (yyyy-MM-dd): ");
            try {
                endDate = LocalDate.parse(scanner.nextLine().trim(), DATE_FORMATTER);
                if (endDate.isBefore(startDate)) {
                    System.out.println("End date cannot be before start date!");
                    endDate = null;
                }
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format! Please use yyyy-MM-dd.");
            }
        }
        
        System.out.print("Enter sprint goal: ");
        String goal = scanner.nextLine().trim();
        
        Sprint sprint = new Sprint(name, startDate, endDate, goal);
        sprintManager.addSprint(sprint);
        
        System.out.println("\nSprint created successfully!");
        waitForEnter();
    }
    
    private void selectSprint() {
        clearScreen();
        List<Sprint> allSprints = sprintManager.getAllSprints();
        
        if (allSprints.isEmpty()) {
            System.out.println("No sprints available. Create a sprint first.");
            waitForEnter();
            return;
        }
        
        System.out.println("====== SELECT SPRINT ======");
        
        for (int i = 0; i < allSprints.size(); i++) {
            Sprint sprint = allSprints.get(i);
            System.out.println((i + 1) + ". " + sprint);
        }
        
        System.out.print("\nEnter the number of the sprint to select: ");
        String input = scanner.nextLine().trim();
        
        try {
            int selection = Integer.parseInt(input) - 1;
            if (selection >= 0 && selection < allSprints.size()) {
                sprintManager.setCurrentSprint(allSprints.get(selection));
                System.out.println("Sprint selected: " + allSprints.get(selection).getName());
            } else {
                System.out.println("Invalid selection!");
            }
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number!");
        }
        
        waitForEnter();
    }
    
    private void viewSprintDetails() {
        clearScreen();
        Sprint currentSprint = sprintManager.getCurrentSprint();
        
        if (currentSprint == null) {
            System.out.println("No sprint selected! Please select a sprint first.");
            waitForEnter();
            return;
        }
        
        System.out.println("====== SPRINT DETAILS ======");
        System.out.println("Name: " + currentSprint.getName());
        System.out.println("Duration: " + 
                currentSprint.getStartDate().format(DATE_FORMATTER) + " to " + 
                currentSprint.getEndDate().format(DATE_FORMATTER));
        System.out.println("Status: " + 
                (currentSprint.isCompleted() ? "Completed" : 
                (currentSprint.isActive() ? "Active" : "Future")));
        System.out.println("Goal: " + currentSprint.getGoal());
        System.out.println("Completion: " + currentSprint.getCompletionPercentage() + "%");
        System.out.println("Total Story Points: " + currentSprint.getTotalStoryPoints());
        
        List<UserStory> userStories = currentSprint.getUserStories();
        System.out.println("\nUser Stories: " + userStories.size());
        
        for (int i = 0; i < userStories.size(); i++) {
            UserStory story = userStories.get(i);
            System.out.println("  " + (i + 1) + ". " + story.getTitle() + 
                    " (" + story.getPriority() + ", " + story.getStoryPoints() + " points) - " + 
                    story.getCompletionPercentage() + "% complete");
        }
        
        waitForEnter();
    }
    
    // User story management
    private void showUserStoryMenu(Sprint sprint) {
        boolean running = true;
        
        while (running) {
            clearScreen();
            List<UserStory> userStories = sprint.getUserStories();
            
            System.out.println("====== USER STORIES FOR SPRINT: " + sprint.getName() + " ======");
            
            for (int i = 0; i < userStories.size(); i++) {
                UserStory story = userStories.get(i);
                System.out.println((i + 1) + ". " + story);
            }
            
            System.out.println("\nUSER STORY MENU");
            System.out.println("1. Add New User Story");
            System.out.println("2. Edit User Story");
            System.out.println("3. Remove User Story");
            System.out.println("4. View User Story Details");
            System.out.println("5. Manage Tasks");
            System.out.println("6. Back to Sprint Menu");
            
            System.out.print("\nEnter your choice: ");
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    addUserStory(sprint);
                    break;
                case "2":
                    editUserStory(sprint);
                    break;
                case "3":
                    removeUserStory(sprint);
                    break;
                case "4":
                    viewUserStoryDetails(sprint);
                    break;
                case "5":
                    manageUserStoryTasks(sprint);
                    break;
                case "6":
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice. Press Enter to continue...");
                    scanner.nextLine();
                    break;
            }
        }
    }
    
    private void addUserStory(Sprint sprint) {
        clearScreen();
        System.out.println("====== ADD NEW USER STORY ======");
        
        System.out.print("Enter story title: ");
        String title = scanner.nextLine().trim();
        
        System.out.print("Enter story description: ");
        String description = scanner.nextLine().trim();
        
        UserStory.Priority priority = null;
        while (priority == null) {
            System.out.println("Select priority:");
            System.out.println("1. Low");
            System.out.println("2. Medium");
            System.out.println("3. High");
            System.out.println("4. Critical");
            
            System.out.print("Enter your choice (1-4): ");
            String priorityChoice = scanner.nextLine().trim();
            
            switch (priorityChoice) {
                case "1":
                    priority = UserStory.Priority.LOW;
                    break;
                case "2":
                    priority = UserStory.Priority.MEDIUM;
                    break;
                case "3":
                    priority = UserStory.Priority.HIGH;
                    break;
                case "4":
                    priority = UserStory.Priority.CRITICAL;
                    break;
                default:
                    System.out.println("Invalid choice! Please try again.");
                    break;
            }
        }
        
        int storyPoints = -1;
        while (storyPoints < 0) {
            System.out.print("Enter story points (0-13): ");
            try {
                storyPoints = Integer.parseInt(scanner.nextLine().trim());
                if (storyPoints < 0 || storyPoints > 13) {
                    System.out.println("Story points should be between 0 and 13!");
                    storyPoints = -1;
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number!");
            }
        }
        
        UserStory userStory = new UserStory(title, description, priority, storyPoints);
        sprint.addUserStory(userStory);
        
        System.out.println("\nUser story added successfully!");
        waitForEnter();
    }
    
    private void editUserStory(Sprint sprint) {
        clearScreen();
        List<UserStory> userStories = sprint.getUserStories();
        
        if (userStories.isEmpty()) {
            System.out.println("No user stories available to edit!");
            waitForEnter();
            return;
        }
        
        System.out.println("====== EDIT USER STORY ======");
        
        for (int i = 0; i < userStories.size(); i++) {
            UserStory story = userStories.get(i);
            System.out.println((i + 1) + ". " + story);
        }
        
        System.out.print("\nSelect a user story to edit (1-" + userStories.size() + "): ");
        String input = scanner.nextLine().trim();
        
        try {
            int selection = Integer.parseInt(input) - 1;
            if (selection >= 0 && selection < userStories.size()) {
                UserStory selectedStory = userStories.get(selection);
                
                System.out.println("\nEditing User Story: " + selectedStory.getTitle());
                
                System.out.print("Enter new title (or press Enter to keep current): ");
                String title = scanner.nextLine().trim();
                if (!title.isEmpty()) {
                    selectedStory.setTitle(title);
                }
                
                System.out.print("Enter new description (or press Enter to keep current): ");
                String description = scanner.nextLine().trim();
                if (!description.isEmpty()) {
                    selectedStory.setDescription(description);
                }
                
                System.out.println("Current priority: " + selectedStory.getPriority());
                System.out.println("Do you want to change the priority? (y/n): ");
                if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
                    UserStory.Priority priority = null;
                    while (priority == null) {
                        System.out.println("Select new priority:");
                        System.out.println("1. Low");
                        System.out.println("2. Medium");
                        System.out.println("3. High");
                        System.out.println("4. Critical");
                        
                        System.out.print("Enter your choice (1-4): ");
                        String priorityChoice = scanner.nextLine().trim();
                        
                        switch (priorityChoice) {
                            case "1":
                                priority = UserStory.Priority.LOW;
                                break;
                            case "2":
                                priority = UserStory.Priority.MEDIUM;
                                break;
                            case "3":
                                priority = UserStory.Priority.HIGH;
                                break;
                            case "4":
                                priority = UserStory.Priority.CRITICAL;
                                break;
                            default:
                                System.out.println("Invalid choice! Please try again.");
                                break;
                        }
                    }
                    selectedStory.setPriority(priority);
                }
                
                System.out.println("Current story points: " + selectedStory.getStoryPoints());
                System.out.println("Do you want to change the story points? (y/n): ");
                if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
                    int storyPoints = -1;
                    while (storyPoints < 0) {
                        System.out.print("Enter new story points (0-13): ");
                        try {
                            storyPoints = Integer.parseInt(scanner.nextLine().trim());
                            if (storyPoints < 0 || storyPoints > 13) {
                                System.out.println("Story points should be between 0 and 13!");
                                storyPoints = -1;
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Please enter a valid number!");
                        }
                    }
                    selectedStory.setStoryPoints(storyPoints);
                }
                
                System.out.println("\nUser story updated successfully!");
            } else {
                System.out.println("Invalid selection!");
            }
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number!");
        }
        
        waitForEnter();
    }
    
    private void removeUserStory(Sprint sprint) {
        clearScreen();
        List<UserStory> userStories = sprint.getUserStories();
        
        if (userStories.isEmpty()) {
            System.out.println("No user stories available to remove!");
            waitForEnter();
            return;
        }
        
        System.out.println("====== REMOVE USER STORY ======");
        
        for (int i = 0; i < userStories.size(); i++) {
            UserStory story = userStories.get(i);
            System.out.println((i + 1) + ". " + story);
        }
        
        System.out.print("\nSelect a user story to remove (1-" + userStories.size() + "): ");
        String input = scanner.nextLine().trim();
        
        try {
            int selection = Integer.parseInt(input) - 1;
            if (selection >= 0 && selection < userStories.size()) {
                UserStory selectedStory = userStories.get(selection);
                
                System.out.println("\nAre you sure you want to remove the user story: " + 
                        selectedStory.getTitle() + "? (y/n): ");
                String confirmation = scanner.nextLine().trim();
                
                if (confirmation.equalsIgnoreCase("y")) {
                    sprint.removeUserStory(selectedStory);
                    System.out.println("User story removed successfully!");
                } else {
                    System.out.println("Removal cancelled.");
                }
            } else {
                System.out.println("Invalid selection!");
            }
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number!");
        }
        
        waitForEnter();
    }
    
    private void viewUserStoryDetails(Sprint sprint) {
        clearScreen();
        List<UserStory> userStories = sprint.getUserStories();
        
        if (userStories.isEmpty()) {
            System.out.println("No user stories available to view!");
            waitForEnter();
            return;
        }
        
        System.out.println("====== VIEW USER STORY DETAILS ======");
        
        for (int i = 0; i < userStories.size(); i++) {
            UserStory story = userStories.get(i);
            System.out.println((i + 1) + ". " + story);
        }
        
        System.out.print("\nSelect a user story to view (1-" + userStories.size() + "): ");
        String input = scanner.nextLine().trim();
        
        try {
            int selection = Integer.parseInt(input) - 1;
            if (selection >= 0 && selection < userStories.size()) {
                UserStory selectedStory = userStories.get(selection);
                
                clearScreen();
                System.out.println("====== USER STORY DETAILS ======");
                System.out.println("Title: " + selectedStory.getTitle());
                System.out.println("Description: " + selectedStory.getDescription());
                System.out.println("Priority: " + selectedStory.getPriority());
                System.out.println("Story Points: " + selectedStory.getStoryPoints());
                System.out.println("Completion: " + selectedStory.getCompletionPercentage() + "%");
                
                List<Task> tasks = selectedStory.getTasks();
                System.out.println("\nTasks: " + tasks.size());
                
                for (int i = 0; i < tasks.size(); i++) {
                    Task task = tasks.get(i);
                    System.out.println("  " + (i + 1) + ". " + task.getTitle() + 
                            " - " + task.getStatus());
                    if (task.getAssignedTeamMember() != null) {
                        System.out.println("     Assigned to: " + task.getAssignedTeamMember().getName());
                    }
                }
            } else {
                System.out.println("Invalid selection!");
            }
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number!");
        }
        
        waitForEnter();
    }
    
    private void manageUserStoryTasks(Sprint sprint) {
        clearScreen();
        List<UserStory> userStories = sprint.getUserStories();
        
        if (userStories.isEmpty()) {
            System.out.println("No user stories available. Add a user story first!");
            waitForEnter();
            return;
        }
        
        System.out.println("====== MANAGE TASKS ======");
        
        for (int i = 0; i < userStories.size(); i++) {
            UserStory story = userStories.get(i);
            System.out.println((i + 1) + ". " + story);
        }
        
        System.out.print("\nSelect a user story to manage tasks (1-" + userStories.size() + "): ");
        String input = scanner.nextLine().trim();
        
        try {
            int selection = Integer.parseInt(input) - 1;
            if (selection >= 0 && selection < userStories.size()) {
                UserStory selectedStory = userStories.get(selection);
                showTaskMenu(selectedStory);
            } else {
                System.out.println("Invalid selection!");
                waitForEnter();
            }
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number!");
            waitForEnter();
        }
    }

    private void showTaskMenu(UserStory userStory) {
        boolean running = true;
        
        while (running) {
            clearScreen();
            List<Task> tasks = userStory.getTasks();
            
            System.out.println("====== TASKS FOR USER STORY: " + userStory.getTitle() + " ======");
            
            for (int i = 0; i < tasks.size(); i++) {
                Task task = tasks.get(i);
                System.out.println((i + 1) + ". " + task);
            }
            
            System.out.println("\nTASK MENU");
            System.out.println("1. Add New Task");
            System.out.println("2. Edit Task");
            System.out.println("3. Remove Task");
            System.out.println("4. Assign Task to Team Member");
            System.out.println("5. Update Task Status");
            System.out.println("6. Back to User Story Menu");
            
            System.out.print("\nEnter your choice: ");
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    addTask(userStory);
                    break;
                case "2":
                    editTask(userStory);
                    break;
                case "3":
                    removeTask(userStory);
                    break;
                case "4":
                    assignTask(userStory);
                    break;
                case "5":
                    updateTaskStatus(userStory);
                    break;
                case "6":
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice. Press Enter to continue...");
                    scanner.nextLine();
                    break;
            }
        }
    }
    
    private void addTask(UserStory userStory) {
        clearScreen();
        System.out.println("====== ADD NEW TASK ======");
        
        System.out.print("Enter task title: ");
        String title = scanner.nextLine().trim();
        
        System.out.print("Enter task description: ");
        String description = scanner.nextLine().trim();
        
        Task task = new Task(title, description);
        userStory.addTask(task);
        
        System.out.println("\nTask added successfully!");
        
        // Optionally assign it to a team member
        System.out.print("Do you want to assign this task to a team member? (y/n): ");
        if (scanner.nextLine().trim().equalsIgnoreCase("y")) {
            assignTeamMemberToTask(task);
        }
        
        waitForEnter();
    }
    
    private void editTask(UserStory userStory) {
        clearScreen();
        List<Task> tasks = userStory.getTasks();
        
        if (tasks.isEmpty()) {
            System.out.println("No tasks available to edit!");
            waitForEnter();
            return;
        }
        
        System.out.println("====== EDIT TASK ======");
        
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            System.out.println((i + 1) + ". " + task);
        }
        
        System.out.print("\nSelect a task to edit (1-" + tasks.size() + "): ");
        String input = scanner.nextLine().trim();
        
        try {
            int selection = Integer.parseInt(input) - 1;
            if (selection >= 0 && selection < tasks.size()) {
                Task selectedTask = tasks.get(selection);
                
                System.out.println("\nEditing Task: " + selectedTask.getTitle());
                
                System.out.print("Enter new title (or press Enter to keep current): ");
                String title = scanner.nextLine().trim();
                if (!title.isEmpty()) {
                    selectedTask.setTitle(title);
                }
                
                System.out.print("Enter new description (or press Enter to keep current): ");
                String description = scanner.nextLine().trim();
                if (!description.isEmpty()) {
                    selectedTask.setDescription(description);
                }
                
                System.out.println("\nTask updated successfully!");
            } else {
                System.out.println("Invalid selection!");
            }
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number!");
        }
        
        waitForEnter();
    }
    
    private void removeTask(UserStory userStory) {
        clearScreen();
        List<Task> tasks = userStory.getTasks();
        
        if (tasks.isEmpty()) {
            System.out.println("No tasks available to remove!");
            waitForEnter();
            return;
        }
        
        System.out.println("====== REMOVE TASK ======");
        
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            System.out.println((i + 1) + ". " + task);
        }
        
        System.out.print("\nSelect a task to remove (1-" + tasks.size() + "): ");
        String input = scanner.nextLine().trim();
        
        try {
            int selection = Integer.parseInt(input) - 1;
            if (selection >= 0 && selection < tasks.size()) {
                Task selectedTask = tasks.get(selection);
                
                System.out.println("\nAre you sure you want to remove the task: " + 
                        selectedTask.getTitle() + "? (y/n): ");
                String confirmation = scanner.nextLine().trim();
                
                if (confirmation.equalsIgnoreCase("y")) {
                    userStory.removeTask(selectedTask);
                    System.out.println("Task removed successfully!");
                } else {
                    System.out.println("Removal cancelled.");
                }
            } else {
                System.out.println("Invalid selection!");
            }
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number!");
        }
        
        waitForEnter();
    }
    
    private void assignTask(UserStory userStory) {
        clearScreen();
        List<Task> tasks = userStory.getTasks();
        
        if (tasks.isEmpty()) {
            System.out.println("No tasks available to assign!");
            waitForEnter();
            return;
        }
        
        System.out.println("====== ASSIGN TASK ======");
        
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            String assignee = task.getAssignedTeamMember() != null ? 
                    " (Assigned to: " + task.getAssignedTeamMember().getName() + ")" : 
                    " (Unassigned)";
            System.out.println((i + 1) + ". " + task.getTitle() + assignee);
        }
        
        System.out.print("\nSelect a task to assign (1-" + tasks.size() + "): ");
        String input = scanner.nextLine().trim();
        
        try {
            int selection = Integer.parseInt(input) - 1;
            if (selection >= 0 && selection < tasks.size()) {
                Task selectedTask = tasks.get(selection);
                assignTeamMemberToTask(selectedTask);
            } else {
                System.out.println("Invalid selection!");
                waitForEnter();
            }
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number!");
            waitForEnter();
        }
    }
    
    private void assignTeamMemberToTask(Task task) {
        List<TeamMember> teamMembers = sprintManager.getAllTeamMembers();
        
        if (teamMembers.isEmpty()) {
            System.out.println("No team members available. Add team members first!");
            waitForEnter();
            return;
        }
        
        System.out.println("\nSelect a team member to assign:");
        System.out.println("0. Unassign task");
        
        for (int i = 0; i < teamMembers.size(); i++) {
            TeamMember member = teamMembers.get(i);
            System.out.println((i + 1) + ". " + member.getName());
        }
        
        System.out.print("\nEnter your choice (0-" + teamMembers.size() + "): ");
        String input = scanner.nextLine().trim();
        
        try {
            int selection = Integer.parseInt(input);
            if (selection == 0) {
                task.setAssignedTeamMember(null);
                System.out.println("Task unassigned.");
            } else if (selection > 0 && selection <= teamMembers.size()) {
                TeamMember selectedMember = teamMembers.get(selection - 1);
                task.setAssignedTeamMember(selectedMember);
                System.out.println("Task assigned to " + selectedMember.getName() + ".");
            } else {
                System.out.println("Invalid selection!");
            }
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number!");
        }
        
        waitForEnter();
    }
    
    private void updateTaskStatus(UserStory userStory) {
        clearScreen();
        List<Task> tasks = userStory.getTasks();
        
        if (tasks.isEmpty()) {
            System.out.println("No tasks available to update!");
            waitForEnter();
            return;
        }
        
        System.out.println("====== UPDATE TASK STATUS ======");
        
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            System.out.println((i + 1) + ". " + task.getTitle() + " - " + task.getStatus());
        }
        
        System.out.print("\nSelect a task to update (1-" + tasks.size() + "): ");
        String input = scanner.nextLine().trim();
        
        try {
            int selection = Integer.parseInt(input) - 1;
            if (selection >= 0 && selection < tasks.size()) {
                Task selectedTask = tasks.get(selection);
                
                System.out.println("\nCurrent status: " + selectedTask.getStatus());
                System.out.println("Select new status:");
                System.out.println("1. To Do");
                System.out.println("2. In Progress");
                System.out.println("3. Done");
                
                System.out.print("\nEnter your choice (1-3): ");
                String statusChoice = scanner.nextLine().trim();
                
                switch (statusChoice) {
                    case "1":
                        selectedTask.setStatus(Task.Status.TO_DO);
                        System.out.println("Status updated to To Do.");
                        break;
                    case "2":
                        selectedTask.setStatus(Task.Status.IN_PROGRESS);
                        System.out.println("Status updated to In Progress.");
                        break;
                    case "3":
                        selectedTask.setStatus(Task.Status.DONE);
                        System.out.println("Status updated to Done.");
                        break;
                    default:
                        System.out.println("Invalid choice! Status not updated.");
                        break;
                }
            } else {
                System.out.println("Invalid selection!");
            }
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number!");
        }
        
        waitForEnter();
    }
    
    // Team member management
    private void showTeamMemberMenu() {
        boolean running = true;
        
        while (running) {
            clearScreen();
            List<TeamMember> teamMembers = sprintManager.getAllTeamMembers();
            
            System.out.println("====== TEAM MEMBER MANAGEMENT ======");
            
            for (int i = 0; i < teamMembers.size(); i++) {
                TeamMember member = teamMembers.get(i);
                System.out.println((i + 1) + ". " + member);
            }
            
            System.out.println("\nTEAM MEMBER MENU");
            System.out.println("1. Add New Team Member");
            System.out.println("2. Edit Team Member");
            System.out.println("3. Remove Team Member");
            System.out.println("4. View Team Member Details");
            System.out.println("5. Back to Main Menu");
            
            System.out.print("\nEnter your choice: ");
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    addTeamMember();
                    break;
                case "2":
                    editTeamMember();
                    break;
                case "3":
                    removeTeamMember();
                    break;
                case "4":
                    viewTeamMemberDetails();
                    break;
                case "5":
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice. Press Enter to continue...");
                    scanner.nextLine();
                    break;
            }
        }
    }
    
    // Helper methods
    private void clearScreen() {
        for (int i = 0; i < 50; i++) {
            System.out.println();
        }
    }
    
    private void waitForEnter() {
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }
    
    private void saveData() {
        try {
            dataStorage.saveData(sprintManager);
            System.out.println("Data saved successfully!");
        } catch (IOException e) {
            System.out.println("Error saving data: " + e.getMessage());
            e.printStackTrace();
        }
        waitForEnter();
    }
    
    // Placeholder for GitHub integration methods
    private void showGitHubMenu() {
        boolean running = true;
        
        while (running) {
            clearScreen();
            System.out.println("====== GITHUB INTEGRATION ======");
            
            System.out.println("\nGITHUB MENU");
            System.out.println("1. Configure GitHub Connection");
            System.out.println("2. Import Issues as User Stories");
            System.out.println("3. Import Issues as Tasks");
            System.out.println("4. Back to Main Menu");
            
            System.out.print("\nEnter your choice: ");
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    configureGitHub();
                    break;
                case "2":
                    importIssuesAsStories();
                    break;
                case "3":
                    importIssuesAsTasks();
                    break;
                case "4":
                    running = false;
                    break;
                default:
                    System.out.println("Invalid choice. Press Enter to continue...");
                    scanner.nextLine();
                    break;
            }
        }
    }
    
    // Team member management methods
    private void addTeamMember() {
        clearScreen();
        System.out.println("====== ADD TEAM MEMBER ======");
        
        System.out.print("Enter team member name: ");
        String name = scanner.nextLine().trim();
        
        System.out.print("Enter email: ");
        String email = scanner.nextLine().trim();
        
        System.out.print("Enter GitHub username: ");
        String githubUsername = scanner.nextLine().trim();
        
        TeamMember teamMember = new TeamMember(name, email, githubUsername);
        sprintManager.addTeamMember(teamMember);
        
        System.out.println("\nTeam member added successfully!");
        waitForEnter();
    }
    
    private void editTeamMember() {
        clearScreen();
        List<TeamMember> teamMembers = sprintManager.getAllTeamMembers();
        
        if (teamMembers.isEmpty()) {
            System.out.println("No team members available to edit!");
            waitForEnter();
            return;
        }
        
        System.out.println("====== EDIT TEAM MEMBER ======");
        
        for (int i = 0; i < teamMembers.size(); i++) {
            TeamMember member = teamMembers.get(i);
            System.out.println((i + 1) + ". " + member);
        }
        
        System.out.print("\nSelect a team member to edit (1-" + teamMembers.size() + "): ");
        String input = scanner.nextLine().trim();
        
        try {
            int selection = Integer.parseInt(input) - 1;
            if (selection >= 0 && selection < teamMembers.size()) {
                TeamMember selectedMember = teamMembers.get(selection);
                
                System.out.println("\nEditing Team Member: " + selectedMember.getName());
                
                System.out.print("Enter new name (or press Enter to keep current): ");
                String name = scanner.nextLine().trim();
                if (!name.isEmpty()) {
                    selectedMember.setName(name);
                }
                
                System.out.print("Enter new email (or press Enter to keep current): ");
                String email = scanner.nextLine().trim();
                if (!email.isEmpty()) {
                    selectedMember.setEmail(email);
                }
                
                System.out.print("Enter new GitHub username (or press Enter to keep current): ");
                String githubUsername = scanner.nextLine().trim();
                if (!githubUsername.isEmpty()) {
                    selectedMember.setGithubUsername(githubUsername);
                }
                
                System.out.println("\nTeam member updated successfully!");
            } else {
                System.out.println("Invalid selection!");
            }
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number!");
        }
        
        waitForEnter();
    }
    
    private void removeTeamMember() {
        clearScreen();
        List<TeamMember> teamMembers = sprintManager.getAllTeamMembers();
        
        if (teamMembers.isEmpty()) {
            System.out.println("No team members available to remove!");
            waitForEnter();
            return;
        }
        
        System.out.println("====== REMOVE TEAM MEMBER ======");
        
        for (int i = 0; i < teamMembers.size(); i++) {
            TeamMember member = teamMembers.get(i);
            System.out.println((i + 1) + ". " + member);
        }
        
        System.out.print("\nSelect a team member to remove (1-" + teamMembers.size() + "): ");
        String input = scanner.nextLine().trim();
        
        try {
            int selection = Integer.parseInt(input) - 1;
            if (selection >= 0 && selection < teamMembers.size()) {
                TeamMember selectedMember = teamMembers.get(selection);
                
                System.out.println("\nAre you sure you want to remove the team member: " + 
                        selectedMember.getName() + "? (y/n): ");
                System.out.println("Note: This will unassign all tasks assigned to this team member.");
                String confirmation = scanner.nextLine().trim();
                
                if (confirmation.equalsIgnoreCase("y")) {
                    sprintManager.removeTeamMember(selectedMember);
                    System.out.println("Team member removed successfully!");
                } else {
                    System.out.println("Removal cancelled.");
                }
            } else {
                System.out.println("Invalid selection!");
            }
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number!");
        }
        
        waitForEnter();
    }
    
    private void viewTeamMemberDetails() {
        clearScreen();
        List<TeamMember> teamMembers = sprintManager.getAllTeamMembers();
        
        if (teamMembers.isEmpty()) {
            System.out.println("No team members available to view!");
            waitForEnter();
            return;
        }
        
        System.out.println("====== VIEW TEAM MEMBER DETAILS ======");
        
        for (int i = 0; i < teamMembers.size(); i++) {
            TeamMember member = teamMembers.get(i);
            System.out.println((i + 1) + ". " + member);
        }
        
        System.out.print("\nSelect a team member to view (1-" + teamMembers.size() + "): ");
        String input = scanner.nextLine().trim();
        
        try {
            int selection = Integer.parseInt(input) - 1;
            if (selection >= 0 && selection < teamMembers.size()) {
                TeamMember selectedMember = teamMembers.get(selection);
                
                clearScreen();
                System.out.println("====== TEAM MEMBER DETAILS ======");
                System.out.println("Name: " + selectedMember.getName());
                System.out.println("Email: " + (selectedMember.getEmail() != null ? selectedMember.getEmail() : "Not set"));
                System.out.println("GitHub Username: " + (selectedMember.getGithubUsername() != null ? selectedMember.getGithubUsername() : "Not set"));
                
                List<Task> tasks = selectedMember.getAssignedTasks();
                int completedTasks = sprintManager.getCompletedTasksCountForTeamMember(selectedMember);
                
                System.out.println("\nAssigned Tasks: " + tasks.size());
                System.out.println("Completed Tasks: " + completedTasks);
                
                for (int i = 0; i < tasks.size(); i++) {
                    Task task = tasks.get(i);
                    System.out.println("  " + (i + 1) + ". " + task.getTitle() + " - " + task.getStatus());
                    if (task.getParentStory() != null) {
                        System.out.println("     Part of story: " + task.getParentStory().getTitle());
                    }
                }
            } else {
                System.out.println("Invalid selection!");
            }
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number!");
        }
        
        waitForEnter();
    }
    
    // GitHub integration methods
    private void configureGitHub() {
        clearScreen();
        System.out.println("====== CONFIGURE GITHUB CONNECTION ======");
        
        System.out.println("\nTo connect to GitHub, you need a personal access token.");
        System.out.println("You can create one at https://github.com/settings/tokens");
        System.out.println("Make sure it has the 'repo' scope to access repository issues.");
        
        System.out.print("\nEnter your GitHub personal access token: ");
        String token = scanner.nextLine().trim();
        
        if (token.isEmpty()) {
            System.out.println("Token is required. Operation cancelled.");
            waitForEnter();
            return;
        }
        
        System.out.print("Enter repository owner (username or organization): ");
        String owner = scanner.nextLine().trim();
        
        if (owner.isEmpty()) {
            System.out.println("Repository owner is required. Operation cancelled.");
            waitForEnter();
            return;
        }
        
        System.out.print("Enter repository name: ");
        String repo = scanner.nextLine().trim();
        
        if (repo.isEmpty()) {
            System.out.println("Repository name is required. Operation cancelled.");
            waitForEnter();
            return;
        }
        
        // Store the credentials (in a real application, these would be stored securely)
        // For this demo, we'll just keep them in memory
        try {
            GitHubIntegration gitHub = new GitHubIntegration(token, owner, repo);
            
            // Test the connection by fetching one issue
            List<GitHubIntegration.Issue> issues = gitHub.fetchIssues("open", null);
            System.out.println("\nConnection successful! Found " + issues.size() + " open issues.");
            gitHub.close();
            
            // Save the credentials (would normally be done securely)
            System.out.println("GitHub connection configured successfully!");
        } catch (Exception e) {
            System.out.println("\nError connecting to GitHub: " + e.getMessage());
            e.printStackTrace();
        }
        
        waitForEnter();
    }
    
    private void importIssuesAsStories() {
        if (sprintManager.getCurrentSprint() == null) {
            System.out.println("No sprint selected! Please select a sprint first.");
            waitForEnter();
            return;
        }
        
        clearScreen();
        System.out.println("====== IMPORT GITHUB ISSUES AS USER STORIES ======");
        
        // In a real application, these would be retrieved from stored credentials
        System.out.print("Enter your GitHub personal access token: ");
        String token = scanner.nextLine().trim();
        
        System.out.print("Enter repository owner: ");
        String owner = scanner.nextLine().trim();
        
        System.out.print("Enter repository name: ");
        String repo = scanner.nextLine().trim();
        
        System.out.print("Enter issue state (open, closed, all): ");
        String state = scanner.nextLine().trim().toLowerCase();
        if (!state.equals("open") && !state.equals("closed") && !state.equals("all")) {
            state = "open";
        }
        
        System.out.print("Enter label for filtering (optional): ");
        String label = scanner.nextLine().trim();
        
        System.out.print("Enter label that identifies user stories: ");
        String storyLabel = scanner.nextLine().trim();
        if (storyLabel.isEmpty()) {
            storyLabel = "user-story";
        }
        
        try {
            GitHubIntegration gitHub = new GitHubIntegration(token, owner, repo);
            
            // Fetch issues
            List<GitHubIntegration.Issue> issues = gitHub.fetchIssues(state, label.isEmpty() ? null : label);
            
            if (issues.isEmpty()) {
                System.out.println("No issues found with the specified criteria.");
                gitHub.close();
                waitForEnter();
                return;
            }
            
            System.out.println("\nFound " + issues.size() + " issues.");
            
            // Create a map of GitHub usernames to team members
            Map<String, TeamMember> teamMemberMap = new HashMap<>();
            for (TeamMember member : sprintManager.getAllTeamMembers()) {
                if (member.getGithubUsername() != null && !member.getGithubUsername().isEmpty()) {
                    teamMemberMap.put(member.getGithubUsername(), member);
                }
            }
            
            // Convert issues to user stories
            List<UserStory> userStories = gitHub.convertIssuesToUserStories(issues, teamMemberMap, storyLabel);
            
            // Add the user stories to the current sprint
            System.out.println("Adding " + userStories.size() + " user stories to sprint: " + 
                    sprintManager.getCurrentSprint().getName());
            
            for (UserStory story : userStories) {
                sprintManager.getCurrentSprint().addUserStory(story);
            }
            
            System.out.println("\nImported " + userStories.size() + " user stories with their tasks successfully!");
            gitHub.close();
        } catch (Exception e) {
            System.out.println("\nError importing from GitHub: " + e.getMessage());
            e.printStackTrace();
        }
        
        waitForEnter();
    }
    
    private void importIssuesAsTasks() {
        if (sprintManager.getCurrentSprint() == null) {
            System.out.println("No sprint selected! Please select a sprint first.");
            waitForEnter();
            return;
        }
        
        List<UserStory> userStories = sprintManager.getCurrentSprint().getUserStories();
        if (userStories.isEmpty()) {
            System.out.println("No user stories in the current sprint! Add a user story first.");
            waitForEnter();
            return;
        }
        
        clearScreen();
        System.out.println("====== IMPORT GITHUB ISSUES AS TASKS ======");
        
        // Select a user story to add tasks to
        for (int i = 0; i < userStories.size(); i++) {
            UserStory story = userStories.get(i);
            System.out.println((i + 1) + ". " + story);
        }
        
        System.out.print("\nSelect a user story to add tasks to (1-" + userStories.size() + "): ");
        String input = scanner.nextLine().trim();
        
        UserStory selectedStory;
        try {
            int selection = Integer.parseInt(input) - 1;
            if (selection >= 0 && selection < userStories.size()) {
                selectedStory = userStories.get(selection);
            } else {
                System.out.println("Invalid selection!");
                waitForEnter();
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number!");
            waitForEnter();
            return;
        }
        
        // In a real application, these would be retrieved from stored credentials
        System.out.print("Enter your GitHub personal access token: ");
        String token = scanner.nextLine().trim();
        
        System.out.print("Enter repository owner: ");
        String owner = scanner.nextLine().trim();
        
        System.out.print("Enter repository name: ");
        String repo = scanner.nextLine().trim();
        
        System.out.print("Enter issue state (open, closed, all): ");
        String state = scanner.nextLine().trim().toLowerCase();
        if (!state.equals("open") && !state.equals("closed") && !state.equals("all")) {
            state = "open";
        }
        
        System.out.print("Enter label for filtering (optional): ");
        String label = scanner.nextLine().trim();
        
        try {
            GitHubIntegration gitHub = new GitHubIntegration(token, owner, repo);
            
            // Fetch issues
            List<GitHubIntegration.Issue> issues = gitHub.fetchIssues(state, label.isEmpty() ? null : label);
            
            if (issues.isEmpty()) {
                System.out.println("No issues found with the specified criteria.");
                gitHub.close();
                waitForEnter();
                return;
            }
            
            System.out.println("\nFound " + issues.size() + " issues.");
            
            // Create a map of GitHub usernames to team members
            Map<String, TeamMember> teamMemberMap = new HashMap<>();
            for (TeamMember member : sprintManager.getAllTeamMembers()) {
                if (member.getGithubUsername() != null && !member.getGithubUsername().isEmpty()) {
                    teamMemberMap.put(member.getGithubUsername(), member);
                }
            }
            
            // Import issues as tasks
            int tasksAdded = 0;
            for (GitHubIntegration.Issue issue : issues) {
                Task task = new Task(issue.getTitle(), issue.getBody());
                task.setGithubIssueUrl(issue.getHtmlUrl());
                
                // Set status based on issue state
                if ("closed".equals(issue.getState())) {
                    task.setStatus(Task.Status.DONE);
                } else {
                    task.setStatus(Task.Status.TO_DO);
                }
                
                // Assign team member if assignee exists and matches a team member
                if (issue.getAssignee() != null && teamMemberMap.containsKey(issue.getAssignee())) {
                    task.setAssignedTeamMember(teamMemberMap.get(issue.getAssignee()));
                }
                
                selectedStory.addTask(task);
                tasksAdded++;
            }
            
            System.out.println("\nAdded " + tasksAdded + " tasks to user story: " + selectedStory.getTitle());
            gitHub.close();
        } catch (Exception e) {
            System.out.println("\nError importing from GitHub: " + e.getMessage());
            e.printStackTrace();
        }
        
        waitForEnter();
    }
}