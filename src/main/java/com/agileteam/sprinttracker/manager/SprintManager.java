package com.agileteam.sprinttracker.manager;

import com.agileteam.sprinttracker.model.Sprint;
import com.agileteam.sprinttracker.model.Task;
import com.agileteam.sprinttracker.model.TeamMember;
import com.agileteam.sprinttracker.model.UserStory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Manages the creation, deletion, and management of multiple sprints.
 */
public class SprintManager {
    private List<Sprint> sprints;
    private List<TeamMember> teamMembers;
    private Sprint currentSprint;

    public SprintManager() {
        this.sprints = new ArrayList<>();
        this.teamMembers = new ArrayList<>();
    }

    // Sprint management
    public List<Sprint> getAllSprints() {
        return sprints;
    }

    public void addSprint(Sprint sprint) {
        sprints.add(sprint);
        // If this is the first sprint or the only active sprint, set it as the current sprint
        if (currentSprint == null || (!currentSprint.isActive() && sprint.isActive())) {
            currentSprint = sprint;
        }
    }

    public boolean removeSprint(Sprint sprint) {
        boolean removed = sprints.remove(sprint);
        if (removed && currentSprint == sprint) {
            // If we removed the current sprint, try to set another active sprint as current
            currentSprint = sprints.stream()
                    .filter(Sprint::isActive)
                    .findFirst()
                    .orElse(sprints.isEmpty() ? null : sprints.get(0));
        }
        return removed;
    }

    public Optional<Sprint> getSprintById(String id) {
        return sprints.stream()
                .filter(sprint -> sprint.getId().equals(id))
                .findFirst();
    }

    public Sprint getCurrentSprint() {
        return currentSprint;
    }

    public void setCurrentSprint(Sprint sprint) {
        if (sprints.contains(sprint)) {
            currentSprint = sprint;
        } else {
            throw new IllegalArgumentException("Sprint is not managed by this sprint manager");
        }
    }

    // Team member management
    public List<TeamMember> getAllTeamMembers() {
        return teamMembers;
    }

    public void addTeamMember(TeamMember teamMember) {
        teamMembers.add(teamMember);
    }

    public boolean removeTeamMember(TeamMember teamMember) {
        // Unassign all tasks assigned to this team member
        List<Task> assignedTasks = teamMember.getAssignedTasks();
        for (Task task : new ArrayList<>(assignedTasks)) {
            task.setAssignedTeamMember(null);
        }
        
        return teamMembers.remove(teamMember);
    }

    public Optional<TeamMember> getTeamMemberByEmail(String email) {
        return teamMembers.stream()
                .filter(member -> member.getEmail() != null && member.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    public Optional<TeamMember> getTeamMemberByGithubUsername(String username) {
        return teamMembers.stream()
                .filter(member -> member.getGithubUsername() != null && 
                        member.getGithubUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    /**
     * Finds a team member by their ID.
     * 
     * @param id The ID of the team member to find
     * @return An Optional containing the team member if found, empty Optional otherwise
     */
    public Optional<TeamMember> getTeamMemberById(String id) {
        return teamMembers.stream()
                .filter(member -> member.getId().equals(id))
                .findFirst();
    }

    // Statistical methods
    public List<Sprint> getActiveSprintsSorted() {
        return sprints.stream()
                .filter(Sprint::isActive)
                .sorted(Comparator.comparing(Sprint::getEndDate))
                .collect(Collectors.toList());
    }

    public List<UserStory> getHighPriorityStories() {
        return sprints.stream()
                .flatMap(sprint -> sprint.getUserStories().stream())
                .filter(story -> story.getPriority() == UserStory.Priority.HIGH || 
                                story.getPriority() == UserStory.Priority.CRITICAL)
                .sorted(Comparator.comparing(UserStory::getPriority))
                .collect(Collectors.toList());
    }

    public List<Task> getTasksAssignedToMember(TeamMember teamMember) {
        return teamMember.getAssignedTasks();
    }

    public int getCompletedTasksCountForTeamMember(TeamMember teamMember) {
        return (int) teamMember.getAssignedTasks().stream()
                .filter(task -> task.getStatus() == Task.Status.DONE)
                .count();
    }
} 