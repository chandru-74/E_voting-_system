/**
 * ui-features.js
 * Tamil Nadu E-Voting — Shared UI Features
 *
 * Features:
 *  1. Dark Mode toggle (persisted in localStorage)
 *  2. Background Color picker (persisted in localStorage)
 *  3. Aadhaar input: exactly 12 digits, no more
 *  4. Mobile input: exactly 10 digits, no more
 *  5. Numeric-only enforcement
 *  6. Character counter badge for Aadhaar & Mobile fields
 *
 * FIX (this revision):
 *  - The keydown "field is full" guard used window.getSelection() to
 *    detect whether the user had text selected inside the input, so
 *    typing could replace it. window.getSelection() only tracks the
 *    document/contenteditable selection — it never sees a selection
 *    made inside an <input>, so that check was always an empty string
 *    and the guard always treated the field as "nothing selected."
 *    Net effect: once Aadhaar/Mobile hit their max length, selecting
 *    the digits and typing over them did nothing — you had to delete
 *    first. Now uses input.selectionStart/selectionEnd, which is the
 *    correct way to detect a selection inside a form field.
 */

(function () {
    'use strict';

    /* ══════════════════════════════════════════
       CONSTANTS
       ══════════════════════════════════════════ */
    const STORAGE_THEME = 'tn-vote-theme';   // 'light' | 'dark'
    const STORAGE_BG    = 'tn-vote-bg';      // 'default' | 'blue' | 'green' | 'warm' | 'purple' | 'gray'
    const AADHAAR_LEN   = 12;
    const MOBILE_LEN    = 10;

    /* ══════════════════════════════════════════
       1. THEME (DARK / LIGHT MODE)
       ══════════════════════════════════════════ */
    function getTheme()  { return localStorage.getItem(STORAGE_THEME) || 'light'; }
    function saveTheme(t){ localStorage.setItem(STORAGE_THEME, t); }

    function applyTheme(theme) {
        document.documentElement.setAttribute('data-theme', theme === 'dark' ? 'dark' : '');
    }

    function toggleTheme() {
        const next = getTheme() === 'dark' ? 'light' : 'dark';
        saveTheme(next);
        applyTheme(next);
    }

    /* ══════════════════════════════════════════
       2. BACKGROUND COLOR PICKER
       ══════════════════════════════════════════ */
    function getBg()   { return localStorage.getItem(STORAGE_BG) || 'default'; }
    function saveBg(b) { localStorage.setItem(STORAGE_BG, b); }

    function applyBg(bg) {
        document.documentElement.setAttribute('data-bg', bg === 'default' ? '' : bg);
        // Update active swatch indicator
        document.querySelectorAll('.bg-swatch').forEach(s => {
            s.classList.toggle('active', s.dataset.bg === bg);
        });
    }

    /* ══════════════════════════════════════════
       3. INJECT SETTINGS TOOLBAR INTO HEADER
          (runs once per page, inserts into .ec-right)
       ══════════════════════════════════════════ */
    function buildToolbar() {
        const ecRight = document.querySelector('.ec-right');
        if (!ecRight) return;

        // ── Dark mode toggle ──
        const darkBtn = document.createElement('button');
        darkBtn.className = 'dark-toggle';
        darkBtn.setAttribute('aria-label', 'Toggle dark mode');
        darkBtn.innerHTML = `
            <span>${getTheme() === 'dark' ? '☀️' : '🌙'}</span>
            <span class="toggle-track"><span class="toggle-thumb"></span></span>
        `;
        darkBtn.addEventListener('click', () => {
            toggleTheme();
            // Update icon
            const icon = darkBtn.querySelector('span:first-child');
            icon.textContent = getTheme() === 'dark' ? '☀️' : '🌙';
        });

        // ── Background colour swatches ──
        const bgPicker = document.createElement('div');
        bgPicker.className = 'bg-picker';
        bgPicker.setAttribute('aria-label', 'Choose background colour');
        bgPicker.title = 'Background colour';

        const swatches = [
            { key: 'default', label: 'Default grey'  },
            { key: 'blue',    label: 'Blue tint'     },
            { key: 'green',   label: 'Green tint'    },
            { key: 'warm',    label: 'Warm orange'   },
            { key: 'purple',  label: 'Purple tint'   },
            { key: 'gray',    label: 'Neutral grey'  },
        ];

        swatches.forEach(({ key, label }) => {
            const dot = document.createElement('span');
            dot.className = 'bg-swatch';
            dot.dataset.bg = key;
            dot.title = label;
            dot.setAttribute('role', 'button');
            dot.setAttribute('tabindex', '0');
            dot.addEventListener('click', () => { saveBg(key); applyBg(key); });
            dot.addEventListener('keydown', e => { if (e.key === 'Enter' || e.key === ' ') { saveBg(key); applyBg(key); } });
            bgPicker.appendChild(dot);
        });

        // ── Assemble settings toolbar ──
        const toolbar = document.createElement('div');
        toolbar.className = 'settings-toolbar';
        toolbar.appendChild(bgPicker);
        toolbar.appendChild(darkBtn);

        // Insert before flag pill or as first child of ec-right
        const flagPill = ecRight.querySelector('.flag-pill');
        ecRight.insertBefore(toolbar, flagPill || ecRight.firstChild);
    }

    /* ══════════════════════════════════════════
       4. INPUT LENGTH ENFORCEMENT
          Aadhaar = exactly 12 digits
          Mobile  = exactly 10 digits
       ══════════════════════════════════════════ */

    /**
     * Adds a character counter badge and enforces max length.
     * @param {HTMLInputElement} input
     * @param {number} maxLen
     */
    function addCharCounter(input, maxLen) {
        // Wrap the input
        const parent = input.parentNode;
        const wrapper = document.createElement('div');
        wrapper.className = 'input-limit-wrap';
        parent.insertBefore(wrapper, input);
        wrapper.appendChild(input);

        // Counter badge
        const counter = document.createElement('span');
        counter.className = 'char-counter';
        counter.setAttribute('aria-live', 'polite');
        wrapper.appendChild(counter);

        function update() {
            const len = input.value.length;
            counter.textContent = `${len}/${maxLen}`;
            counter.classList.remove('at-limit', 'over-limit', 'shake');

            if (len > maxLen) {
                // Hard cut — never allow more than maxLen chars
                input.value = input.value.slice(0, maxLen);
                counter.classList.add('at-limit');
                // Trigger shake animation
                void counter.offsetWidth; // reflow to restart animation
                counter.classList.add('shake');
            } else if (len === maxLen) {
                counter.classList.add('at-limit');
            }
        }

        // Enforce on every input event
        input.addEventListener('input', () => {
            // Strip non-digits first
            input.value = input.value.replace(/\D/g, '');
            // Then enforce max length
            if (input.value.length > maxLen) {
                input.value = input.value.slice(0, maxLen);
            }
            update();
        });

        // Enforce paste
        input.addEventListener('paste', (e) => {
            e.preventDefault();
            const pasted = (e.clipboardData || window.clipboardData).getData('text');
            const digits = pasted.replace(/\D/g, '').slice(0, maxLen);
            input.value = digits;
            update();
            // Trigger a native input event so other listeners fire
            input.dispatchEvent(new Event('input', { bubbles: true }));
        });

        // Enforce keydown: block non-numeric keys (allow control keys)
        input.addEventListener('keydown', (e) => {
            const allowed = [
                'Backspace','Delete','ArrowLeft','ArrowRight',
                'Tab','Home','End','Enter'
            ];
            const isCtrl = e.ctrlKey || e.metaKey; // allow Ctrl+A, Ctrl+C, Ctrl+V, etc.
            const isDigit = /^[0-9]$/.test(e.key);

            if (!isDigit && !allowed.includes(e.key) && !isCtrl) {
                e.preventDefault(); // Block letters, symbols, etc.
            }

            // Block digit only if already at max AND nothing is selected to
            // overwrite. input.selectionStart/selectionEnd is the correct
            // way to read a selection inside a form field — window.getSelection()
            // does not see it.
            const hasSelection = input.selectionStart !== input.selectionEnd;
            if (isDigit && input.value.length >= maxLen && !isCtrl && !hasSelection) {
                e.preventDefault();
                // Visual feedback
                counter.classList.remove('shake');
                void counter.offsetWidth;
                counter.classList.add('shake');
            }
        });

        // Initial render
        update();
    }

    /**
     * Find and enhance all Aadhaar and Mobile inputs on the page.
     */
    function enhanceInputs() {
        // Aadhaar inputs: id="aadhaar" or name="aadhaar"
        document.querySelectorAll('input[id="aadhaar"], input[name="aadhaar"]').forEach(inp => {
            inp.maxLength = AADHAAR_LEN;
            inp.inputMode = 'numeric';
            addCharCounter(inp, AADHAAR_LEN);
        });

        // Mobile inputs: id="mobile" or name="mobile"
        document.querySelectorAll('input[id="mobile"], input[name="mobile"]').forEach(inp => {
            inp.maxLength = MOBILE_LEN;
            inp.inputMode = 'numeric';
            addCharCounter(inp, MOBILE_LEN);
        });

        // OTP input: exactly 6 digits
        document.querySelectorAll('input[id="otp"], input[name="otp"]').forEach(inp => {
            inp.maxLength = 6;
            inp.inputMode = 'numeric';
            // Already handled in page-specific scripts, just ensure numeric
            inp.addEventListener('input', () => {
                inp.value = inp.value.replace(/\D/g, '').slice(0, 6);
            });
        });

        // Generic numeric fields: sanitize all inputmode="numeric"
        document.querySelectorAll('input[inputmode="numeric"]').forEach(inp => {
            if (!inp.dataset.enhanced) {
                inp.addEventListener('input', () => {
                    inp.value = inp.value.replace(/\D/g, '');
                });
                inp.dataset.enhanced = 'true';
            }
        });
    }

    /* ══════════════════════════════════════════
       5. HIDE FIELD ERRORS ON INPUT
       ══════════════════════════════════════════ */
    function wireErrorHiding() {
        document.querySelectorAll('.form-group input, .form-group select').forEach(inp => {
            inp.addEventListener('input', () => {
                const err = inp.closest('.form-group')?.querySelector('.field-error');
                if (err) err.style.display = 'none';
            });
        });
    }

    /* ══════════════════════════════════════════
       INIT — runs on DOMContentLoaded
       ══════════════════════════════════════════ */
    function init() {
        // Apply persisted preferences immediately
        applyTheme(getTheme());
        applyBg(getBg());

        // Build toolbar
        buildToolbar();

        // Apply active swatch after toolbar is built
        applyBg(getBg());

        // Enhance inputs
        enhanceInputs();
        wireErrorHiding();

        console.log('[ui-features] Initialized — theme:', getTheme(), '| bg:', getBg());
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    // Expose for debugging
    window.tnVoteUI = { toggleTheme, applyTheme, applyBg, enhanceInputs };

})();