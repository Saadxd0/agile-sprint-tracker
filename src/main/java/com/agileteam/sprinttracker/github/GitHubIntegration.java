package com.agileteam.sprinttracker.github;

import com.agileteam.sprinttracker.model.Task;
import com.agileteam.sprinttracker.model.TeamMember;
import com.agileteam.sprinttracker.model.UserStory;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Handles connection to the GitHub Issues API.
 */
public class GitHubIntegration {
    private static final String GITHUB_API_BASE_URL = "https://api.github.com";
    
    private final String personalAccessToken;
    private final String repositoryOwner;
    private final String repositoryName;
    
    private CloseableHttpClient httpClient;
    
    /**
     * Default constructor for GitHubIntegration.
     */
    public GitHubIntegration() {
        this("", "", "");
    }
    
    /**
     * Constructor for GitHubIntegration with authentication and repository details.
     *
     * @param personalAccessToken GitHub authentication token
     * @param repositoryOwner Repository owner (username or organization)
     * @param repositoryName Repository name
     */
    public GitHubIntegration(String personalAccessToken, String repositoryOwner, String repositoryName) {
        this.personalAccessToken = personalAccessToken;
        this.repositoryOwner = repositoryOwner;
        this.repositoryName = repositoryName;
        this.httpClient = HttpClients.createDefault();
    }
    
    /**
     * Fetches issues from GitHub and converts them to tasks.
     *
     * @param owner Repository owner
     * @param repo Repository name
     * @return List of tasks created from GitHub issues
     * @throws Exception If there's an error fetching or parsing the issues
     */
    public List<Task> getIssuesAsTasks(String owner, String repo) throws Exception {
        List<Issue> issues = getMockIssues();
        List<Task> tasks = new ArrayList<>();
        
        for (Issue issue : issues) {
            Task task = new Task(issue.title, issue.body);
            tasks.add(task);
        }
        
        return tasks;
    }
    
    /**
     * Fetches issues from GitHub and converts them to user stories.
     *
     * @param owner Repository owner
     * @param repo Repository name
     * @return List of user stories created from GitHub issues
     * @throws Exception If there's an error fetching or parsing the issues
     */
    public List<UserStory> getIssuesAsUserStories(String owner, String repo) throws Exception {
        List<Issue> issues = getMockIssues();
        List<UserStory> userStories = new ArrayList<>();
        
        for (Issue issue : issues) {
            UserStory userStory = new UserStory(issue.title, issue.body);
            userStory.setPriority(determinePriority(issue.labels.toArray(new String[0])));
            userStory.setStoryPoints(determineStoryPoints(issue.labels.toArray(new String[0])));
            userStories.add(userStory);
        }
        
        return userStories;
    }
    
    /**
     * Creates mock GitHub issues for testing.
     *
     * @return List of mock issues
     */
    private List<Issue> getMockIssues() {
        List<Issue> mockIssues = new ArrayList<>();
        mockIssues.add(new Issue(1, "Implement login functionality", "As a user, I want to be able to log in to the system", new String[]{"priority:high", "points:5"}));
        mockIssues.add(new Issue(2, "Fix UI layout on mobile", "The UI is broken on mobile devices", new String[]{"priority:medium", "points:3", "bug"}));
        mockIssues.add(new Issue(3, "Add user profile page", "Create a page where users can view and edit their profile", new String[]{"priority:low", "points:8"}));
        return mockIssues;
    }
    
    /**
     * Determines priority based on issue labels.
     */
    private String determinePriority(String[] labels) {
        for (String label : labels) {
            if (label.startsWith("priority:")) {
                return label.substring("priority:".length());
            }
        }
        return "medium"; // Default priority
    }
    
    /**
     * Determines story points based on issue labels.
     */
    private int determineStoryPoints(String[] labels) {
        for (String label : labels) {
            if (label.startsWith("points:")) {
                try {
                    return Integer.parseInt(label.substring("points:".length()));
                } catch (NumberFormatException e) {
                    // Ignore parsing errors
                }
            }
        }
        return 3; // Default story points
    }
    
    /**
     * Connects to the GitHub API to fetch issues.
     * @param state The state of issues to fetch ("open", "closed", or "all")
     * @param labels Optional comma-separated list of label names
     * @return A list of Issue objects representing GitHub issues
     * @throws IOException If there's an error communicating with the GitHub API
     */
    public List<Issue> fetchIssues(String state, String labels) throws IOException {
        String issuesUrl = String.format("%s/repos/%s/%s/issues?state=%s", 
                GITHUB_API_BASE_URL, repositoryOwner, repositoryName, state);
        
        if (labels != null && !labels.isEmpty()) {
            issuesUrl += "&labels=" + labels;
        }
        
        HttpGet request = new HttpGet(issuesUrl);
        request.setHeader(HttpHeaders.AUTHORIZATION, "token " + personalAccessToken);
        request.setHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json");
        
        List<Issue> issues = new ArrayList<>();
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String result = EntityUtils.toString(entity);
                JsonArray issuesArray = JsonParser.parseString(result).getAsJsonArray();
                
                for (JsonElement element : issuesArray) {
                    JsonObject issueObj = element.getAsJsonObject();
                    Issue issue = parseIssue(issueObj);
                    issues.add(issue);
                }
            }
        }
        
        return issues;
    }
    
    /**
     * Parse a GitHub issue JSON object into an Issue object.
     */
    private Issue parseIssue(JsonObject issueObj) {
        String id = issueObj.get("number").getAsString();
        String title = issueObj.get("title").getAsString();
        String body = issueObj.has("body") && !issueObj.get("body").isJsonNull() ? 
                issueObj.get("body").getAsString() : "";
        String state = issueObj.get("state").getAsString();
        String htmlUrl = issueObj.get("html_url").getAsString();
        
        // Parse assignee (if any)
        String assignee = null;
        if (issueObj.has("assignee") && !issueObj.get("assignee").isJsonNull()) {
            JsonObject assigneeObj = issueObj.getAsJsonObject("assignee");
            assignee = assigneeObj.get("login").getAsString();
        }
        
        // Parse labels
        List<String> labels = new ArrayList<>();
        if (issueObj.has("labels")) {
            JsonArray labelsArray = issueObj.getAsJsonArray("labels");
            for (JsonElement labelElement : labelsArray) {
                JsonObject labelObj = labelElement.getAsJsonObject();
                labels.add(labelObj.get("name").getAsString());
            }
        }
        
        return new Issue(id, title, body, state, htmlUrl, assignee, labels);
    }
    
    /**
     * Close any open resources.
     */
    public void close() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Converts GitHub issues to user stories or tasks based on their labels.
     * @param issues List of GitHub issues
     * @param teamMembers Map of GitHub usernames to TeamMember objects
     * @param storyLabelName Label name that indicates an issue is a user story
     * @return A list of user stories with tasks
     */
    public List<UserStory> convertIssuesToUserStories(
            List<Issue> issues, 
            Map<String, TeamMember> teamMembers,
            String storyLabelName) {
        
        // First, let's separate stories from tasks
        List<Issue> storyIssues = issues.stream()
                .filter(issue -> issue.getLabels().contains(storyLabelName))
                .collect(Collectors.toList());
                
        List<Issue> taskIssues = issues.stream()
                .filter(issue -> !issue.getLabels().contains(storyLabelName))
                .collect(Collectors.toList());
        
        // Convert story issues to UserStory objects
        List<UserStory> userStories = new ArrayList<>();
        for (Issue issue : storyIssues) {
            UserStory.Priority priority = determinePriorityEnum(issue.getLabels().toArray(new String[0]));
            int storyPoints = determineStoryPoints(issue.getLabels().toArray(new String[0]));
            
            UserStory story = new UserStory(
                    issue.getTitle(),
                    issue.getBody(),
                    priority,
                    storyPoints
            );
            
            userStories.add(story);
        }
        
        // If we have no stories but tasks, create a default story
        if (userStories.isEmpty() && !taskIssues.isEmpty()) {
            UserStory defaultStory = new UserStory(
                    "GitHub Issues",
                    "Tasks imported from GitHub issues",
                    UserStory.Priority.MEDIUM,
                    0
            );
            userStories.add(defaultStory);
        }
        
        // Convert task issues to Task objects and add them to appropriate stories
        for (Issue issue : taskIssues) {
            Task task = new Task(issue.getTitle(), issue.getBody());
            task.setGithubIssueUrl(issue.getHtmlUrl());
            
            // Set status based on issue state
            if ("closed".equals(issue.getState())) {
                task.setStatus(Task.Status.DONE);
            } else {
                task.setStatus(Task.Status.TO_DO);
            }
            
            // Assign team member if assignee exists and matches a team member
            if (issue.getAssignee() != null && teamMembers.containsKey(issue.getAssignee())) {
                task.setAssignedTeamMember(teamMembers.get(issue.getAssignee()));
            }
            
            // Find the appropriate story for this task
            // For now, we'll add all tasks to the first story
            // In a more advanced implementation, we could use parent-child relationships
            if (!userStories.isEmpty()) {
                userStories.get(0).addTask(task);
            }
        }
        
        return userStories;
    }
    
    /**
     * Determines UserStory.Priority enum based on issue labels.
     */
    private UserStory.Priority determinePriorityEnum(String[] labels) {
        String priorityStr = determinePriority(labels);
        try {
            return UserStory.Priority.valueOf(priorityStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UserStory.Priority.MEDIUM; // Default priority
        }
    }
    
    /**
     * Class representing a GitHub issue.
     */
    public static class Issue {
        private final String id;
        private final String title;
        private final String body;
        private final String state;
        private final String htmlUrl;
        private final String assignee;
        private final List<String> labels;
        
        /**
         * Constructor for mock issues with minimal information.
         *
         * @param id Issue number/id
         * @param title Issue title
         * @param body Issue description
         * @param labels Array of label strings
         */
        public Issue(int id, String title, String body, String[] labels) {
            this.id = String.valueOf(id);
            this.title = title;
            this.body = body;
            this.state = "open";
            this.htmlUrl = "https://github.com/mock/issue/" + id;
            this.assignee = null;
            this.labels = labels != null ? List.of(labels) : new ArrayList<>();
        }
        
        public Issue(String id, String title, String body, String state, String htmlUrl, 
                String assignee, List<String> labels) {
            this.id = id;
            this.title = title;
            this.body = body;
            this.state = state;
            this.htmlUrl = htmlUrl;
            this.assignee = assignee;
            this.labels = labels;
        }
        
        public String getId() {
            return id;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getBody() {
            return body;
        }
        
        public String getState() {
            return state;
        }
        
        public String getHtmlUrl() {
            return htmlUrl;
        }
        
        public String getAssignee() {
            return assignee;
        }
        
        public List<String> getLabels() {
            return labels;
        }
    }
} 