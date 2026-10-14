<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Devices"/>
<c:set var="nav" value="devices"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<div class="card">
    <h1>Devices</h1>
    <c:if test="${not empty message}"><div class="msg"><c:out value="${message}"/></div></c:if>
    <p class="hint">A device shows ONLINE while its page is open and polling, OFFLINE when it has been silent for ${offlineAfter} seconds, and MAINTENANCE when a technician has taken it out of service.</p>
</div>

<div class="card">
    <table>
        <thead>
        <tr><th>Code</th><th>Type</th><th>Model</th><th>Status</th><th>Last seen</th><c:if test="${canWrite}"><th>Action</th></c:if></tr>
        </thead>
        <tbody>
        <c:forEach var="d" items="${devices}">
            <tr>
                <td><c:out value="${d.deviceCode}"/></td>
                <td><c:out value="${d.deviceType}"/></td>
                <td><c:out value="${d.modelName}"/></td>
                <td><span class="pill ${d.status == 'ONLINE' ? 'ACTIVE' : d.status == 'MAINTENANCE' ? 'PENDING' : 'LOCKED'}"><c:out value="${d.status}"/></span></td>
                <td><fmt:formatDate value="${d.lastPing}" pattern="yyyy-MM-dd HH:mm:ss"/></td>
                <c:if test="${canWrite}">
                    <td>
                        <form method="post" action="${ctx}/admin/devices" class="inline">
                            <input type="hidden" name="id" value="${d.deviceId}">
                            <c:choose>
                                <c:when test="${d.status == 'MAINTENANCE'}">
                                    <button type="submit" name="action" value="online" class="primary">Back to work</button>
                                </c:when>
                                <c:otherwise>
                                    <button type="submit" name="action" value="maintenance">Put in maintenance</button>
                                </c:otherwise>
                            </c:choose>
                        </form>
                    </td>
                </c:if>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
