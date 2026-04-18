// Stage 7 agentic dashboard — status polling, card actions, inline chat console,
// live curl lines, fallback/step/max-steps badges, docs modal.

(function() {
    var POLL_MS = 3000;
    var CURL_BASE = window.location.origin || ('http://' + window.location.host);

    // Per-demo-id console state: { consoleEl, agentId, demoId, supportsLogin }
    var openConsoles = new Map();

    // Cached card metadata (id -> { card, port, basePath, supportsLogin, traceKind })
    var cards = new Map();

    document.addEventListener('DOMContentLoaded', function() {
        var root = document.querySelector('.agent-cards') || document;
        root.querySelectorAll('.stage-card[data-demo-id]').forEach(function(card) {
            cards.set(card.dataset.demoId, {
                card: card,
                port: card.dataset.demoPort,
                basePath: card.dataset.demoBasePath,
                supportsLogin: card.dataset.demoSupportsLogin === 'true',
                traceKind: card.dataset.demoTraceKind
            });
        });
        pollAllStatuses();
        setInterval(pollAllStatuses, POLL_MS);
    });

    // ===== Status polling =====

    function pollAllStatuses() {
        cards.forEach(function(_, id) {
            fetchJson('/dashboard/agentic/' + id + '/status')
                .then(function(res) {
                    if (res.ok && res.body) applyStatus(id, res.body);
                    else applyStatus(id, { up: false });
                })
                .catch(function() { applyStatus(id, { up: false }); });
        });
    }

    function applyStatus(id, data) {
        var pill = document.getElementById('agentic-status-' + id);
        var text = document.getElementById('agentic-status-text-' + id);
        var provider = document.getElementById('agentic-provider-' + id);
        var model = document.getElementById('agentic-model-' + id);
        var hintBox = document.getElementById('agentic-offline-hint-' + id);
        var hintCmd = document.getElementById('agentic-offline-cmd-' + id);
        if (!pill) return;

        var up = !!data.up;
        pill.style.background = up ? '#28a745' : 'var(--spring-muted, #6c757d)';
        if (text) text.textContent = up ? 'up' : 'offline';
        if (provider) provider.textContent = up && data.provider ? data.provider : '—';
        if (model) model.textContent = up && data.model ? data.model : '—';
        if (hintBox) hintBox.style.display = up ? 'none' : 'flex';
        if (hintCmd && data.startCommand) hintCmd.textContent = data.startCommand;

        var meta = cards.get(id);
        if (meta && meta.card) {
            meta.card.querySelectorAll('.agentic-action-btn').forEach(function(b) {
                b.disabled = !up;
            });
        }
    }

    // ===== Card-level actions =====

    window.copyAgenticCommand = function(demoId) {
        var cmd = document.getElementById('agentic-offline-cmd-' + demoId);
        var text = cmd ? cmd.textContent : ('./workshop.sh agentic start ' + demoId);
        navigator.clipboard.writeText(text).then(function() {
            toast('Copied: ' + text, 'success');
        }, function() {
            toast('Clipboard write failed', 'danger');
        });
    };

    window.agenticShowDocs = function(demoId) {
        fetch('/dashboard/docs?path=' + encodeURIComponent('/dashboard/agentic/' + demoId + '/status'))
            .then(function(r) { return r.ok ? r.json() : null; })
            .then(function(data) {
                if (!data) {
                    toast('No documentation found for demo ' + demoId, 'warning');
                    return;
                }
                renderDocModal(data.fullSection || '');
            })
            .catch(function(e) { toast('Docs error: ' + (e && e.message || e), 'danger'); });
    };

    function renderDocModal(markdown) {
        var modal = document.getElementById('doc-modal');
        var body = document.getElementById('doc-modal-body');
        if (!modal || !body) {
            // Page has no doc-modal — fall back to a simple in-page overlay.
            ensureFallbackDocModal();
            modal = document.getElementById('doc-modal');
            body = document.getElementById('doc-modal-body');
        }
        var html;
        try {
            html = (typeof docMarked !== 'undefined' && docMarked.parse)
                ? docMarked.parse(markdown)
                : '<pre>' + escapeHtml(markdown) + '</pre>';
        } catch (e) {
            html = '<pre>' + escapeHtml(markdown) + '</pre>';
        }
        body.innerHTML = html;
        try {
            var mermaidEls = body.querySelectorAll('.mermaid');
            if (mermaidEls.length > 0 && typeof mermaid !== 'undefined') {
                mermaid.run({ nodes: mermaidEls });
            }
        } catch (e) { /* best-effort */ }
        modal.style.display = 'flex';
        document.body.style.overflow = 'hidden';
    }

    function ensureFallbackDocModal() {
        if (document.getElementById('doc-modal')) return;
        var overlay = document.createElement('div');
        overlay.id = 'doc-modal';
        overlay.style.cssText = 'display:none;position:fixed;inset:0;background:rgba(0,0,0,0.55);z-index:2000;align-items:flex-start;justify-content:center;padding:40px 20px;overflow-y:auto;';
        overlay.onclick = function(e) { if (e.target === overlay) window.closeDocModal(); };
        overlay.innerHTML = '<div style="background:var(--spring-card,#fff);color:var(--spring-text,#111);border:1px solid var(--spring-border,#ccc);border-radius:8px;max-width:900px;width:100%;padding:20px;max-height:85vh;overflow-y:auto;">'
            + '<div class="d-flex justify-content-between align-items-center mb-3 pb-2" style="border-bottom:1px solid var(--spring-border,#ccc);">'
            + '<h5 class="mb-0">Documentation</h5>'
            + '<button class="btn btn-sm btn-outline-secondary" onclick="closeDocModal()">✕</button></div>'
            + '<div id="doc-modal-body" class="markdown-body"></div></div>';
        document.body.appendChild(overlay);
    }

    window.closeDocModal = function(event) {
        if (event && event.target !== event.currentTarget) return;
        var modal = document.getElementById('doc-modal');
        if (modal) modal.style.display = 'none';
        document.body.style.overflow = '';
    };

    // ===== List agents (inspector panel or toast) =====

    window.agenticListAgents = function(btn) {
        var demoId = btn.dataset.demoId;
        fetchJson('/dashboard/agentic/' + demoId + '/agents')
            .then(function(res) {
                if (!res.ok) return handleError(res);
                var ids = Array.isArray(res.body) ? res.body : [];
                renderInspector(demoId, ids);
            })
            .catch(function(e) { toast('List agents failed: ' + (e && e.message || e), 'danger'); });
    };

    function renderInspector(demoId, ids) {
        var host = document.getElementById('agentic-inspector');
        if (!host) {
            // No inspector panel on the page — show a compact toast listing.
            var msg = ids.length
                ? 'Agents for demo ' + demoId + ': ' + ids.join(', ')
                : 'No agents created for demo ' + demoId + '.';
            toast(msg, 'info');
            return;
        }
        var html = '<div class="d-flex justify-content-between align-items-center mb-2">' +
            '<span class="text-muted small text-uppercase fw-bold">Agents — demo ' + escapeHtml(demoId) + '</span>' +
            '<span class="text-success small">' + ids.length + '</span></div>';
        if (!ids.length) {
            html += '<p class="text-muted small mb-0">No agents created yet.</p>';
        } else {
            html += '<ul class="list-unstyled small mb-0">';
            ids.forEach(function(id) {
                html += '<li><code>' + escapeHtml(id) + '</code></li>';
            });
            html += '</ul>';
        }
        host.innerHTML = html;
    }

    // ===== Open console =====

    window.agenticOpenConsole = function(btn) {
        var demoId = btn.dataset.demoId;
        var existing = openConsoles.get(demoId);
        if (existing && existing.consoleEl) {
            // Toggle visibility.
            var shown = existing.consoleEl.style.display !== 'none';
            existing.consoleEl.style.display = shown ? 'none' : '';
            if (!shown) reloadAgentList(demoId);
            return;
        }
        var meta = cards.get(demoId);
        if (!meta) return;
        var html = buildConsoleHtml(demoId, meta.supportsLogin);
        meta.card.insertAdjacentHTML('afterend', html);
        var consoleEl = document.getElementById('agentic-console-' + demoId);
        if (!consoleEl) return;
        consoleEl.style.display = '';
        var state = { consoleEl: consoleEl, agentId: null, demoId: demoId, supportsLogin: meta.supportsLogin };
        openConsoles.set(demoId, state);
        wireConsole(state);
        reloadAgentList(demoId);
        refreshCurl(state);
    };

    function buildConsoleHtml(demoId, supportsLogin) {
        var sampleEmails = [
            'kayla.castro@example.com',
            'rickey.walker@example.com',
            'dustin.henderson@example.com'
        ];
        var sampleLinks = sampleEmails.map(function(e) {
            return '<li><a href="#" class="sample-email" data-email="' + e + '" style="color:var(--spring-link,#6db33f);text-decoration:none;font-size:0.72rem;"><code style="background:transparent;padding:0;font-size:0.72rem;">' + e + '</code></a></li>';
        }).join('');
        var login = supportsLogin
            ? '<div class="col-md-3 border-start ps-3" style="border-color:var(--spring-border)!important;">'
            + '<h6 class="mb-2" style="color:var(--spring-text)"><i class="bi bi-person-badge"></i> ACME Login</h6>'
            + '<div class="small text-muted mb-2" style="font-size:0.75rem;line-height:1.35;">'
            + 'Simulates logging in as a customer from the seed dataset — the agent can then reason about "you" as a real customer with an address and order history.'
            + '</div>'
            + '<div class="small text-muted mb-1" style="font-size:0.72rem;">'
            + '<i class="bi bi-info-circle me-1"></i>Try one of these emails:'
            + '</div>'
            + '<ul class="list-unstyled mb-2" style="padding-left:0.25rem;">' + sampleLinks + '</ul>'
            + '<form class="d-flex flex-column gap-1" data-role="login-form">'
            + '<input type="email" name="email" class="form-control form-control-sm" placeholder="email" data-role="login-email" required>'
            + '<button type="submit" class="btn btn-sm btn-outline-secondary" data-role="login-button">Login</button></form>'
            + '<div class="mt-2 small text-muted" data-role="login-status"></div>'
            + '<div class="mt-3 p-2" style="background:rgba(106,27,154,0.08);border-left:3px solid #6a1b9a;border-radius:4px;font-size:0.72rem;line-height:1.35;">'
            + '<div class="fw-bold mb-1" style="color:var(--spring-text);">'
            + '<i class="bi bi-lightbulb me-1" style="color:#6a1b9a;"></i>Tip: trigger a multi-step trace'
            + '</div>'
            + '<div class="text-muted mb-1">Copy this into the message input to see multiple inner thoughts (one per step):</div>'
            + '<button type="button" class="btn btn-sm btn-outline-secondary w-100 mb-1 sample-prompt" style="font-size:0.7rem;" data-prompt="My Spring Boot app won&apos;t start. Think in 3 separate send_message calls (requestReinvocation=true between them): (1) most likely causes, (2) how to confirm each, (3) recommended first fix. Then stop.">'
            + '<i class="bi bi-clipboard me-1"></i>Use this prompt'
            + '</button>'
            + '<pre class="mb-0 p-2" style="background:var(--spring-bg-alt);color:var(--spring-text);font-size:0.68rem;border-radius:3px;white-space:pre-wrap;line-height:1.35;">My Spring Boot app won&apos;t start. Think in 3 separate send_message calls (requestReinvocation=true between them): (1) most likely causes, (2) how to confirm each, (3) recommended first fix. Then stop.</pre>'
            + '</div>'
            + '</div>'
            : '';
        var centerCols = supportsLogin ? 'col-md-6' : 'col-md-9';
        return '<div class="stage-card mt-3" id="agentic-console-' + demoId + '" data-demo-id="' + demoId + '" style="border-left-color:#6a1b9a;">'
            + '<div class="d-flex justify-content-between align-items-center mb-2">'
            + '<span class="text-muted small">Console — demo ' + demoId + '</span>'
            + '<button type="button" class="btn btn-sm btn-outline-secondary" data-action="toggle-fullscreen" title="Toggle fullscreen">'
            + '<i class="bi bi-arrows-fullscreen"></i></button>'
            + '</div>'
            + '<div class="row">'
            + '<div class="col-md-3 border-end pe-3" style="border-color:var(--spring-border)!important;">'
            + '<h6 class="mb-2" style="color:var(--spring-text)"><i class="bi bi-people"></i> Agents</h6>'
            + '<ul class="list-unstyled agentic-agent-list small mb-2" data-role="agent-list"></ul>'
            + '<form class="d-flex gap-1 mb-2" data-role="create-form">'
            + '<input type="text" name="id" class="form-control form-control-sm" placeholder="agent id" required>'
            + '<button type="submit" class="btn btn-sm btn-spring-green">Create</button></form>'
            + '<div class="d-flex flex-column gap-1">'
            + '<button type="button" class="btn btn-sm btn-outline-secondary" data-action="reset-memory"><i class="bi bi-arrow-counterclockwise"></i> Reset memory</button>'
            + '<button type="button" class="btn btn-sm btn-outline-danger" data-action="delete-agent"><i class="bi bi-trash"></i> Delete</button>'
            + '</div></div>'
            + '<div class="' + centerCols + ' px-3">'
            + '<div class="d-flex align-items-center gap-2 mb-1" data-role="active-agent-header" style="font-size:0.85rem;">'
            + '<i class="bi bi-robot" style="color:#6a1b9a"></i>'
            + '<span class="text-muted">Active agent:</span>'
            + '<code data-role="active-agent-id" style="font-weight:600;">—</code>'
            + '</div>'
            + '<div class="agentic-bubbles mb-2" data-role="bubbles" style="min-height:200px;max-height:400px;overflow-y:auto;background:var(--spring-bg-alt);padding:0.5rem;border-radius:6px;"></div>'
            + '<form class="d-flex gap-1 mb-1" data-role="message-form">'
            + '<textarea name="text" rows="2" class="form-control form-control-sm" placeholder="Message the agent…" required></textarea>'
            + '<button type="submit" class="btn btn-sm btn-spring-green">Send</button></form>'
            + '<div class="small text-muted mb-1" style="font-size:0.75rem;">'
            + '<i class="bi bi-keyboard me-1"></i>'
            + '<kbd>Enter</kbd> to send &middot; <kbd>Shift</kbd>+<kbd>Enter</kbd> for a newline'
            + '</div>'
            + '<div class="curl-line small text-muted"><code data-role="curl" style="word-break:break-all"></code>'
            + '<button type="button" class="btn btn-sm btn-outline-secondary ms-1 copy-curl"><i class="bi bi-clipboard"></i></button>'
            + '</div></div>' + login + '</div></div>';
    }

    // ===== Console wiring =====

    function wireConsole(state) {
        var el = state.consoleEl;

        var createForm = el.querySelector('[data-role="create-form"]');
        if (createForm) createForm.addEventListener('submit', function(e) {
            e.preventDefault();
            var input = createForm.querySelector('input[name="id"]');
            var id = (input && input.value || '').trim();
            if (!id) return;
            createAgent(state, id).then(function() {
                input.value = '';
                reloadAgentList(state.demoId, id);
            });
        });

        var msgForm = el.querySelector('[data-role="message-form"]');
        if (msgForm) {
            var msgTextarea = msgForm.querySelector('textarea[name="text"]');
            if (msgTextarea) {
                msgTextarea.addEventListener('keydown', function(e) {
                    // Enter submits; Shift+Enter inserts a newline (default behavior).
                    if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
                        e.preventDefault();
                        if (typeof msgForm.requestSubmit === 'function') {
                            msgForm.requestSubmit();
                        } else {
                            msgForm.dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
                        }
                    }
                });
            }
            msgForm.addEventListener('submit', function(e) {
                e.preventDefault();
                var ta = msgForm.querySelector('textarea[name="text"]');
                var text = (ta && ta.value || '').trim();
                if (!text) return;
                if (!state.agentId) {
                    toast('Select or create an agent first.', 'warning');
                    return;
                }
                sendMessage(state, text);
                ta.value = '';
                refreshCurl(state);
            });
            var ta = msgForm.querySelector('textarea[name="text"]');
            if (ta) ta.addEventListener('input', function() { refreshCurl(state); });
        }

        var copyBtn = el.querySelector('.copy-curl');
        if (copyBtn) copyBtn.addEventListener('click', function() {
            var code = el.querySelector('[data-role="curl"]');
            if (!code) return;
            navigator.clipboard.writeText(code.textContent).then(function() {
                var orig = copyBtn.innerHTML;
                copyBtn.innerHTML = '<i class="bi bi-check2"></i>';
                setTimeout(function() { copyBtn.innerHTML = orig; }, 1500);
            });
        });

        var requireAgent = function(fn) {
            return function() {
                if (!state.agentId) { toast('Select an agent first.', 'warning'); return; }
                fn();
            };
        };
        var resetBtn = el.querySelector('[data-action="reset-memory"]');
        if (resetBtn) resetBtn.addEventListener('click', requireAgent(function() { resetAgent(state); }));
        var delBtn = el.querySelector('[data-action="delete-agent"]');
        if (delBtn) delBtn.addEventListener('click', requireAgent(function() { deleteAgent(state); }));

        var fsBtn = el.querySelector('[data-action="toggle-fullscreen"]');
        if (fsBtn) fsBtn.addEventListener('click', function() {
            // Remember scroll position before entering fullscreen so we can
            // restore it on exit (position:fixed removes the element from
            // flow, so the page below collapses and scrollTop resets).
            var isFs = el.classList.toggle('agentic-fullscreen');
            fsBtn.innerHTML = isFs
                ? '<i class="bi bi-fullscreen-exit"></i>'
                : '<i class="bi bi-arrows-fullscreen"></i>';
            fsBtn.title = isFs ? 'Exit fullscreen' : 'Toggle fullscreen';
            document.body.classList.toggle('agentic-fullscreen-lock', isFs);
            if (isFs) {
                state._preFsScrollY = window.scrollY;
            } else {
                // On exit: after the browser reflows, scroll the console
                // card back into view so the topbar + demo cards are visible
                // again instead of leaving the viewport at a stale offset.
                requestAnimationFrame(function() {
                    var targetY = typeof state._preFsScrollY === 'number' ? state._preFsScrollY : 0;
                    window.scrollTo({ top: targetY, behavior: 'auto' });
                });
            }
        });

        if (state.supportsLogin) {
            var loginForm = el.querySelector('[data-role="login-form"]');
            if (loginForm) loginForm.addEventListener('submit', function(e) {
                e.preventDefault();
                // Toggle login/logout based on current state.customer.
                if (state.customer) {
                    acmeLogout(state);
                } else {
                    var input = loginForm.querySelector('input[name="email"]');
                    var email = (input && input.value || '').trim();
                    if (email) acmeLogin(state, email);
                }
            });
            // Click a sample email → autofill the input.
            el.querySelectorAll('.sample-email').forEach(function(a) {
                a.addEventListener('click', function(evt) {
                    evt.preventDefault();
                    var input = el.querySelector('[data-role="login-form"] input[name="email"]');
                    if (input) { input.value = a.dataset.email; input.focus(); }
                });
            });
            // Click the sample-prompt button → paste into the message textarea.
            el.querySelectorAll('.sample-prompt').forEach(function(btn) {
                btn.addEventListener('click', function() {
                    var ta = el.querySelector('[data-role="message-form"] textarea[name="text"]');
                    if (ta) {
                        ta.value = btn.dataset.prompt;
                        ta.focus();
                        refreshCurl(state);
                    }
                    toast('Prompt inserted — create/select an agent and hit Send.', 'info');
                });
            });
        }
    }

    function createAgent(state, id) {
        var payload = { id: id };
        // If the user is logged in to ACME, pass the customer context so the agent's system
        // prompt knows about them (name / email / address / customer id / etc).
        if (state.customer) payload.customer = state.customer;
        var bodyStr = JSON.stringify(payload);
        console.log('[agentic] createAgent payload-keys:', Object.keys(payload));
        console.log('[agentic] createAgent state.customer type:', typeof state.customer,
                    'truthy:', !!state.customer,
                    'isNull:', state.customer === null,
                    'isUndefined:', state.customer === undefined,
                    'keys:', state.customer ? Object.keys(state.customer) : 'n/a');
        console.log('[agentic] createAgent stringified body (' + bodyStr.length + ' chars):', bodyStr);
        return fetchJson('/dashboard/agentic/' + state.demoId + '/agents', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: bodyStr
        }).then(function(res) {
            if (!res.ok) { handleError(res); return null; }
            toast('Agent ' + id + ' created.', 'success');
            return res.body;
        });
    }

    function resetAgent(state) {
        return fetchJson('/dashboard/agentic/' + state.demoId + '/agents/' + state.agentId + '/reset', {
            method: 'POST'
        }).then(function(res) {
            if (!res.ok) { handleError(res); return; }
            clearBubbles(state);
            toast('Memory cleared for ' + state.agentId, 'success');
        });
    }

    function deleteAgent(state) {
        return fetchJson('/dashboard/agentic/' + state.demoId + '/agents/' + state.agentId, {
            method: 'DELETE'
        }).then(function(res) {
            if (!res.ok) { handleError(res); return; }
            toast('Agent ' + state.agentId + ' deleted.', 'success');
            clearBubbles(state);
            var deletedId = state.agentId;
            state.agentId = null;
            reloadAgentList(state.demoId, null, deletedId);
        });
    }

    function sendMessage(state, text) {
        renderBubble(state, { role: 'user', message: text });
        var thinkingEl = renderThinkingBubble(state);
        return fetchJson('/dashboard/agentic/' + state.demoId + '/agents/' + state.agentId + '/messages', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ text: text })
        }).catch(function(e) {
            if (thinkingEl && thinkingEl.parentNode) thinkingEl.parentNode.removeChild(thinkingEl);
            toast('Send failed: ' + (e && e.message || e), 'danger');
            throw e;
        }).then(function(res) {
            if (thinkingEl && thinkingEl.parentNode) thinkingEl.parentNode.removeChild(thinkingEl);
            if (!res.ok) { handleError(res); return; }
            var body = res.body || {};
            if (Array.isArray(body.steps)) {
                // Model-directed loop: aggregate all steps into a single bubble. The visible
                // message is the full trace numbered 1..n; inner thoughts are likewise numbered
                // so the reader can follow the reasoning across steps without each turn being
                // its own bubble.
                var steps = body.steps;
                var n = steps.length;
                var messageLines = steps.map(function(step, idx) {
                    var prefix = n > 1 ? (idx + 1) + '. ' : '';
                    return prefix + (step.message == null ? '' : String(step.message));
                });
                var thoughtsArray = steps.map(function(step) {
                    return step.innerThoughts == null ? '' : String(step.innerThoughts);
                });
                var anyFallback = steps.some(function(s) { return !!s.isFallback; });
                var maxStepsHit = steps.some(function(s) { return !!s.maxStepsHit; });
                var lastReinvoke = n > 0 && !!steps[n - 1].requestReinvocation;
                renderBubble(state, {
                    role: 'agent',
                    message: messageLines.join('\n\n'),
                    thoughts: thoughtsArray,
                    isFallback: anyFallback,
                    maxStepsHit: maxStepsHit,
                    stepLabel: n > 1 ? (n + ' steps' + (lastReinvoke ? ' → stopped at MAX' : '')) : null
                });
            } else {
                renderBubble(state, {
                    role: 'agent',
                    message: body.message,
                    innerThoughts: body.innerThoughts,
                    isFallback: !!body.isFallback
                });
            }
        });
    }

    // Apply the logged-in customer (or clear it) on every existing agent by calling the new
    // POST /agents/{id}/context endpoint. Runs in the background — we don't await or surface
    // individual errors; the agents list just gets its system prompts updated.
    function pushContextToExistingAgents(state) {
        fetchJson('/dashboard/agentic/' + state.demoId + '/agents').then(function(listRes) {
            if (!listRes.ok) return;
            var ids = Array.isArray(listRes.body) ? listRes.body : [];
            if (!ids.length) return;
            var payload = state.customer ? { customer: state.customer } : {};
            ids.forEach(function(id) {
                fetchJson('/dashboard/agentic/' + state.demoId + '/agents/' + id + '/context', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload),
                }).then(function(r) {
                    console.log('[agentic] pushContext to', id, 'ok=', r.ok);
                });
            });
            if (ids.length > 0) {
                toast('Login context applied to ' + ids.length + ' existing agent'
                    + (ids.length === 1 ? '' : 's') + '.', 'success');
            }
        });
    }

    // Toggle the login form between "Login" (no customer yet) and "Logout" (customer set).
    function refreshLoginForm(state) {
        if (!state.consoleEl) return;
        var btn = state.consoleEl.querySelector('[data-role="login-button"]');
        var input = state.consoleEl.querySelector('[data-role="login-email"]');
        if (!btn || !input) return;
        if (state.customer) {
            btn.textContent = 'Logout';
            btn.classList.remove('btn-outline-secondary');
            btn.classList.add('btn-outline-danger');
            input.setAttribute('disabled', 'disabled');
            input.required = false;
        } else {
            btn.textContent = 'Login';
            btn.classList.remove('btn-outline-danger');
            btn.classList.add('btn-outline-secondary');
            input.removeAttribute('disabled');
            input.required = true;
            input.value = '';
        }
    }

    function acmeLogout(state) {
        // Clear local state and push an empty context to every existing agent so their system
        // prompts drop the User Context block and new agents created after this also have no
        // context.
        state.customer = null;
        var statusEl = state.consoleEl.querySelector('[data-role="login-status"]');
        if (statusEl) statusEl.textContent = 'Logged out.';
        refreshLoginForm(state);
        pushContextToExistingAgents(state); // payload becomes {} — clears server-side context
        toast('Logged out — context cleared from agents.', 'info');
    }

    function acmeLogin(state, email) {
        var statusEl = state.consoleEl.querySelector('[data-role="login-status"]');
        if (statusEl) statusEl.textContent = 'Logging in…';
        return fetchJson('/dashboard/agentic/02/acme/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: email })
        }).then(function(res) {
            if (!res.ok) {
                if (statusEl) statusEl.textContent = 'Login failed.';
                handleError(res);
                return;
            }
            // Store the full Customer record so new agents created after login carry the
            // context into their system prompt. The JS never inspects individual fields —
            // the agent's controller on :8092 knows how to flatten it.
            state.customer = (res.body && typeof res.body === 'object') ? res.body : null;
            console.log('[agentic] acmeLogin stored customer in state', {
                demoId: state.demoId,
                hasCustomer: !!state.customer,
                customer: state.customer,
            });
            refreshLoginForm(state);
            // Push the new customer context into every existing agent so agents created BEFORE
            // login also gain the user context in their system prompt. Memory is preserved.
            pushContextToExistingAgents(state);
            var name = typeof res.body === 'string' ? res.body : (res.body && res.body.name) || '(unknown)';
            if (statusEl) {
                statusEl.innerHTML =
                    'Logged in as <strong>' + escapeHtml(name) + '</strong>'
                    + '<div class="text-muted" style="font-size:0.7rem;margin-top:0.25rem;">'
                    + 'Create a new agent to include this context in its system prompt.'
                    + '</div>';
            }
            toast('ACME login: ' + name + ' — create a new agent to use this context', 'success');
        });
    }

    // ===== Agent list =====

    function reloadAgentList(demoId, preferId, removedId) {
        var state = openConsoles.get(demoId);
        if (!state) return;
        var listEl = state.consoleEl.querySelector('[data-role="agent-list"]');
        if (!listEl) return;
        fetchJson('/dashboard/agentic/' + demoId + '/agents').then(function(res) {
            if (!res.ok) { handleError(res); return; }
            var ids = Array.isArray(res.body) ? res.body : [];
            listEl.innerHTML = '';
            if (!ids.length) {
                listEl.innerHTML = '<li class="text-muted small">No agents yet.</li>';
                state.agentId = null;
                refreshCurl(state);
                return;
            }
            ids.forEach(function(id) {
                var li = document.createElement('li');
                li.className = 'agentic-agent-item d-flex align-items-center gap-2';
                li.style.cssText = 'cursor:pointer;padding:2px 6px;border-radius:4px;';
                li.innerHTML = '<i class="bi bi-robot"></i> <code>' + escapeHtml(id) + '</code>';
                li.addEventListener('click', function() { selectAgent(state, id); });
                listEl.appendChild(li);
            });
            // Selection logic: prefer explicit preferId, else keep current if still present,
            // else pick first. Skip removedId.
            var keepCurrent = state.agentId && ids.indexOf(state.agentId) >= 0 && state.agentId !== removedId;
            var nextId = preferId && ids.indexOf(preferId) >= 0 ? preferId :
                (keepCurrent ? state.agentId : ids[0]);
            if (nextId) selectAgent(state, nextId);
        });
    }

    function selectAgent(state, id) {
        state.agentId = id;
        var listEl = state.consoleEl.querySelector('[data-role="agent-list"]');
        if (listEl) {
            listEl.querySelectorAll('li').forEach(function(li) {
                var isActive = li.textContent.indexOf(id) >= 0;
                li.style.background = isActive ? 'var(--spring-bg-alt, rgba(0,0,0,0.05))' : '';
                li.style.fontWeight = isActive ? '600' : '';
            });
        }
        var activeEl = state.consoleEl.querySelector('[data-role="active-agent-id"]');
        if (activeEl) activeEl.textContent = id || '—';
        refreshCurl(state);
        loadLog(state);
    }

    function loadLog(state) {
        if (!state.agentId) return;
        fetchJson('/dashboard/agentic/' + state.demoId + '/agents/' + state.agentId + '/log')
            .then(function(res) {
                if (!res.ok) return; // silent — log is optional
                clearBubbles(state);
                var entries = Array.isArray(res.body) ? res.body :
                    (res.body && Array.isArray(res.body.entries) ? res.body.entries : []);
                entries.forEach(function(entry) {
                    if (!entry) return;
                    var role = entry.role || (entry.user ? 'user' : 'agent');
                    renderBubble(state, {
                        role: role === 'user' || role === 'USER' ? 'user' : 'agent',
                        message: entry.text || entry.message || '',
                        thoughts: Array.isArray(entry.thoughts) ? entry.thoughts : null,
                        innerThoughts: entry.innerThoughts,
                        isFallback: !!entry.isFallback
                    });
                });
            })
            .catch(function() { /* swallow — log is best-effort */ });
    }

    // ===== Bubble rendering =====

    function renderBubble(state, opts) {
        var bubblesEl = state.consoleEl.querySelector('[data-role="bubbles"]');
        if (!bubblesEl) return;
        var role = opts.role || 'agent';
        var isUser = role === 'user';
        var wrap = document.createElement('div');
        wrap.className = 'agentic-bubble mb-2 p-2 ' +
            (isUser ? 'ms-auto bg-primary text-white' : 'me-auto');
        wrap.style.cssText = isUser
            ? 'max-width:75%;border-radius:12px 12px 2px 12px;'
            : 'max-width:85%;background:var(--spring-bg);border:1px solid var(--spring-border);border-radius:12px 12px 12px 2px;';

        // Speaker label — "you:" for user bubbles, "<agent-id>:" for agent bubbles.
        var speaker = document.createElement('div');
        speaker.className = 'bubble-speaker';
        speaker.style.cssText = 'font-size:0.7rem;font-weight:700;letter-spacing:0.03em;text-transform:uppercase;opacity:0.75;margin-bottom:0.2rem;';
        speaker.textContent = isUser ? 'you' : ((state.agentId || 'agent') + '');
        wrap.appendChild(speaker);

        var msg = document.createElement('div');
        msg.className = 'bubble-message small';
        var raw = opts.message == null ? '' : String(opts.message);
        if (!isUser && typeof docMarked !== 'undefined' && typeof docMarked.parse === 'function') {
            // Agent bubbles render markdown (bold, lists, headings, code). User bubbles
            // stay as literal text (users aren't expected to type markdown).
            msg.classList.add('markdown-body');
            try {
                msg.innerHTML = docMarked.parse(raw);
            } catch (e) {
                msg.style.whiteSpace = 'pre-wrap';
                msg.textContent = raw;
            }
        } else {
            msg.style.whiteSpace = 'pre-wrap';
            msg.textContent = raw;
        }
        wrap.appendChild(msg);

        // Normalise inner thoughts to an array. `opts.thoughts` (array) wins over the legacy
        // `opts.innerThoughts` (joined string) so multi-step demo-02 replies get one expander
        // per step, while single-thought demo-01 / single-step demo-02 replies still get one.
        var thoughtsArr;
        if (Array.isArray(opts.thoughts) && opts.thoughts.length > 0) {
            thoughtsArr = opts.thoughts;
        } else if (opts.innerThoughts != null && String(opts.innerThoughts).length > 0) {
            thoughtsArr = [String(opts.innerThoughts)];
        } else {
            thoughtsArr = [];
        }
        var fallbackMarker = thoughtsArr.some(function(t) {
            return String(t || '').indexOf('[fallback: ') === 0;
        });
        var isFallback = !!opts.isFallback || fallbackMarker;

        if (!isUser && thoughtsArr.length) {
            var multi = thoughtsArr.length > 1;
            thoughtsArr.forEach(function(t, idx) {
                var det = document.createElement('details');
                det.className = 'bubble-thoughts mt-1';
                var label = multi ? 'inner thought ' + (idx + 1) : 'inner thoughts';
                det.innerHTML =
                    '<summary class="text-muted small" style="cursor:pointer"></summary>'
                    + '<div class="thoughts-body small text-muted mt-1" style="white-space:pre-wrap"></div>';
                det.querySelector('summary').textContent = label;
                det.querySelector('.thoughts-body').textContent = String(t || '');
                wrap.appendChild(det);
            });
        }

        if (!isUser) {
            var badges = document.createElement('div');
            badges.className = 'bubble-badges d-flex gap-1 mt-1';
            var addBadge = function(cls, text, title) {
                var b = document.createElement('span');
                b.className = 'badge ' + cls;
                if (title) b.title = title;
                b.textContent = text;
                badges.appendChild(b);
            };
            if (opts.stepLabel) addBadge('bg-info', opts.stepLabel);
            if (isFallback) addBadge('bg-warning text-dark', '⚠ fallback',
                'Model replied without calling the tool — see SPRING_AI_STAGE_7.md#ollama-fallback-behavior');
            if (opts.maxStepsHit) addBadge('bg-danger', '⚠ max steps');
            if (badges.children.length) wrap.appendChild(badges);
        }

        bubblesEl.appendChild(wrap);
        bubblesEl.scrollTop = bubblesEl.scrollHeight;
    }

    // Renders an animated placeholder "thinking" bubble, returns the element so the caller
    // can remove it once the real response arrives.
    function renderThinkingBubble(state) {
        var bubblesEl = state.consoleEl.querySelector('[data-role="bubbles"]');
        if (!bubblesEl) return null;
        var wrap = document.createElement('div');
        wrap.className = 'agentic-bubble agentic-thinking mb-2 p-2 me-auto';
        wrap.style.cssText =
            'max-width:85%;background:var(--spring-bg);border:1px solid var(--spring-border);border-radius:12px 12px 12px 2px;';
        var speaker = document.createElement('div');
        speaker.className = 'bubble-speaker';
        speaker.style.cssText =
            'font-size:0.7rem;font-weight:700;letter-spacing:0.03em;text-transform:uppercase;opacity:0.75;margin-bottom:0.2rem;';
        speaker.textContent = (state.agentId || 'agent') + '';
        wrap.appendChild(speaker);
        var inner = document.createElement('div');
        inner.className = 'bubble-message small d-flex align-items-center gap-2';
        inner.innerHTML =
            '<span class="thinking-dots" aria-hidden="true"><span></span><span></span><span></span></span>' +
            '<span class="text-muted" style="font-style:italic;">thinking…</span>';
        wrap.appendChild(inner);
        bubblesEl.appendChild(wrap);
        bubblesEl.scrollTop = bubblesEl.scrollHeight;
        return wrap;
    }

    function clearBubbles(state) {
        var el = state.consoleEl.querySelector('[data-role="bubbles"]');
        if (el) el.innerHTML = '';
    }

    // ===== Live curl line =====

    function refreshCurl(state) {
        var codeEl = state.consoleEl.querySelector('[data-role="curl"]');
        if (!codeEl) return;
        var ta = state.consoleEl.querySelector('textarea[name="text"]');
        var text = ta ? ta.value : '';
        var agentId = state.agentId || '<agent-id>';
        var bodyJson = JSON.stringify({ text: text });
        codeEl.textContent =
            "curl -s -X POST " + CURL_BASE + "/dashboard/agentic/" + state.demoId +
            "/agents/" + agentId + "/messages " +
            "-H 'Content-Type: application/json' " +
            "-d '" + bodyJson.replace(/'/g, "'\\''") + "'";
    }

    // ===== Fetch wrapper — returns {ok, body, status} =====

    function fetchJson(url, init) {
        return fetch(url, init).then(function(r) {
            return r.text().then(function(txt) {
                var body;
                if (txt) {
                    try { body = JSON.parse(txt); }
                    catch (e) { body = txt; }
                } else {
                    body = null;
                }
                return { ok: r.ok, status: r.status, body: body };
            });
        });
    }

    function handleError(res) {
        var body = res && res.body || {};
        var hint = body && body.hint ? body.hint : '';
        var err = body && body.error ? body.error :
            (body && body.detail ? body.detail :
                (typeof body === 'string' ? body : 'request failed'));
        var kind = (res.status >= 500 && res.status <= 503) ? 'warning' : 'danger';
        var msg = '[' + res.status + '] ' + err + (hint ? ' — ' + hint : '');
        toast(msg, kind);
    }

    // ===== Toast =====

    var toastHost = null;
    function ensureToastHost() {
        if (toastHost) return toastHost;
        toastHost = document.createElement('div');
        toastHost.className = 'toast-container position-fixed bottom-0 end-0 p-3';
        toastHost.style.zIndex = '2050';
        document.body.appendChild(toastHost);
        return toastHost;
    }

    function toast(message, kind) {
        var host = ensureToastHost();
        var bg = 'bg-secondary';
        if (kind === 'success') bg = 'bg-success';
        else if (kind === 'warning') bg = 'bg-warning text-dark';
        else if (kind === 'danger') bg = 'bg-danger';
        else if (kind === 'info') bg = 'bg-info text-dark';
        var wrap = document.createElement('div');
        wrap.className = 'toast align-items-center text-white ' + bg + ' border-0 show';
        wrap.setAttribute('role', 'alert');
        wrap.innerHTML =
            '<div class="d-flex">' +
            '<div class="toast-body">' + escapeHtml(message) + '</div>' +
            '<button type="button" class="btn-close btn-close-white me-2 m-auto" aria-label="Close"></button>' +
            '</div>';
        var closeBtn = wrap.querySelector('.btn-close');
        closeBtn.addEventListener('click', function() { wrap.remove(); });
        host.appendChild(wrap);
        setTimeout(function() { if (wrap.parentNode) wrap.remove(); }, 4000);
    }

    // ===== utils =====

    function escapeHtml(s) {
        return String(s == null ? '' : s)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }
})();
