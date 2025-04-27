package com.agileteam.sprinttracker.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents an Agile user story.
 */
public class UserStory {
    public enum Priority {
        LOW("Low"),
        MEDIUM("Medium"),
        HIGH("High"),
        CRITICAL("Critical");

        private final String displayName;

        Priority(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private String id;
    private String title;
    private String description;
    private Priority priority;
    private int storyPoints;
    private List<Task> tasks;
    private Sprint parentSprint;

    /**
     * Creates a user story with title and description, setting default values for priority and story points.
     * 
     * @param title The title of the user story
     * @param description The description of the user story
     */
    public UserStory(String title, String description) {
        this(title, description, Priority.MEDIUM, 3);
    }

    /**
     * Creates a user story with title, description, and string-based priority.
     * This constructor helps with JSON deserialization.
     * 
     * @param title The title of the user story
     * @param description The description of the user story
     * @param priority The priority as a string (will be converted to enum)
     * @param storyPoints The number of story points
     */
    public UserStory(String title, String description, String priority, int storyPoints) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        try {
            this.priority = Priority.valueOf(priority.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.priority = Priority.MEDIUM;
        }
        this.storyPoints = storyPoints;
        this.tasks = new ArrayList<>();
    }

    public UserStory(String title, String description, Priority priority, int storyPoints) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.storyPoints = storyPoints;
        this.tasks = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }
    
    /**
     * Sets the priority from a string value.
     * 
     * @param priorityStr The priority as a string (case-insensitive)
     */
    public void setPriority(String priorityStr) {
        try {
            this.priority = Priority.valueOf(priorityStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.priority = Priority.MEDIUM;
        }
    }

    public int getStoryPoints() {
        return storyPoints;
    }

    public void setStoryPoints(int storyPoints) {
        this.storyPoints = storyPoints;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void addTask(Task task) {
        tasks.add(task);
        task.setParentStory(this);
    }

    public boolean removeTask(Task task) {
        boolean removed = tasks.remove(task);
        if (removed && task.getParentStory() == this) {
            task.setParentStory(null);
        }
        return removed;
    }

    public Sprint getParentSprint() {
        return parentSprint;
    }

    public void setParentSprint(Sprint parentSprint) {
        this.parentSprint = parentSprint;
    }

    /**
     * Calculates the percentage of completed tasks.
     * @return Percentage of completed tasks (0-100)
     */
    public int getCompletionPercentage() {
        if (tasks.isEmpty()) {
            return 0;
        }
        
        long completedTasks = tasks.stream()
                .filter(task -> task.getStatus() == Task.Status.DONE)
                .count();
        
        return (int) ((completedTasks * 100) / tasks.size());
    }

    /**
     * Finds a task by ID within this user story.
     * 
     * @param taskId The ID of the task to find
     * @return Optional containing the task if found, empty Optional otherwise
     */
    public Optional<Task> getTaskById(String taskId) {
        return tasks.stream()
                .filter(task -> task.getId().equals(taskId))
                .findFirst();
    }

    /**
     * Updates an existing task in this user story.
     * 
     * @param updatedTask The updated task (must have the same ID as an existing task)
     * @return true if the task was updated, false if no matching task was found
     */
    public boolean updateTask(Task updatedTask) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId().equals(updatedTask.getId())) {
                tasks.set(i, updatedTask);
                updatedTask.setParentStory(this);
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a copy of this user story with a new ID.
     * Tasks are not copied.
     * 
     * @return A new user story with the same title, description, priority and story points
     */
    public UserStory copy() {
        UserStory copy = new UserStory(this.title, this.description, this.priority, this.storyPoints);
        return copy;
    }

    /**
     * Creates a deep copy of this user story with a new ID, including tasks.
     * 
     * @return A new user story with copies of all fields and tasks
     */
    public UserStory deepCopy() {
        UserStory copy = new UserStory(this.title, this.description, this.priority, this.storyPoints);
        
        for (Task task : this.tasks) {
            Task taskCopy = new Task(task.getTitle(), task.getDescription());
            taskCopy.setStatus(task.getStatus());
            
            // Don't copy assigned team member as that creates circular references
            // Don't copy GitHub issue URL as that should be unique
            
            copy.addTask(taskCopy);
        }
        
        return copy;
    }

    /**
     * Updates this user story with values from another user story.
     * This does not update tasks or the parent sprint.
     * 
     * @param other The user story to copy values from
     */
    public void updateFrom(UserStory other) {
        this.title = other.title;
        this.description = other.description;
        this.priority = other.priority;
        this.storyPoints = other.storyPoints;
    }
    
    /**
     * Checks if all tasks in this user story are complete.
     * 
     * @return true if all tasks are done, false otherwise. Returns true if there are no tasks.
     */
    public boolean isComplete() {
        if (tasks.isEmpty()) {
            return false;
        }
        
        return tasks.stream()
                .allMatch(task -> task.getStatus() == Task.Status.DONE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserStory userStory = (UserStory) o;
        return Objects.equals(id, userStory.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return title + " (" + priority.getDisplayName() + ", " + storyPoints + " points) - " 
                + getCompletionPercentage() + "% complete";
    }
} 