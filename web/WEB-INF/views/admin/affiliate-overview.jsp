<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Affiliate overview"/>
<c:set var="nav" value="affiliates"/>
<c:set var="affTab" value="overview"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<%@ include file="/WEB-INF/views/common/affiliate-tabs.jspf" %>
<div class="card">
    <form method="get" action="${ctx}/admin/affiliate-overview" class="filters">
        <label>Period
            <select name="days" onchange="this.form.submit()">
                <option value="7" ${days == 7 ? 'selected' : ''}>Last 7 days</option>
                <option value="30" ${days == 30 ? 'selected' : ''}>Last 30 days</option>
                <option value="90" ${days == 90 ? 'selected' : ''}>Last 90 days</option>
                <option value="365" ${days == 365 ? 'selected' : ''}>Last 12 months</option>
            </select>
        </label>
        <a class="btn primary" href="${ctx}/admin/campaigns">Create campaign</a>
        <a class="btn" href="${ctx}/admin/affiliate-overview?export=1">Export CSV</a>
    </form>
</div>
<div class="kpis">
    <div class="card"><span class="kpi-label">Affiliates</span><div class="kpi-value">${kpi.total}</div><p class="kpi-sub">${kpi.active} active, ${kpi.pending} pending</p></div>
    <div class="card"><span class="kpi-label">Referred orders</span><div class="kpi-value">${kpi.orders}</div><p class="kpi-sub">Last ${days} days</p></div>
    <div class="card"><span class="kpi-label">Affiliate revenue</span><div class="kpi-value accent"><fmt:formatNumber value="${kpi.revenue}" pattern="#,##0"/></div><p class="kpi-sub">VND, completed orders</p></div>
    <div class="card"><span class="kpi-label">Commissions</span><div class="kpi-value"><fmt:formatNumber value="${kpi.commissions}" pattern="#,##0"/></div><p class="kpi-sub">VND, not rejected or reversed</p></div>
    <div class="card"><span class="kpi-label">Pending payouts</span><div class="kpi-value"><fmt:formatNumber value="${kpi.pendingPayouts}" pattern="#,##0"/></div><p class="kpi-sub">Available, not yet paid</p></div>
</div>

<div class="two-col">
    <div class="card">
        <h2>Affiliate revenue over time</h2>
        <c:choose>
            <c:when test="${empty daily}"><p class="hint">No referred orders in this period.</p></c:when>
            <c:otherwise>
                <div class="bar-chart">
                    <c:forEach var="d" items="${daily}">
                        <div class="bar" style="height: ${d[2] * 100 / dailyMax}%;" title="${d[0]}: <fmt:formatNumber value='${d[2]}' pattern='#,##0'/> VND, ${d[1]} order(s), commission <fmt:formatNumber value='${d[3]}' pattern='#,##0'/>"></div>
                    </c:forEach>
                </div>
                <p class="hint">Each bar is one day with referred orders. Hover for the figures.</p>
            </c:otherwise>
        </c:choose>
    </div>
    <div class="card">
        <h2>Revenue by campaign</h2>
        <table>
            <tbody>
            <c:forEach var="r" items="${byCampaign}">
                <tr><td><c:out value="${r[0]}"/></td><td>${r[1]} order(s)</td><td><fmt:formatNumber value="${r[2]}" pattern="#,##0"/></td></tr>
            </c:forEach>
            <c:if test="${empty byCampaign}"><tr><td>No data.</td></tr></c:if>
            </tbody>
        </table>
    </div>
</div>

<div class="two-col" style="margin-top: 24px;">
    <div class="card">
        <h2>Top performing affiliates</h2>
        <table>
            <thead><tr><th>Name</th><th>Orders</th><th>Revenue</th><th>Commission</th></tr></thead>
            <tbody>
            <c:forEach var="r" items="${top}">
                <tr><td><c:out value="${r[0]}"/></td><td>${r[1]}</td><td><fmt:formatNumber value="${r[2]}" pattern="#,##0"/></td><td><fmt:formatNumber value="${r[3]}" pattern="#,##0"/></td></tr>
            </c:forEach>
            <c:if test="${empty top}"><tr><td colspan="4">No data.</td></tr></c:if>
            </tbody>
        </table>
        <h3>Top products through affiliates</h3>
        <table>
            <tbody>
            <c:forEach var="r" items="${products}">
                <tr><td><c:out value="${r[0]}"/></td><td>${r[1]} sold</td><td><fmt:formatNumber value="${r[2]}" pattern="#,##0"/></td></tr>
            </c:forEach>
            <c:if test="${empty products}"><tr><td>Product lines are only stored for cart orders.</td></tr></c:if>
            </tbody>
        </table>
    </div>
    <div class="card">
        <h2>Pending commission approvals (${kpi.toApprove})</h2>
        <table>
            <tbody>
            <c:forEach var="c" items="${pending}">
                <tr>
                    <td><c:out value="${c.affiliateName}"/><br><span class="hint"><c:out value="${c.orderCode}"/></span></td>
                    <td><fmt:formatNumber value="${c.amount}" pattern="#,##0"/></td>
                    <td><a class="btn" href="${ctx}/admin/commissions?id=${c.commissionId}">Review</a></td>
                </tr>
            </c:forEach>
            <c:if test="${empty pending}"><tr><td>Nothing waiting.</td></tr></c:if>
            </tbody>
        </table>
        <h3>Recent referral orders</h3>
        <table>
            <tbody>
            <c:forEach var="c" items="${recent}">
                <tr><td><c:out value="${c.orderCode}"/></td><td><c:out value="${c.affiliateName}"/></td><td><fmt:formatNumber value="${c.orderAmount}" pattern="#,##0"/></td><td><span class="pill ${c.status}">${c.status}</span></td></tr>
            </c:forEach>
            <c:if test="${empty recent}"><tr><td>No referral orders yet.</td></tr></c:if>
            </tbody>
        </table>
    </div>
</div>

<div class="card" style="margin-top: 24px;">
    <h2>Review flags</h2>
    <c:forEach var="f" items="${flags}">
        <div class="alert-line"><b><c:out value="${f[0]}"/></b> (<c:out value="${f[1]}"/>): <c:out value="${f[3]}"/></div>
    </c:forEach>
    <c:if test="${empty flags}"><p>No flags right now.</p></c:if>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
