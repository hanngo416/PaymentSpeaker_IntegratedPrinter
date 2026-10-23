<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Affiliate"/>
<c:set var="nav" value="affiliates"/>
<c:set var="affTab" value="affiliates"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<%@ include file="/WEB-INF/views/common/affiliate-tabs.jspf" %>
<c:set var="f" value="${empty editForm ? a : editForm}"/>
<div class="card">
    <h2><c:out value="${a.name}"/> <span class="pill ${a.status}">${a.status}</span></h2>
    <p><a href="${ctx}/admin/affiliates">Back to the list</a></p>
    <div class="kpis">
        <div class="card"><span class="kpi-label">Referral code</span><div class="kpi-value"><c:out value="${a.referralCode}"/></div><p class="kpi-sub">Type it on the order screen</p></div>
        <div class="card"><span class="kpi-label">Referred orders</span><div class="kpi-value">${a.orderCount}</div><p class="kpi-sub"><fmt:formatNumber value="${a.revenue}" pattern="#,##0"/> VND revenue</p></div>
        <div class="card"><span class="kpi-label">Pending balance</span><div class="kpi-value"><fmt:formatNumber value="${wallet.pending}" pattern="#,##0"/></div><p class="kpi-sub">Not payable yet</p></div>
        <div class="card"><span class="kpi-label">Available balance</span><div class="kpi-value accent"><fmt:formatNumber value="${wallet.available}" pattern="#,##0"/></div><p class="kpi-sub">Ready to pay</p></div>
        <div class="card"><span class="kpi-label">Total paid</span><div class="kpi-value"><fmt:formatNumber value="${wallet.paid}" pattern="#,##0"/></div><p class="kpi-sub">Adjustments: <fmt:formatNumber value="${wallet.adjustments}" pattern="#,##0"/></p></div>
    </div>
</div>

<div class="card narrow">
    <h2>Profile</h2>
    <form method="post" action="${ctx}/admin/affiliates">
        <input type="hidden" name="action" value="update">
        <input type="hidden" name="id" value="${a.affiliateId}">
            <label>Name <input type="text" name="name" maxlength="60" value="<c:out value='${f.name}'/>">
                <c:if test="${not empty errors.name}"><div class="field-error"><c:out value="${errors.name}"/></div></c:if>
            </label>
            <label>Email <input type="email" name="email" maxlength="100" value="<c:out value='${f.email}'/>">
                <c:if test="${not empty errors.email}"><div class="field-error"><c:out value="${errors.email}"/></div></c:if>
            </label>
        <label>Phone <input type="text" name="phone" maxlength="15" value="<c:out value='${f.phone}'/>">
            <c:if test="${not empty errors.phone}"><div class="field-error"><c:out value="${errors.phone}"/></div></c:if>
        </label>
        <label>Online-channel commission rate (%) <input type="text" name="rate" maxlength="6" value="<c:out value='${f.rate}'/>">
            <c:if test="${not empty errors.rate}"><div class="field-error"><c:out value="${errors.rate}"/></div></c:if>
        </label>
        <label>Bank name <input type="text" name="bankName" maxlength="60" value="<c:out value='${f.bankName}'/>"></label>
        <label>Bank account <input type="text" name="bankAccount" maxlength="30" value="<c:out value='${f.bankAccount}'/>"></label>
        <label>Account holder <input type="text" name="bankHolder" maxlength="100" value="<c:out value='${f.bankHolder}'/>">
            <c:if test="${not empty errors.bank}"><div class="field-error"><c:out value="${errors.bank}"/></div></c:if>
        </label>
        <button type="submit" class="primary">Save profile</button>
    </form>
    <form method="post" action="${ctx}/admin/affiliates" class="inline">
        <input type="hidden" name="id" value="${a.affiliateId}">
        <input type="hidden" name="from" value="detail">
        <c:choose>
            <c:when test="${a.status == 'ACTIVE'}"><button type="submit" name="action" value="suspend" onclick="return confirm('Suspend this affiliate?');">Suspend</button></c:when>
            <c:when test="${a.status == 'PENDING'}"><button type="submit" name="action" value="approve">Approve</button></c:when>
            <c:otherwise><button type="submit" name="action" value="activate">Activate</button></c:otherwise>
        </c:choose>
    </form>
</div>

<div class="card">
    <h2>Commission history</h2>
    <table>
        <thead><tr><th>Date</th><th>Order</th><th>Campaign</th><th>Rate</th><th>Order amount</th><th>Commission</th><th>Status</th></tr></thead>
        <tbody>
        <c:forEach var="c" items="${commissions}">
            <tr>
                <td><fmt:formatDate value="${c.createdAt}" pattern="yyyy-MM-dd"/></td>
                <td><c:out value="${c.orderCode}"/></td>
                <td><c:out value="${c.campaignName}"/></td>
                <td><c:out value="${c.rateText}"/></td>
                <td><fmt:formatNumber value="${c.orderAmount}" pattern="#,##0"/></td>
                <td><fmt:formatNumber value="${c.amount}" pattern="#,##0"/></td>
                <td><span class="pill ${c.status}">${c.status}</span></td>
            </tr>
        </c:forEach>
        <c:if test="${empty commissions}"><tr><td colspan="7">No commissions yet.</td></tr></c:if>
        </tbody>
    </table>
</div>

<div class="card">
    <h2>Payout history</h2>
    <table>
        <thead><tr><th>Payout</th><th>Batch</th><th>Gross</th><th>Deductions</th><th>Net</th><th>Paid at</th><th>Reference</th><th>Status</th></tr></thead>
        <tbody>
        <c:forEach var="p" items="${payouts}">
            <tr>
                <td>#${p.payoutId}</td><td><c:out value="${p.batchCode}"/></td>
                <td><fmt:formatNumber value="${p.gross}" pattern="#,##0"/></td>
                <td><fmt:formatNumber value="${p.deduction}" pattern="#,##0"/></td>
                <td><fmt:formatNumber value="${p.net}" pattern="#,##0"/></td>
                <td><fmt:formatDate value="${p.paidAt}" pattern="yyyy-MM-dd"/></td>
                <td><c:out value="${p.reference}"/></td>
                <td><span class="pill ${p.status}">${p.status}</span></td>
            </tr>
        </c:forEach>
        <c:if test="${empty payouts}"><tr><td colspan="8">No payouts yet.</td></tr></c:if>
        </tbody>
    </table>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
