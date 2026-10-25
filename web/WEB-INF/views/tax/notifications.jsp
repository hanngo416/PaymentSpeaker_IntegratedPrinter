<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Tax notifications"/>
<c:set var="nav" value="tax"/>
<c:set var="taxTab" value="notifications"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<%@ include file="/WEB-INF/views/common/tax-tabs.jspf" %>
<div class="card">
    <h1>Tax notifications <c:if test="${unread > 0}"><span class="pill NEW">${unread} new</span></c:if></h1>
    <p class="hint">Worked out from your current data each time you open the page. Each item opens the page where you can fix it. Read marks are kept for your session only.</p>
    <form method="get" action="${ctx}/tax/notifications" class="filters">
        <label>Type
            <select name="type">
                <option value="">All</option>
                <option value="DEADLINE" ${typeFilter == 'DEADLINE' ? 'selected' : ''}>Deadlines</option>
                <option value="OVERDUE" ${typeFilter == 'OVERDUE' ? 'selected' : ''}>Overdue</option>
                <option value="MISSING_INFO" ${typeFilter == 'MISSING_INFO' ? 'selected' : ''}>Missing information</option>
                <option value="MISSING_DOCS" ${typeFilter == 'MISSING_DOCS' ? 'selected' : ''}>Missing documents</option>
                <option value="INVOICE" ${typeFilter == 'INVOICE' ? 'selected' : ''}>Invoices</option>
                <option value="DISCREPANCY" ${typeFilter == 'DISCREPANCY' ? 'selected' : ''}>Revenue differences</option>
                <option value="REGULATION" ${typeFilter == 'REGULATION' ? 'selected' : ''}>Rules to verify</option>
            </select>
        </label>
        <label><input type="checkbox" name="unread" value="1" ${unreadOnly ? 'checked' : ''}> Unread only</label>
        <button type="submit" class="primary">Filter</button>
    </form>
    <form method="post" action="${ctx}/tax/notifications" class="inline"><button type="submit" name="action" value="all">Mark all as read</button></form>
</div>
<div class="card">
    <table>
        <tbody>
        <c:forEach var="n" items="${items}">
            <tr style="${n.read ? 'opacity: 0.6;' : ''}">
                <td><span class="pill ${n.type == 'OVERDUE' ? 'HIGH' : 'PENDING'}">${n.type}</span></td>
                <td><b><c:out value="${n.title}"/></b><br><span class="hint"><c:out value="${n.detail}"/></span></td>
                <td>
                    <form method="post" action="${ctx}/tax/notifications" class="inline">
                        <input type="hidden" name="key" value="${n.key}">
                        <button type="submit" name="action" value="open" class="primary">Open</button>
                        <button type="submit" name="action" value="${n.read ? 'unread' : 'read'}">${n.read ? 'Mark unread' : 'Mark read'}</button>
                    </form>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty items}"><tr><td>Nothing needs your attention.</td></tr></c:if>
        </tbody>
    </table>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
