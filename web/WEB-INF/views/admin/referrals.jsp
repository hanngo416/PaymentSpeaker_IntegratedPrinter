<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Referral tracking"/>
<c:set var="nav" value="affiliates"/>
<c:set var="affTab" value="referrals"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<%@ include file="/WEB-INF/views/common/affiliate-tabs.jspf" %>
<div class="card">
    <h2>Fraud and review flags</h2>
    <p class="hint">Raised automatically: five or more referred orders from one affiliate within 24 hours, and every commission you or the system put on hold. A flag does not block payment by itself; review it in Commissions.</p>
    <c:forEach var="f" items="${flags}">
        <div class="alert-line"><b><c:out value="${f[0]}"/></b> (<c:out value="${f[1]}"/>): <c:out value="${f[3]}"/></div>
    </c:forEach>
    <c:if test="${empty flags}"><p>No flags right now.</p></c:if>
</div>

<c:if test="${not empty detail}">
    <div class="card">
        <h2>Referral journey</h2>
        <p><b><c:out value="${detail.affiliateName}"/></b> &rarr; campaign <b><c:out value="${detail.campaignName}"/></b> &rarr; order <b><c:out value="${detail.orderCode}"/></b>
            (<fmt:formatNumber value="${detail.orderAmount}" pattern="#,##0"/> VND, saved at the counter) &rarr; commission <b><fmt:formatNumber value="${detail.amount}" pattern="#,##0"/> VND</b>
            <span class="pill ${detail.status}">${detail.status}</span></p>
        <p class="hint">The affiliate who introduced the customer and the employee who sold the order are two different roles. The seller is the order's creator.</p>
        <a class="btn" href="${ctx}/admin/commissions?id=${detail.commissionId}">Open the commission</a>
        <a class="btn" href="${ctx}/admin/referrals">Close</a>
    </div>
</c:if>

<div class="card">
    <form method="get" action="${ctx}/admin/referrals" class="filters">
        <label>Search <input type="text" name="q" maxlength="60" placeholder="Order or affiliate" value="<c:out value='${q}'/>"></label>
        <label>Attribution status
            <select name="status">
                <option value="">All</option>
                <c:forEach var="s" items="${statuses}"><option value="${s}" ${statusFilter == s ? 'selected' : ''}>${s}</option></c:forEach>
            </select>
        </label>
        <label>Period
            <select name="days">
                <option value="0">All time</option>
                <option value="7" ${daysFilter == 7 ? 'selected' : ''}>Last 7 days</option>
                <option value="30" ${daysFilter == 30 ? 'selected' : ''}>Last 30 days</option>
            </select>
        </label>
        <button type="submit" class="primary">Filter</button>
        <a class="btn" href="${ctx}/admin/referrals">Reset</a>
    </form>
    <table>
        <thead><tr><th>Date</th><th>Affiliate</th><th>Campaign</th><th>Source</th><th>Order</th><th>Amount</th><th>Attribution</th><th></th></tr></thead>
        <tbody>
        <c:forEach var="c" items="${result.items}">
            <tr>
                <td><fmt:formatDate value="${c.createdAt}" pattern="yyyy-MM-dd HH:mm"/></td>
                <td><c:out value="${c.affiliateName}"/></td>
                <td><c:out value="${c.campaignName}"/></td>
                <td>Referral code at checkout</td>
                <td><c:out value="${c.orderCode}"/></td>
                <td><fmt:formatNumber value="${c.orderAmount}" pattern="#,##0"/></td>
                <td><span class="pill ${c.status}">${c.status}</span></td>
                <td><a class="btn" href="${ctx}/admin/referrals?id=${c.commissionId}">Journey</a></td>
            </tr>
        </c:forEach>
        <c:if test="${empty result.items}"><tr><td colspan="8">No referred orders found.</td></tr></c:if>
        </tbody>
    </table>
    <c:set var="qe"><c:out value="${q}"/></c:set>
    <c:set var="pagerBase" value="${ctx}/admin/referrals?q=${qe}&amp;status=${statusFilter}&amp;days=${daysFilter}&amp;"/>
    <%@ include file="/WEB-INF/views/common/pager.jspf" %>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
