<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Alert center"/>
<c:set var="nav" value="alerts"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<div class="card">
    <div class="head-row">
        <h1>Alert center</h1>
    </div>
    <c:if test="${not empty message}"><div class="msg"><c:out value="${message}"/></div></c:if>
    <div class="alert-summary-grid">
        <div class="alert-stat-card stat-new">
            <div class="stat-icon-wrap">
                <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clip-rule="evenodd"/></svg>
            </div>
            <div>
                <span class="stat-label">New Alerts</span>
                <strong class="stat-count">${empty counts['NEW'] ? 0 : counts['NEW']}</strong>
            </div>
        </div>
        <div class="alert-stat-card stat-progress">
            <div class="stat-icon-wrap">
                <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 2.829a1 1 0 101.415-1.415L11 9.586V6z" clip-rule="evenodd"/></svg>
            </div>
            <div>
                <span class="stat-label">In Progress</span>
                <strong class="stat-count">${empty counts['IN_PROGRESS'] ? 0 : counts['IN_PROGRESS']}</strong>
            </div>
        </div>
        <div class="alert-stat-card stat-resolved">
            <div class="stat-icon-wrap">
                <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/></svg>
            </div>
            <div>
                <span class="stat-label">Resolved</span>
                <strong class="stat-count">${empty counts['RESOLVED'] ? 0 : counts['RESOLVED']}</strong>
            </div>
        </div>
        <div class="alert-stat-card stat-dismissed">
            <div class="stat-icon-wrap">
                <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"/></svg>
            </div>
            <div>
                <span class="stat-label">Dismissed</span>
                <strong class="stat-count">${empty counts['DISMISSED'] ? 0 : counts['DISMISSED']}</strong>
            </div>
        </div>
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
    <table class="table-alerts">
        <thead>
        <tr>
            <th>Time</th>
            <th>Rule</th>
            <th>Severity</th>
            <th>Amount (VND)</th>
            <th>Reason & Details</th>
            <th>Status</th>
            <th class="th-decision">${canWrite ? 'Decision' : 'Handled by'}</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="a" items="${result.items}">
            <tr>
                <td class="cell-time"><fmt:formatDate value="${a.createdAt}" pattern="yyyy-MM-dd HH:mm:ss"/></td>
                <td><span class="rule-code-badge"><c:out value="${a.ruleCode}"/></span></td>
                <td><span class="pill ${a.severity}"><c:out value="${a.severity}"/></span></td>
                <td class="num"><fmt:formatNumber value="${a.amount}" pattern="#,##0"/></td>
                <td class="cell-reason">
                    <div class="reason-text"><c:out value="${a.reason}"/></div>
                    <c:if test="${not empty a.content}">
                        <div class="reason-snippet" title="<c:out value="${a.content}"/>">
                            <span class="snippet-label">Tx:</span>
                            <code><c:out value="${a.content}"/></code>
                        </div>
                    </c:if>
                </td>
                <td><span class="pill ${a.status}"><c:out value="${a.status}"/></span></td>
                <td class="cell-decision">
                    <c:choose>
                        <c:when test="${(a.status == 'NEW' || a.status == 'IN_PROGRESS') && canWrite}">
                            <form method="post" action="${ctx}/alerts" class="alert-decision-form">
                                <input type="hidden" name="id" value="${a.alertId}">
                                <div class="alert-input-container">
                                    <svg class="alert-input-icon" viewBox="0 0 20 20" fill="currentColor">
                                        <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />
                                    </svg>
                                    <input type="text" name="notes" maxlength="500" class="alert-input-field" placeholder="Ghi chú xử lý (bắt buộc khi Dismiss)...">
                                </div>
                                <div class="alert-action-btns">
                                    <c:if test="${a.status == 'NEW'}">
                                        <button type="submit" name="decision" value="IN_PROGRESS" class="btn-alert-action btn-alert-review" formnovalidate title="Bắt đầu xem xét alert này">
                                            <svg viewBox="0 0 20 20" fill="currentColor"><path d="M10 12a2 2 0 100-4 2 2 0 000 4z"/><path fill-rule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clip-rule="evenodd"/></svg>
                                            Start review
                                        </button>
                                    </c:if>
                                    <button type="submit" name="decision" value="RESOLVED" class="btn-alert-action btn-alert-resolve" title="Xác nhận giải quyết thành công">
                                        <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/></svg>
                                        Resolve
                                    </button>
                                    <button type="submit" name="decision" value="DISMISSED" class="btn-alert-action btn-alert-dismiss" onclick="if(!this.form.notes.value.trim()){alert('Vui lòng nhập ghi chú lý do trước khi Dismiss!');this.form.notes.focus();return false;}" title="Bỏ qua cảnh báo (bắt buộc có ghi chú)">
                                        <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/></svg>
                                        Dismiss
                                    </button>
                                </div>
                            </form>
                        </c:when>
                        <c:when test="${not empty a.resolvedBy}">
                            <div class="alert-handled-card ${a.status == 'RESOLVED' ? 'is-resolved' : 'is-dismissed'}">
                                <div class="alert-handled-top">
                                    <span class="auditor-badge">
                                        <svg class="auditor-icon" viewBox="0 0 20 20" fill="currentColor">
                                            <path fill-rule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clip-rule="evenodd"/>
                                        </svg>
                                        <c:out value="${a.resolvedBy}"/>
                                    </span>
                                    <span class="handled-state-chip ${a.status == 'RESOLVED' ? 'chip-resolved' : 'chip-dismissed'}">
                                        ${a.status == 'RESOLVED' ? 'Resolved' : 'Dismissed'}
                                    </span>
                                </div>
                                <c:if test="${not empty a.resolvedNotes}">
                                    <div class="handled-note-quote">
                                        <span class="quote-symbol">“</span><c:out value="${a.resolvedNotes}"/><span class="quote-symbol">”</span>
                                    </div>
                                </c:if>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <span class="text-muted-dash">—</span>
                        </c:otherwise>
                    </c:choose>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty result.items}">
            <tr><td colspan="7" class="empty-state-cell">No alerts match the filter.</td></tr>
        </c:if>
        </tbody>
    </table>
    <div class="table-footer-bar">
        <span class="total-text">${result.total} alert(s), page ${result.page} of ${result.totalPages}</span>
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
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
