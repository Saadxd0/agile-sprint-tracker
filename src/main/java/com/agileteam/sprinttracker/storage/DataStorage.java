package com.agileteam.sprinttracker.storage;

import com.agileteam.sprinttracker.manager.SprintManager;
import com.agileteam.sprinttracker.model.Sprint;
import com.agileteam.sprinttracker.model.Task;
import com.agileteam.sprinttracker.model.TeamMember;
import com.agileteam.sprinttracker.model.UserStory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Responsible for saving and loading project data.
 */
public class DataStorage {
    private static final String DATA_DIRECTORY = "data";
    private static final String SPRINTS_FILE = DATA_DIRECTORY + File.separator + "sprints.json";
    private static final String TEAM_MEMBERS_FILE = DATA_DIRECTORY + File.separator + "team_members.json";
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    // Custom TypeAdapters for serializing/deserializing LocalDate
    private static class LocalDateAdapter extends TypeAdapter<LocalDate> {
        @Override
        public void write(JsonWriter out, LocalDate value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.format(DATE_FORMATTER));
            }
        }

        @Override
        public LocalDate read(JsonReader in) throws IOException {
            String dateStr = in.nextString();
            return (dateStr == null || dateStr.isEmpty()) ? null : LocalDate.parse(dateStr, DATE_FORMATTER);
        }
    }
    
    // Initialize Gson with custom adapters
    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .create();
    
    public DataStorage() {
        // Create the data directory if it doesn't exist
        File dir = new File(DATA_DIRECTORY);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    /**
     * Saves all sprint and team member data to files.
     * @param sprintManager The sprint manager containing all data
     * @throws IOException If there was an error writing to files
     */
    public void saveData(SprintManager sprintManager) throws IOException {
        saveTeamMembers(sprintManager.getAllTeamMembers());
        saveSprints(sprintManager.getAllSprints(), sprintManager.getCurrentSprint());
    }
    
    /**
     * Loads all sprint and team member data from files.
     * @return A populated SprintManager
     * @throws IOException If there was an error reading from files
     */
    public SprintManager loadData() throws IOException {
        SprintManager sprintManager = new SprintManager();
        
        // First load team members so we can reference them when loading sprints
        List<TeamMember> teamMembers = loadTeamMembers();
        for (TeamMember member : teamMembers) {
            sprintManager.addTeamMember(member);
        }
        
        // Load sprints and set the current sprint
        Sprint currentSprint = loadSprints(sprintManager);
        if (currentSprint != null) {
            sprintManager.setCurrentSprint(currentSprint);
        }
        
        return sprintManager;
    }
    
    /**
     * Saves team members to a JSON file.
     */
    private void saveTeamMembers(List<TeamMember> teamMembers) throws IOException {
        JsonArray membersArray = new JsonArray();
        
        for (TeamMember member : teamMembers) {
            JsonObject memberObj = new JsonObject();
            memberObj.addProperty("id", member.getName());  // Using name as ID for simplicity
            memberObj.addProperty("name", member.getName());
            memberObj.addProperty("email", member.getEmail());
            memberObj.addProperty("githubUsername", member.getGithubUsername());
            
            // We don't save tasks here, as they'll be saved with the sprints
            membersArray.add(memberObj);
        }
        
        try (FileWriter writer = new FileWriter(TEAM_MEMBERS_FILE)) {
            gson.toJson(membersArray, writer);
        }
    }
    
    /**
     * Loads team members from a JSON file.
     */
    private List<TeamMember> loadTeamMembers() throws IOException {
        List<TeamMember> teamMembers = new ArrayList<>();
        File file = new File(TEAM_MEMBERS_FILE);
        
        if (!file.exists()) {
            return teamMembers;  // Return empty list if file doesn't exist
        }
        
        try (FileReader reader = new FileReader(file)) {
            JsonArray membersArray = JsonParser.parseReader(reader).getAsJsonArray();
            
            for (JsonElement element : membersArray) {
                JsonObject memberObj = element.getAsJsonObject();
                String name = memberObj.get("name").getAsString();
                String email = memberObj.has("email") ? memberObj.get("email").getAsString() : null;
                String githubUsername = memberObj.has("githubUsername") ? 
                        memberObj.get("githubUsername").getAsString() : null;
                
                teamMembers.add(new TeamMember(name, email, githubUsername));
            }
        }
        
        return teamMembers;
    }
    
    /**
     * Saves sprints to a JSON file.
     */
    private void saveSprints(List<Sprint> sprints, Sprint currentSprint) throws IOException {
        JsonObject rootObj = new JsonObject();
        JsonArray sprintsArray = new JsonArray();
        
        // Save the ID of the current sprint
        if (currentSprint != null) {
            rootObj.addProperty("currentSprintId", currentSprint.getId());
        }
        
        // Save each sprint
        for (Sprint sprint : sprints) {
            JsonObject sprintObj = new JsonObject();
            sprintObj.addProperty("id", sprint.getId());
            sprintObj.addProperty("name", sprint.getName());
            sprintObj.addProperty("startDate", sprint.getStartDate().format(DATE_FORMATTER));
            sprintObj.addProperty("endDate", sprint.getEndDate().format(DATE_FORMATTER));
            sprintObj.addProperty("goal", sprint.getGoal());
            
            // Save user stories
            JsonArray storiesArray = new JsonArray();
            for (UserStory story : sprint.getUserStories()) {
                JsonObject storyObj = new JsonObject();
                storyObj.addProperty("id", story.getId());
                storyObj.addProperty("title", story.getTitle());
                storyObj.addProperty("description", story.getDescription());
                storyObj.addProperty("priority", story.getPriority().name());
                storyObj.addProperty("storyPoints", story.getStoryPoints());
                
                // Save tasks
                JsonArray tasksArray = new JsonArray();
                for (Task task : story.getTasks()) {
                    JsonObject taskObj = new JsonObject();
                    taskObj.addProperty("id", task.getId());
                    taskObj.addProperty("title", task.getTitle());
                    taskObj.addProperty("description", task.getDescription());
                    taskObj.addProperty("status", task.getStatus().name());
                    
                    if (task.getGithubIssueUrl() != null) {
                        taskObj.addProperty("githubIssueUrl", task.getGithubIssueUrl());
                    }
                    
                    // Save assigned team member (just save the name as reference)
                    if (task.getAssignedTeamMember() != null) {
                        taskObj.addProperty("assignedTeamMember", task.getAssignedTeamMember().getName());
                    }
                    
                    tasksArray.add(taskObj);
                }
                storyObj.add("tasks", tasksArray);
                
                storiesArray.add(storyObj);
            }
            sprintObj.add("userStories", storiesArray);
            
            sprintsArray.add(sprintObj);
        }
        
        rootObj.add("sprints", sprintsArray);
        
        try (FileWriter writer = new FileWriter(SPRINTS_FILE)) {
            gson.toJson(rootObj, writer);
        }
    }
    
    /**
     * Loads sprints from a JSON file and connects them to team members.
     * @return The current sprint, or null if none was saved
     */
    private Sprint loadSprints(SprintManager sprintManager) throws IOException {
        File file = new File(SPRINTS_FILE);
        Sprint currentSprint = null;
        Map<String, TeamMember> teamMemberMap = new HashMap<>();
        
        // Create a map of team member names to objects for quick lookup
        for (TeamMember member : sprintManager.getAllTeamMembers()) {
            teamMemberMap.put(member.getName(), member);
        }
        
        if (!file.exists()) {
            return null;  // Return null if file doesn't exist
        }
        
        try (FileReader reader = new FileReader(file)) {
            JsonObject rootObj = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray sprintsArray = rootObj.getAsJsonArray("sprints");
            String currentSprintId = rootObj.has("currentSprintId") ? 
                    rootObj.get("currentSprintId").getAsString() : null;
            
            Map<String, Sprint> sprintMap = new HashMap<>();
            
            // First pass: create all sprints
            for (JsonElement element : sprintsArray) {
                JsonObject sprintObj = element.getAsJsonObject();
                String id = sprintObj.get("id").getAsString();
                String name = sprintObj.get("name").getAsString();
                LocalDate startDate = LocalDate.parse(sprintObj.get("startDate").getAsString(), DATE_FORMATTER);
                LocalDate endDate = LocalDate.parse(sprintObj.get("endDate").getAsString(), DATE_FORMATTER);
                String goal = sprintObj.has("goal") ? sprintObj.get("goal").getAsString() : "";
                
                Sprint sprint = new Sprint(name, startDate, endDate, goal);
                // We need to manually set the ID to match the saved one
                try {
                    java.lang.reflect.Field idField = Sprint.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(sprint, id);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                sprintManager.addSprint(sprint);
                sprintMap.put(id, sprint);
                
                // Check if this is the current sprint
                if (id.equals(currentSprintId)) {
                    currentSprint = sprint;
                }
            }
            
            // Second pass: add user stories and tasks
            for (JsonElement element : sprintsArray) {
                JsonObject sprintObj = element.getAsJsonObject();
                String sprintId = sprintObj.get("id").getAsString();
                Sprint sprint = sprintMap.get(sprintId);
                
                if (sprint == null) continue;
                
                JsonArray storiesArray = sprintObj.getAsJsonArray("userStories");
                for (JsonElement storyElement : storiesArray) {
                    JsonObject storyObj = storyElement.getAsJsonObject();
                    String storyId = storyObj.get("id").getAsString();
                    String title = storyObj.get("title").getAsString();
                    String description = storyObj.get("description").getAsString();
                    UserStory.Priority priority = UserStory.Priority.valueOf(
                            storyObj.get("priority").getAsString());
                    int storyPoints = storyObj.get("storyPoints").getAsInt();
                    
                    UserStory story = new UserStory(title, description, priority, storyPoints);
                    // Set the ID manually
                    try {
                        java.lang.reflect.Field idField = UserStory.class.getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(story, storyId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    
                    sprint.addUserStory(story);
                    
                    // Add tasks
                    JsonArray tasksArray = storyObj.getAsJsonArray("tasks");
                    for (JsonElement taskElement : tasksArray) {
                        JsonObject taskObj = taskElement.getAsJsonObject();
                        String taskId = taskObj.get("id").getAsString();
                        String taskTitle = taskObj.get("title").getAsString();
                        String taskDescription = taskObj.get("description").getAsString();
                        Task.Status status = Task.Status.valueOf(taskObj.get("status").getAsString());
                        
                        Task task = new Task(taskTitle, taskDescription);
                        task.setStatus(status);
                        
                        // Set GitHub issue URL if it exists
                        if (taskObj.has("githubIssueUrl")) {
                            task.setGithubIssueUrl(taskObj.get("githubIssueUrl").getAsString());
                        }
                        
                        // Set the ID manually
                        try {
                            java.lang.reflect.Field idField = Task.class.getDeclaredField("id");
                            idField.setAccessible(true);
                            idField.set(task, taskId);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        
                        story.addTask(task);
                        
                        // Assign team member if one was assigned
                        if (taskObj.has("assignedTeamMember")) {
                            String teamMemberName = taskObj.get("assignedTeamMember").getAsString();
                            TeamMember member = teamMemberMap.get(teamMemberName);
                            if (member != null) {
                                task.setAssignedTeamMember(member);
                            }
                        }
                    }
                }
            }
        }
        
        return currentSprint;
    }
} 