/**
 * Sprints Module for the Agile Sprint Tracker
 */

// Initialize sprint-related event listeners
document.addEventListener('DOMContentLoaded', () => {
    // Back to sprints button
    document.getElementById('back-to-sprints-btn').addEventListener('click', () => {
        app.showSprintsSection();
    });
    
    // Create sprint button
    document.getElementById('create-sprint-btn').addEventListener('click', () => {
        openSprintModal();
    });
    
    // Save sprint button
    document.getElementById('save-sprint-btn').addEventListener('click', () => {
        saveSprint();
    });
    
    // Edit sprint button
    document.getElementById('edit-sprint-btn').addEventListener('click', () => {
        editCurrentSprint();
    });
    
    // Delete sprint button
    document.getElementById('delete-sprint-btn').addEventListener('click', () => {
        confirmDeleteSprint();
    });
    
    // Add story button
    document.getElementById('add-story-btn').addEventListener('click', () => {
        openStoryModal();
    });
});

// Render sprints list
function renderSprints(sprints) {
    const container = document.getElementById('sprints-container');
    container.innerHTML = '';
    
    if (!sprints || sprints.length === 0) {
        container.innerHTML = `
            <div class="col-12 text-center">
                <p class="text-muted mt-4">No sprints found. Click "Create Sprint" to get started.</p>
            </div>
        `;
        return;
    }
    
    sprints.forEach(sprint => {
        const progress = calculateSprintProgress(sprint);
        const card = document.createElement('div');
        card.className = 'col';
        card.innerHTML = `
            <div class="card sprint-card" data-sprint-id="${sprint.id}">
                <div class="card-body">
                    <h5 class="card-title">${sprint.name}</h5>
                    <p class="sprint-dates">
                        <i class="fas fa-calendar-alt me-2"></i>
                        ${formatDate(sprint.startDate)} - ${formatDate(sprint.endDate)}
                    </p>
                    <p class="card-text">
                        <strong>Stories:</strong> ${sprint.userStories ? sprint.userStories.length : 0}
                    </p>
                    <div class="progress mb-2">
                        <div class="progress-bar" role="progressbar" style="width: ${progress}%" 
                            aria-valuenow="${progress}" aria-valuemin="0" aria-valuemax="100">
                            ${progress}%
                        </div>
                    </div>
                </div>
            </div>
        `;
        
        // Add click event to open sprint details
        card.querySelector('.sprint-card').addEventListener('click', () => {
            app.showSprintDetails(sprint.id);
        });
        
        container.appendChild(card);
    });
}

// Render sprint details
function renderSprintDetails(sprint) {
    // Set sprint title and dates
    document.getElementById('sprint-title').textContent = sprint.name;
    document.getElementById('sprint-start-date').textContent = formatDate(sprint.startDate);
    document.getElementById('sprint-end-date').textContent = formatDate(sprint.endDate);
    
    // Calculate and set progress
    const progress = calculateSprintProgress(sprint);
    document.getElementById('sprint-progress-bar').style.width = `${progress}%`;
    document.getElementById('sprint-progress-text').textContent = `${progress}%`;
    
    // Render user stories
    const container = document.getElementById('user-stories-container');
    container.innerHTML = '';
    
    if (!sprint.userStories || sprint.userStories.length === 0) {
        container.innerHTML = `
            <div class="text-center mt-4">
                <p class="text-muted">No user stories found. Click "Add User Story" to get started.</p>
            </div>
        `;
        return;
    }
    
    sprint.userStories.forEach(story => {
        const card = document.createElement('div');
        card.className = 'card story-card mb-3';
        card.setAttribute('data-story-id', story.id);
        
        const tasksCount = story.tasks ? story.tasks.length : 0;
        const completedTasksCount = story.tasks ? story.tasks.filter(t => t.status === 'DONE').length : 0;
        const storyProgress = tasksCount === 0 ? 0 : Math.round((completedTasksCount / tasksCount) * 100);
        
        card.innerHTML = `
            <div class="card-header d-flex justify-content-between align-items-center">
                <h5 class="mb-0">${story.title}</h5>
                <div class="d-flex align-items-center">
                    <span class="story-priority ${getPriorityClass(story.priority)} me-3">${story.priority}</span>
                    <div class="story-points">${story.storyPoints}</div>
                </div>
            </div>
            <div class="card-body">
                <p>${story.description}</p>
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <span class="badge bg-secondary me-2">${tasksCount} Tasks</span>
                        <span class="badge bg-success">${completedTasksCount} Completed</span>
                    </div>
                    <div class="d-flex align-items-center">
                        <div class="progress me-3" style="width: 120px;">
                            <div class="progress-bar" role="progressbar" style="width: ${storyProgress}%" 
                                aria-valuenow="${storyProgress}" aria-valuemin="0" aria-valuemax="100">
                                ${storyProgress}%
                            </div>
                        </div>
                        <button class="btn btn-sm btn-danger delete-story-btn" data-story-id="${story.id}">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </div>
            </div>
        `;
        
        // Add click event to open story details
        card.addEventListener('click', (e) => {
            // Don't trigger if clicked on the delete button
            if (e.target.closest('.delete-story-btn')) {
                return;
            }
            app.showStoryDetails(sprint.id, story.id);
        });
        
        // Add delete button event listener
        card.querySelector('.delete-story-btn').addEventListener('click', (e) => {
            e.stopPropagation(); // Prevent card click event
            confirmDeleteStoryFromSprint(sprint, story);
        });
        
        container.appendChild(card);
    });
}

// Open sprint creation/edit modal
function openSprintModal(sprint = null) {
    const modal = new bootstrap.Modal(document.getElementById('sprint-modal'));
    const modalTitle = document.getElementById('sprint-modal-title');
    const form = document.getElementById('sprint-form');
    
    // Reset form
    form.reset();
    
    if (sprint) {
        // Edit mode
        modalTitle.textContent = 'Edit Sprint';
        document.getElementById('sprint-name').value = sprint.name;
        document.getElementById('sprint-start').value = formatDateForInput(sprint.startDate);
        document.getElementById('sprint-end').value = formatDateForInput(sprint.endDate);
    } else {
        // Create mode
        modalTitle.textContent = 'Create Sprint';
        
        // Set default dates (today and +2 weeks)
        const today = new Date();
        const twoWeeksLater = new Date();
        twoWeeksLater.setDate(today.getDate() + 14);
        
        document.getElementById('sprint-start').value = formatDateForInput(today);
        document.getElementById('sprint-end').value = formatDateForInput(twoWeeksLater);
    }
    
    // Store sprint ID if editing
    document.getElementById('sprint-modal').setAttribute('data-sprint-id', sprint ? sprint.id : '');
    
    modal.show();
}

// Save sprint
async function saveSprint() {
    const form = document.getElementById('sprint-form');
    
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    
    const sprintId = document.getElementById('sprint-modal').getAttribute('data-sprint-id');
    const isEditing = sprintId !== '';
    
    const sprint = {
        name: document.getElementById('sprint-name').value,
        startDate: document.getElementById('sprint-start').value,
        endDate: document.getElementById('sprint-end').value
    };
    
    if (isEditing) {
        sprint.id = sprintId;
    } else {
        sprint.id = generateUUID();
        sprint.userStories = [];
    }
    
    try {
        showLoading();
        
        if (isEditing) {
            await api.updateSprint(sprintId, sprint);
        } else {
            await api.createSprint(sprint);
        }
        
        // Close modal
        bootstrap.Modal.getInstance(document.getElementById('sprint-modal')).hide();
        
        // Refresh view
        if (isEditing && app.currentSprint && app.currentSprint.id === sprintId) {
            app.showSprintDetails(sprintId);
        } else {
            app.showSprintsSection();
        }
        
        hideLoading();
    } catch (error) {
        hideLoading();
        handleApiError(error);
    }
}

// Edit current sprint
function editCurrentSprint() {
    if (!app.currentSprint) return;
    openSprintModal(app.currentSprint);
}

// Confirm sprint deletion
function confirmDeleteSprint() {
    if (!app.currentSprint) return;
    
    showConfirmModal(
        'Delete Sprint',
        `Are you sure you want to delete sprint "${app.currentSprint.name}"? This action cannot be undone.`,
        deleteSprint
    );
}

// Delete sprint
async function deleteSprint() {
    if (!app.currentSprint) return;
    
    try {
        showLoading();
        
        await api.deleteSprint(app.currentSprint.id);
        
        // Navigate back to sprints list
        app.showSprintsSection();
        
        hideLoading();
    } catch (error) {
        hideLoading();
        handleApiError(error);
    }
}

// Function to confirm deleting a story from sprint view
function confirmDeleteStoryFromSprint(sprint, story) {
    showConfirmModal(
        'Delete User Story',
        `Are you sure you want to delete story "${story.title}"? This action cannot be undone.`,
        () => deleteStoryFromSprint(sprint.id, story.id)
    );
}

// Function to delete a story from sprint view
async function deleteStoryFromSprint(sprintId, storyId) {
    try {
        showLoading();
        
        await api.deleteUserStory(sprintId, storyId);
        
        // Refresh sprint details view
        app.showSprintDetails(sprintId);
        
        hideLoading();
    } catch (error) {
        hideLoading();
        handleApiError(error);
    }
} 