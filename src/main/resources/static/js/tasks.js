/**
 * Tasks Module for the Agile Sprint Tracker
 */

// Initialize task-related event listeners
document.addEventListener('DOMContentLoaded', () => {
    // Save task button
    document.getElementById('save-task-btn').addEventListener('click', () => {
        saveTask();
    });
});

// Open task creation/edit modal
function openTaskModal(task = null) {
    if (!app.currentSprint || !app.currentStory) return;
    
    const modal = new bootstrap.Modal(document.getElementById('task-modal'));
    const modalTitle = document.getElementById('task-modal-title');
    const form = document.getElementById('task-form');
    
    // Reset form
    form.reset();
    
    // Update team members dropdown
    updateTeamMemberDropdown();
    
    if (task) {
        // Edit mode
        modalTitle.textContent = 'Edit Task';
        document.getElementById('task-title-input').value = task.title;
        document.getElementById('task-status-input').value = task.status;
        
        if (task.assignedTeamMemberId) {
            document.getElementById('task-assigned-input').value = task.assignedTeamMemberId;
        }
    } else {
        // Create mode
        modalTitle.textContent = 'Create Task';
        document.getElementById('task-status-input').value = 'TODO';
    }
    
    // Store task ID if editing
    document.getElementById('task-modal').setAttribute('data-task-id', task ? task.id : '');
    
    modal.show();
}

// Update team members dropdown in task modal
function updateTeamMemberDropdown() {
    const select = document.getElementById('task-assigned-input');
    
    // Clear existing options except the first one (unassigned)
    while (select.options.length > 1) {
        select.options.remove(1);
    }
    
    // Add team members as options
    if (app.teamMembers && app.teamMembers.length > 0) {
        app.teamMembers.forEach(member => {
            const option = document.createElement('option');
            option.value = member.id;
            option.textContent = member.name;
            select.appendChild(option);
        });
    }
}

// Save task
async function saveTask() {
    if (!app.currentSprint || !app.currentStory) return;
    
    const form = document.getElementById('task-form');
    
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    
    const taskId = document.getElementById('task-modal').getAttribute('data-task-id');
    const isEditing = taskId !== '';
    
    // Get values from form
    const title = document.getElementById('task-title-input').value;
    const status = document.getElementById('task-status-input').value;
    const assignedTeamMemberId = document.getElementById('task-assigned-input').value || null;
    
    // Fix status format for backend
    let backendStatus = status;
    if (status === 'TODO') {
        backendStatus = 'TO_DO';
    }
    
    // Create the task object with corrected format for backend
    const task = {
        title: title,
        status: backendStatus,
        assignedTeamMemberId: assignedTeamMemberId
    };
    
    if (isEditing) {
        task.id = taskId;
    } else {
        task.id = generateUUID();
    }
    
    console.log('Sending task to API:', task);
    
    try {
        showLoading();
        
        if (isEditing) {
            await api.updateTask(app.currentSprint.id, app.currentStory.id, taskId, task);
        } else {
            const response = await api.createTask(app.currentSprint.id, app.currentStory.id, task);
            console.log('Task creation response:', response);
        }
        
        // Close modal
        bootstrap.Modal.getInstance(document.getElementById('task-modal')).hide();
        
        // Refresh view with direct API call to get fresh story data
        try {
            const freshStory = await api.getUserStory(app.currentSprint.id, app.currentStory.id);
            console.log('Refreshed story data after task operation:', freshStory);
            
            // Update the app's current story
            app.currentStory = freshStory;
            
            // Update the story in the current sprint
            if (app.currentSprint && app.currentSprint.userStories) {
                const storyIndex = app.currentSprint.userStories.findIndex(s => s.id === app.currentStory.id);
                if (storyIndex >= 0) {
                    app.currentSprint.userStories[storyIndex] = freshStory;
                }
            }
            
            // Render the updated story
            renderStoryDetails(freshStory, app.teamMembers);
        } catch (refreshError) {
            console.error('Error refreshing story data:', refreshError);
            // Fall back to regular refresh
            app.showStoryDetails(app.currentSprint.id, app.currentStory.id);
        }
        
        hideLoading();
    } catch (error) {
        hideLoading();
        handleApiError(error);
    }
}

// Edit task
function editTask(taskId) {
    if (!app.currentStory || !app.currentStory.tasks) return;
    
    const task = app.currentStory.tasks.find(t => t.id === taskId);
    if (task) {
        openTaskModal(task);
    }
}

// Confirm task deletion
function confirmDeleteTask(task) {
    if (!app.currentSprint || !app.currentStory) return;
    
    showConfirmModal(
        'Delete Task',
        `Are you sure you want to delete task "${task.title}"? This action cannot be undone.`,
        () => deleteTask(task.id)
    );
}

// Delete task
async function deleteTask(taskId) {
    if (!app.currentSprint || !app.currentStory) return;
    
    try {
        showLoading();
        
        await api.deleteTask(app.currentSprint.id, app.currentStory.id, taskId);
        
        // Refresh view
        app.showStoryDetails(app.currentSprint.id, app.currentStory.id);
        
        hideLoading();
    } catch (error) {
        hideLoading();
        handleApiError(error);
    }
}

// Move task to a different status
async function moveTask(taskId, newStatus) {
    if (!app.currentSprint || !app.currentStory || !taskId || !newStatus) return;
    
    try {
        showLoading();
        
        // Find task
        const task = app.currentStory.tasks.find(t => t.id === taskId);
        if (!task) {
            throw new Error(`Task not found with ID: ${taskId}`);
        }
        
        // Create task update with new status
        const updatedTask = {
            id: task.id,
            title: task.title,
            description: task.description || '',
            status: newStatus,
            assignedTeamMemberId: task.assignedTeamMemberId || null
        };
        
        console.log(`Moving task ${taskId} to ${newStatus}`);
        
        // Debug the update
        console.log('Sending updated task:', updatedTask);
        
        // Convert frontend status to backend format
        // This ensures we send the correct format to the server
        let backendStatus = newStatus;
        if (newStatus === 'TODO') {
            backendStatus = 'TO_DO';
        }
        
        // Always use the backend format for the API call
        const apiPayload = {
            ...updatedTask,
            status: backendStatus
        };
        
        console.log('API payload with correct backend status format:', apiPayload);
        
        // Update task with backend format status
        try {
            await api.updateTask(app.currentSprint.id, app.currentStory.id, taskId, apiPayload);
            console.log('Task status updated successfully');
        } catch (updateError) {
            console.error('Error updating task status:', updateError);
            throw updateError;
        }
        
        // Refresh view with direct API call to get fresh story data
        try {
            const freshStory = await api.getUserStory(app.currentSprint.id, app.currentStory.id);
            console.log('Refreshed story data after task move:', freshStory);
            
            // Update the app's current story
            app.currentStory = freshStory;
            
            // Update the story in the current sprint
            if (app.currentSprint && app.currentSprint.userStories) {
                const storyIndex = app.currentSprint.userStories.findIndex(s => s.id === app.currentStory.id);
                if (storyIndex >= 0) {
                    app.currentSprint.userStories[storyIndex] = freshStory;
                }
            }
            
            // Render the updated story
            renderStoryDetails(freshStory, app.teamMembers);
        } catch (refreshError) {
            console.error('Error refreshing story data:', refreshError);
            // Fall back to regular refresh
            app.showStoryDetails(app.currentSprint.id, app.currentStory.id);
        }
        
        hideLoading();
    } catch (error) {
        hideLoading();
        handleApiError(error);
    }
} 