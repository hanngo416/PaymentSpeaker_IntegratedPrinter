<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Platform commission reports"/>
<c:set var="nav" value="affiliates"/>
<c:set var="chTab" value="reports"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<%@ include file="/WEB-INF/views/common/channel-tabs.jspf" %>
<div class="card">
    <form method="get" action="${ctx}/admin/channel-reports" class="filters">
        <label>Report
            <select name="type">
                <c:forEach var="t" items="${types}"><option value="${t.key}" ${type == t.key ? 'selected' : ''}><c:out value="${t.value}"/></option></c:forEach>
            </select>
        </label>
        <label>From <input type="date" name="from" value="<c:out value='${fromFilter}'/>"></label>
        <label>To <input type="date" name="to" value="<c:out value='${toFilter}'/>"></label>
        <c:if test="${usesChannel}">
            <label>Platform
                <select name="channel">
                    <option value="">All</option>
                    <option value="SHOPEE" ${channelFilter == 'SHOPEE' ? 'selected' : ''}>Shopee</option>
                    <option value="TIKTOK" ${channelFilter == 'TIKTOK' ? 'selected' : ''}>TikTok Shop</option>
                </select>
            </label>
        </c:if>
        <label>Partner
            <select name="affiliateId">
                <option value="">All</option>
                <c:forEach var="a" items="${affiliates}"><option value="${a.affiliateId}" ${affiliateFilter == a.affiliateId ? 'selected' : ''}><c:out value="${a.name}"/></option></c:forEach>
            </select>
        </label>
        <c:if test="${usesStatus}">
            <label>Status
                <select name="status">
                    <option value="">Active only</option>
                    <c:forEach var="s" items="PENDING,APPROVED,PAID,REJECTED,CANCELLED"><option value="${s}" ${statusFilter == s ? 'selected' : ''}>${s}</option></c:forEach>
                </select>
            </label>
        </c:if>
        <button type="submit" class="primary">Run</button>
        <a class="btn" href="${ctx}/admin/channel-reports?export=1&amp;type=${type}&amp;from=${fromFilter}&amp;to=${toFilter}&amp;channel=${channelFilter}&amp;affiliateId=${affiliateFilter}&amp;status=${statusFilter}">Export CSV</a>
    </form>
    <p class="hint">
        <c:choose>
            <c:when test="${type == 'paid_history'}">Dates filter the payment date of confirmed payouts.</c:when>
            <c:when test="${type == 'reconciliation'}">Dates filter the day the payout was created. Difference = payout total minus the sum of its items; it should be 0.</c:when>
            <c:otherwise>Dates filter the order date. Amounts are in VND.</c:otherwise>
        </c:choose>
    </p>
    <h2><c:out value="${title}"/></h2>
    <table>
        <thead><tr><c:forEach var="h" items="${header}"><th><c:out value="${h}"/></th></c:forEach></tr></thead>
        <tbody>
        <c:forEach var="r" items="${rows.items}">
            <tr><c:forEach var="cell" items="${r}"><td><c:out value="${cell}"/></td></c:forEach></tr>
        </c:forEach>
        <c:if test="${empty rows.items}"><tr><td colspan="12">No data for these filters.</td></tr></c:if>
        </tbody>
    </table>
    <c:set var="result" value="${rows}"/>
    <c:set var="pagerBase" value="${ctx}/admin/channel-reports?type=${type}&amp;from=${fromFilter}&amp;to=${toFilter}&amp;channel=${channelFilter}&amp;affiliateId=${affiliateFilter}&amp;status=${statusFilter}&amp;"/>
    <%@ include file="/WEB-INF/views/common/pager.jspf" %>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
