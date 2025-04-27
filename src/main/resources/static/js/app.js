/**
 * Main Application for the Agile Sprint Tracker
 */
class App {
    constructor() {
        this.sprints = [];
        this.teamMembers = [];
        this.currentSprint = null;
        this.currentStory = null;
        this.currentSection = 'sprints';
        
        // Initialize components
        this.initNavigation();
    }
    
    // Initialize navigation
    initNavigation() {
        document.getElementById('nav-sprints').addEventListener('click', (e) => {
            e.preventDefault();
            this.showSprintsSection();
        });
        
        document.getElementById('nav-team').addEventListener('click', (e) => {
            e.preventDefault();
            this.showTeamSection();
        });
        
        document.getElementById('nav-github').addEventListener('click', (e) => {
            e.preventDefault();
            this.showGitHubSection();
        });
    }
    
    // Load initial data and render the application
    async init() {
        try {
            showLoading();
            
            // Load team members
            this.teamMembers = await api.getAllTeamMembers();
            
            // Start with sprints section
            this.showSprintsSection();
            
            hideLoading();
        } catch (error) {
            hideLoading();
            handleApiError(error);
        }
    }
    
    // Show Sprints Section
    async showSprintsSection() {
        this.currentSection = 'sprints';
        updateNavActive('nav-sprints');
        
        try {
            showLoading();
            
            // Load sprints
            this.sprints = await api.getAllSprints();
            
            // Show sprints view
            showView('sprints-view');
            
            // Render sprints
            renderSprints(this.sprints);
            
            hideLoading();
        } catch (error) {
            hideLoading();
            handleApiError(error);
        }
    }
    
    // Show Team Section
    async showTeamSection() {
        this.currentSection = 'team';
        updateNavActive('nav-team');
        
        try {
            showLoading();
            
            // Refresh team members
            this.teamMembers = await api.getAllTeamMembers();
            
            // Show team view
            showView('team-members-view');
            
            // Render team members
            renderTeamMembers(this.teamMembers);
            
            hideLoading();
        } catch (error) {
            hideLoading();
            handleApiError(error);
        }
    }
    
    // Show GitHub Section
    showGitHubSection() {
        this.currentSection = 'github';
        updateNavActive('nav-github');
        showView('github-view');
        initGitHubForm();
    }
    
    // Show Sprint Details
    async showSprintDetails(sprintId) {
        try {
            showLoading();
            
            // Get sprint details
            this.currentSprint = await api.getSprint(sprintId);
            
            // Show sprint details view
            showView('sprint-details-view');
            
            // Render sprint details
            renderSprintDetails(this.currentSprint);
            
            hideLoading();
        } catch (error) {
            hideLoading();
            handleApiError(error);
        }
    }
    
    // Show User Story Details
    async showStoryDetails(sprintId, storyId) {
        try {
            showLoading();
            
            // Get fresh story details directly from the API
            try {
                // Try to fetch the story directly (with latest tasks)
                const freshStory = await api.getUserStory(sprintId, storyId);
                console.log('Fresh story data from API:', freshStory);
                this.currentStory = freshStory;
                
                // Update the current sprint's story with this fresh data
                if (this.currentSprint) {
                    const storyIndex = this.currentSprint.userStories.findIndex(s => s.id === storyId);
                    if (storyIndex >= 0) {
                        this.currentSprint.userStories[storyIndex] = freshStory;
                    }
                }
            } catch (e) {
                console.log('Could not get fresh story, falling back to sprint data');
                
                // Fall back to getting the story from the sprint
                if (!this.currentSprint || this.currentSprint.id !== sprintId) {
                    this.currentSprint = await api.getSprint(sprintId);
                }
                
                // Get story details
                this.currentStory = this.currentSprint.userStories.find(s => s.id === storyId);
            }
            
            if (!this.currentStory) {
                throw new Error('User story not found');
            }
            
            // Debug task information
            console.log('Story tasks:', this.currentStory.tasks);
            if (this.currentStory.tasks && this.currentStory.tasks.length > 0) {
                console.log('Task statuses:', this.currentStory.tasks.map(t => t.status));
                console.log('TODO tasks:', this.currentStory.tasks.filter(t => t.status === 'TODO').length);
                console.log('TO_DO tasks:', this.currentStory.tasks.filter(t => t.status === 'TO_DO').length);
            }
            
            // Show story details view
            showView('story-details-view');
            
            // Render story details
            renderStoryDetails(this.currentStory, this.teamMembers);
            
            hideLoading();
        } catch (error) {
            hideLoading();
            handleApiError(error);
        }
    }
}

// Create app instance
const app = new App();

// Initialize app on DOM loaded
document.addEventListener('DOMContentLoaded', () => {
    app.init();
}); 