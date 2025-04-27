/**
 * Team Module for the Agile Sprint Tracker
 */

// Initialize team-related event listeners
document.addEventListener('DOMContentLoaded', () => {
    // Add team member button
    document.getElementById('add-team-member-btn').addEventListener('click', () => {
        openTeamMemberModal();
    });
    
    // Save team member button
    document.getElementById('save-team-member-btn').addEventListener('click', () => {
        saveTeamMember();
    });
});

// Render team members list
function renderTeamMembers(teamMembers) {
    const container = document.getElementById('team-members-container');
    container.innerHTML = '';
    
    if (!teamMembers || teamMembers.length === 0) {
        container.innerHTML = `
            <div class="col-12 text-center">
                <p class="text-muted mt-4">No team members found. Click "Add Team Member" to get started.</p>
            </div>
        `;
        return;
    }
    
    teamMembers.forEach(member => {
        const card = document.createElement('div');
        card.className = 'col';
        card.innerHTML = `
            <div class="card team-member-card">
                <div class="card-body">
                    <h5 class="card-title">${member.name}</h5>
                    <p class="card-text">${member.email}</p>
                    ${member.githubUsername ? `<p class="github-username"><i class="fab fa-github me-2"></i>${member.githubUsername}</p>` : ''}
                    <div class="team-member-actions">
                        <button class="btn btn-sm btn-outline-warning me-2 edit-team-member-btn" data-member-id="${member.id}">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="btn btn-sm btn-outline-danger delete-team-member-btn" data-member-id="${member.id}">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                </div>
            </div>
        `;
        
        // Add event listeners
        card.querySelector('.edit-team-member-btn').addEventListener('click', () => {
            editTeamMember(member);
        });
        
        card.querySelector('.delete-team-member-btn').addEventListener('click', () => {
            confirmDeleteTeamMember(member);
        });
        
        container.appendChild(card);
    });
}

// Open team member creation/edit modal
function openTeamMemberModal(member = null) {
    const modal = new bootstrap.Modal(document.getElementById('team-member-modal'));
    const modalTitle = document.getElementById('team-member-modal-title');
    const form = document.getElementById('team-member-form');
    
    // Reset form
    form.reset();
    
    if (member) {
        // Edit mode
        modalTitle.textContent = 'Edit Team Member';
        document.getElementById('team-member-name').value = member.name;
        document.getElementById('team-member-email').value = member.email;
        document.getElementById('team-member-github').value = member.githubUsername || '';
    } else {
        // Create mode
        modalTitle.textContent = 'Add Team Member';
    }
    
    // Store member ID if editing
    document.getElementById('team-member-modal').setAttribute('data-member-id', member ? member.id : '');
    
    modal.show();
}

// Save team member
async function saveTeamMember() {
    const form = document.getElementById('team-member-form');
    
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }
    
    const memberId = document.getElementById('team-member-modal').getAttribute('data-member-id');
    const isEditing = memberId !== '';
    
    const teamMember = {
        name: document.getElementById('team-member-name').value,
        email: document.getElementById('team-member-email').value,
        githubUsername: document.getElementById('team-member-github').value || null
    };
    
    if (isEditing) {
        teamMember.id = memberId;
    } else {
        teamMember.id = generateUUID();
    }
    
    try {
        showLoading();
        
        if (isEditing) {
            await api.updateTeamMember(memberId, teamMember);
        } else {
            await api.createTeamMember(teamMember);
        }
        
        // Close modal
        bootstrap.Modal.getInstance(document.getElementById('team-member-modal')).hide();
        
        // Refresh team members
        app.teamMembers = await api.getAllTeamMembers();
        
        // Refresh view if in team section
        if (app.currentSection === 'team') {
            renderTeamMembers(app.teamMembers);
        }
        
        hideLoading();
    } catch (error) {
        hideLoading();
        handleApiError(error);
    }
}

// Edit team member
function editTeamMember(member) {
    openTeamMemberModal(member);
}

// Confirm team member deletion
function confirmDeleteTeamMember(member) {
    showConfirmModal(
        'Delete Team Member',
        `Are you sure you want to delete team member "${member.name}"? This may affect tasks assigned to them.`,
        () => deleteTeamMember(member.id)
    );
}

// Delete team member
async function deleteTeamMember(memberId) {
    try {
        showLoading();
        
        await api.deleteTeamMember(memberId);
        
        // Refresh team members
        app.teamMembers = await api.getAllTeamMembers();
        
        // Refresh view if in team section
        if (app.currentSection === 'team') {
            renderTeamMembers(app.teamMembers);
        }
        
        hideLoading();
    } catch (error) {
        hideLoading();
        handleApiError(error);
    }
} 