<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Maintenance tickets"/>
<c:set var="nav" value="tickets"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<div class="card">
    <h1>Maintenance tickets</h1>
    <c:if test="${not empty message}"><div class="msg"><c:out value="${message}"/></div></c:if>
    <form method="post" action="${ctx}/tickets" class="filters">
        <input type="hidden" name="action" value="create">
        <label>Device
            <select name="deviceId">
                <c:forEach var="d" items="${devices}">
                    <option value="${d.deviceId}"><c:out value="${d.deviceCode}"/> (<c:out value="${d.deviceType}"/>)</option>
                </c:forEach>
            </select>
        </label>
        <label>Problem
            <input type="text" name="description" maxlength="500" required placeholder="for example: printer cutter jammed">
        </label>
        <button type="submit" class="primary">Report problem</button>
    </form>
    <form method="get" action="${ctx}/tickets" class="filters">
        <label>Status
            <select name="status">
                <option value="">All</option>
                <c:forEach var="s" items="${statuses}">
                    <option value="${s}" ${status == s ? 'selected' : ''}>${s}</option>
                </c:forEach>
            </select>
        </label>
        <button type="submit">Filter</button>
    </form>
</div>

<div class="card">
    <table>
        <thead>
        <tr><th>Time</th><th>Device</th><th>Reported by</th><th>Problem</th><th>Status</th><th>${canHandle ? 'Technician' : 'Technician notes'}</th></tr>
        </thead>
        <tbody>
        <c:forEach var="t" items="${tickets}">
            <tr>
                <td><fmt:formatDate value="${t.createdAt}" pattern="yyyy-MM-dd HH:mm"/></td>
                <td><c:out value="${t.deviceCode}"/></td>
                <td><c:out value="${t.reportedBy}"/></td>
                <td class="wrap"><c:out value="${t.description}"/></td>
                <td><span class="pill ${t.status == 'RESOLVED' ? 'RESOLVED' : 'PENDING'}"><c:out value="${t.status}"/></span></td>
                <td class="wrap">
                    <c:choose>
                        <c:when test="${canHandle && t.status != 'RESOLVED'}">
                            <form method="post" action="${ctx}/tickets" class="inline">
                                <input type="hidden" name="action" value="update">
                                <input type="hidden" name="id" value="${t.ticketId}">
                                <input type="text" name="notes" maxlength="500" placeholder="what was done" value="<c:out value='${t.techNotes}'/>">
                                <c:if test="${t.status == 'OPEN'}"><button type="submit" name="status" value="IN_PROGRESS">Take it</button></c:if>
                                <button type="submit" name="status" value="RESOLVED" class="primary">Resolve</button>
                            </form>
                        </c:when>
                        <c:otherwise>
                            <c:out value="${t.techNotes}"/>
                            <c:if test="${not empty t.resolvedAt}"><br><small>Resolved <fmt:formatDate value="${t.resolvedAt}" pattern="yyyy-MM-dd HH:mm"/></small></c:if>
                        </c:otherwise>
                    </c:choose>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty tickets}">
            <tr><td colspan="6">No tickets match the filter.</td></tr>
        </c:if>
        </tbody>
    </table>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
