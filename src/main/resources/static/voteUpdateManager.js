/**
 * voteUpdateManager.js
 * Manages SSE (Server-Sent Events) connection for live vote updates.
 *
 * LEARNING NOTES (for college students):
 * ─────────────────────────────────────────────────────────────────
 * SSE = Server-Sent Events
 *   - A one-way channel: SERVER pushes data TO the browser automatically.
 *   - The browser opens a single HTTP connection and keeps it alive.
 *   - Every time the server sends a message, the browser fires an event.
 *   - Simpler than WebSockets for one-direction data (like live scores).
 *
 * Reconnection strategy: Exponential Backoff
 *   - First failure  → wait 3 seconds
 *   - Second failure → wait 6 seconds
 *   - Third failure  → wait 12 seconds  (doubles each time)
 *   - Capped at 30 seconds max
 *   - Why? So we don't spam the server with reconnect requests.
 *
 * Page Visibility API
 *   - Detects when user switches browser tabs.
 *   - If reconnect was given up, try again when user comes back.
 * ─────────────────────────────────────────────────────────────────
 *
 * WHAT CHANGED from original:
 *  - setStatus() now drives the .status-bar CSS classes (connected / connecting / disconnected)
 *    instead of changing inline text colour — so dark mode works automatically.
 *  - statusBar element is looked up properly by ID.
 *  - Removed duplicate inline status logic that existed in live-results.html.
 *  - Added JSDoc comments throughout for learning.
 *  - FIX: className / dot selector now match the actual markup prefix
 *    ("lr-status-bar" / "lr-s-dot") used in live-results.html. The
 *    previous "status-bar" / ".s-dot" values had no matching CSS rules,
 *    so the status bar lost all styling the moment the SSE connection
 *    opened and setStatus() ran for the first time.
 */
(function () {
    'use strict';

    /* ══════════════════════════════════════════
       CONFIGURATION
       ══════════════════════════════════════════ */
    const CONFIG = {
        ENDPOINT:               '/api/stream/events',  // SSE endpoint on your Spring Boot server
        MAX_RECONNECT_ATTEMPTS: 5,                     // Give up after 5 failures
        INITIAL_RECONNECT_DELAY: 3000,                 // 3 seconds first wait
        MAX_RECONNECT_DELAY:    30000,                 // Never wait more than 30 seconds
    };

    /* ══════════════════════════════════════════
       STATE
       All mutable state in one object — easier to debug.
       ══════════════════════════════════════════ */
    const state = {
        connected:         false,
        reconnectAttempts: 0,
        eventSource:       null,
        lastUpdate:        null,
        visible:           !document.hidden,
    };

    /* ══════════════════════════════════════════
       DOM REFERENCES
       ══════════════════════════════════════════ */
    const statusBar    = document.getElementById('statusBar');
    const statusText   = document.getElementById('statusText');
    const lastUpdateEl = document.getElementById('lastUpdate');
    const resultsTable = document.getElementById('resultsTable');

    /* ══════════════════════════════════════════
       STATUS DISPLAY
       Uses CSS classes (connected / connecting / disconnected)
       so dark mode colour variables apply automatically.
       ══════════════════════════════════════════ */

    /**
     * Set the status bar message and visual state.
     * @param {string} message - Text to show
     * @param {'connected'|'connecting'|'disconnected'} cssClass
     */
    function setStatus(message, cssClass) {
        if (statusText)  statusText.textContent = message;
        if (statusBar)   statusBar.className = `lr-status-bar ${cssClass}`;

        // Dot pulse animation: only animate when connected/connecting
        const dot = statusBar?.querySelector('.lr-s-dot');
        if (dot) {
            dot.style.animation = cssClass === 'disconnected'
                ? 'none'
                : 'pulse 2s ease-in-out infinite';
        }
    }

    /* ══════════════════════════════════════════
       TIMESTAMP
       ══════════════════════════════════════════ */

    /**
     * Update the "Last Updated" timestamp display to current time.
     */
    function stampTime() {
        if (!lastUpdateEl) return;
        const now = new Date();
        const hh  = String(now.getHours()).padStart(2, '0');
        const mm  = String(now.getMinutes()).padStart(2, '0');
        const ss  = String(now.getSeconds()).padStart(2, '0');
        lastUpdateEl.textContent = `${hh}:${mm}:${ss}`;
        state.lastUpdate = now;
    }

    /* ══════════════════════════════════════════
       TABLE FLASH
       Visual feedback when new vote data arrives.
       Rows briefly dim then fade back in — signals
       a refresh happened without a full page reload.
       ══════════════════════════════════════════ */

    /**
     * Flash all table rows to indicate new data received.
     * In production, call fetch('/api/results') here and re-render the table.
     */
    function flashTableRows() {
        if (!resultsTable) return;

        stampTime();

        const rows = resultsTable.querySelectorAll('tbody tr');
        rows.forEach((row, index) => {
            // Stagger: each row fades back with a slight delay after the previous
            row.style.opacity = '0.45';
            setTimeout(() => {
                row.style.opacity = '1';
                row.style.transition = 'opacity 300ms ease';
            }, 80 + index * 35);
        });

        /*
         * PRODUCTION TODO:
         * Replace the flash with a real data update:
         *
         * fetch('/api/results')
         *   .then(r => r.json())
         *   .then(data => {
         *       updateTableRows(data);   // your function to re-render <tbody>
         *       updateStats(data);       // update stat-card numbers
         *       stampTime();
         *   })
         *   .catch(err => console.error('[LiveUpdate] Fetch failed:', err));
         */
    }

    /* ══════════════════════════════════════════
       RECONNECT DELAY — EXPONENTIAL BACKOFF
       ══════════════════════════════════════════ */

    /**
     * Calculate how long to wait before reconnecting.
     * Formula: initialDelay × 2^attempts  (capped at maxDelay)
     * Jitter (random 0–1 s) prevents all clients reconnecting simultaneously.
     *
     * Example:
     *   attempt 1 → 3s × 2^1 = 6s  + jitter
     *   attempt 2 → 3s × 2^2 = 12s + jitter
     *   attempt 3 → 3s × 2^3 = 24s + jitter  (capped at 30s)
     *
     * @returns {number} delay in milliseconds
     */
    function getReconnectDelay() {
        const base  = CONFIG.INITIAL_RECONNECT_DELAY * Math.pow(2, state.reconnectAttempts);
        const capped = Math.min(base, CONFIG.MAX_RECONNECT_DELAY);
        const jitter = Math.random() * 1000;
        return capped + jitter;
    }

    /* ══════════════════════════════════════════
       SSE CONNECTION
       ══════════════════════════════════════════ */

    /**
     * Open a new SSE connection to the server.
     * Handles open, vote-update events, and errors with auto-reconnect.
     */
    function connect() {
        // Guard: EventSource not supported in very old browsers
        if (!window.EventSource) {
            setStatus('⚠ Live updates not supported in this browser', 'disconnected');
            return;
        }

        setStatus('⏳ Connecting to live updates…', 'connecting');

        // Create the SSE connection
        // The browser sends GET /api/stream/events with Accept: text/event-stream
        state.eventSource = new EventSource(CONFIG.ENDPOINT);

        /**
         * onopen: fires when the HTTP connection is established.
         * Reset reconnect counter because we have a fresh connection.
         */
        state.eventSource.onopen = function () {
            state.connected        = true;
            state.reconnectAttempts = 0;
            setStatus('● Live — receiving vote updates', 'connected');
            stampTime();
            console.log('[LiveUpdate] Connected to event stream');
        };

        /**
         * vote-update: custom event sent by your Spring Boot backend.
         * In Spring Boot you'd do:
         *   SseEmitter emitter = ...;
         *   emitter.send(SseEmitter.event().name("vote-update").data("refresh"));
         */
        state.eventSource.addEventListener('vote-update', function (event) {
            console.log('[LiveUpdate] Vote update received:', event.data);
            flashTableRows();
        });

        /**
         * onerror: fires when the connection drops (server down, network loss, etc.)
         * We close the broken connection and schedule a reconnect.
         */
        state.eventSource.onerror = function (error) {
            state.connected = false;
            state.eventSource.close(); // Important: always close before reconnecting
            console.warn('[LiveUpdate] Connection error:', error);

            // Too many failures → give up
            if (state.reconnectAttempts >= CONFIG.MAX_RECONNECT_ATTEMPTS) {
                setStatus('✗ Connection lost. Refresh the page to try again.', 'disconnected');
                return;
            }

            // Schedule reconnect
            state.reconnectAttempts++;
            const delay        = getReconnectDelay();
            const delaySecs    = Math.round(delay / 1000);
            setStatus(
                `⏳ Reconnecting in ${delaySecs}s (${state.reconnectAttempts}/${CONFIG.MAX_RECONNECT_ATTEMPTS})…`,
                'connecting'
            );

            console.log(`[LiveUpdate] Reconnecting in ${delaySecs}s…`);
            setTimeout(connect, delay);
        };
    }

    /* ══════════════════════════════════════════
       PAGE VISIBILITY API
       If the user comes back to the tab and the
       connection was abandoned, restart it.
       ══════════════════════════════════════════ */
    document.addEventListener('visibilitychange', function () {
        state.visible = !document.hidden;
        console.log('[LiveUpdate] Visibility:', state.visible ? 'visible' : 'hidden');

        if (state.visible && !state.connected) {
            // User returned to tab — try connecting again from scratch
            console.log('[LiveUpdate] Page visible again — attempting reconnect');
            state.reconnectAttempts = 0;
            connect();
        }
    });

    /* ══════════════════════════════════════════
       CLEANUP ON PAGE UNLOAD
       Close the SSE connection cleanly so the server
       doesn't keep an open stream for a gone client.
       ══════════════════════════════════════════ */
    window.addEventListener('beforeunload', function () {
        if (state.eventSource) {
            state.eventSource.close();
            console.log('[LiveUpdate] Connection closed on page unload');
        }
    });

    /* ══════════════════════════════════════════
       INIT
       ══════════════════════════════════════════ */
    function init() {
        console.log('[LiveUpdate] Initializing live update manager');
        connect();
    }

    // Start after DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    /* ══════════════════════════════════════════
       PUBLIC API (for debugging in browser console)
       Open DevTools and type: voteUpdateManager.state
       to inspect the connection state.
       ══════════════════════════════════════════ */
    window.voteUpdateManager = {
        connect,
        state,
        flashTableRows,
        setStatus,
        stampTime,
    };

})();