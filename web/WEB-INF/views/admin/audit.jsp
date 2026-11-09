<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Audit log"/>
<c:set var="nav" value="audit"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<div class="card">
    <h1>Audit log</h1>
    <p class="hint">The history of who did what in the system: logins, orders, payments received, shifts, alerts and account changes. Records are only added, the application has no way to edit or delete them. Each record is chained to the one before it by a hash, so any change is detected.</p>
    <a class="btn primary" href="${ctx}/admin/audit?verify=1">Verify integrity</a>
    <c:if test="${not empty verification}">
        <c:choose>
            <c:when test="${verification.intact}">
                <div class="msg">The chain is intact: ${verification.rows} record(s) checked, none changed.</div>
            </c:when>
            <c:otherwise>
                <div class="error">The chain is broken<c:if test="${not empty verification.brokenAt}"> at record #${verification.brokenAt}</c:if>. <c:out value="${verification.reason}"/></div>
            </c:otherwise>
        </c:choose>
    </c:if>
</div>

<div class="card">
    <form method="get" action="${ctx}/admin/audit" class="filters">
        <label>Search
            <input type="text" name="q" value="<c:out value='${q}'/>" placeholder="Search who, id or detail..." maxlength="100">
        </label>
        <label>Action
            <select name="action">
                <option value="">All actions</option>
                <c:forEach var="a" items="${actions}">
                    <option value="<c:out value='${a}'/>" ${pickedActions.contains(a) ? 'selected' : ''}><c:out value="${a}"/></option>
                </c:forEach>
            </select>
        </label>
        <label>From
            <input type="date" name="from" value="<c:out value='${from}'/>">
        </label>
        <label>To
            <input type="date" name="to" value="<c:out value='${to}'/>">
        </label>
        <button type="submit" class="primary">Filter</button>
        <a class="btn" href="${ctx}/admin/audit">Reset</a>
    </form>
    <table>
        <thead>
        <tr><th>#</th><th>Time</th><th>Who</th><th>What happened</th></tr>
        </thead>
        <tbody>
        <c:forEach var="r" items="${result.items}">
            <tr>
                <td>${r.logId}</td>
                <td><fmt:formatDate value="${r.createdAt}" pattern="yyyy-MM-dd HH:mm:ss"/></td>
                <td><c:out value="${r.actor}"/></td>
                <td class="wrap" title="<c:out value='${r.action} ${r.entity} ${r.entityId}'/>"><c:out value="${r.summary}"/></td>            </tr>
        </c:forEach>
        <c:if test="${empty result.items}">
            <tr><td colspan="4">No records yet.</td></tr>
        </c:if>
        </tbody>
    </table>
    <p>${result.total} record(s), page ${result.page} of ${result.totalPages}</p>
    <div class="pages">
        <c:if test="${result.page > 1}">
            <a href="${ctx}/admin/audit?page=${result.page - 1}&amp;q=<c:out value='${q}'/><c:out value='${actionQuery}'/>&amp;from=<c:out value='${from}'/>&amp;to=<c:out value='${to}'/>" title="Previous page">&laquo;</a>
        </c:if>
        <c:forEach begin="1" end="${result.totalPages}" var="p">
            <c:choose>
                <c:when test="${p == result.page}"><span>${p}</span></c:when>
                <c:otherwise><a href="${ctx}/admin/audit?page=${p}&amp;q=<c:out value='${q}'/><c:out value='${actionQuery}'/>&amp;from=<c:out value='${from}'/>&amp;to=<c:out value='${to}'/>">${p}</a></c:otherwise>
            </c:choose>
        </c:forEach>
        <c:if test="${result.page < result.totalPages}">
            <a href="${ctx}/admin/audit?page=${result.page + 1}&amp;q=<c:out value='${q}'/><c:out value='${actionQuery}'/>&amp;from=<c:out value='${from}'/>&amp;to=<c:out value='${to}'/>" title="Next page">&raquo;</a>
        </c:if>
    </div>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
