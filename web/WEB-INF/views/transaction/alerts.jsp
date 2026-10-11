<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Alert center"/>
<c:set var="nav" value="alerts"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<div class="card">
    <h1>Alert center</h1>
    <c:if test="${not empty message}"><div class="msg"><c:out value="${message}"/></div></c:if>
    <p class="summary">
        <span>New: <strong>${empty counts['NEW'] ? 0 : counts['NEW']}</strong></span>
        <span>In progress: <strong>${empty counts['IN_PROGRESS'] ? 0 : counts['IN_PROGRESS']}</strong></span>
        <span>Resolved: <strong>${empty counts['RESOLVED'] ? 0 : counts['RESOLVED']}</strong></span>
        <span>Dismissed: <strong>${empty counts['DISMISSED'] ? 0 : counts['DISMISSED']}</strong></span>
    </p>
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
            <th>Time</th><th>Rule</th><th>Severity</th><th>Amount (VND)</th><th>Reason</th><th>Status</th>
            <th>${canWrite ? 'Decision' : 'Handled by'}</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="a" items="${result.items}">
            <tr>
                <td><fmt:formatDate value="${a.createdAt}" pattern="yyyy-MM-dd HH:mm:ss"/></td>
                <td><c:out value="${a.ruleCode}"/></td>
                <td><span class="pill ${a.severity}"><c:out value="${a.severity}"/></span></td>
                <td class="num"><fmt:formatNumber value="${a.amount}" pattern="#,##0"/></td>
                <td class="wrap"><c:out value="${a.reason}"/><br><small>Content: <c:out value="${a.content}"/></small></td>
                <td><span class="pill ${a.status}"><c:out value="${a.status}"/></span></td>
                <td class="wrap">
                    <c:choose>
                        <c:when test="${(a.status == 'NEW' || a.status == 'IN_PROGRESS') && canWrite}">
                            <form method="post" action="${ctx}/alerts" class="inline">
                                <input type="hidden" name="id" value="${a.alertId}">
                                <input type="text" name="notes" maxlength="500" placeholder="note (optional)">
                                <button type="submit" name="decision" value="RESOLVED" class="primary">Resolve</button>
                                <button type="submit" name="decision" value="DISMISSED">Dismiss</button>
                            </form>
                        </c:when>
                        <c:when test="${not empty a.resolvedBy}">
                            <c:out value="${a.resolvedBy}"/><c:if test="${not empty a.resolvedNotes}">: <c:out value="${a.resolvedNotes}"/></c:if>
                        </c:when>
                        <c:otherwise>-</c:otherwise>
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
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
