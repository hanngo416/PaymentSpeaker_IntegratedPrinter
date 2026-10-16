<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Alert center"/>
<c:set var="nav" value="alerts"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<div class="card">
    <h1>Alert center</h1>
    <c:if test="${not empty message}"><div class="msg"><c:out value="${message}"/></div></c:if>
    <div class="summary">
        <span>New: <span class="pill NEW">${empty counts['NEW'] ? 0 : counts['NEW']}</span></span>
        <span>In progress: <span class="pill IN_PROGRESS">${empty counts['IN_PROGRESS'] ? 0 : counts['IN_PROGRESS']}</span></span>
        <span>Resolved: <span class="pill RESOLVED">${empty counts['RESOLVED'] ? 0 : counts['RESOLVED']}</span></span>
        <span>Dismissed: <span class="pill DISMISSED">${empty counts['DISMISSED'] ? 0 : counts['DISMISSED']}</span></span>
    </div>
    <form method="get" action="${ctx}/alerts" class="filters">
        <label>Status
            <select name="status">
                <option value="">All</option>
                <c:forEach var="s" items="${statuses}">
                    <option value="${s}" ${f.status == s ? 'selected' : ''}>${s}</option>
                </c:forEach>
            </select>
        </label>
        <label>Severity
            <select name="severity">
                <option value="">All</option>
                <c:forEach var="s" items="${severities}">
                    <option value="${s}" ${f.severity == s ? 'selected' : ''}>${s}</option>
                </c:forEach>
            </select>
        </label>
        <button type="submit" class="primary">Filter</button>
        <a class="btn" href="${ctx}/alerts">Reset</a>
    </form>
</div>

<div class="card">
    <table>
        <thead>
        <tr>
            <th>Time</th>
            <th>Rule</th>
            <th>Severity</th>
            <th style="width: 140px;">Amount (VND)</th>
            <th>Reason</th>
            <th style="width: 90px;">Status</th>
            <th style="min-width: 180px; max-width: 280px;">${canWrite ? 'Actions' : 'Handled by'}</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="a" items="${result.items}">
            <tr>
                <td><fmt:formatDate value="${a.createdAt}" pattern="yyyy-MM-dd HH:mm:ss"/></td>
                <td><c:out value="${a.ruleCode}"/></td>
                <td><span class="pill ${a.severity}"><c:out value="${a.severity}"/></span></td>
                <td class="amount-cell"><fmt:formatNumber value="${a.amount}" pattern="#,##0"/></td>
                <td class="wrap">
                    <c:out value="${a.reason}"/>
                    <c:if test="${not empty a.content}">
                        <br><small class="hint">Content: <c:out value="${a.content}"/></small>
                    </c:if>
                </td>
                <td><span class="pill ${a.status}"><c:out value="${a.status}"/></span></td>
                <td style="min-width: 180px; max-width: 280px;">
                    <c:choose>
                        <c:when test="${(a.status == 'NEW' || a.status == 'IN_PROGRESS') && canWrite}">
                            <div class="actions" style="display: flex; gap: 6px; white-space: nowrap;">
                                <c:if test="${a.status == 'NEW'}">
                                    <form method="post" action="${ctx}/alerts" class="inline">
                                        <input type="hidden" name="id" value="${a.alertId}">
                                        <input type="hidden" name="decision" value="IN_PROGRESS">
                                        <button type="submit" class="btn-action btn-review" title="Start reviewing this alert">
                                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polygon points="10 8 16 12 10 16 10 8"/></svg>
                                            <span>Review</span>
                                        </button>
                                    </form>
                                </c:if>
                                <button type="button" class="btn-action btn-resolve"
                                        onclick="openAlertModal(${a.alertId}, 'RESOLVED', '<c:out value="${a.ruleCode}"/>', '<fmt:formatNumber value="${a.amount}" pattern="#,##0"/>')"
                                        title="Resolve this alert">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                                    <span>Resolve</span>
                                </button>
                                <button type="button" class="btn-action btn-dismiss"
                                        onclick="openAlertModal(${a.alertId}, 'DISMISSED', '<c:out value="${a.ruleCode}"/>', '<fmt:formatNumber value="${a.amount}" pattern="#,##0"/>')"
                                        title="Dismiss this alert">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                                    <span>Dismiss</span>
                                </button>
                            </div>
                        </c:when>
                        <c:when test="${not empty a.resolvedBy}">
                            <div class="handled-cell">
                                <div class="handled-user">
                                    <span class="handled-avatar" aria-hidden="true">
                                        <svg viewBox="0 0 24 24" width="11" height="11" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                                            <circle cx="12" cy="7" r="4"></circle>
                                        </svg>
                                    </span>
                                    <span class="auditor-tag"><c:out value="${a.resolvedBy}"/></span>
                                </div>
                                <c:if test="${not empty a.resolvedNotes}">
                                    <div class="auditor-bubble" title="<c:out value='${a.resolvedNotes}'/> (Click to toggle full text)" onclick="this.classList.toggle('expanded')">
                                        <c:out value="${a.resolvedNotes}"/>
                                    </div>
                                </c:if>
                            </div>
                        </c:when>
                        <c:otherwise>—</c:otherwise>
                    </c:choose>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty result.items}">
            <tr><td colspan="7">No alerts match the filter.</td></tr>
        </c:if>
        </tbody>
    </table>
    <p>${result.total} alert(s), page ${result.page} of ${result.totalPages}</p>
    <div class="pages">
        <c:forEach begin="1" end="${result.totalPages}" var="p">
            <c:choose>
                <c:when test="${p == result.page}"><span>${p}</span></c:when>
                <c:otherwise>
                    <a href="${ctx}/alerts?status=${f.status}&severity=${f.severity}&page=${p}">${p}</a>
                </c:otherwise>
            </c:choose>
        </c:forEach>
    </div>
</div>

<div class="modal-backdrop" id="alertModal" hidden>
    <div class="modal" role="dialog" aria-modal="true" aria-labelledby="modalTitle">
        <button type="button" class="modal-close" id="alertModalClose" aria-label="Close">&times;</button>
        <p class="modal-kicker" id="modalKicker">Action</p>
        <h2 id="modalTitle" style="margin-top:0;margin-bottom:6px;font-size:var(--fs-xl);">Resolve Alert</h2>
        <p id="modalSub" class="hint" style="margin-bottom:16px;"></p>
        <form method="post" action="${ctx}/alerts" id="modalForm">
            <input type="hidden" name="id" id="modalAlertId">
            <input type="hidden" name="decision" id="modalDecision">
            <label id="modalNotesLabel">
                <span id="modalNotesPrompt">Handling notes:</span>
                <input type="text" name="notes" id="modalNotes" maxlength="500" placeholder="Enter a note...">
            </label>
            <div style="display:flex;justify-content:flex-end;gap:10px;margin-top:20px;">
                <button type="button" class="btn" id="modalCancelBtn">Cancel</button>
                <button type="submit" class="btn primary" id="modalSubmitBtn">Confirm</button>
            </div>
        </form>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
<script>
(function() {
    var modal = document.getElementById('alertModal');
    if (!modal) return;
    var modalId = document.getElementById('modalAlertId');
    var modalDec = document.getElementById('modalDecision');
    var modalTitle = document.getElementById('modalTitle');
    var modalKicker = document.getElementById('modalKicker');
    var modalSub = document.getElementById('modalSub');
    var modalNotes = document.getElementById('modalNotes');
    var modalPrompt = document.getElementById('modalNotesPrompt');
    var submitBtn = document.getElementById('modalSubmitBtn');
    var closeBtn = document.getElementById('alertModalClose');
    var cancelBtn = document.getElementById('modalCancelBtn');

    window.openAlertModal = function(id, decision, rule, amount) {
        modalId.value = id;
        modalDec.value = decision;
        modalNotes.value = '';
        modalSub.textContent = 'Alert #' + id + ' · Rule: ' + rule + ' · ' + amount + ' VND';

        if (decision === 'RESOLVED') {
            modalKicker.textContent = 'Resolve Alert';
            modalTitle.textContent = 'Resolve alert #' + id;
            modalPrompt.textContent = 'Resolution note (optional):';
            modalNotes.placeholder = 'Optional note explaining the resolution...';
            modalNotes.required = false;
            submitBtn.textContent = 'Confirm Resolve';
            submitBtn.className = 'btn primary';
        } else {
            modalKicker.textContent = 'Dismiss Alert';
            modalTitle.textContent = 'Dismiss alert #' + id;
            modalPrompt.textContent = 'Reason for dismissal (required):';
            modalNotes.placeholder = 'Explain why this alert is dismissed (false alarm, duplicate, etc.)...';
            modalNotes.required = true;
            submitBtn.textContent = 'Confirm Dismiss';
            submitBtn.className = 'btn danger';
        }

        modal.hidden = false;
        document.body.style.overflow = 'hidden';
        setTimeout(function() { modalNotes.focus(); }, 50);
    };

    function closeModal() {
        modal.hidden = true;
        document.body.style.overflow = '';
    }

    if (closeBtn) closeBtn.addEventListener('click', closeModal);
    if (cancelBtn) cancelBtn.addEventListener('click', closeModal);
    modal.addEventListener('click', function(e) {
        if (e.target === modal) closeModal();
    });
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape' && !modal.hidden) closeModal();
    });
})();
</script>
</body>
</html>
