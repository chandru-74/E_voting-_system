/**
 * Real-time Vote Update Handler
 * Manages EventSource connections for live vote counting
 * Place this in a separate JS file: assets/js/real-time-updates.js
 */

class VoteUpdateManager {
    constructor(containerId = 'results') {
        this.containerId = containerId;
        this.eventSource = null;
        this.maxVotes = 0;
        this.resultsData = {};
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 3000;
    }

    /**
     * Initialize the EventSource connection
     */
    init() {
        this.connectEventSource();
    }

    /**
     * Establish EventSource connection with error handling
     */
    connectEventSource() {
        try {
            this.eventSource = new EventSource('/api/stream/events');

            this.eventSource.onopen = () => {
                console.log('✓ Connected to vote stream');
                this.reconnectAttempts = 0;
                this.showConnectionStatus(true);
            };

            this.eventSource.onmessage = (event) => {
                this.handleVoteUpdate(event);
            };

            this.eventSource.onerror = (error) => {
                console.error('✗ EventSource error:', error);
                this.handleConnectionError();
            };

            window.addEventListener('beforeunload', () => {
                this.close();
            });

        } catch (error) {
            console.error('Failed to create EventSource:', error);
            this.handleConnectionError();
        }
    }

    /**
     * Handle incoming vote updates
     */
    handleVoteUpdate(event) {
        try {
            const data = JSON.parse(event.data);

            // Update local data
            this.resultsData = data;

            // Calculate max votes for progress bars
            this.maxVotes = Math.max(...Object.values(data).map(v => Number(v) || 0), 1);

            // Render updates
            this.updateDisplay();

        } catch (error) {
            console.error('Error parsing vote data:', error);
        }
    }

    /**
     * Update the DOM with new vote counts
     */
    updateDisplay() {
        const container = document.getElementById(this.containerId);

        if (!container) {
            console.warn('Results container not found');
            return;
        }

        if (Object.keys(this.resultsData).length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-icon">🗳️</div>
                    <p>No votes cast yet. Be the first to vote!</p>
                </div>
            `;
            return;
        }

        // Sort results by vote count (descending)
        const sorted = Object.entries(this.resultsData)
            .map(([name, votes]) => ({ name, votes: Number(votes) || 0 }))
            .sort((a, b) => b.votes - a.votes);

        // Render each result
        container.innerHTML = sorted.map((result, index) => {
            const percentage = this.maxVotes > 0
                ? ((result.votes / this.maxVotes) * 100).toFixed(1)
                : 0;

            return `
                <div class="result-item" data-candidate="${result.name}">
                    <div class="result-header">
                        <div class="candidate-info">
                            <span class="candidate-avatar">👤</span>
                            <div class="candidate-details">
                                <h3 class="candidate-name">${this.escapeHtml(result.name)}</h3>
                            </div>
                        </div>
                        <div class="vote-info">
                            <div class="vote-badge">
                                <div class="vote-count">${result.votes}</div>
                                <div class="vote-label">VOTES</div>
                            </div>
                        </div>
                    </div>

                    <div class="progress-section">
                        <div class="progress-header">
                            <span class="progress-label">Vote Share</span>
                            <span class="progress-percentage">${percentage}%</span>
                        </div>
                        <div class="progress-bar">
                            <div class="progress-fill" style="width: ${Math.max(2, percentage)}%"></div>
                        </div>
                    </div>
                </div>
            `;
        }).join('');

        // Update statistics
        this.updateStats(sorted);
    }

    /**
     * Update statistics cards
     */
    updateStats(sortedResults) {
        const totalVotes = sortedResults.reduce((sum, r) => sum + r.votes, 0);
        const leadingCandidate = sortedResults[0];

        const totalVotesEl = document.getElementById('totalVotes');
        const leadingEl = document.getElementById('leadingCandidate');
        const countEl = document.getElementById('candidateCount');

        if (totalVotesEl) {
            totalVotesEl.textContent = totalVotes;
        }
        if (leadingEl && leadingCandidate) {
            leadingEl.textContent = leadingCandidate.name.substring(0, 25);
        }
        if (countEl) {
            countEl.textContent = sortedResults.length;
        }
    }

    /**
     * Handle connection errors with reconnection logic
     */
    handleConnectionError() {
        this.showConnectionStatus(false);

        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            const delay = this.reconnectDelay * this.reconnectAttempts;

            console.log(`Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts}) in ${delay}ms...`);

            setTimeout(() => {
                this.connectEventSource();
            }, delay);
        } else {
            this.showErrorMessage('Connection lost. Please refresh the page to restore connection.');
        }
    }

    /**
     * Show/hide connection status indicator
     */
    showConnectionStatus(connected) {
        const badge = document.querySelector('.live-badge');
        if (!badge) return;

        if (connected) {
            badge.classList.remove('disconnected');
            badge.style.opacity = '1';
        } else {
            badge.classList.add('disconnected');
            badge.style.opacity = '0.6';
        }
    }

    /**
     * Display error message to user
     */
    showErrorMessage(message) {
        const container = document.getElementById(this.containerId);
        if (container) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-icon">⚠️</div>
                    <p>${this.escapeHtml(message)}</p>
                </div>
            `;
        }
    }

    /**
     * Escape HTML special characters
     */
    escapeHtml(text) {
        const map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        };
        return String(text).replace(/[&<>"']/g, m => map[m]);
    }

    /**
     * Close EventSource connection
     */
    close() {
        if (this.eventSource) {
            this.eventSource.close();
            console.log('EventSource closed');
        }
    }
}

/**
 * Initialize on page load
 */
document.addEventListener('DOMContentLoaded', function() {
    const manager = new VoteUpdateManager('results');
    manager.init();

    // Expose for debugging
    window.voteUpdateManager = manager;
});

/**
 * Utility: Auto-reload page if connection fails completely
 */
window.addEventListener('load', function() {
    let connectionCheckInterval = setInterval(function() {
        if (!navigator.onLine) {
            console.warn('No internet connection');
            clearInterval(connectionCheckInterval);
        }
    }, 5000);
});