<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Audit log"/>
<c:set var="nav" value="audit"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<div class="card">
    <h1>Audit log</h1>
    <p class="hint">Every important action is written here. Each record carries the hash of the record before it, so changing or deleting an old record breaks the chain after it.</p>
    <c:if test="${not empty check}">
        <c:choose>
            <c:when test="${check.ok}">
                <div class="msg">Chain intact: ${check.checked} record(s) checked, none changed or removed.</div>
            </c:when>
            <c:otherwise>
                <div class="alert-banner">Chain broken at record #${check.brokenLogId}: <c:out value="${check.reason}"/>. ${check.checked} record(s) before it are still good.</div>
            </c:otherwise>
        </c:choose>
    </c:if>
    <a class="btn primary" href="${ctx}/admin/audit?verify=1">Verify the whole chain</a>
</div>

<div class="card">
    <table>
        <thead>
        <tr><th>#</th><th>Time</th><th>Actor</th><th>Action</th><th>Entity</th><th>Detail</th><th>Hash</th></tr>
        </thead>
        <tbody>
        <c:forEach var="r" items="${result.items}">
            <tr>
                <td>${r.logId}</td>
                <td><fmt:formatDate value="${r.createdAt}" pattern="yyyy-MM-dd HH:mm:ss"/></td>
                <td><c:out value="${r.actor}"/></td>
                <td><c:out value="${r.action}"/></td>
                <td><c:out value="${r.entity}"/> <c:out value="${r.entityId}"/></td>
                <td class="wrap"><c:out value="${r.detail}"/></td>
                <td><small><c:out value="${r.currHash.substring(0, 12)}"/>...</small></td>
            </tr>
        </c:forEach>
        <c:if test="${empty result.items}">
            <tr><td colspan="7">No records yet.</td></tr>
        </c:if>
        </tbody>
    </table>
    <p>${result.total} record(s), page ${result.page} of ${result.totalPages}</p>
    <div class="pages">
        <c:forEach begin="1" end="${result.totalPages}" var="p">
            <c:choose>
                <c:when test="${p == result.page}"><span>${p}</span></c:when>
                <c:otherwise><a href="${ctx}/admin/audit?page=${p}">${p}</a></c:otherwise>
            </c:choose>
        </c:forEach>
    </div>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
