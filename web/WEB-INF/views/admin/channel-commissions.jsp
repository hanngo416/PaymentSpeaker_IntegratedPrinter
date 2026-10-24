<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Platform commissions"/>
<c:set var="nav" value="affiliates"/>
<c:set var="chTab" value="commissions"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<%@ include file="/WEB-INF/views/common/channel-tabs.jspf" %>
<div class="kpis">
    <div class="card"><span class="kpi-label">Pending approval</span><div class="kpi-value"><fmt:formatNumber value="${totals.PENDING}" pattern="#,##0"/></div><p class="kpi-sub">Generated, not yet an expense</p></div>
    <div class="card"><span class="kpi-label">Approved, unpaid</span><div class="kpi-value accent"><fmt:formatNumber value="${totals.APPROVED}" pattern="#,##0"/></div><p class="kpi-sub">Owed to the partners</p></div>
    <div class="card"><span class="kpi-label">Paid</span><div class="kpi-value"><fmt:formatNumber value="${totals.PAID}" pattern="#,##0"/></div><p class="kpi-sub">Settled in confirmed payouts</p></div>
    <div class="card"><span class="kpi-label">Rejected or cancelled</span><div class="kpi-value"><fmt:formatNumber value="${totals.REJECTED + totals.CANCELLED}" pattern="#,##0"/></div><p class="kpi-sub">Not owed</p></div>
</div>

<c:if test="${not empty detail}">
    <div class="card">
        <h2>Commission #${detail.commissionId} <span class="pill ${detail.status}">${detail.status}</span></h2>
        <table class="kv">
            <tr><td>Affiliate partner</td><td><c:out value="${detail.affiliateName}"/></td></tr>
            <tr><td>Platform</td><td><c:out value="${detail.channel}"/></td></tr>
            <tr><td>Platform order ID</td><td><c:out value="${detail.platformOrderId}"/></td></tr>
            <tr><td>Record reference</td><td><c:out value="${detail.externalRef}"/></td></tr>
            <tr><td>Order date</td><td><c:out value="${detail.orderDate}"/></td></tr>
            <tr><td>Commission base</td><td><fmt:formatNumber value="${detail.baseAmount}" pattern="#,##0"/> VND</td></tr>
            <tr><td>Type and rate (kept at creation)</td><td><c:out value="${detail.commissionType}"/> <fmt:formatNumber value="${detail.commissionRate}" pattern="#,##0.##"/><c:if test="${detail.commissionType == 'PERCENT'}">%</c:if></td></tr>
            <tr><td>Calculated by Tinky</td><td><fmt:formatNumber value="${detail.calculatedAmount}" pattern="#,##0"/> VND</td></tr>
            <tr><td>Reported by the platform</td><td><c:choose><c:when test="${detail.reportedAmount != null}"><fmt:formatNumber value="${detail.reportedAmount}" pattern="#,##0"/> VND (from the imported file, used as the amount owed)</c:when><c:otherwise>Not available (typed by the store)</c:otherwise></c:choose></td></tr>
            <tr><td>Final approved amount</td><td><c:choose><c:when test="${detail.approvedAmount != null}"><b><fmt:formatNumber value="${detail.approvedAmount}" pattern="#,##0"/> VND</b></c:when><c:otherwise>Not approved yet</c:otherwise></c:choose></td></tr>
            <tr><td>Source of data</td><td><c:out value="${detail.dataSource}"/></td></tr>
            <tr><td>Attribution</td><td><c:out value="${detail.attribution}"/></td></tr>
            <tr><td>Created</td><td><fmt:formatDate value="${detail.createdAt}" pattern="yyyy-MM-dd HH:mm"/></td></tr>
            <tr><td>Decided by</td><td><c:out value="${detail.approvedByName}"/> <fmt:formatDate value="${detail.approvedAt}" pattern="yyyy-MM-dd HH:mm"/></td></tr>
            <c:if test="${not empty detail.rejectReason}"><tr><td>Reason</td><td><c:out value="${detail.rejectReason}"/></td></tr></c:if>
            <c:if test="${not empty detail.notes}"><tr><td>Notes</td><td><c:out value="${detail.notes}"/></td></tr></c:if>
            <c:if test="${not empty detail.payoutReference}"><tr><td>Payout</td><td><a href="${ctx}/admin/channel-payouts?id=${detail.payoutId}"><c:out value="${detail.payoutReference}"/></a></td></tr></c:if>
        </table>
        <c:if test="${canWrite && (detail.status == 'PENDING' || (detail.status == 'APPROVED' && detail.payoutId == null))}">
            <form method="post" action="${ctx}/admin/channel-commissions" class="filters">
                <input type="hidden" name="id" value="${detail.commissionId}">
                <label>Reason (needed to reject or cancel) <input type="text" name="reason" minlength="5" maxlength="200"></label>
                <c:if test="${detail.status == 'PENDING'}">
                    <button type="submit" name="action" value="approve" class="primary" onclick="return confirm('Approve this commission? It becomes payable.');">Approve</button>
                    <button type="submit" name="action" value="reject" onclick="return confirm('Reject this commission?');">Reject</button>
                </c:if>
                <c:if test="${detail.status == 'APPROVED'}">
                    <button type="submit" name="action" value="cancel" onclick="return confirm('Cancel this approved commission?');">Cancel commission</button>
                </c:if>
            </form>
        </c:if>
        <c:if test="${detail.status == 'APPROVED' && detail.payoutId != null}"><p class="hint">This commission is reserved in a draft payout and cannot be cancelled until it is removed from it.</p></c:if>
        <c:if test="${detail.status == 'PAID'}"><p class="hint">A paid commission is locked and never edited.</p></c:if>
        <a class="btn" href="${ctx}/admin/channel-commissions">Close</a>
    </div>
</c:if>

<c:if test="${canWrite}">
    <div class="card">
        <details ${showForm ? 'open' : ''}>
            <summary><b>Add a commission by hand</b></summary>
            <p class="hint">Use this when the platform report is not available. The commission is calculated by Tinky and is marked as typed by the store, not verified by the platform.</p>
            <c:if test="${not empty formErrors}"><div class="error"><ul><c:forEach var="e" items="${formErrors}"><li><c:out value="${e}"/></li></c:forEach></ul></div></c:if>
            <form method="post" action="${ctx}/admin/channel-commissions" class="filters">
                <input type="hidden" name="action" value="create">
                <label>Platform
                    <select name="channel" required>
                        <option value="SHOPEE" ${param.channel == 'SHOPEE' ? 'selected' : ''}>Shopee</option>
                        <option value="TIKTOK" ${param.channel == 'TIKTOK' ? 'selected' : ''}>TikTok Shop</option>
                    </select>
                </label>
                <label>Affiliate partner
                    <select name="affiliateId" required>
                        <option value="">Choose</option>
                        <c:forEach var="a" items="${affiliates}">
                            <c:if test="${a.status == 'ACTIVE'}"><option value="${a.affiliateId}" ${param.affiliateId == a.affiliateId ? 'selected' : ''}><c:out value="${a.name}"/></option></c:if>
                        </c:forEach>
                    </select>
                </label>
                <label>Platform order ID <input type="text" name="orderId" required maxlength="60" value="<c:out value='${param.orderId}'/>"></label>
                <label>Order date <input type="date" name="orderDate" required value="<c:out value='${param.orderDate}'/>"></label>
                <label>Commission base (VND) <input type="text" name="base" required maxlength="12" inputmode="numeric" value="<c:out value='${param.base}'/>"></label>
                <label>Type
                    <select name="type">
                        <option value="PERCENT" ${param.type != 'FIXED' ? 'selected' : ''}>Percent of the base</option>
                        <option value="FIXED" ${param.type == 'FIXED' ? 'selected' : ''}>Fixed amount (VND)</option>
                    </select>
                </label>
                <label>Rate or amount <input type="text" name="rate" maxlength="11" placeholder="empty = affiliate rate" value="<c:out value='${param.rate}'/>"></label>
                <label>Notes <input type="text" name="notes" maxlength="200" value="<c:out value='${param.notes}'/>"></label>
                <button type="submit" class="primary">Add commission</button>
            </form>
        </details>
    </div>
</c:if>

<div class="card">
    <form method="get" action="${ctx}/admin/channel-commissions" class="filters">
        <label>Search <input type="text" name="q" maxlength="60" placeholder="Order ID, reference or partner" value="<c:out value='${q}'/>"></label>
        <label>Platform
            <select name="channel">
                <option value="">All</option>
                <option value="SHOPEE" ${channelFilter == 'SHOPEE' ? 'selected' : ''}>Shopee</option>
                <option value="TIKTOK" ${channelFilter == 'TIKTOK' ? 'selected' : ''}>TikTok Shop</option>
            </select>
        </label>
        <label>Partner
            <select name="affiliateId">
                <option value="">All</option>
                <c:forEach var="a" items="${affiliates}"><option value="${a.affiliateId}" ${affiliateFilter == a.affiliateId ? 'selected' : ''}><c:out value="${a.name}"/></option></c:forEach>
            </select>
        </label>
        <label>Status
            <select name="status">
                <option value="">All</option>
                <c:forEach var="s" items="PENDING,APPROVED,PAID,REJECTED,CANCELLED"><option value="${s}" ${statusFilter == s ? 'selected' : ''}>${s}</option></c:forEach>
            </select>
        </label>
        <label>From <input type="date" name="from" value="<c:out value='${fromFilter}'/>"></label>
        <label>To <input type="date" name="to" value="<c:out value='${toFilter}'/>"></label>
        <button type="submit" class="primary">Filter</button>
        <a class="btn" href="${ctx}/admin/channel-commissions">Reset</a>
        <c:set var="qe"><c:out value="${q}"/></c:set>
        <a class="btn" href="${ctx}/admin/channel-commissions?export=1&amp;q=${qe}&amp;channel=${channelFilter}&amp;affiliateId=${affiliateFilter}&amp;status=${statusFilter}&amp;from=${fromFilter}&amp;to=${toFilter}">Export CSV</a>
    </form>
    <table>
        <thead><tr><th>ID</th><th>Order date</th><th>Partner</th><th>Platform</th><th>Platform order</th><th>Base</th><th>Commission</th><th>Source</th><th>Status</th><th></th></tr></thead>
        <tbody>
        <c:forEach var="c" items="${commissions.items}">
            <tr>
                <td>#${c.commissionId}</td>
                <td><c:out value="${c.orderDate}"/></td>
                <td><c:out value="${c.affiliateName}"/></td>
                <td><c:out value="${c.channel}"/></td>
                <td><c:out value="${c.platformOrderId}"/></td>
                <td><fmt:formatNumber value="${c.baseAmount}" pattern="#,##0"/></td>
                <td><b><fmt:formatNumber value="${c.payableAmount}" pattern="#,##0"/></b></td>
                <td><c:out value="${c.dataSource}"/></td>
                <td><span class="pill ${c.status}">${c.status}</span></td>
                <td><a href="${ctx}/admin/channel-commissions?id=${c.commissionId}&amp;q=${qe}&amp;channel=${channelFilter}&amp;status=${statusFilter}&amp;page=${commissions.page}">Details</a></td>
            </tr>
        </c:forEach>
        <c:if test="${empty commissions.items}"><tr><td colspan="10">No commissions found. Import a CSV file or add one by hand.</td></tr></c:if>
        </tbody>
    </table>
    <c:set var="result" value="${commissions}"/>
    <c:set var="pagerBase" value="${ctx}/admin/channel-commissions?q=${qe}&amp;channel=${channelFilter}&amp;affiliateId=${affiliateFilter}&amp;status=${statusFilter}&amp;from=${fromFilter}&amp;to=${toFilter}&amp;"/>
    <%@ include file="/WEB-INF/views/common/pager.jspf" %>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
