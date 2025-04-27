package com.agileteam.sprinttracker.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a task within a user story.
 */
public class Task {
    public enum Status {
        TO_DO("To Do"),
        IN_PROGRESS("In Progress"),
        DONE("Done");

        private final String displayName;

        Status(String displayName) {
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
    private Status status;
    private TeamMember assignedTeamMember;
    private UserStory parentStory;
    private String githubIssueUrl;

    public Task(String title, String description) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.status = Status.TO_DO;
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Sets the status from a string value.
     * Handles different string formats like "TODO", "TO_DO", "To Do", etc.
     * 
     * @param statusStr The status as a string
     */
    public void setStatus(String statusStr) {
        if (statusStr == null || statusStr.isEmpty()) {
            this.status = Status.TO_DO;
            return;
        }
        
        // Normalize the status string
        String normalized = statusStr.toUpperCase().replace(" ", "_");
        
        try {
            // Try to match exact enum name
            this.status = Status.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Handle common variations
            if (normalized.equals("TODO")) {
                this.status = Status.TO_DO;
            } else if (normalized.equals("INPROGRESS") || normalized.equals("IN_PROGRESS")) {
                this.status = Status.IN_PROGRESS;
            } else if (normalized.equals("DONE")) {
                this.status = Status.DONE;
            } else {
                // Default to TO_DO if no match
                this.status = Status.TO_DO;
            }
        }
    }

    public TeamMember getAssignedTeamMember() {
        return assignedTeamMember;
    }

    public void setAssignedTeamMember(TeamMember assignedTeamMember) {
        // If already assigned to someone else, remove from their list
        if (this.assignedTeamMember != null && !this.assignedTeamMember.equals(assignedTeamMember)) {
            this.assignedTeamMember.removeTask(this);
        }
        
        this.assignedTeamMember = assignedTeamMember;
        
        // Add to new team member's list if not null
        if (assignedTeamMember != null && !assignedTeamMember.getAssignedTasks().contains(this)) {
            assignedTeamMember.getAssignedTasks().add(this);
        }
    }

    public UserStory getParentStory() {
        return parentStory;
    }

    public void setParentStory(UserStory parentStory) {
        this.parentStory = parentStory;
    }

    public String getGithubIssueUrl() {
        return githubIssueUrl;
    }

    public void setGithubIssueUrl(String githubIssueUrl) {
        this.githubIssueUrl = githubIssueUrl;
    }

    /**
     * Creates a copy of this task with a new ID.
     * Does not copy parent story or assigned team member to avoid circular references.
     * 
     * @return A new task with the same title, description and status
     */
    public Task copy() {
        Task copy = new Task(this.title, this.description);
        copy.setStatus(this.status);
        copy.setGithubIssueUrl(this.githubIssueUrl);
        return copy;
    }

    /**
     * Updates this task with values from another task.
     * This does not update the parent story, ID, or add bidirectional links.
     * Null values from other task are ignored to prevent overwriting existing data.
     * 
     * @param other The task to copy values from
     */
    public void updateFrom(Task other) {
        if (other.title != null) {
            this.title = other.title;
        }
        if (other.description != null) {
            this.description = other.description;
        }
        if (other.status != null) {
            this.status = other.status;
        }
        if (other.githubIssueUrl != null) {
            this.githubIssueUrl = other.githubIssueUrl;
        }
        
        // Only update assignedTeamMember if it's not null and different
        if (other.assignedTeamMember != null && !Objects.equals(this.assignedTeamMember, other.assignedTeamMember)) {
            setAssignedTeamMember(other.assignedTeamMember);
        }
    }
    
    /**
     * Determines if the task is complete.
     * 
     * @return true if status is DONE, false otherwise
     */
    public boolean isComplete() {
        return status == Status.DONE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return Objects.equals(id, task.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(title);
        
        if (assignedTeamMember != null) {
            sb.append(" (Assigned to: ").append(assignedTeamMember.getName()).append(")");
        }
        
        sb.append(" - ").append(status.getDisplayName());
        
        return sb.toString();
    }
} 