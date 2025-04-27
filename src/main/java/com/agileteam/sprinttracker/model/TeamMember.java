package com.agileteam.sprinttracker.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a team member in an Agile team.
 */
public class TeamMember {
    private String id;
    private String name;
    private String email;
    private String githubUsername;
    private List<Task> assignedTasks;
    private String role;

    public TeamMember(String name, String email, String githubUsername) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.email = email;
        this.githubUsername = githubUsername;
        this.assignedTasks = new ArrayList<>();
        this.role = "Team Member";
    }

    /**
     * Creates a TeamMember with a specific ID (for use when creating copies)
     */
    private TeamMember(String id, String name, String email, String githubUsername, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.githubUsername = githubUsername;
        this.assignedTasks = new ArrayList<>();
        this.role = role;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGithubUsername() {
        return githubUsername;
    }

    public void setGithubUsername(String githubUsername) {
        this.githubUsername = githubUsername;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }

    public List<Task> getAssignedTasks() {
        return assignedTasks;
    }

    public void assignTask(Task task) {
        assignedTasks.add(task);
        task.setAssignedTeamMember(this);
    }

    public void removeTask(Task task) {
        assignedTasks.remove(task);
        if (task.getAssignedTeamMember() == this) {
            task.setAssignedTeamMember(null);
        }
    }

    /**
     * Find a task by its ID from the list of assigned tasks.
     * 
     * @param taskId the ID of the task to find
     * @return an Optional containing the task if found, or empty Optional if not found
     */
    public Optional<Task> findTaskById(String taskId) {
        return assignedTasks.stream()
                .filter(task -> task.getId().equals(taskId))
                .findFirst();
    }

    /**
     * Creates a shallow copy of this TeamMember without any assigned tasks.
     * The copy will have the same ID as the original.
     * 
     * @return a copy of this TeamMember
     */
    public TeamMember copy() {
        return new TeamMember(id, name, email, githubUsername, role);
    }

    /**
     * Creates a deep copy of this TeamMember, including copies of all assigned tasks.
     * The copy will have the same ID as the original.
     * 
     * @return a deep copy of this TeamMember
     */
    public TeamMember deepCopy() {
        TeamMember copy = copy();
        
        // Create copies of all tasks and add them to the copy
        for (Task task : assignedTasks) {
            Task taskCopy = task.copy();
            copy.assignedTasks.add(taskCopy);
            // Don't set the assignedTeamMember on the task copy to avoid circular references
        }
        
        return copy;
    }

    /**
     * Updates this TeamMember with data from another TeamMember.
     * The ID remains unchanged, only the metadata is updated.
     * Assigned tasks are not affected.
     * 
     * @param other the TeamMember to copy data from
     */
    public void updateFrom(TeamMember other) {
        this.name = other.name;
        this.email = other.email;
        this.githubUsername = other.githubUsername;
        this.role = other.role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeamMember that = (TeamMember) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name + " (" + (githubUsername != null ? "@" + githubUsername : email) + ")";
    }
} 