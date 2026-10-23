<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Wallet and payouts"/>
<c:set var="nav" value="affiliates"/>
<c:set var="affTab" value="payouts"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<%@ include file="/WEB-INF/views/common/affiliate-tabs.jspf" %>
<div class="kpis">
    <div class="card"><span class="kpi-label">Available to pay</span><div class="kpi-value accent"><fmt:formatNumber value="${totalAvailable}" pattern="#,##0"/></div><p class="kpi-sub">All affiliates, VND</p></div>
    <div class="card"><span class="kpi-label">Ready for a payout</span><div class="kpi-value">${eligible.size()}</div><p class="kpi-sub">At least <fmt:formatNumber value="${payoutMin}" pattern="#,##0"/> VND and bank details</p></div>
</div>

<div class="card">
    <h2>Create payout batch</h2>
    <c:choose>
        <c:when test="${empty eligible}"><p class="hint">Nobody has reached the minimum payout yet.</p></c:when>
        <c:otherwise>
            <form method="post" action="${ctx}/admin/payouts" id="batchForm">
                <input type="hidden" name="action" value="pay">
                <table>
                    <thead><tr><th></th><th>Affiliate</th><th>Bank</th><th>Commission</th><th>Deductions</th><th>Net</th></tr></thead>
                    <tbody>
                    <c:forEach var="row" items="${eligible}">
                        <tr>
                            <td><input type="checkbox" name="affiliateId" value="${row[0].affiliateId}" checked></td>
                            <td><c:out value="${row[0].name}"/></td>
                            <td><c:out value="${row[0].bankName}"/> <c:out value="${row[0].bankAccount}"/><br><span class="hint"><c:out value="${row[0].bankHolder}"/></span></td>
                            <td><fmt:formatNumber value="${row[1]}" pattern="#,##0"/></td>
                            <td><fmt:formatNumber value="${row[2]}" pattern="#,##0"/></td>
                            <td><b><fmt:formatNumber value="${row[1] - row[2]}" pattern="#,##0"/></b></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
                <label>Bank transaction reference
                    <input type="text" name="reference" required minlength="6" maxlength="60" placeholder="e.g. FT26103100123">
                    <span class="hint">Pay the money from your bank first, then type the reference here. Confirming marks the commissions as paid and they can never be edited afterwards.</span>
                </label>
                <button type="submit" class="primary" onclick="return confirm('Confirm the payout of the selected affiliates? This cannot be undone.');">Confirm payout</button>
            </form>
        </c:otherwise>
    </c:choose>
</div>

<div class="card">
    <form method="get" action="${ctx}/admin/payouts" class="filters">
        <label>Search <input type="text" name="q" maxlength="60" placeholder="Affiliate, reference or batch" value="<c:out value='${q}'/>"></label>
        <label>Status
            <select name="status">
                <option value="">All</option>
                <option value="REQUESTED" ${statusFilter == 'REQUESTED' ? 'selected' : ''}>Requested</option>
                <option value="PAID" ${statusFilter == 'PAID' ? 'selected' : ''}>Paid</option>
            </select>
        </label>
        <button type="submit" class="primary">Filter</button>
        <a class="btn" href="${ctx}/admin/payouts">Reset</a>
        <a class="btn" href="${ctx}/admin/payouts?export=1&amp;status=${statusFilter}">Export payout history</a>
    </form>
    <table>
        <thead><tr><th>ID</th><th>Affiliate</th><th>Bank</th><th>Commission</th><th>Deductions</th><th>Net</th><th>Paid at</th><th>Reference</th><th>Status</th></tr></thead>
        <tbody>
        <c:forEach var="p" items="${payouts.items}">
            <tr>
                <td>#${p.payoutId}<br><span class="hint"><c:out value="${p.batchCode}"/></span></td>
                <td><c:out value="${p.affiliateName}"/></td>
                <td><c:out value="${p.bankInfo}"/></td>
                <td><fmt:formatNumber value="${p.gross}" pattern="#,##0"/></td>
                <td><fmt:formatNumber value="${p.deduction}" pattern="#,##0"/></td>
                <td><fmt:formatNumber value="${p.net}" pattern="#,##0"/></td>
                <td><fmt:formatDate value="${p.paidAt}" pattern="yyyy-MM-dd HH:mm"/></td>
                <td><c:out value="${p.reference}"/></td>
                <td><span class="pill ${p.status}">${p.status}</span></td>
            </tr>
        </c:forEach>
        <c:if test="${empty payouts.items}"><tr><td colspan="9">No payouts found.</td></tr></c:if>
        </tbody>
    </table>
    <c:set var="result" value="${payouts}"/>
    <c:set var="qe"><c:out value="${q}"/></c:set>
    <c:set var="pagerBase" value="${ctx}/admin/payouts?q=${qe}&amp;status=${statusFilter}&amp;"/>
    <%@ include file="/WEB-INF/views/common/pager.jspf" %>
</div>

<div class="card narrow">
    <h2>Program settings</h2>
    <form method="post" action="${ctx}/admin/payouts">
        <input type="hidden" name="action" value="settings">
        <label>Days before a pending commission can be bulk approved <input type="number" name="reviewDays" min="0" max="60" value="${reviewDays}"></label>
        <label>Days before an approved commission becomes payable (return window) <input type="number" name="payableDays" min="0" max="120" value="${payableDays}"></label>
        <label>Minimum payout (VND) <input type="number" name="payoutMin" min="0" max="50000000" value="${payoutMin}"></label>
        <label>Tax withholding for external affiliates (%) <input type="number" name="withholdPct" min="0" max="50" value="${withholdPct}"></label>
        <label>Withholding applies from a payout of (VND) <input type="number" name="withholdFrom" min="0" max="50000000" value="${withholdFrom}"></label>
        <span class="hint">The withholding rate is a setting to confirm with your accountant.</span>
        <button type="submit" class="primary">Save settings</button>
    </form>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
