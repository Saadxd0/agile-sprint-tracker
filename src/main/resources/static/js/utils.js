/**
 * Utility functions for the Agile Sprint Tracker
 */

// Format date from yyyy-MM-dd to a more readable format
function formatDate(dateString) {
    if (!dateString) return 'N/A';
    
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
    });
}

// Calculate progress percentage for a sprint
function calculateSprintProgress(sprint) {
    if (!sprint || !sprint.userStories || sprint.userStories.length === 0) {
        return 0;
    }
    
    let totalTasks = 0;
    let completedTasks = 0;
    
    sprint.userStories.forEach(story => {
        if (story.tasks && story.tasks.length > 0) {
            totalTasks += story.tasks.length;
            completedTasks += story.tasks.filter(task => task.status === 'DONE').length;
        }
    });
    
    return totalTasks === 0 ? 0 : Math.round((completedTasks / totalTasks) * 100);
}

// Show loading spinner
function showLoading() {
    document.getElementById('loading-spinner').classList.remove('d-none');
}

// Hide loading spinner
function hideLoading() {
    document.getElementById('loading-spinner').classList.add('d-none');
}

// Show a view and hide others
function showView(viewId) {
    const views = [
        'sprints-view',
        'sprint-details-view',
        'story-details-view',
        'team-members-view',
        'github-view'
    ];
    
    views.forEach(view => {
        const element = document.getElementById(view);
        if (view === viewId) {
            element.classList.remove('d-none');
        } else {
            element.classList.add('d-none');
        }
    });
}

// Update navigation active state
function updateNavActive(navId) {
    const navItems = [
        'nav-sprints',
        'nav-team',
        'nav-github'
    ];
    
    navItems.forEach(item => {
        const element = document.getElementById(item);
        if (item === navId) {
            element.classList.add('active');
        } else {
            element.classList.remove('active');
        }
    });
}

// Show a confirmation modal
function showConfirmModal(title, message, callback) {
    const modal = new bootstrap.Modal(document.getElementById('confirm-modal'));
    document.getElementById('confirm-modal-title').textContent = title;
    document.getElementById('confirm-modal-text').textContent = message;
    
    const confirmButton = document.getElementById('confirm-action-btn');
    
    // Remove existing event listeners
    const newConfirmButton = confirmButton.cloneNode(true);
    confirmButton.parentNode.replaceChild(newConfirmButton, confirmButton);
    
    // Add new event listener
    newConfirmButton.addEventListener('click', () => {
        modal.hide();
        callback();
    });
    
    modal.show();
}

// Show an alert modal
function showAlertModal(title, message) {
    const modal = new bootstrap.Modal(document.getElementById('alert-modal'));
    document.getElementById('alert-modal-title').textContent = title;
    document.getElementById('alert-modal-text').textContent = message;
    modal.show();
}

// Generate a UUID
function generateUUID() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        const r = Math.random() * 16 | 0;
        const v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
}

// Format date input value (yyyy-MM-dd)
function formatDateForInput(dateString) {
    if (!dateString) return '';
    
    const date = new Date(dateString);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    
    return `${year}-${month}-${day}`;
}

// Get priority CSS class
function getPriorityClass(priority) {
    switch(priority) {
        case 'HIGH':
            return 'priority-high';
        case 'MEDIUM':
            return 'priority-medium';
        case 'LOW':
            return 'priority-low';
        default:
            return 'priority-medium';
    }
}

// Handle API errors
function handleApiError(error) {
    console.error('API Error:', error);
    showAlertModal('Error', error.message || 'An error occurred while communicating with the server.');
} 