package com.agileteam.sprinttracker.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Represents an Agile sprint.
 */
public class Sprint {
    private String id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String goal;
    private List<UserStory> userStories;
    private boolean active;

    public Sprint(String name, LocalDate startDate, LocalDate endDate, String goal) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.goal = goal;
        this.userStories = new ArrayList<>();
        this.active = false;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }
    
    /**
     * Gets the list of user stories in this sprint.
     * 
     * @return List of user stories
     */
    public List<UserStory> getUserStories() {
        return userStories;
    }

    /**
     * Finds a user story by ID within this sprint.
     * 
     * @param id The ID of the user story to find
     * @return Optional containing the user story if found, empty Optional otherwise
     */
    public Optional<UserStory> getUserStoryById(String id) {
        return userStories.stream()
                .filter(story -> story.getId().equals(id))
                .findFirst();
    }

    /**
     * Finds a task by ID within any user story in this sprint.
     * 
     * @param taskId The ID of the task to find
     * @return Optional containing the task if found, empty Optional otherwise
     */
    public Optional<Task> getTaskById(String taskId) {
        return getAllTasks().stream()
                .filter(task -> task.getId().equals(taskId))
                .findFirst();
    }

    public void addUserStory(UserStory userStory) {
        userStories.add(userStory);
        userStory.setParentSprint(this);
    }

    public boolean removeUserStory(UserStory userStory) {
        boolean removed = userStories.remove(userStory);
        if (removed && userStory.getParentSprint() == this) {
            userStory.setParentSprint(null);
        }
        return removed;
    }

    /**
     * Updates an existing user story in this sprint.
     * 
     * @param updatedStory The updated user story (must have the same ID as an existing story)
     * @return true if the story was updated, false if no matching story was found
     */
    public boolean updateUserStory(UserStory updatedStory) {
        for (int i = 0; i < userStories.size(); i++) {
            if (userStories.get(i).getId().equals(updatedStory.getId())) {
                userStories.set(i, updatedStory);
                updatedStory.setParentSprint(this);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Creates a shallow copy of this sprint without any user stories.
     * 
     * @return A new Sprint instance with the same properties but no user stories
     */
    public Sprint copy() {
        Sprint copy = new Sprint(this.name, this.startDate, this.endDate, this.goal);
        copy.setActive(this.active);
        
        // Use reflection to set the same ID
        try {
            java.lang.reflect.Field idField = Sprint.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(copy, this.id);
        } catch (Exception e) {
            // In case of reflection error, just keep the generated ID
            e.printStackTrace();
        }
        
        return copy;
    }
    
    /**
     * Creates a deep copy of this sprint including copies of all user stories.
     * 
     * @return A new Sprint instance with the same properties and copies of all user stories
     */
    public Sprint deepCopy() {
        Sprint copy = this.copy();
        
        // Add copies of all user stories
        for (UserStory story : this.userStories) {
            copy.addUserStory(story.deepCopy());
        }
        
        return copy;
    }
    
    /**
     * Updates this sprint's fields from another sprint object.
     * This does not update the ID or the user stories.
     * 
     * @param other The sprint to copy fields from
     */
    public void updateFrom(Sprint other) {
        if (other == null) return;
        
        this.name = other.name;
        this.startDate = other.startDate;
        this.endDate = other.endDate;
        this.goal = other.goal;
        this.active = other.active;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Calculates total story points in this sprint.
     * @return Sum of all story points
     */
    public int getTotalStoryPoints() {
        return userStories.stream()
                .mapToInt(UserStory::getStoryPoints)
                .sum();
    }

    /**
     * Calculates the overall completion percentage of this sprint.
     * @return Percentage of completed tasks across all user stories (0-100)
     */
    public int getCompletionPercentage() {
        if (userStories.isEmpty()) {
            return 0;
        }
        
        int totalTasks = 0;
        int completedTasks = 0;
        
        for (UserStory userStory : userStories) {
            List<Task> tasks = userStory.getTasks();
            totalTasks += tasks.size();
            completedTasks += tasks.stream()
                    .filter(task -> task.getStatus() == Task.Status.DONE)
                    .count();
        }
        
        return totalTasks == 0 ? 0 : (completedTasks * 100) / totalTasks;
    }

    /**
     * Checks if this sprint is ongoing (current date is between start and end date).
     * @return true if the sprint is ongoing, false otherwise
     */
    public boolean isOngoing() {
        LocalDate now = LocalDate.now();
        return !now.isBefore(startDate) && !now.isAfter(endDate);
    }

    /**
     * Gets all tasks across all user stories in this sprint.
     */
    public List<Task> getAllTasks() {
        return userStories.stream()
                .flatMap(story -> story.getTasks().stream())
                .collect(Collectors.toList());
    }

    /**
     * Checks if the sprint is completed (end date has passed).
     */
    public boolean isCompleted() {
        return LocalDate.now().isAfter(endDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sprint sprint = (Sprint) o;
        return Objects.equals(id, sprint.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String status = isCompleted() ? "Completed" : (isOngoing() ? "Active" : "Future");
        
        return name + " (" + startDate.format(formatter) + " to " + endDate.format(formatter) + 
               ") - " + status + " - " + getCompletionPercentage() + "% complete";
    }
} 