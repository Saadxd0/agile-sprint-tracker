/**
 * API Client for the Agile Sprint Tracker
 */
class ApiClient {
    constructor() {
        this.baseUrl = window.location.origin + '/api';
    }

    // Generic request methods
    async get(url) {
        try {
            const response = await fetch(`${this.baseUrl}${url}`);
            if (!response.ok) {
                throw new Error(`API Error: ${response.status} - ${response.statusText}`);
            }
            return await response.json();
        } catch (error) {
            console.error(`GET Error for ${url}:`, error);
            throw error;
        }
    }

    async post(url, data) {
        try {
            console.log(`Sending POST to ${url} with data:`, data);
            const response = await fetch(`${this.baseUrl}${url}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(data)
            });
            
            let errorText = '';
            try {
                const responseText = await response.text();
                console.log(`Response for ${url}:`, responseText);
                
                if (!response.ok) {
                    errorText = responseText;
                    throw new Error(`API Error: ${response.status} - ${response.statusText}\n${errorText}`);
                }
                
                if (responseText.trim()) {
                    try {
                        const jsonResponse = JSON.parse(responseText);
                        console.log(`Parsed JSON for ${url}:`, jsonResponse);
                        return jsonResponse;
                    } catch (parseError) {
                        console.error(`Error parsing response JSON for ${url}:`, parseError);
                        throw parseError;
                    }
                }
                return {};
            } catch (parseError) {
                console.error(`Error parsing response from ${url}:`, parseError);
                if (!response.ok) {
                    throw new Error(`API Error: ${response.status} - ${response.statusText}\n${errorText}`);
                }
                throw parseError;
            }
        } catch (error) {
            console.error(`POST Error for ${url}:`, error);
            throw error;
        }
    }

    async put(url, data) {
        try {
            const response = await fetch(`${this.baseUrl}${url}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(data)
            });
            if (!response.ok) {
                throw new Error(`API Error: ${response.status} - ${response.statusText}`);
            }
            return await response.json();
        } catch (error) {
            console.error(`PUT Error for ${url}:`, error);
            throw error;
        }
    }

    async delete(url) {
        try {
            const response = await fetch(`${this.baseUrl}${url}`, {
                method: 'DELETE'
            });
            if (!response.ok) {
                throw new Error(`API Error: ${response.status} - ${response.statusText}`);
            }
            return true;
        } catch (error) {
            console.error(`DELETE Error for ${url}:`, error);
            throw error;
        }
    }

    // Sprint API methods
    async getAllSprints() {
        return this.get('/sprints');
    }

    async getSprint(id) {
        return this.get(`/sprints/${id}`);
    }

    async createSprint(sprint) {
        return this.post('/sprints', sprint);
    }

    async updateSprint(id, sprint) {
        return this.put(`/sprints/${id}`, sprint);
    }

    async deleteSprint(id) {
        return this.delete(`/sprints/${id}`);
    }

    // User Story API methods
    async getUserStories(sprintId) {
        return this.get(`/sprints/${sprintId}/stories`);
    }

    async getUserStory(sprintId, storyId) {
        return this.get(`/sprints/${sprintId}/stories/${storyId}`);
    }

    async createUserStory(sprintId, story) {
        return this.post(`/sprints/${sprintId}/stories`, story);
    }

    async updateUserStory(sprintId, storyId, story) {
        return this.put(`/sprints/${sprintId}/stories/${storyId}`, story);
    }

    async deleteUserStory(sprintId, storyId) {
        return this.delete(`/sprints/${sprintId}/stories/${storyId}`);
    }

    // Task API methods
    async getTasks(sprintId, storyId) {
        return this.get(`/sprints/${sprintId}/stories/${storyId}/tasks`);
    }

    async getTask(sprintId, storyId, taskId) {
        return this.get(`/sprints/${sprintId}/stories/${storyId}/tasks/${taskId}`);
    }

    async createTask(sprintId, storyId, task) {
        return this.post(`/sprints/${sprintId}/stories/${storyId}/tasks`, task);
    }

    async updateTask(sprintId, storyId, taskId, task) {
        return this.put(`/sprints/${sprintId}/stories/${storyId}/tasks/${taskId}`, task);
    }

    async deleteTask(sprintId, storyId, taskId) {
        return this.delete(`/sprints/${sprintId}/stories/${storyId}/tasks/${taskId}`);
    }

    // Team Member API methods
    async getAllTeamMembers() {
        return this.get('/team-members');
    }

    async getTeamMember(id) {
        return this.get(`/team-members/${id}`);
    }

    async createTeamMember(teamMember) {
        return this.post('/team-members', teamMember);
    }

    async updateTeamMember(id, teamMember) {
        return this.put(`/team-members/${id}`, teamMember);
    }

    async deleteTeamMember(id) {
        return this.delete(`/team-members/${id}`);
    }

    // GitHub API methods
    async getGitHubIssues(owner, repo) {
        return this.get(`/github/issues?owner=${owner}&repo=${repo}`);
    }

    async importGitHubIssues(owner, repo, sprintId, userStoryId) {
        return this.post(`/github/issues?owner=${owner}&repo=${repo}&sprintId=${sprintId}&userStoryId=${userStoryId}`);
    }
}

// Create global API client instance
const api = new ApiClient(); 