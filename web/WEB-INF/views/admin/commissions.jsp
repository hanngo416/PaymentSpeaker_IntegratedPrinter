<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Commissions"/>
<c:set var="nav" value="affiliates"/>
<c:set var="affTab" value="commissions"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<%@ include file="/WEB-INF/views/common/affiliate-tabs.jspf" %>
<div class="kpis">
    <div class="card"><span class="kpi-label">Pending (estimated)</span><div class="kpi-value"><fmt:formatNumber value="${totals.PENDING}" pattern="#,##0"/></div><p class="kpi-sub">Waiting for your approval</p></div>
    <div class="card"><span class="kpi-label">Approved (confirmed)</span><div class="kpi-value"><fmt:formatNumber value="${totals.APPROVED}" pattern="#,##0"/></div><p class="kpi-sub">Becomes payable after the return window</p></div>
    <div class="card"><span class="kpi-label">Available (payable)</span><div class="kpi-value accent"><fmt:formatNumber value="${totals.AVAILABLE}" pattern="#,##0"/></div><p class="kpi-sub">Ready for a payout</p></div>
    <div class="card"><span class="kpi-label">Paid</span><div class="kpi-value"><fmt:formatNumber value="${totals.PAID}" pattern="#,##0"/></div><p class="kpi-sub">Settled</p></div>
    <div class="card"><span class="kpi-label">Reversed</span><div class="kpi-value"><fmt:formatNumber value="${totals.REVERSED}" pattern="#,##0"/></div><p class="kpi-sub">Cancelled or refunded orders</p></div>
</div>

<c:if test="${not empty detail}">
    <div class="card">
        <h2>Commission #${detail.commissionId} <span class="pill ${detail.status}">${detail.status}</span></h2>
        <table class="kv">
            <tr><td>Affiliate</td><td><c:out value="${detail.affiliateName}"/></td></tr>
            <tr><td>Order</td><td><c:out value="${detail.orderCode}"/> (<fmt:formatNumber value="${detail.orderAmount}" pattern="#,##0"/> VND)</td></tr>
            <tr><td>Eligible amount</td><td><fmt:formatNumber value="${detail.eligibleAmount}" pattern="#,##0"/> VND at <c:out value="${detail.rateText}"/></td></tr>
            <tr><td>Original commission</td><td><fmt:formatNumber value="${detail.originalAmount}" pattern="#,##0"/> VND</td></tr>
            <tr><td>Current commission</td><td><b><fmt:formatNumber value="${detail.amount}" pattern="#,##0"/> VND</b></td></tr>
            <tr><td>Rule snapshot</td><td><c:out value="${detail.ruleSnapshot}"/></td></tr>
            <c:if test="${not empty detail.reason}"><tr><td>Reason</td><td><c:out value="${detail.reason}"/></td></tr></c:if>
        </table>
        <c:if test="${detail.status != 'PAID' && detail.status != 'REJECTED' && detail.status != 'REVERSED' || detail.status == 'PAID'}">
            <c:if test="${detail.status != 'REJECTED' && detail.status != 'REVERSED'}">
                <h3>Refund or adjust</h3>
                <p class="hint"><c:choose><c:when test="${detail.status == 'PAID'}">This commission is already paid and is never edited. An adjustment is created and taken from the affiliate's next payout.</c:when><c:otherwise>The commission is reduced and the change is written to the audit log.</c:otherwise></c:choose></p>
                <form method="post" action="${ctx}/admin/commissions" class="filters">
                    <input type="hidden" name="action" value="adjust">
                    <input type="hidden" name="id" value="${detail.commissionId}">
                    <label>Type
                        <select name="mode">
                            <option value="full">Full refund</option>
                            <option value="partial">Partial refund (% of the commission)</option>
                            <option value="manual">Manual reduction (negative VND)</option>
                        </select>
                    </label>
                    <label>Value <input type="text" name="value" maxlength="12" placeholder="e.g. 30 or -5000"></label>
                    <label>Reason <input type="text" name="reason" required minlength="5" maxlength="200"></label>
                    <button type="submit" class="primary" onclick="return confirm('Apply this adjustment?');">Apply</button>
                </form>
            </c:if>
        </c:if>
        <c:if test="${detail.status == 'PENDING' || detail.status == 'ON_HOLD' || detail.status == 'APPROVED'}">
            <form method="post" action="${ctx}/admin/commissions" class="filters">
                <input type="hidden" name="id" value="${detail.commissionId}">
                <label>Reason for reject or hold <input type="text" name="reason" minlength="5" maxlength="200"></label>
                <c:if test="${detail.status != 'APPROVED'}"><button type="submit" name="action" value="approve">Approve</button></c:if>
                <button type="submit" name="action" value="reject">Reject</button>
                <c:if test="${detail.status != 'ON_HOLD'}"><button type="submit" name="action" value="hold">Put on hold</button></c:if>
            </form>
        </c:if>
        <a class="btn" href="${ctx}/admin/commissions">Close</a>
    </div>
</c:if>

<div class="card">
    <form method="get" action="${ctx}/admin/commissions" class="filters">
        <label>Search <input type="text" name="q" maxlength="60" placeholder="Order or affiliate" value="<c:out value='${q}'/>"></label>
        <label>Status
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
                <option value="90" ${daysFilter == 90 ? 'selected' : ''}>Last 90 days</option>
            </select>
        </label>
        <button type="submit" class="primary">Filter</button>
        <a class="btn" href="${ctx}/admin/commissions">Reset</a>
        <a class="btn" href="${ctx}/admin/commissions?export=1&amp;status=${statusFilter}&amp;days=${daysFilter}">Export CSV</a>
    </form>
    <form method="post" action="${ctx}/admin/commissions" class="inline" onsubmit="return confirm('Approve ${bulkCount} pending commission(s) that are older than ${reviewDays} day(s)? Held items and cancelled orders are skipped.');">
        <button type="submit" name="action" value="bulk" ${bulkCount == 0 ? 'disabled' : ''}>Bulk approve eligible (${bulkCount})</button>
    </form>
    <table>
        <thead><tr><th>ID</th><th>Affiliate</th><th>Order</th><th>Order amount</th><th>Eligible</th><th>Rate</th><th>Commission</th><th>Created</th><th>Status</th><th>Action</th></tr></thead>
        <tbody>
        <c:forEach var="c" items="${result.items}">
            <tr>
                <td>#${c.commissionId}</td>
                <td><c:out value="${c.affiliateName}"/></td>
                <td><c:out value="${c.orderCode}"/></td>
                <td><fmt:formatNumber value="${c.orderAmount}" pattern="#,##0"/></td>
                <td><fmt:formatNumber value="${c.eligibleAmount}" pattern="#,##0"/></td>
                <td><c:out value="${c.rateText}"/></td>
                <td><fmt:formatNumber value="${c.amount}" pattern="#,##0"/><c:if test="${c.amount != c.originalAmount}"><br><span class="hint">was <fmt:formatNumber value="${c.originalAmount}" pattern="#,##0"/></span></c:if></td>
                <td><fmt:formatDate value="${c.createdAt}" pattern="yyyy-MM-dd"/></td>
                <td><span class="pill ${c.status}">${c.status}</span></td>
                <td>
                    <a class="btn" href="${ctx}/admin/commissions?id=${c.commissionId}">Details</a>
                    <c:if test="${c.status == 'PENDING' || c.status == 'ON_HOLD'}">
                        <form method="post" action="${ctx}/admin/commissions" class="inline">
                            <input type="hidden" name="id" value="${c.commissionId}">
                            <button type="submit" name="action" value="approve">Approve</button>
                        </form>
                    </c:if>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty result.items}"><tr><td colspan="10">No commissions found.</td></tr></c:if>
        </tbody>
    </table>
    <c:set var="qe"><c:out value="${q}"/></c:set>
    <c:set var="pagerBase" value="${ctx}/admin/commissions?q=${qe}&amp;status=${statusFilter}&amp;days=${daysFilter}&amp;"/>
    <%@ include file="/WEB-INF/views/common/pager.jspf" %>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
