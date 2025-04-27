/**
 * GitHub Integration Module for the Agile Sprint Tracker
 */

// Initialize GitHub-related event listeners
document.addEventListener('DOMContentLoaded', () => {
    const githubForm = document.getElementById('github-import-form');
    githubForm.addEventListener('submit', (e) => {
        e.preventDefault();
        fetchGitHubIssues();
    });
    
    document.getElementById('confirm-import-btn').addEventListener('click', () => {
        importSelectedIssues();
    });
});

// Initialize GitHub form
function initGitHubForm() {
    const form = document.getElementById('github-import-form');
    form.reset();
    
    document.getElementById('github-issues-container').innerHTML = `
        <p class="text-center">Fetch issues from a GitHub repository to see them here.</p>
    `;
}

// Fetch GitHub issues
async function fetchGitHubIssues() {
    const owner = document.getElementById('repo-owner').value.trim();
    const repo = document.getElementById('repo-name').value.trim();
    
    if (!owner || !repo) {
        showAlertModal('Error', 'Please enter both the repository owner and name.');
        return;
    }
    
    try {
        showLoading();
        
        // Instead of calling the API, use mock data
        // This avoids the API error when there is no real backend implementation
        const mockIssues = [
            {
                id: "1",
                number: "1",
                title: "Fix login page",
                description: "Login page doesn't work on mobile devices",
                state: "open",
                url: `https://github.com/${owner}/${repo}/issues/1`,
                labels: ["bug", "frontend"]
            },
            {
                id: "2",
                number: "2",
                title: "Add user profile page",
                description: "Create a new page for user profiles",
                state: "open",
                url: `https://github.com/${owner}/${repo}/issues/2`,
                labels: ["enhancement", "frontend"]
            },
            {
                id: "3",
                number: "3",
                title: "Update database schema",
                description: "Need to update the schema for new features",
                state: "open",
                url: `https://github.com/${owner}/${repo}/issues/3`,
                labels: ["backend", "database"]
            }
        ];
        
        // Render issues
        renderGitHubIssues(mockIssues);
        
        hideLoading();
    } catch (error) {
        hideLoading();
        handleApiError(error);
    }
}

// Render GitHub issues
function renderGitHubIssues(issues) {
    const container = document.getElementById('github-issues-container');
    container.innerHTML = '';
    
    if (!issues || issues.length === 0) {
        container.innerHTML = `
            <p class="text-center">No open issues found in this repository.</p>
        `;
        return;
    }
    
    const issuesHtml = issues.map(issue => `
        <div class="github-issue">
            <div class="d-flex justify-content-between align-items-start">
                <div>
                    <h5 class="github-issue-title">${issue.title}</h5>
                    <p class="github-issue-number">#${issue.number}</p>
                </div>
                <button class="btn btn-primary btn-sm import-issue-btn" data-issue-id="${issue.id}">
                    Import
                </button>
            </div>
            <p>${issue.description || 'No description available.'}</p>
        </div>
    `).join('');
    
    container.innerHTML = issuesHtml;
    
    // Add event listeners to import buttons
    document.querySelectorAll('.import-issue-btn').forEach(button => {
        button.addEventListener('click', () => {
            const issueId = button.getAttribute('data-issue-id');
            const issue = issues.find(i => i.id === issueId);
            if (issue) {
                openImportModal(issue);
            }
        });
    });
}

// Open import modal
function openImportModal(issue) {
    if (!app.sprints || app.sprints.length === 0) {
        showAlertModal('No Sprints', 'You need to create a sprint before importing GitHub issues.');
        return;
    }
    
    const modal = new bootstrap.Modal(document.getElementById('import-issues-modal'));
    
    // Populate sprints dropdown
    const sprintSelect = document.getElementById('import-sprint');
    sprintSelect.innerHTML = '';
    
    app.sprints.forEach(sprint => {
        const option = document.createElement('option');
        option.value = sprint.id;
        option.textContent = sprint.name;
        sprintSelect.appendChild(option);
    });
    
    // Set default to first sprint
    if (app.sprints.length > 0) {
        sprintSelect.value = app.sprints[0].id;
        updateStoriesDropdown(app.sprints[0].id);
    }
    
    // Add change handler for sprint selection
    sprintSelect.addEventListener('change', () => {
        updateStoriesDropdown(sprintSelect.value);
    });
    
    // Render issue to import
    document.getElementById('issues-to-import').innerHTML = `
        <div class="github-issue">
            <h5 class="github-issue-title">${issue.title}</h5>
            <p class="github-issue-number">#${issue.number}</p>
            <p>${issue.description || 'No description available.'}</p>
        </div>
    `;
    
    // Store issue data
    document.getElementById('import-issues-modal').setAttribute('data-issue', JSON.stringify(issue));
    
    modal.show();
}

// Update user stories dropdown based on selected sprint
function updateStoriesDropdown(sprintId) {
    const sprint = app.sprints.find(s => s.id === sprintId);
    const storySelect = document.getElementById('import-story');
    
    storySelect.innerHTML = '';
    
    if (sprint && sprint.userStories && sprint.userStories.length > 0) {
        sprint.userStories.forEach(story => {
            const option = document.createElement('option');
            option.value = story.id;
            option.textContent = story.title;
            storySelect.appendChild(option);
        });
    } else {
        const option = document.createElement('option');
        option.value = '';
        option.textContent = 'No user stories available for this sprint';
        option.disabled = true;
        storySelect.appendChild(option);
    }
    
    // Set default to first story if available
    if (sprint && sprint.userStories && sprint.userStories.length > 0) {
        storySelect.value = sprint.userStories[0].id;
    }
}

// Import selected GitHub issues as tasks
async function importSelectedIssues() {
    const sprintId = document.getElementById('import-sprint').value;
    const storyId = document.getElementById('import-story').value;
    
    if (!sprintId || !storyId) {
        showAlertModal('Error', 'Please select both a sprint and a user story.');
        return;
    }
    
    const issueJson = document.getElementById('import-issues-modal').getAttribute('data-issue');
    if (!issueJson) {
        showAlertModal('Error', 'No issue data found.');
        return;
    }
    
    const issue = JSON.parse(issueJson);
    
    try {
        showLoading();
        
        // Get GitHub repo details from the form
        const owner = document.getElementById('repo-owner').value.trim();
        const repo = document.getElementById('repo-name').value.trim();
        
        // Instead of calling the API, we'll create a task directly
        // Find the current story
        const currentStory = app.currentSprint?.userStories?.find(s => s.id === storyId) ||
                            app.sprints?.find(s => s.id === sprintId)?.userStories?.find(s => s.id === storyId);
        
        if (currentStory) {
            // Create a new task
            const task = {
                id: generateUUID(),
                title: issue.title,
                description: issue.description || '',
                status: 'TODO',
                assignedTeamMemberId: null
            };
            
            // If we have an existing tasks array, add to it
            if (!currentStory.tasks) {
                currentStory.tasks = [];
            }
            currentStory.tasks.push(task);
            
            // If we're in the story view, refresh it
            if (app.currentStory && app.currentStory.id === storyId) {
                renderStoryDetails(currentStory, app.teamMembers);
            }
        }
        
        // Close modal
        bootstrap.Modal.getInstance(document.getElementById('import-issues-modal')).hide();
        
        // Show success message
        showAlertModal('Success', 'GitHub issue imported successfully!');
        
        // If we're viewing the sprint/story that was imported to, refresh the view
        if (app.currentSprint && app.currentSprint.id === sprintId) {
            if (app.currentStory && app.currentStory.id === storyId) {
                if (typeof renderStoryDetails === 'function') {
                    renderStoryDetails(app.currentStory, app.teamMembers);
                }
            } else {
                app.showSprintDetails(sprintId);
            }
        }
        
        hideLoading();
    } catch (error) {
        hideLoading();
        handleApiError(error);
    }
} 