package com.agileteam.sprinttracker.api;

import com.agileteam.sprinttracker.github.GitHubIntegration;
import com.agileteam.sprinttracker.manager.SprintManager;
import com.agileteam.sprinttracker.model.Sprint;
import com.agileteam.sprinttracker.model.Task;
import com.agileteam.sprinttracker.model.TeamMember;
import com.agileteam.sprinttracker.model.UserStory;
import com.agileteam.sprinttracker.storage.DataStorage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonParseException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.servlet.FilterHolder;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

public class ApiServer {
    private final SprintManager sprintManager;
    private final DataStorage dataStorage;
    private final Gson gson;
    private final int port;
    private Server server;
    private GitHubIntegration gitHubIntegration;

    public ApiServer(SprintManager sprintManager, DataStorage dataStorage) {
        this(sprintManager, dataStorage, 8080);
    }

    public ApiServer(SprintManager sprintManager, DataStorage dataStorage, int port) {
        this.sprintManager = sprintManager;
        this.dataStorage = dataStorage;
        this.port = port;
        
        // Create a custom Gson instance with LocalDate type adapters and exclusion strategies
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDate.class, new LocalDateSerializer())
            .registerTypeAdapter(LocalDate.class, new LocalDateDeserializer())
            .setExclusionStrategies(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes f) {
                    // Skip fields that would cause circular references
                    return f.getName().equals("parentSprint") || 
                           f.getName().equals("parentStory") ||
                           f.getName().equals("assignedTasks") ||  // Skip team member's assigned tasks
                           f.getName().equals("userStories") ||    // Skip sprint's user stories in some contexts
                           f.getName().equals("tasks");            // Skip story's tasks in some contexts
                }

                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return false;
                }
            })
            .disableHtmlEscaping()  // Prevent HTML escaping in JSON
            .serializeNulls()       // Include null fields
            .create();
            
        this.gitHubIntegration = new GitHubIntegration("", "", "");
    }
    
    // Custom LocalDate serializer
    private static class LocalDateSerializer implements JsonSerializer<LocalDate> {
        private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

        @Override
        public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
            // Direct string conversion instead of using formatter.format()
            if (src == null) {
                return null;
            }
            return new JsonPrimitive(src.toString());
        }
    }

    // Custom LocalDate deserializer
    private static class LocalDateDeserializer implements JsonDeserializer<LocalDate> {
        @Override
        public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            try {
                if (json == null || json.isJsonNull()) {
                    return null;
                }
                String dateStr = json.getAsString();
                if (dateStr == null || dateStr.isEmpty()) {
                    return null;
                }
                return LocalDate.parse(dateStr);
            } catch (Exception e) {
                System.err.println("Error parsing date: " + json);
                e.printStackTrace();
                throw new JsonParseException("Error parsing date: " + json, e);
            }
        }
    }

    public void start() throws Exception {
        server = new Server(port);
        
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        
        // Add CORS filter
        CrossOriginFilter corsFilter = new CrossOriginFilter();
        FilterHolder corsFilterHolder = new FilterHolder(corsFilter);
        context.addFilter(corsFilterHolder, "/*", EnumSet.of(DispatcherType.REQUEST));
        
        // Simple API configuration - use a single dispatcher servlet
        context.addServlet(new ServletHolder(new ApiDispatcherServlet()), "/api/*");
        
        // Serve static files
        context.addServlet(new ServletHolder(new StaticFileServlet()), "/*");
        
        server.start();
        System.out.println("API Server started on port " + port);
    }
    
    public void stop() throws Exception {
        if (server != null) {
            server.stop();
        }
    }
    
    // Helper method to parse request body
    private <T> T parseRequestBody(HttpServletRequest req, Class<T> clazz) throws IOException {
        String body = req.getReader().lines().collect(Collectors.joining());
        return gson.fromJson(body, clazz);
    }

    // Helper method to write JSON response
    private void writeJsonResponse(HttpServletResponse resp, Object data) throws IOException {
        try {
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            
            // Create a custom GSON instance with more aggressive exclusion for this specific response
            // This helps prevent circular references in complex nested objects
            Gson responseGson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDate.class, new LocalDateSerializer())
                .setExclusionStrategies(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        // Exclude fields that commonly cause circular references
                        return f.getName().equals("parentSprint") || 
                               f.getName().equals("parentStory") ||
                               f.getName().equals("assignedTasks") ||
                               (f.getDeclaringClass() == Sprint.class && f.getName().equals("userStories")) ||
                               (f.getDeclaringClass() == UserStory.class && f.getName().equals("tasks"));
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return false;
                    }
                })
                .disableHtmlEscaping()
                .create();
                
            String json = responseGson.toJson(data);
            resp.getWriter().write(json);
        } catch (StackOverflowError e) {
            System.err.println("StackOverflowError when serializing to JSON: " + e.getMessage());
            e.printStackTrace();
            
            // Try to create a simplified response
            try {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                
                // Create a very simplified representation
                JsonObject simplifiedResponse = simplifyObject(data);
                resp.getWriter().write(simplifiedResponse.toString());
            } catch (Exception fallbackError) {
                // If all else fails, send a very basic error response
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Error serializing response due to possible circular reference");
                resp.getWriter().write(error.toString());
            }
        } catch (Exception e) {
            System.err.println("Error writing JSON response: " + e.getMessage());
            e.printStackTrace();
            
            // Send a simplified error response
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonObject error = new JsonObject();
            error.addProperty("error", "Error serializing response: " + e.getMessage());
            resp.getWriter().write(error.toString());
        }
    }
    
    /**
     * Creates a simplified JsonObject from a complex object to avoid circular references
     */
    private JsonObject simplifyObject(Object obj) {
        JsonObject result = new JsonObject();
        
        if (obj instanceof Sprint) {
            Sprint sprint = (Sprint) obj;
            result.addProperty("id", sprint.getId());
            result.addProperty("name", sprint.getName());
            
            if (sprint.getStartDate() != null) {
                result.addProperty("startDate", sprint.getStartDate().toString());
            }
            
            if (sprint.getEndDate() != null) {
                result.addProperty("endDate", sprint.getEndDate().toString());
            }
            
            result.addProperty("userStoriesCount", sprint.getUserStories().size());
            
        } else if (obj instanceof UserStory) {
            UserStory story = (UserStory) obj;
            result.addProperty("id", story.getId());
            result.addProperty("title", story.getTitle());
            result.addProperty("tasksCount", story.getTasks().size());
            
        } else if (obj instanceof Task) {
            Task task = (Task) obj;
            result.addProperty("id", task.getId());
            result.addProperty("title", task.getTitle());
            
            if (task.getStatus() != null) {
                result.addProperty("status", task.getStatus().name());
            }
            
        } else if (obj instanceof TeamMember) {
            TeamMember member = (TeamMember) obj;
            result.addProperty("id", member.getId());
            result.addProperty("name", member.getName());
            
        } else if (obj instanceof List) {
            JsonArray array = new JsonArray();
            List<?> list = (List<?>) obj;
            
            for (Object item : list) {
                if (item != null) {
                    array.add(simplifyObject(item));
                }
            }
            
            return result;
        } else {
            // For unknown types, just add an ID if possible
            try {
                java.lang.reflect.Method getId = obj.getClass().getMethod("getId");
                String id = (String) getId.invoke(obj);
                result.addProperty("id", id);
                result.addProperty("type", obj.getClass().getSimpleName());
            } catch (Exception e) {
                result.addProperty("error", "Unable to simplify object of type: " + obj.getClass().getName());
            }
        }
        
        return result;
    }

    // A single dispatcher servlet that handles all API requests
    private class ApiDispatcherServlet extends HttpServlet {
        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String method = req.getMethod();
            String pathInfo = req.getPathInfo();
            
            System.out.println("\n[API REQUEST] " + method + " " + pathInfo);
            
            if (pathInfo == null) {
                System.err.println("Path info is null");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            
            try {
                // Handle different API paths
                if (pathInfo.matches("^/sprints/?$")) {
                    // Sprints endpoints
                    System.out.println("Handling /sprints endpoint");
                    if (method.equals("GET")) {
                        handleGetAllSprints(req, resp);
                    } else if (method.equals("POST")) {
                        handleCreateSprint(req, resp);
                    } else {
                        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    }
                } else if (pathInfo.matches("^/sprints/[^/]+/?$")) {
                    // Single sprint endpoint
                    String sprintId = pathInfo.substring("/sprints/".length()).replace("/", "");
                    System.out.println("Handling /sprints/{id} endpoint, sprintId: " + sprintId);
                    
                    if (method.equals("GET")) {
                        handleGetSprintById(sprintId, req, resp);
                    } else if (method.equals("PUT")) {
                        handleUpdateSprint(sprintId, req, resp);
                    } else if (method.equals("DELETE")) {
                        handleDeleteSprint(sprintId, req, resp);
                    } else {
                        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    }
                } else if (pathInfo.matches("^/sprints/[^/]+/stories/?$")) {
                    // User stories for a sprint
                    String sprintId = pathInfo.substring("/sprints/".length(), pathInfo.indexOf("/stories"));
                    System.out.println("Handling /sprints/{id}/stories endpoint, sprintId: " + sprintId);
                    
                    if (method.equals("GET")) {
                        handleGetUserStories(sprintId, req, resp);
                    } else if (method.equals("POST")) {
                        System.out.println("Creating a user story for sprint: " + sprintId);
                        handleCreateUserStory(sprintId, req, resp);
                    } else {
                        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    }
                } else if (pathInfo.matches("^/sprints/[^/]+/stories/[^/]+/?$")) {
                    // Single user story endpoint
                    String sprintId = pathInfo.substring("/sprints/".length(), pathInfo.indexOf("/stories"));
                    String storyId = pathInfo.substring(pathInfo.indexOf("/stories/") + "/stories/".length()).replace("/", "");
                    System.out.println("Handling /sprints/{id}/stories/{id} endpoint, sprintId: " + sprintId + ", storyId: " + storyId);
                    
                    if (method.equals("GET")) {
                        handleGetUserStoryById(sprintId, storyId, req, resp);
                    } else if (method.equals("PUT")) {
                        handleUpdateUserStory(sprintId, storyId, req, resp);
                    } else if (method.equals("DELETE")) {
                        handleDeleteUserStory(sprintId, storyId, req, resp);
                    } else {
                        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    }
                } else if (pathInfo.matches("^/sprints/[^/]+/stories/[^/]+/tasks/?$")) {
                    // Tasks for a user story
                    String sprintId = pathInfo.substring("/sprints/".length(), pathInfo.indexOf("/stories"));
                    String storyId = pathInfo.substring(pathInfo.indexOf("/stories/") + "/stories/".length(), pathInfo.indexOf("/tasks"));
                    System.out.println("Handling /sprints/{id}/stories/{id}/tasks endpoint, sprintId: " + sprintId + ", storyId: " + storyId);
                    
                    if (method.equals("GET")) {
                        handleGetTasks(sprintId, storyId, req, resp);
                    } else if (method.equals("POST")) {
                        handleCreateTask(sprintId, storyId, req, resp);
                    } else {
                        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    }
                } else if (pathInfo.matches("^/sprints/[^/]+/stories/[^/]+/tasks/[^/]+/?$")) {
                    // Single task endpoint
                    String sprintId = pathInfo.substring("/sprints/".length(), pathInfo.indexOf("/stories"));
                    String storyId = pathInfo.substring(pathInfo.indexOf("/stories/") + "/stories/".length(), pathInfo.indexOf("/tasks"));
                    String taskId = pathInfo.substring(pathInfo.indexOf("/tasks/") + "/tasks/".length()).replace("/", "");
                    System.out.println("Handling /sprints/{id}/stories/{id}/tasks/{id} endpoint, sprintId: " + sprintId + ", storyId: " + storyId + ", taskId: " + taskId);
                    
                    if (method.equals("GET")) {
                        handleGetTaskById(sprintId, storyId, taskId, req, resp);
                    } else if (method.equals("PUT")) {
                        handleUpdateTask(sprintId, storyId, taskId, req, resp);
                    } else if (method.equals("DELETE")) {
                        handleDeleteTask(sprintId, storyId, taskId, req, resp);
                    } else {
                        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    }
                } else if (pathInfo.matches("^/team-members/?$")) {
                    // Team members endpoints
                    System.out.println("Handling /team-members endpoint");
                    
                    if (method.equals("GET")) {
                        handleGetAllTeamMembers(req, resp);
                    } else if (method.equals("POST")) {
                        handleCreateTeamMember(req, resp);
                    } else {
                        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    }
                } else if (pathInfo.matches("^/team-members/[^/]+/?$")) {
                    // Single team member endpoint
                    String teamMemberId = pathInfo.substring("/team-members/".length()).replace("/", "");
                    System.out.println("Handling /team-members/{id} endpoint, teamMemberId: " + teamMemberId);
                    
                    if (method.equals("GET")) {
                        handleGetTeamMemberById(teamMemberId, req, resp);
                    } else if (method.equals("PUT")) {
                        handleUpdateTeamMember(teamMemberId, req, resp);
                    } else if (method.equals("DELETE")) {
                        handleDeleteTeamMember(teamMemberId, req, resp);
                    } else {
                        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    }
                } else if (pathInfo.matches("^/github/issues/?$")) {
                    // GitHub issues endpoints
                    System.out.println("Handling /github/issues endpoint");
                    
                    if (method.equals("GET")) {
                        handleGetGitHubIssues(req, resp);
                    } else if (method.equals("POST")) {
                        handleImportGitHubIssues(req, resp);
                    } else {
                        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    }
                } else {
                    // Unknown endpoint
                    System.err.println("Unknown API endpoint: " + pathInfo);
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.setContentType("application/json");
                    resp.getWriter().write("{\"error\": \"Unknown API endpoint: " + pathInfo + "\"}");
                }
            } catch (Exception e) {
                System.err.println("Exception in service method: " + e.getMessage());
                e.printStackTrace();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.setContentType("application/json");
                resp.getWriter().write("{\"error\": \"Internal server error: " + e.getMessage() + "\"}");
            }
        }
        
        private void handleGetAllSprints(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            try {
                List<Sprint> sprints = sprintManager.getAllSprints();
                
                // Flag to track if we fixed any null statuses
                boolean fixedNullStatus = false;
                
                // Create a simplified representation without circular references
                JsonArray sprintsArray = new JsonArray();
                for (Sprint sprint : sprints) {
                    JsonObject sprintObj = new JsonObject();
                    sprintObj.addProperty("id", sprint.getId());
                    sprintObj.addProperty("name", sprint.getName());
                    sprintObj.addProperty("startDate", sprint.getStartDate().toString());
                    sprintObj.addProperty("endDate", sprint.getEndDate().toString());
                    sprintObj.addProperty("goal", sprint.getGoal());
                    sprintObj.addProperty("active", sprint.isActive());
                    sprintObj.addProperty("totalStoryPoints", sprint.getTotalStoryPoints());
                    sprintObj.addProperty("completionPercentage", sprint.getCompletionPercentage());
                    
                    // Create a simplified array of user stories
                    JsonArray storiesArray = new JsonArray();
                    for (UserStory story : sprint.getUserStories()) {
                        JsonObject storyObj = new JsonObject();
                        storyObj.addProperty("id", story.getId());
                        storyObj.addProperty("title", story.getTitle());
                        storyObj.addProperty("description", story.getDescription());
                        storyObj.addProperty("priority", story.getPriority().name());
                        storyObj.addProperty("storyPoints", story.getStoryPoints());
                        storyObj.addProperty("completionPercentage", story.getCompletionPercentage());
                        
                        // Add a simplified tasks array
                        JsonArray tasksArray = new JsonArray();
                        for (Task task : story.getTasks()) {
                            JsonObject taskObj = new JsonObject();
                            taskObj.addProperty("id", task.getId());
                            taskObj.addProperty("title", task.getTitle());
                            taskObj.addProperty("description", task.getDescription());
                            
                            // Handle null task status
                            if (task.getStatus() != null) {
                                // Convert task status for frontend compatibility
                                String taskStatus = task.getStatus().name();
                                if ("TO_DO".equals(taskStatus)) {
                                    taskStatus = "TODO";
                                }
                                taskObj.addProperty("status", taskStatus);
                            } else {
                                // Default to TODO if status is null
                                taskObj.addProperty("status", "TODO");
                                
                                // Try to fix the task status in the database
                                try {
                                    task.setStatus(Task.Status.TO_DO);
                                    System.out.println("Fixed null status for task: " + task.getId());
                                    fixedNullStatus = true;
                                } catch (Exception e) {
                                    System.err.println("Failed to fix null status for task: " + task.getId());
                                }
                            }
                            
                            if (task.getAssignedTeamMember() != null) {
                                JsonObject memberObj = new JsonObject();
                                memberObj.addProperty("id", task.getAssignedTeamMember().getId());
                                memberObj.addProperty("name", task.getAssignedTeamMember().getName());
                                taskObj.add("assignedTeamMember", memberObj);
                            }
                            
                            tasksArray.add(taskObj);
                        }
                        storyObj.add("tasks", tasksArray);
                        
                        storiesArray.add(storyObj);
                    }
                    sprintObj.add("userStories", storiesArray);
                    
                    sprintsArray.add(sprintObj);
                }
                
                // Save data if we fixed any null statuses
                if (fixedNullStatus) {
                    try {
                        dataStorage.saveData(sprintManager);
                        System.out.println("Saved data after fixing null task statuses");
                    } catch (Exception e) {
                        System.err.println("Error saving data after fixing null task statuses: " + e.getMessage());
                    }
                }
                
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().write(sprintsArray.toString());
            } catch (Exception e) {
                System.err.println("Error getting all sprints: " + e.getMessage());
                e.printStackTrace();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Error getting all sprints: " + e.getMessage());
                resp.getWriter().write(error.toString());
            }
        }
        
        private void handleCreateSprint(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            Sprint sprint = parseRequestBody(req, Sprint.class);
            sprintManager.addSprint(sprint);
            dataStorage.saveData(sprintManager);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            writeJsonResponse(resp, sprint);
        }
        
        private void handleGetSprintById(String sprintId, HttpServletRequest req, HttpServletResponse resp) throws IOException {
            try {
                Optional<Sprint> sprintOpt = sprintManager.getSprintById(sprintId);
                if (!sprintOpt.isPresent()) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Sprint not found with ID: " + sprintId);
                    resp.getWriter().write(error.toString());
                    return;
                }
                
                Sprint sprint = sprintOpt.get();
                
                // Create a simplified representation without circular references
                JsonObject sprintObj = new JsonObject();
                sprintObj.addProperty("id", sprint.getId());
                sprintObj.addProperty("name", sprint.getName());
                sprintObj.addProperty("startDate", sprint.getStartDate().toString());
                sprintObj.addProperty("endDate", sprint.getEndDate().toString());
                sprintObj.addProperty("goal", sprint.getGoal());
                sprintObj.addProperty("active", sprint.isActive());
                sprintObj.addProperty("totalStoryPoints", sprint.getTotalStoryPoints());
                sprintObj.addProperty("completionPercentage", sprint.getCompletionPercentage());
                
                // Create a simplified array of user stories
                JsonArray storiesArray = new JsonArray();
                for (UserStory story : sprint.getUserStories()) {
                    JsonObject storyObj = new JsonObject();
                    storyObj.addProperty("id", story.getId());
                    storyObj.addProperty("title", story.getTitle());
                    storyObj.addProperty("description", story.getDescription());
                    storyObj.addProperty("priority", story.getPriority().name());
                    storyObj.addProperty("storyPoints", story.getStoryPoints());
                    storyObj.addProperty("completionPercentage", story.getCompletionPercentage());
                    
                    // Add a simplified tasks array
                    JsonArray tasksArray = new JsonArray();
                    for (Task task : story.getTasks()) {
                        JsonObject taskObj = new JsonObject();
                        taskObj.addProperty("id", task.getId());
                        taskObj.addProperty("title", task.getTitle());
                        taskObj.addProperty("description", task.getDescription());
                        
                        // Handle null task status
                        if (task.getStatus() != null) {
                            // Convert task status for frontend compatibility
                            String taskStatus = task.getStatus().name();
                            if ("TO_DO".equals(taskStatus)) {
                                taskStatus = "TODO";
                            }
                            taskObj.addProperty("status", taskStatus);
                        } else {
                            // Default to TODO if status is null
                            taskObj.addProperty("status", "TODO");
                            
                            // Try to fix the task status in the database
                            try {
                                task.setStatus(Task.Status.TO_DO);
                                System.out.println("Fixed null status for task: " + task.getId());
                                // Save the updated data
                                dataStorage.saveData(sprintManager);
                            } catch (Exception e) {
                                System.err.println("Failed to fix null status for task: " + task.getId());
                            }
                        }
                        
                        if (task.getAssignedTeamMember() != null) {
                            JsonObject memberObj = new JsonObject();
                            memberObj.addProperty("id", task.getAssignedTeamMember().getId());
                            memberObj.addProperty("name", task.getAssignedTeamMember().getName());
                            taskObj.add("assignedTeamMember", memberObj);
                        }
                        
                        tasksArray.add(taskObj);
                    }
                    storyObj.add("tasks", tasksArray);
                    
                    storiesArray.add(storyObj);
                }
                sprintObj.add("userStories", storiesArray);
                
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().write(sprintObj.toString());
            } catch (Exception e) {
                System.err.println("Error getting sprint by ID: " + e.getMessage());
                e.printStackTrace();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Error getting sprint by ID: " + e.getMessage());
                resp.getWriter().write(error.toString());
            }
        }
        
        private void handleUpdateSprint(String sprintId, HttpServletRequest req, HttpServletResponse resp) throws IOException {
            Sprint updatedSprint = parseRequestBody(req, Sprint.class);
            boolean success = updateSprint(sprintId, updatedSprint);
            
            if (!success) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            dataStorage.saveData(sprintManager);
            writeJsonResponse(resp, updatedSprint);
        }
        
        private void handleDeleteSprint(String sprintId, HttpServletRequest req, HttpServletResponse resp) throws IOException {
            Optional<Sprint> sprintOpt = sprintManager.getSprintById(sprintId);
            if (!sprintOpt.isPresent()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            boolean success = sprintManager.removeSprint(sprintOpt.get());
            if (!success) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            dataStorage.saveData(sprintManager);
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
        
        private void handleGetAllTeamMembers(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            List<TeamMember> teamMembers = sprintManager.getAllTeamMembers();
            writeJsonResponse(resp, teamMembers);
        }
        
        private void handleCreateTeamMember(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            TeamMember teamMember = parseRequestBody(req, TeamMember.class);
            sprintManager.addTeamMember(teamMember);
            
            dataStorage.saveData(sprintManager);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            writeJsonResponse(resp, teamMember);
        }
        
        private void handleGetTeamMemberById(String teamMemberId, HttpServletRequest req, HttpServletResponse resp) throws IOException {
            Optional<TeamMember> teamMemberOpt = sprintManager.getTeamMemberById(teamMemberId);
            if (!teamMemberOpt.isPresent()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Team member not found with ID: " + teamMemberId);
                resp.getWriter().write(error.toString());
                return;
            }
            
            writeJsonResponse(resp, teamMemberOpt.get());
        }
        
        private void handleUpdateTeamMember(String teamMemberId, HttpServletRequest req, HttpServletResponse resp) throws IOException {
            TeamMember updatedTeamMember = parseRequestBody(req, TeamMember.class);
            Optional<TeamMember> existingMemberOpt = sprintManager.getTeamMemberById(teamMemberId);
            
            if (!existingMemberOpt.isPresent()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            // Update the existing team member with data from the updated one
            TeamMember existingMember = existingMemberOpt.get();
            existingMember.setName(updatedTeamMember.getName());
            existingMember.setEmail(updatedTeamMember.getEmail());
            existingMember.setRole(updatedTeamMember.getRole());
            existingMember.setGithubUsername(updatedTeamMember.getGithubUsername());
            
            dataStorage.saveData(sprintManager);
            writeJsonResponse(resp, existingMember);
        }
        
        private void handleDeleteTeamMember(String teamMemberId, HttpServletRequest req, HttpServletResponse resp) throws IOException {
            Optional<TeamMember> teamMemberOpt = sprintManager.getTeamMemberById(teamMemberId);
            if (!teamMemberOpt.isPresent()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            boolean success = sprintManager.removeTeamMember(teamMemberOpt.get());
            if (!success) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            dataStorage.saveData(sprintManager);
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
        
        private void handleGetUserStories(String sprintId, HttpServletRequest req, HttpServletResponse resp) throws IOException {
            try {
                Optional<Sprint> sprintOpt = sprintManager.getSprintById(sprintId);
                if (!sprintOpt.isPresent()) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Sprint not found with ID: " + sprintId);
                    resp.getWriter().write(error.toString());
                    return;
                }
                
                List<UserStory> userStories = sprintOpt.get().getUserStories();
                
                // Create a simplified array of user stories
                JsonArray storiesArray = new JsonArray();
                for (UserStory story : userStories) {
                    JsonObject storyObj = new JsonObject();
                    storyObj.addProperty("id", story.getId());
                    storyObj.addProperty("title", story.getTitle());
                    storyObj.addProperty("description", story.getDescription());
                    storyObj.addProperty("priority", story.getPriority().name());
                    storyObj.addProperty("storyPoints", story.getStoryPoints());
                    storyObj.addProperty("completionPercentage", story.getCompletionPercentage());
                    
                    // Add a simplified tasks array
                    JsonArray tasksArray = new JsonArray();
                    for (Task task : story.getTasks()) {
                        JsonObject taskObj = new JsonObject();
                        taskObj.addProperty("id", task.getId());
                        taskObj.addProperty("title", task.getTitle());
                        taskObj.addProperty("description", task.getDescription());
                        
                        // Handle null task status
                        if (task.getStatus() != null) {
                            // Convert task status for frontend compatibility
                            String taskStatus = task.getStatus().name();
                            if ("TO_DO".equals(taskStatus)) {
                                taskStatus = "TODO";
                            }
                            taskObj.addProperty("status", taskStatus);
                        } else {
                            // Default to TODO if status is null
                            taskObj.addProperty("status", "TODO");
                            
                            // Try to fix the task status in the database
                            try {
                                task.setStatus(Task.Status.TO_DO);
                                System.out.println("Fixed null status for task: " + task.getId());
                            } catch (Exception e) {
                                System.err.println("Failed to fix null status for task: " + task.getId());
                            }
                        }
                        
                        if (task.getAssignedTeamMember() != null) {
                            JsonObject memberObj = new JsonObject();
                            memberObj.addProperty("id", task.getAssignedTeamMember().getId());
                            memberObj.addProperty("name", task.getAssignedTeamMember().getName());
                            taskObj.add("assignedTeamMember", memberObj);
                        }
                        
                        tasksArray.add(taskObj);
                    }
                    storyObj.add("tasks", tasksArray);
                    
                    storiesArray.add(storyObj);
                }
                
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().write(storiesArray.toString());
            } catch (Exception e) {
                System.err.println("Error getting user stories: " + e.getMessage());
                e.printStackTrace();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Error getting user stories: " + e.getMessage());
                resp.getWriter().write(error.toString());
            }
        }
        
        private void handleCreateUserStory(String sprintId, HttpServletRequest req, HttpServletResponse resp) throws IOException {
            try {
                // Log the request for debugging
                System.out.println("Creating user story for sprint: " + sprintId);
                String body = req.getReader().lines().collect(Collectors.joining());
                System.out.println("Request body: " + body);
                
                // Parse body manually to handle potential issues
                UserStory userStory = gson.fromJson(body, UserStory.class);
                
                if (userStory == null) {
                    System.out.println("Failed to parse user story from request");
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
                
                Optional<Sprint> sprintOpt = sprintManager.getSprintById(sprintId);
                if (!sprintOpt.isPresent()) {
                    System.out.println("Sprint not found: " + sprintId);
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                
                Sprint sprint = sprintOpt.get();
                sprint.addUserStory(userStory);
                dataStorage.saveData(sprintManager);
                resp.setStatus(HttpServletResponse.SC_CREATED);
                writeJsonResponse(resp, userStory);
                System.out.println("User story created successfully: " + userStory.getId());
            } catch (Exception e) {
                System.err.println("Error creating user story: " + e.getMessage());
                e.printStackTrace();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Error creating user story: " + e.getMessage());
                resp.getWriter().write(gson.toJson(error));
            }
        }
        
        private void handleGetUserStoryById(String sprintId, String storyId, HttpServletRequest req, HttpServletResponse resp) throws IOException {
            try {
                Optional<Sprint> sprintOpt = sprintManager.getSprintById(sprintId);
                if (!sprintOpt.isPresent()) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Sprint not found with ID: " + sprintId);
                    resp.getWriter().write(error.toString());
                    return;
                }
                
                Sprint sprint = sprintOpt.get();
                Optional<UserStory> userStoryOpt = sprint.getUserStoryById(storyId);
                if (!userStoryOpt.isPresent()) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "User story not found with ID: " + storyId);
                    resp.getWriter().write(error.toString());
                    return;
                }
                
                UserStory story = userStoryOpt.get();
                
                // Create a simplified JSON object
                JsonObject storyObj = new JsonObject();
                storyObj.addProperty("id", story.getId());
                storyObj.addProperty("title", story.getTitle());
                storyObj.addProperty("description", story.getDescription());
                storyObj.addProperty("priority", story.getPriority().name());
                storyObj.addProperty("storyPoints", story.getStoryPoints());
                storyObj.addProperty("completionPercentage", story.getCompletionPercentage());
                
                // Add a simplified tasks array
                JsonArray tasksArray = new JsonArray();
                for (Task task : story.getTasks()) {
                    JsonObject taskObj = new JsonObject();
                    taskObj.addProperty("id", task.getId());
                    taskObj.addProperty("title", task.getTitle());
                    taskObj.addProperty("description", task.getDescription());
                    
                    // Handle null task status
                    if (task.getStatus() != null) {
                        // Convert task status for frontend compatibility
                        String taskStatus = task.getStatus().name();
                        if ("TO_DO".equals(taskStatus)) {
                            taskStatus = "TODO";
                        }
                        taskObj.addProperty("status", taskStatus);
                    } else {
                        // Default to TODO if status is null
                        taskObj.addProperty("status", "TODO");
                        
                        // Try to fix the task status in the database
                        try {
                            task.setStatus(Task.Status.TO_DO);
                            System.out.println("Fixed null status for task: " + task.getId());
                            // Save the updated data
                            dataStorage.saveData(sprintManager);
                        } catch (Exception e) {
                            System.err.println("Failed to fix null status for task: " + task.getId());
                        }
                    }
                    
                    if (task.getAssignedTeamMember() != null) {
                        JsonObject memberObj = new JsonObject();
                        memberObj.addProperty("id", task.getAssignedTeamMember().getId());
                        memberObj.addProperty("name", task.getAssignedTeamMember().getName());
                        taskObj.add("assignedTeamMember", memberObj);
                    }
                    
                    tasksArray.add(taskObj);
                }
                storyObj.add("tasks", tasksArray);
                
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().write(storyObj.toString());
            } catch (Exception e) {
                System.err.println("Error getting user story by ID: " + e.getMessage());
                e.printStackTrace();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Error getting user story by ID: " + e.getMessage());
                resp.getWriter().write(error.toString());
            }
        }
        
        private void handleUpdateUserStory(String sprintId, String storyId, HttpServletRequest req, HttpServletResponse resp) throws IOException {
            UserStory updatedUserStory = parseRequestBody(req, UserStory.class);
            Optional<Sprint> sprintOpt = sprintManager.getSprintById(sprintId);
            if (!sprintOpt.isPresent()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            Sprint sprint = sprintOpt.get();
            Optional<UserStory> userStoryOpt = sprint.getUserStoryById(storyId);
            if (!userStoryOpt.isPresent()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            UserStory existingUserStory = userStoryOpt.get();
            existingUserStory.updateFrom(updatedUserStory);
            dataStorage.saveData(sprintManager);
            writeJsonResponse(resp, existingUserStory);
        }
        
        private void handleDeleteUserStory(String sprintId, String storyId, HttpServletRequest req, HttpServletResponse resp) throws IOException {
            Optional<Sprint> sprintOpt = sprintManager.getSprintById(sprintId);
            if (!sprintOpt.isPresent()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            Sprint sprint = sprintOpt.get();
            Optional<UserStory> userStoryOpt = sprint.getUserStoryById(storyId);
            if (!userStoryOpt.isPresent()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            boolean success = sprint.removeUserStory(userStoryOpt.get());
            if (!success) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            dataStorage.saveData(sprintManager);
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
        
        private void handleGetTasks(String sprintId, String storyId, HttpServletRequest req, HttpServletResponse resp) throws IOException {
            Optional<Sprint> sprintOpt = sprintManager.getSprintById(sprintId);
            if (!sprintOpt.isPresent()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            Optional<UserStory> userStoryOpt = sprintOpt.get().getUserStoryById(storyId);
            if (!userStoryOpt.isPresent()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            List<Task> tasks = userStoryOpt.get().getTasks();
            writeJsonResponse(resp, tasks);
        }
        
        private void handleCreateTask(String sprintId, String storyId, HttpServletRequest req, HttpServletResponse resp) throws IOException {
            try {
                System.out.println("Creating task for sprint: " + sprintId + ", story: " + storyId);
                String body = req.getReader().lines().collect(Collectors.joining());
                System.out.println("Request body: " + body);
                
                // Parse the task manually with proper error handling
                JsonObject taskJson;
                try {
                    taskJson = JsonParser.parseString(body).getAsJsonObject();
                } catch (Exception e) {
                    System.err.println("Failed to parse request JSON: " + e.getMessage());
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Invalid JSON in request: " + e.getMessage());
                    resp.getWriter().write(error.toString());
                    return;
                }
                
                // Create task with required fields
                String title = taskJson.has("title") ? taskJson.get("title").getAsString() : "New Task";
                String description = taskJson.has("description") ? taskJson.get("description").getAsString() : "";
                Task task = new Task(title, description);
                
                // Set optional fields if they exist
                if (taskJson.has("id") && !taskJson.get("id").isJsonNull()) {
                    // Use provided ID if available (unusual, but allowed)
                    String id = taskJson.get("id").getAsString();
                    try {
                        java.lang.reflect.Field idField = Task.class.getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(task, id);
                    } catch (Exception e) {
                        System.err.println("Failed to set task ID: " + e.getMessage());
                        // Continue with auto-generated ID
                    }
                }
                
                if (taskJson.has("status") && !taskJson.get("status").isJsonNull()) {
                    String status = taskJson.get("status").getAsString();
                    task.setStatus(status);
                    System.out.println("Task status set to: " + task.getStatus().name());

                    // For the response, make sure we use the format the frontend expects
                    String responseStatus = task.getStatus().name();
                    if (responseStatus.equals("TO_DO")) {
                        responseStatus = "TODO";
                    } else if (responseStatus.equals("IN_PROGRESS")) {
                        responseStatus = "IN_PROGRESS";
                    } else if (responseStatus.equals("DONE")) {
                        responseStatus = "DONE";
                    }
                    taskJson.addProperty("status", responseStatus);
                } else {
                    // Default to TO_DO if status is missing
                    task.setStatus(Task.Status.TO_DO);
                    System.out.println("Setting default status TO_DO for task: " + task.getId());
                    taskJson.addProperty("status", "TODO");
                }
                
                // Process assigned team member if present
                if (taskJson.has("assignedTeamMemberId") && !taskJson.get("assignedTeamMemberId").isJsonNull()) {
                    String teamMemberId = taskJson.get("assignedTeamMemberId").getAsString();
                    Optional<TeamMember> teamMemberOpt = sprintManager.getTeamMemberById(teamMemberId);
                    if (teamMemberOpt.isPresent()) {
                        task.setAssignedTeamMember(teamMemberOpt.get());
                    }
                }
                
                // Find the sprint and story
                Optional<Sprint> sprintOpt = sprintManager.getSprintById(sprintId);
                if (!sprintOpt.isPresent()) {
                    System.out.println("Sprint not found: " + sprintId);
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Sprint not found with ID: " + sprintId);
                    resp.getWriter().write(error.toString());
                    return;
                }
                
                Optional<UserStory> userStoryOpt = sprintOpt.get().getUserStoryById(storyId);
                if (!userStoryOpt.isPresent()) {
                    System.out.println("User story not found: " + storyId);
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "User story not found with ID: " + storyId);
                    resp.getWriter().write(error.toString());
                    return;
                }
                
                // Add task to story
                UserStory userStory = userStoryOpt.get();
                userStory.addTask(task);
                System.out.println("Added task to story. Status: " + task.getStatus().name() + 
                                   ", Display name: " + task.getStatus().getDisplayName() + 
                                   ", Tasks count: " + userStory.getTasks().size());
                
                // Save data
                dataStorage.saveData(sprintManager);
                
                // Create response
                JsonObject responseJson = new JsonObject();
                responseJson.addProperty("id", task.getId());
                responseJson.addProperty("title", task.getTitle());
                responseJson.addProperty("description", task.getDescription());
                
                // Convert backend status to frontend format
                String status = task.getStatus().name();
                if (status.equals("TO_DO")) {
                    status = "TODO";
                } else if (status.equals("IN_PROGRESS")) {
                    status = "IN_PROGRESS";
                } else if (status.equals("DONE")) {
                    status = "DONE";
                }
                responseJson.addProperty("status", status);
                responseJson.addProperty("statusDisplayName", task.getStatus().getDisplayName());
                
                if (task.getAssignedTeamMember() != null) {
                    JsonObject memberObj = new JsonObject();
                    memberObj.addProperty("id", task.getAssignedTeamMember().getId());
                    memberObj.addProperty("name", task.getAssignedTeamMember().getName());
                    responseJson.add("assignedTeamMember", memberObj);
                    responseJson.addProperty("assignedTeamMemberId", task.getAssignedTeamMember().getId());
                }
                
                // Return the created task
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().write(responseJson.toString());
                
                System.out.println("Task created successfully: " + task.getId());
            } catch (Exception e) {
                System.err.println("Error creating task: " + e.getMessage());
                e.printStackTrace();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Error creating task: " + e.getMessage());
                resp.getWriter().write(error.toString());
            }
        }
        
        private void handleGetTaskById(String sprintId, String storyId, String taskId, HttpServletRequest req, HttpServletResponse resp) throws IOException {
            Optional<Sprint> sprintOpt = sprintManager.getSprintById(sprintId);
            if (!sprintOpt.isPresent()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            Optional<UserStory> userStoryOpt = sprintOpt.get().getUserStoryById(storyId);
            if (!userStoryOpt.isPresent()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            Optional<Task> taskOpt = userStoryOpt.get().getTaskById(taskId);
            if (!taskOpt.isPresent()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            writeJsonResponse(resp, taskOpt.get());
        }
        
        private void handleUpdateTask(String sprintId, String storyId, String taskId, HttpServletRequest req, HttpServletResponse resp) throws IOException {
            try {
                String body = req.getReader().lines().collect(Collectors.joining());
                System.out.println("Update task request body: " + body);
                
                // Parse the request as a JsonObject first to access fields directly
                JsonObject taskJson = JsonParser.parseString(body).getAsJsonObject();
                Task updatedTask = gson.fromJson(body, Task.class);
                
                Optional<Sprint> sprintOpt = sprintManager.getSprintById(sprintId);
                if (!sprintOpt.isPresent()) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                
                Optional<UserStory> userStoryOpt = sprintOpt.get().getUserStoryById(storyId);
                if (!userStoryOpt.isPresent()) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                
                UserStory userStory = userStoryOpt.get();
                Optional<Task> taskOpt = userStory.getTaskById(taskId);
                if (!taskOpt.isPresent()) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                
                Task existingTask = taskOpt.get();
                
                // Check if updatedTask has null status and fix if needed
                if (updatedTask.getStatus() == null) {
                    // Keep existing status if the updated one is null
                    System.out.println("Updated task has null status, keeping existing status: " + 
                        (existingTask.getStatus() != null ? existingTask.getStatus().name() : "null"));
                    
                    // If existing task also has null status, set a default
                    if (existingTask.getStatus() == null) {
                        existingTask.setStatus(Task.Status.TO_DO);
                        System.out.println("Set default TO_DO status for task with null status");
                    }
                } else {
                    System.out.println("Updating task status to: " + updatedTask.getStatus().name());
                }
                
                // Process assigned team member if present in JSON
                if (taskJson.has("assignedTeamMemberId") && !taskJson.get("assignedTeamMemberId").isJsonNull()) {
                    String teamMemberId = taskJson.get("assignedTeamMemberId").getAsString();
                    if (!teamMemberId.isEmpty()) {
                        System.out.println("Assigning team member: " + teamMemberId + " to task: " + taskId);
                        Optional<TeamMember> teamMemberOpt = sprintManager.getTeamMemberById(teamMemberId);
                        if (teamMemberOpt.isPresent()) {
                            existingTask.setAssignedTeamMember(teamMemberOpt.get());
                            System.out.println("Team member assigned successfully");
                        } else {
                            System.err.println("Team member not found with ID: " + teamMemberId);
                        }
                    } else {
                        // Empty string means unassign
                        existingTask.setAssignedTeamMember(null);
                        System.out.println("Unassigned team member from task");
                    }
                }
                
                // Update other task properties
                existingTask.updateFrom(updatedTask);
                
                dataStorage.saveData(sprintManager);
                writeJsonResponse(resp, existingTask);
            } catch (Exception e) {
                System.err.println("Error updating task: " + e.getMessage());
                e.printStackTrace();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Error updating task: " + e.getMessage());
                resp.getWriter().write(gson.toJson(error));
            }
        }
        
        private void handleDeleteTask(String sprintId, String storyId, String taskId, HttpServletRequest req, HttpServletResponse resp) throws IOException {
            Optional<Sprint> sprintOpt = sprintManager.getSprintById(sprintId);
            if (!sprintOpt.isPresent()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            Optional<UserStory> userStoryOpt = sprintOpt.get().getUserStoryById(storyId);
            if (!userStoryOpt.isPresent()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            UserStory userStory = userStoryOpt.get();
            Optional<Task> taskOpt = userStory.getTaskById(taskId);
            if (!taskOpt.isPresent()) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            boolean success = userStory.removeTask(taskOpt.get());
            if (!success) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            dataStorage.saveData(sprintManager);
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }

        /**
         * Handle GET request for GitHub issues
         */
        private void handleGetGitHubIssues(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String owner = req.getParameter("owner");
            String repo = req.getParameter("repo");
            
            if (owner == null || repo == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Missing required parameters: owner and repo");
                resp.getWriter().write(error.toString());
                return;
            }
            
            try {
                // Create mock issues instead of actual GitHub API call (for demo purposes)
                List<GitHubIntegration.Issue> mockIssues = new ArrayList<>();
                mockIssues.add(new GitHubIntegration.Issue("1", "Fix login page", "Login page doesn't work on mobile devices", 
                        "open", "https://github.com/" + owner + "/" + repo + "/issues/1", null, 
                        List.of("bug", "frontend")));
                mockIssues.add(new GitHubIntegration.Issue("2", "Add user profile page", "Create a new page for user profiles", 
                        "open", "https://github.com/" + owner + "/" + repo + "/issues/2", null, 
                        List.of("enhancement", "frontend")));
                mockIssues.add(new GitHubIntegration.Issue("3", "Update database schema", "Need to update the schema for new features", 
                        "open", "https://github.com/" + owner + "/" + repo + "/issues/3", null, 
                        List.of("backend", "database")));
                
                // Convert to JSON response format expected by frontend
                JsonArray issuesArray = new JsonArray();
                for (GitHubIntegration.Issue issue : mockIssues) {
                    JsonObject issueObj = new JsonObject();
                    issueObj.addProperty("id", issue.getId());
                    issueObj.addProperty("number", issue.getId());
                    issueObj.addProperty("title", issue.getTitle());
                    issueObj.addProperty("description", issue.getBody());
                    issueObj.addProperty("url", issue.getHtmlUrl());
                    issueObj.addProperty("state", issue.getState());
                    
                    JsonArray labelsArray = new JsonArray();
                    for (String label : issue.getLabels()) {
                        labelsArray.add(label);
                    }
                    issueObj.add("labels", labelsArray);
                    
                    issuesArray.add(issueObj);
                }
                
                resp.setContentType("application/json");
                resp.getWriter().write(issuesArray.toString());
            } catch (Exception e) {
                System.err.println("Error fetching GitHub issues: " + e.getMessage());
                e.printStackTrace();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Error fetching GitHub issues: " + e.getMessage());
                resp.getWriter().write(error.toString());
            }
        }
        
        /**
         * Handle POST request to import GitHub issues as tasks
         */
        private void handleImportGitHubIssues(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String owner = req.getParameter("owner");
            String repo = req.getParameter("repo");
            String sprintId = req.getParameter("sprintId");
            String storyId = req.getParameter("userStoryId");
            
            if (owner == null || repo == null || sprintId == null || storyId == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Missing required parameters: owner, repo, sprintId, and userStoryId");
                resp.getWriter().write(error.toString());
                return;
            }
            
            try {
                Optional<Sprint> sprintOpt = sprintManager.getSprintById(sprintId);
                if (!sprintOpt.isPresent()) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "Sprint not found with ID: " + sprintId);
                    resp.getWriter().write(error.toString());
                    return;
                }
                
                Optional<UserStory> storyOpt = sprintOpt.get().getUserStoryById(storyId);
                if (!storyOpt.isPresent()) {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    JsonObject error = new JsonObject();
                    error.addProperty("error", "User story not found with ID: " + storyId);
                    resp.getWriter().write(error.toString());
                    return;
                }
                
                UserStory story = storyOpt.get();
                
                // Create a mock task (for demo purposes)
                Task task = new Task("Imported GitHub Issue", "This task was imported from GitHub");
                task.setGithubIssueUrl("https://github.com/" + owner + "/" + repo + "/issues/1");
                
                // Add the task to the story
                story.addTask(task);
                
                // Save data
                dataStorage.saveData(sprintManager);
                
                // Return success response
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.setContentType("application/json");
                JsonObject response = new JsonObject();
                response.addProperty("success", true);
                response.addProperty("message", "GitHub issue imported successfully");
                resp.getWriter().write(response.toString());
            } catch (Exception e) {
                System.err.println("Error importing GitHub issues: " + e.getMessage());
                e.printStackTrace();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                JsonObject error = new JsonObject();
                error.addProperty("error", "Error importing GitHub issues: " + e.getMessage());
                resp.getWriter().write(error.toString());
            }
        }
    }
    
    private class StaticFileServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Serve static files from resources/static directory
            String path = req.getPathInfo();
            System.out.println("Requested path: " + path);
            
            if (path == null || path.equals("/")) {
                path = "/index.html";
                System.out.println("Root path, serving index.html");
            }
            
            try {
                // Serve the file from resources/static
                String contentType = getContentType(path);
                resp.setContentType(contentType);
                System.out.println("Content type: " + contentType);
                
                if (path.startsWith("/")) path = path.substring(1);
                
                String resourcePath = "static/" + path;
                System.out.println("Looking for resource: " + resourcePath);
                
                java.net.URL resource = getClass().getClassLoader().getResource(resourcePath);
                if (resource == null) {
                    System.err.println("Resource not found: " + resourcePath);
                    
                    // For client-side routing, serve index.html for paths that don't exist
                    if (!path.contains(".")) {
                        System.out.println("Possible client-side route, serving index.html");
                        resource = getClass().getClassLoader().getResource("static/index.html");
                        resp.setContentType("text/html");
                    }
                    
                    if (resource == null) {
                        System.err.println("Could not find fallback resource");
                        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        resp.getWriter().write("Error 404: Resource not found - " + path);
                        return;
                    }
                }
                
                try {
                    org.eclipse.jetty.util.IO.copy(resource.openStream(), resp.getOutputStream());
                    System.out.println("Successfully served: " + path);
                } catch (Exception e) {
                    System.err.println("Error copying resource stream for: " + path);
                    e.printStackTrace();
                    throw e;
                }
            } catch (Exception e) {
                System.err.println("Error serving static file: " + path + " - " + e.getMessage());
                e.printStackTrace();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.setContentType("text/html");
                resp.getWriter().write("<html><body><h1>Error 500: Internal Server Error</h1><p>Error serving static file: " 
                    + path + "</p><p>Error details: " + e.getMessage() + "</p></body></html>");
            }
        }
        
        private String getContentType(String path) {
            if (path.endsWith(".html")) return "text/html";
            if (path.endsWith(".css")) return "text/css";
            if (path.endsWith(".js")) return "application/javascript";
            if (path.endsWith(".json")) return "application/json";
            if (path.endsWith(".png")) return "image/png";
            if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
            if (path.endsWith(".svg")) return "image/svg+xml";
            return "text/plain";
        }
    }
    
    // Helper methods
    
    // Method to update a sprint
    private boolean updateSprint(String sprintId, Sprint updatedSprint) {
        Optional<Sprint> existingSprintOpt = sprintManager.getSprintById(sprintId);
        if (!existingSprintOpt.isPresent()) {
            return false;
        }
        
        Sprint existingSprint = existingSprintOpt.get();
        existingSprint.setName(updatedSprint.getName());
        existingSprint.setStartDate(updatedSprint.getStartDate());
        existingSprint.setEndDate(updatedSprint.getEndDate());
        existingSprint.setGoal(updatedSprint.getGoal());
        
        return true;
    }
    
    // Method to get a user story by ID
    private UserStory getUserStoryById(Sprint sprint, String userStoryId) {
        if (sprint == null || userStoryId == null) {
            return null;
        }
        
        Optional<UserStory> userStoryOpt = sprint.getUserStoryById(userStoryId);
        return userStoryOpt.orElse(null);
    }
    
    // Method to update a user story
    private boolean updateUserStory(Sprint sprint, String userStoryId, UserStory updatedUserStory) {
        UserStory existingStory = getUserStoryById(sprint, userStoryId);
        if (existingStory == null) {
            return false;
        }
        
        existingStory.updateFrom(updatedUserStory);
        return true;
    }
    
    // Method to get a task by ID
    private Task getTaskById(UserStory userStory, String taskId) {
        if (userStory == null || taskId == null) {
            return null;
        }
        
        Optional<Task> taskOpt = userStory.getTaskById(taskId);
        return taskOpt.orElse(null);
    }
    
    // Method to update a task
    private boolean updateTask(UserStory userStory, String taskId, Task updatedTask) {
        Task existingTask = getTaskById(userStory, taskId);
        if (existingTask == null) {
            return false;
        }
        
        existingTask.updateFrom(updatedTask);
        return true;
    }
    
    // Method to remove a task
    private boolean removeTask(UserStory userStory, String taskId) {
        Task task = getTaskById(userStory, taskId);
        if (task == null) {
            return false;
        }
        
        return userStory.removeTask(task);
    }
} 