/**
 * User Stories Module for the Agile Sprint Tracker
 */

// Initialize story-related event listeners
document.addEventListener('DOMContentLoaded', () => {
    // Back to sprint button
    document.getElementById('back-to-sprint-btn').addEventListener('click', () => {
        if (app.currentSprint) {
            app.showSprintDetails(app.currentSprint.id);
        } else {
            app.showSprintsSection();
        }
    });
    
    // Save story button
    document.getElementById('save-story-btn').addEventListener('click', () => {
        saveUserStory();
    });
    
    // Edit story button
    document.getElementById('edit-story-btn').addEventListener('click', () => {
        editCurrentStory();
    });
    
    // Delete story button
    document.getElementById('delete-story-btn').addEventListener('click', () => {
        confirmDeleteStory();
    });
    
    // Add task button
    document.getElementById('add-task-btn').addEventListener('click', () => {
        openTaskModal();
    });
});

// Open user story creation/edit modal
function openStoryModal(story = null) {
    if (!app.currentSprint) return;
    
    const modal = new bootstrap.Modal(document.getElementById('story-modal'));
    const modalTitle = document.getElementById('story-modal-title');
    const form = document.getElementById('story-form');
    
    // Reset form
    form.reset();
    
    if (story) {
        // Edit mode
        modalTitle.textContent = 'Edit User Story';
        document.getElementById('story-title-input').value = story.title;
        document.getElementById('story-description-input').value = story.description;
        document.getElementById('story-priority-input').value = story.priority;
        document.getElementById('story-points-input').value = story.storyPoints;
    } else {
        // Create mode
        modalTitle.textContent = 'Create User Story';
        document.getElementById('story-priority-input').value = 'MEDIUM';
        document.getElementById('story-points-input').value = '1';
    }
    
    // Store story ID if editing
    document.getElementById('story-modal').setAttribute('data-story-id', story ? story.id : '');
    
    modal.show();
}

// Save user story
async function saveUserStory() {
    if (!app.currentSprint) return;
    
    const form = document.getElementById('story-form');
    
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    
    const storyId = document.getElementById('story-modal').getAttribute('data-story-id');
    const isEditing = storyId !== '';
    
    const story = {
        title: document.getElementById('story-title-input').value,
        description: document.getElementById('story-description-input').value,
        priority: document.getElementById('story-priority-input').value.toUpperCase(),
        storyPoints: parseInt(document.getElementById('story-points-input').value, 10)
    };
    
    if (isEditing) {
        story.id = storyId;
    } else {
        story.id = generateUUID();
        story.tasks = [];
    }
    
    try {
        showLoading();
        
        if (isEditing) {
            await api.updateUserStory(app.currentSprint.id, storyId, story);
        } else {
            await api.createUserStory(app.currentSprint.id, story);
        }
        
        // Close modal
        bootstrap.Modal.getInstance(document.getElementById('story-modal')).hide();
        
        // Refresh view
        if (isEditing && app.currentStory && app.currentStory.id === storyId) {
            app.showStoryDetails(app.currentSprint.id, storyId);
        } else {
            app.showSprintDetails(app.currentSprint.id);
        }
        
        hideLoading();
    } catch (error) {
        hideLoading();
        handleApiError(error);
    }
}

// Edit current user story
function editCurrentStory() {
    if (!app.currentStory) return;
    openStoryModal(app.currentStory);
}

// Confirm user story deletion
function confirmDeleteStory() {
    if (!app.currentSprint || !app.currentStory) return;
    
    showConfirmModal(
        'Delete User Story',
        `Are you sure you want to delete story "${app.currentStory.title}"? This action cannot be undone.`,
        deleteUserStory
    );
}

// Delete user story
async function deleteUserStory() {
    if (!app.currentSprint || !app.currentStory) return;
    
    try {
        showLoading();
        
        await api.deleteUserStory(app.currentSprint.id, app.currentStory.id);
        
        // Navigate back to sprint details
        app.showSprintDetails(app.currentSprint.id);
        
        hideLoading();
    } catch (error) {
        hideLoading();
        handleApiError(error);
    }
}

// Render user story details
function renderStoryDetails(story, teamMembers) {
    // Set story title and details
    document.getElementById('story-title').textContent = story.title;
    document.getElementById('story-description').textContent = story.description;
    document.getElementById('story-priority').textContent = story.priority;
    document.getElementById('story-points').textContent = story.storyPoints;
    
    // Render tasks by status
    renderTasks(story.tasks || [], teamMembers);
    
    // Set up drag and drop after rendering tasks
    setupDragAndDrop();
}

// Set up drag and drop functionality
function setupDragAndDrop() {
    const taskCards = document.querySelectorAll('.task-card');
    const dropZones = document.querySelectorAll('.tasks-container');
    
    // Make task cards draggable
    taskCards.forEach(card => {
        card.setAttribute('draggable', 'true');
        
        card.addEventListener('dragstart', (e) => {
            e.dataTransfer.setData('text/plain', card.getAttribute('data-task-id'));
            card.classList.add('dragging');
        });
        
        card.addEventListener('dragend', () => {
            card.classList.remove('dragging');
        });
    });
    
    // Set up drop zones
    dropZones.forEach(zone => {
        zone.addEventListener('dragover', (e) => {
            e.preventDefault();
            zone.classList.add('drag-over');
        });
        
        zone.addEventListener('dragleave', () => {
            zone.classList.remove('drag-over');
        });
        
        zone.addEventListener('drop', (e) => {
            e.preventDefault();
            zone.classList.remove('drag-over');
            
            const taskId = e.dataTransfer.getData('text/plain');
            const newStatus = getStatusFromZoneId(zone.id);
            
            if (taskId && newStatus) {
                moveTask(taskId, newStatus);
            }
        });
    });
}

// Get status from zone ID
function getStatusFromZoneId(zoneId) {
    switch(zoneId) {
        case 'tasks-todo':
            return 'TODO';
        case 'tasks-inprogress':
            return 'IN_PROGRESS';
        case 'tasks-done':
            return 'DONE';
        default:
            return null;
    }
}

// Render tasks grouped by status
function renderTasks(tasks, teamMembers) {
    // Clear all task containers
    document.getElementById('tasks-todo').innerHTML = '';
    document.getElementById('tasks-inprogress').innerHTML = '';
    document.getElementById('tasks-done').innerHTML = '';
    
    // If no tasks, show message
    if (!tasks || tasks.length === 0) {
        document.getElementById('tasks-todo').innerHTML = `
            <p class="text-muted text-center">No tasks found. Click "Add Task" to get started.</p>
        `;
        return;
    }
    
    // Organize tasks by status
    console.log('All tasks:', tasks);
    // Check both formats
    const todoTasks = tasks.filter(t => t.status === 'TODO' || t.status === 'TO_DO');
    const inProgressTasks = tasks.filter(t => t.status === 'IN_PROGRESS');
    const doneTasks = tasks.filter(t => t.status === 'DONE');
    
    console.log('Tasks by status:', {
        todo: todoTasks.length,
        inProgress: inProgressTasks.length,
        done: doneTasks.length
    });
    
    // Render each group
    renderTaskGroup(todoTasks, 'tasks-todo', teamMembers);
    renderTaskGroup(inProgressTasks, 'tasks-inprogress', teamMembers);
    renderTaskGroup(doneTasks, 'tasks-done', teamMembers);
}

// Render a group of tasks
function renderTaskGroup(tasks, containerId, teamMembers) {
    const container = document.getElementById(containerId);
    
    if (!tasks || tasks.length === 0) {
        container.innerHTML = `
            <p class="text-muted text-center">No tasks</p>
        `;
        return;
    }
    
    tasks.forEach(task => {
        const assignee = task.assignedTeamMemberId 
            ? teamMembers.find(m => m.id === task.assignedTeamMemberId)
            : null;
            
        const card = document.createElement('div');
        card.className = 'card task-card';
        card.setAttribute('data-task-id', task.id);
        card.setAttribute('data-status', task.status);
        
        card.innerHTML = `
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-start">
                    <h6 class="card-title mb-2">${task.title}</h6>
                    <div class="dropdown">
                        <button class="btn btn-sm btn-outline-secondary dropdown-toggle" type="button" id="task-${task.id}-dropdown" data-bs-toggle="dropdown" aria-expanded="false">
                            <i class="fas fa-ellipsis-v"></i>
                        </button>
                        <ul class="dropdown-menu dropdown-menu-end" aria-labelledby="task-${task.id}-dropdown">
                            <li><h6 class="dropdown-header">Status</h6></li>
                            <li><button class="dropdown-item move-task-btn" data-task-id="${task.id}" data-status="TODO" ${task.status === 'TODO' ? 'disabled' : ''}>Move to To Do</button></li>
                            <li><button class="dropdown-item move-task-btn" data-task-id="${task.id}" data-status="IN_PROGRESS" ${task.status === 'IN_PROGRESS' ? 'disabled' : ''}>Move to In Progress</button></li>
                            <li><button class="dropdown-item move-task-btn" data-task-id="${task.id}" data-status="DONE" ${task.status === 'DONE' ? 'disabled' : ''}>Move to Done</button></li>
                            <li><hr class="dropdown-divider"></li>
                            <li><button class="dropdown-item edit-task-btn" data-task-id="${task.id}">Edit</button></li>
                            <li><button class="dropdown-item delete-task-btn" data-task-id="${task.id}">Delete</button></li>
                        </ul>
                    </div>
                </div>
                ${assignee ? `
                    <div class="task-assignee">
                        <i class="fas fa-user"></i> ${assignee.name}
                    </div>
                ` : ''}
            </div>
        `;
        
        // Add event listeners
        card.querySelector('.edit-task-btn').addEventListener('click', (e) => {
            e.stopPropagation();
            editTask(task.id);
        });
        
        card.querySelector('.delete-task-btn').addEventListener('click', (e) => {
            e.stopPropagation();
            confirmDeleteTask(task);
        });
        
        // Add move task event listeners
        card.querySelectorAll('.move-task-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const taskId = btn.getAttribute('data-task-id');
                const newStatus = btn.getAttribute('data-status');
                moveTask(taskId, newStatus);
            });
        });
        
        container.appendChild(card);
    });
} 