// Configuration
const API_URL = 'http://127.0.0.1:8080';
let currentResearchId = null;
let ws = null;
let serverCheckInterval = null;
let serverOnline = false;

// Elements
const serverStatus = document.getElementById('serverStatus');
const statusIndicator = document.getElementById('statusIndicator');
const statusText = document.getElementById('statusText');
const refreshBtn = document.getElementById('refreshBtn');
const topicInput = document.getElementById('topicInput');
const agentSelect = document.getElementById('agentSelect');
const newAgentBtn = document.getElementById('newAgentBtn');
const editAgentBtn = document.getElementById('editAgentBtn');
const deleteAgentBtn = document.getElementById('deleteAgentBtn');
const viewReportsBtn = document.getElementById('viewReportsBtn');
const searchBtn = document.getElementById('searchBtn');
const btnText = document.getElementById('btnText');

// Modal elements
const agentModal = document.getElementById('agentModal');
const modalTitle = document.getElementById('modalTitle');
const closeModal = document.getElementById('closeModal');
const cancelAgent = document.getElementById('cancelAgent');
const agentForm = document.getElementById('agentForm');
const reportsModal = document.getElementById('reportsModal');
const closeReportsModal = document.getElementById('closeReportsModal');
const reportsList = document.getElementById('reportsList');
const reportsModalTitle = document.getElementById('reportsModalTitle');

// Form inputs
const agentName = document.getElementById('agentName');
const agentModel = document.getElementById('agentModel');
const agentTemperature = document.getElementById('agentTemperature');
const agentMaxTokens = document.getElementById('agentMaxTokens');
const agentMaxIterations = document.getElementById('agentMaxIterations');

// Current editing agent
let currentEditingAgentId = null;
let allAgents = [];

const progressSection = document.getElementById('progressSection');
const researchId = document.getElementById('researchId');
const progressFill = document.getElementById('progressFill');
const statusUpdates = document.getElementById('statusUpdates');
const metrics = document.getElementById('metrics');
const currentIteration = document.getElementById('currentIteration');
const sourcesFound = document.getElementById('sourcesFound');
const claimsExtracted = document.getElementById('claimsExtracted');

const resultsSection = document.getElementById('resultsSection');
const reportPreview = document.getElementById('reportPreview');
const newSearchBtn = document.getElementById('newSearchBtn');

const errorSection = document.getElementById('errorSection');
const errorText = document.getElementById('errorText');
const retryBtn = document.getElementById('retryBtn');

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    checkServerStatus();
    loadAgents();
    
    searchBtn.addEventListener('click', startResearch);
    topicInput.addEventListener('input', updateSearchButtonState);
    newAgentBtn.addEventListener('click', () => openAgentModal());
    editAgentBtn.addEventListener('click', editCurrentAgent);
    deleteAgentBtn.addEventListener('click', deleteCurrentAgent);
    viewReportsBtn.addEventListener('click', viewAgentReports);
    newSearchBtn.addEventListener('click', resetUI);
    retryBtn.addEventListener('click', resetUI);
    refreshBtn.addEventListener('click', manualRefreshConnection);
    
    // Modal controls
    closeModal.addEventListener('click', closeAgentModal);
    cancelAgent.addEventListener('click', closeAgentModal);
    agentForm.addEventListener('submit', saveAgentForm);
    closeReportsModal.addEventListener('click', closeReportsModalFn);
    
    // Close modal on background click
    agentModal.addEventListener('click', (e) => {
        if (e.target === agentModal) closeAgentModal();
    });
    reportsModal.addEventListener('click', (e) => {
        if (e.target === reportsModal) closeReportsModalFn();
    });
    
    // Update button states when agent selection changes
    agentSelect.addEventListener('change', updateAgentButtonStates);
    
    updateSearchButtonState();
});

// Cleanup on window close
window.addEventListener('beforeunload', () => {
    stopServerPolling();
    if (ws) {
        ws.close();
    }
});

// Check if server is running
async function checkServerStatus() {
    try {
        const response = await fetch(`${API_URL}/api/health`);
        if (response.ok) {
            setServerStatus(true);
        } else {
            setServerStatus(false);
        }
    } catch (error) {
        setServerStatus(false);
    }
}

function setServerStatus(online) {
	const wasOnline = serverOnline;
	serverOnline = online;
    if (online) {
        statusIndicator.className = 'status-indicator online';
        statusText.textContent = 'Server connected';
        
        // Stop polling if server is online
        stopServerPolling();
		if (!wasOnline) {
			loadAgents();
			if (reportsModal && reportsModal.style.display === 'flex' && agentSelect.value) {
				viewAgentReports();
			}
		}
    } else {
        statusIndicator.className = 'status-indicator offline';
        statusText.textContent = 'Server disconnected - Start the backend';
        
        // Start polling to detect when server comes back
        startServerPolling();
    }
    updateSearchButtonState();
}

// Start automatic server status polling
function startServerPolling() {
    // Avoid duplicates
    if (serverCheckInterval) return;
    
    console.log('Starting server polling (every 5 seconds)');
    serverCheckInterval = setInterval(() => {
        checkServerStatus();
    }, 5000); // Check every 5 seconds
}

// Stop polling
function stopServerPolling() {
    if (serverCheckInterval) {
        console.log('Stopping server polling');
        clearInterval(serverCheckInterval);
        serverCheckInterval = null;
    }
}

// Enable the search button only when server is online and topic is non-empty
function updateSearchButtonState() {
    const hasTopic = topicInput && topicInput.value.trim().length > 0;
    searchBtn.disabled = !(serverOnline && hasTopic);
}

// Manual connection refresh
async function manualRefreshConnection() {
    // Button animation
    refreshBtn.classList.add('spinning');
	try {
		await checkServerStatus();
		if (serverOnline) {
			// Reload dynamic data (agents, reports list if open, etc.)
			await loadAgents();
			if (reportsModal && reportsModal.style.display === 'flex' && agentSelect.value) {
				await viewAgentReports();
			}
		}
	} finally {
		// Remove animation after 1 second
		setTimeout(() => {
			refreshBtn.classList.remove('spinning');
		}, 1000);
	}
}


// Start a new research
async function startResearch() {
    const topic = topicInput.value.trim();

    if (!topic) {
        // Do nothing when no topic; avoid showing error UI
        return;
    }

    try {
        searchBtn.disabled = true;
        btnText.textContent = 'Starting...';

        const agentId = agentSelect.value ? parseInt(agentSelect.value) : null;
     
        const payload = { topic, agentId };
        if (!agentId) {
            payload.maxIterations = 3; // Default when no agent is selected
        }
        
        const response = await fetch(`${API_URL}/api/research`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            throw new Error('Error starting research');
        }

        const data = await response.json();
        currentResearchId = data.id;

        // Show progress section
        showProgressSection();
        researchId.textContent = `ID: ${currentResearchId.substring(0, 8)}...`;

        // Connect to WebSocket
        connectWebSocket(currentResearchId);

    } catch (error) {
        console.error('Error starting research:', error);
        showError(error.message);
        resetButton();
    }
}

// Agents Management
async function loadAgents() {
    try {
        const res = await fetch(`${API_URL}/api/agents`);
        if (!res.ok) return;
        const agents = await res.json();
        allAgents = agents;
        renderAgents(agents);
        updateAgentButtonStates();
    } catch (e) {
        console.error('Failed to load agents', e);
    }
}

function renderAgents(agents) {
    agentSelect.innerHTML = '';
    const opt = document.createElement('option');
    opt.value = '';
    opt.textContent = 'Default agent (config)';
    agentSelect.appendChild(opt);
    agents.forEach(a => {
        const o = document.createElement('option');
        o.value = a.id;
        o.textContent = `${a.name} (${a.model})`;
        agentSelect.appendChild(o);
    });
}

function updateAgentButtonStates() {
    const hasAgent = agentSelect.value !== '';
    editAgentBtn.disabled = !hasAgent;
    deleteAgentBtn.disabled = !hasAgent;
    viewReportsBtn.disabled = !hasAgent;
}

// Modal Functions
function openAgentModal(agentId = null) {
    currentEditingAgentId = agentId;
    
    if (agentId) {
        // Edit mode
        const agent = allAgents.find(a => a.id === parseInt(agentId));
        if (!agent) return;
        
        modalTitle.textContent = 'Edit Agent';
        agentName.value = agent.name;
        agentModel.value = agent.model;
        agentTemperature.value = agent.temperature || '';
        agentMaxTokens.value = agent.maxTokens || '';
        agentMaxIterations.value = agent.maxIterations || '';
    } else {
        // Create mode
        modalTitle.textContent = 'New Agent';
        agentForm.reset();
    }
    
    agentModal.style.display = 'flex';
}

function closeAgentModal() {
    agentModal.style.display = 'none';
    agentForm.reset();
    currentEditingAgentId = null;
}

async function saveAgentForm(e) {
    e.preventDefault();
    
    const data = {
        name: agentName.value,
        model: agentModel.value,
        temperature: agentTemperature.value ? parseFloat(agentTemperature.value) : null,
        maxTokens: agentMaxTokens.value ? parseInt(agentMaxTokens.value) : null,
        maxIterations: agentMaxIterations.value ? parseInt(agentMaxIterations.value) : null
    };
    
    try {
        let res;
        if (currentEditingAgentId) {
            // Update
            res = await fetch(`${API_URL}/api/agents/${currentEditingAgentId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });
        } else {
            // Create
            res = await fetch(`${API_URL}/api/agents`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });
        }
        
        if (res.ok) {
            closeAgentModal();
            await loadAgents();
            if (currentEditingAgentId) {
                agentSelect.value = currentEditingAgentId;
            } else {
                const created = await res.json();
                agentSelect.value = created.id;
            }
            updateAgentButtonStates();
        } else {
            const error = await res.json().catch(() => ({}));
            alert(`Error: ${error.error || res.status}`);
        }
    } catch (e) {
        alert('Network error: ' + e.message);
    }
}

async function editCurrentAgent() {
    const agentId = agentSelect.value;
    if (!agentId) return;
    openAgentModal(agentId);
}

async function deleteCurrentAgent() {
    const agentId = agentSelect.value;
    if (!agentId) return;
    
    const agent = allAgents.find(a => a.id === parseInt(agentId));
    if (!agent) return;
    
    if (!confirm(`Delete agent "${agent.name}"?\n\nThis will not delete associated reports.`)) {
        return;
    }
    
    try {
        const res = await fetch(`${API_URL}/api/agents/${agentId}`, {
            method: 'DELETE'
        });
        
        if (res.ok || res.status === 204) {
            agentSelect.value = '';
            await loadAgents();
            updateAgentButtonStates();
        } else {
            alert('Error deleting agent');
        }
    } catch (e) {
        alert('Network error: ' + e.message);
    }
}

// Reports Modal
async function viewAgentReports() {
    const agentId = agentSelect.value;
    if (!agentId) return;
    
    const agent = allAgents.find(a => a.id === parseInt(agentId));
    if (!agent) return;
    
    reportsModalTitle.textContent = `Reports from "${agent.name}"`;
    reportsList.innerHTML = '<p style="text-align:center; color:#6c757d;">Loading...</p>';
    reportsModal.style.display = 'flex';
    
    try {
        const res = await fetch(`${API_URL}/api/agents/${agentId}/reports`);
        if (!res.ok) {
            reportsList.innerHTML = '<p style="text-align:center; color:#dc3545;">Loading error</p>';
            return;
        }
        
        const reports = await res.json();
        renderReports(reports);
    } catch (e) {
        reportsList.innerHTML = '<p style="text-align:center; color:#dc3545;">Network error</p>';
    }
}

function renderReports(reports) {
    if (reports.length === 0) {
        reportsList.innerHTML = `
            <div class="no-reports">
                <div class="no-reports-icon">📊</div>
                <p>No reports for this agent</p>
            </div>
        `;
        return;
    }
    
    reportsList.innerHTML = '';
    reports.forEach(report => {
        const item = document.createElement('div');
        item.className = 'report-item';
        
        const date = new Date(report.createdAt);
        const dateStr = date.toLocaleDateString('en-US', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
        
        item.innerHTML = `
            <div class="report-item-header">
                <div class="report-topic">${escapeHtml(report.topic)}</div>
                <div class="report-date">${dateStr}</div>
            </div>
            <div class="report-id">ID: ${report.id}</div>
        `;
        
        item.addEventListener('click', () => viewReportDetails(report.id));
        reportsList.appendChild(item);
    });
}

async function viewReportDetails(reportId) {
    try {
        const res = await fetch(`${API_URL}/api/reports/${reportId}`);
        if (!res.ok) {
            alert('Error loading report');
            return;
        }
        
        const report = await res.json();
        closeReportsModalFn();
        
        // Show the report in the results section
        progressSection.style.display = 'none';
        resultsSection.style.display = 'block';
        errorSection.style.display = 'none';
        document.querySelector('.search-section').style.display = 'none';
        
        reportPreview.innerHTML = renderMarkdown(report.report);
        scrollReportToTop();
    } catch (e) {
        alert('Network error: ' + e.message);
    }
}

function closeReportsModalFn() {
    reportsModal.style.display = 'none';
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Connect to WebSocket for real-time updates
function connectWebSocket(id) {
    ws = new WebSocket(`ws://127.0.0.1:8080/api/research/${id}/stream`);

    ws.onopen = () => {
        console.log('WebSocket connected');
        addStatusUpdate('Connection established', 'status');
    };

    ws.onmessage = (event) => {
        try {
            const update = JSON.parse(event.data);
            handleUpdate(update);
        } catch (error) {
            console.error('Error parsing update:', error);
        }
    };

    ws.onerror = (error) => {
        console.error('WebSocket error:', error);
        addStatusUpdate('WebSocket connection error', 'error');
    };

    ws.onclose = () => {
        console.log('WebSocket closed');
    };
}

// Handle WebSocket updates
function handleUpdate(update) {
    const { type, data } = update;

    switch (type) {
        case 'STATUS':
            addStatusUpdate(data, 'status');
            // Extract percentage from status if present
            const statusMatch = data.match(/\((\d+)%\)/);
            if (statusMatch) {
                updateProgress(parseInt(statusMatch[1]));
            }
            break;

        case 'ITERATION':
            addStatusUpdate(data, 'iteration');
            // Extract percentage from iteration message
            const iterMatch = data.match(/\((\d+)%\)/);
            if (iterMatch) {
                updateProgress(parseInt(iterMatch[1]));
            }
            break;

        case 'QUERY':
            addStatusUpdate(`${data}`, 'query');
            break;

            
        case 'REPORT':
            addStatusUpdate('Report generated', 'status');
            updateProgress(100);
            showReport(data);
            break;

        case 'ERROR':
            addStatusUpdate(`${data}`, 'error');
            showError(data);
            break;
    }
}

// Add status update to UI
function addStatusUpdate(message, type = 'status') {
    const update = document.createElement('div');
    update.className = `status-update ${type}`;
    update.textContent = message;
    
    const MAX_EVENTS = 8;
    while (statusUpdates.children.length >= MAX_EVENTS) {
        statusUpdates.removeChild(statusUpdates.firstChild);
    }
    
    statusUpdates.appendChild(update);
    
    requestAnimationFrame(() => {
        statusUpdates.scrollTop = statusUpdates.scrollHeight;
    });
}

// Update progress bar
function updateProgress(percent) {
    progressFill.style.width = `${percent}%`;
}

// Update metrics
function incrementMetric(metricName) {
    const element = document.getElementById(metricName);
    const current = parseInt(element.textContent) || 0;
    element.textContent = current + 1;
}

// Show progress section
function showProgressSection() {
    document.querySelector('.search-section').style.display = 'none';
    progressSection.style.display = 'block';
    errorSection.style.display = 'none';
    resultsSection.style.display = 'none';
    metrics.style.display = 'grid';
}

// Show report
async function showReport(markdown) {
    // Close WebSocket
    if (ws) {
        ws.close();
    }

    progressSection.style.display = 'none';
    resultsSection.style.display = 'block';

    // Simple Markdown rendering (basic)
    reportPreview.innerHTML = renderMarkdown(markdown);
}

// Simple Markdown renderer
function renderMarkdown(markdown) {
    return markdown
        .replace(/^### (.*$)/gim, '<h3>$1</h3>')
        .replace(/^## (.*$)/gim, '<h2>$1</h2>')
        .replace(/^# (.*$)/gim, '<h1>$1</h1>')
        .replace(/\*\*(.*)\*\*/gim, '<strong>$1</strong>')
        .replace(/\*(.*)\*/gim, '<em>$1</em>')
        .replace(/^\* (.*$)/gim, '<li>$1</li>')
        .replace(/^- (.*$)/gim, '<li>$1</li>')
        .replace(/\n/gim, '<br>')
        .replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>');
}

// Ensure the report view starts at the top
function scrollReportToTop() {
    try {
        reportPreview.scrollTop = 0;
        const rootScrollable = document.querySelector('.popup-content') || document.scrollingElement || document.documentElement || document.body;
        if (rootScrollable) {
            rootScrollable.scrollTop = 0;
        }
        requestAnimationFrame(() => {
            reportPreview.scrollTop = 0;
            if (rootScrollable) rootScrollable.scrollTop = 0;
            window.scrollTo(0, 0);
        });
    } catch (_) {
        // Fallback
        window.scrollTo(0, 0);
    }
}

// Show error
function showError(message) {
    errorText.textContent = message;
    errorSection.style.display = 'block';
    progressSection.style.display = 'none';
    
    resetButton();
}

// Reset button
function resetButton() {
    btnText.innerHTML = `
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"></circle>
            <path d="M21 21l-4.35-4.35"></path>
        </svg>
        Start research
    `;
    updateSearchButtonState();
}

// Reset UI
function resetUI() {
    document.querySelector('.search-section').style.display = 'block';
    progressSection.style.display = 'none';
    resultsSection.style.display = 'none';
    errorSection.style.display = 'none';
    
    statusUpdates.innerHTML = '';
    progressFill.style.width = '0%';
    metrics.style.display = 'none';
    
    document.getElementById('sourcesFound').textContent = '0';
    document.getElementById('claimsExtracted').textContent = '0';
    
    topicInput.value = '';
    
    currentResearchId = null;
    resetButton();
    
    if (ws) {
        ws.close();
    }
}
