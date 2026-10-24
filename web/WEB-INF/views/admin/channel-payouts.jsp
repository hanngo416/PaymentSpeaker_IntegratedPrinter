<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Platform payouts"/>
<c:set var="nav" value="affiliates"/>
<c:set var="chTab" value="payouts"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<%@ include file="/WEB-INF/views/common/channel-tabs.jspf" %>

<c:if test="${not empty detail}">
    <div class="card">
        <h2>Payout <c:out value="${detail.reference}"/> <span class="pill ${detail.status}">${detail.status}</span></h2>
        <table class="kv">
            <tr><td>Affiliate partner</td><td><c:out value="${detail.affiliateName}"/></td></tr>
            <tr><td>Total amount</td><td><b><fmt:formatNumber value="${detail.totalAmount}" pattern="#,##0"/> VND</b> (${detail.itemCount} commission(s))</td></tr>
            <tr><td>Payment method</td><td><c:out value="${detail.paymentMethod}"/></td></tr>
            <tr><td>Payment reference</td><td><c:out value="${detail.paymentReference}"/></td></tr>
            <tr><td>Payment date</td><td><c:out value="${detail.paymentDate}"/></td></tr>
            <tr><td>Created by</td><td><c:out value="${detail.createdByName}"/> <fmt:formatDate value="${detail.createdAt}" pattern="yyyy-MM-dd HH:mm"/></td></tr>
            <tr><td>Confirmed by</td><td><c:out value="${detail.confirmedByName}"/> <fmt:formatDate value="${detail.confirmedAt}" pattern="yyyy-MM-dd HH:mm"/></td></tr>
            <c:if test="${not empty detail.notes}"><tr><td>Notes</td><td><c:out value="${detail.notes}"/></td></tr></c:if>
        </table>
        <table>
            <thead><tr><th>Commission</th><th>Platform</th><th>Platform order</th><th>Order date</th><th>Base</th><th>Amount</th><th></th></tr></thead>
            <tbody>
            <c:forEach var="i" items="${detail.items}">
                <tr>
                    <td><a href="${ctx}/admin/channel-commissions?id=${i.commissionId}">#${i.commissionId}</a></td>
                    <td><c:out value="${i.channel}"/></td>
                    <td><c:out value="${i.platformOrderId}"/></td>
                    <td><c:out value="${i.orderDate}"/></td>
                    <td><fmt:formatNumber value="${i.baseAmount}" pattern="#,##0"/></td>
                    <td><fmt:formatNumber value="${i.approvedAmount}" pattern="#,##0"/></td>
                    <td>
                        <c:if test="${canWrite && detail.status == 'DRAFT' && detail.itemCount > 1}">
                            <form method="post" action="${ctx}/admin/channel-payouts" style="display:inline">
                                <input type="hidden" name="action" value="remove">
                                <input type="hidden" name="id" value="${detail.payoutId}">
                                <input type="hidden" name="commissionId" value="${i.commissionId}">
                                <button type="submit" onclick="return confirm('Remove this commission from the draft payout?');">Remove</button>
                            </form>
                        </c:if>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
        <c:if test="${canWrite && detail.status == 'DRAFT'}">
            <form method="post" action="${ctx}/admin/channel-payouts" class="filters">
                <input type="hidden" name="id" value="${detail.payoutId}">
                <label>Payment method
                    <select name="method">
                        <option value="BANK_TRANSFER" ${detail.paymentMethod == 'BANK_TRANSFER' ? 'selected' : ''}>Bank transfer</option>
                        <option value="CASH" ${detail.paymentMethod == 'CASH' ? 'selected' : ''}>Cash</option>
                        <option value="OTHER" ${detail.paymentMethod == 'OTHER' ? 'selected' : ''}>Other</option>
                    </select>
                </label>
                <label>Payment reference <input type="text" name="paymentReference" required minlength="3" maxlength="60" placeholder="e.g. FT26103100123"></label>
                <label>Payment date <input type="date" name="paymentDate" required></label>
                <label>Notes <input type="text" name="notes" maxlength="200" value="<c:out value='${detail.notes}'/>"></label>
                <button type="submit" name="action" value="confirm" class="primary" onclick="return confirm('Confirm this payout? The commissions become PAID and can never be edited.');">Confirm payout</button>
                <button type="submit" name="action" value="cancel" formnovalidate onclick="return confirm('Cancel this draft payout?');">Cancel draft</button>
            </form>
            <p class="hint">Tinky does not move money. Pay the partner from your bank first, then type the reference and confirm.</p>
        </c:if>
        <c:if test="${detail.status == 'CONFIRMED'}"><p class="hint">A confirmed payout is locked. A correction needs a separate adjustment, which is not built yet.</p></c:if>
        <a class="btn" href="${ctx}/admin/channel-payouts">Close</a>
    </div>
</c:if>

<c:if test="${canWrite}">
    <div class="card">
        <h2>Approved commissions waiting for payment</h2>
        <c:choose>
            <c:when test="${empty groups}"><p class="hint">Nothing is waiting. Approve commissions first.</p></c:when>
            <c:otherwise>
                <c:forEach var="g" items="${groups}">
                    <form method="post" action="${ctx}/admin/channel-payouts">
                        <input type="hidden" name="action" value="draft">
                        <input type="hidden" name="affiliateId" value="${g.value[0].affiliateId}">
                        <h3><c:out value="${g.value[0].affiliateName}"/> - <fmt:formatNumber value="${groupTotals[g.key]}" pattern="#,##0"/> VND</h3>
                        <table>
                            <thead><tr><th></th><th>Platform</th><th>Platform order</th><th>Order date</th><th>Amount</th></tr></thead>
                            <tbody>
                            <c:forEach var="c" items="${g.value}">
                                <tr>
                                    <td><input type="checkbox" name="commissionId" value="${c.commissionId}" checked></td>
                                    <td><c:out value="${c.channel}"/></td>
                                    <td><c:out value="${c.platformOrderId}"/></td>
                                    <td><c:out value="${c.orderDate}"/></td>
                                    <td><fmt:formatNumber value="${c.payableAmount}" pattern="#,##0"/></td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                        <div class="filters">
                            <label>Payment method
                                <select name="method">
                                    <option value="BANK_TRANSFER">Bank transfer</option>
                                    <option value="CASH">Cash</option>
                                    <option value="OTHER">Other</option>
                                </select>
                            </label>
                            <label>Notes <input type="text" name="notes" maxlength="200"></label>
                            <button type="submit" class="primary">Create draft payout</button>
                        </div>
                    </form>
                </c:forEach>
            </c:otherwise>
        </c:choose>
    </div>
</c:if>

<div class="card">
    <form method="get" action="${ctx}/admin/channel-payouts" class="filters">
        <label>Search <input type="text" name="q" maxlength="60" placeholder="Partner, payout or payment reference" value="<c:out value='${q}'/>"></label>
        <label>Partner
            <select name="affiliateId">
                <option value="">All</option>
                <c:forEach var="a" items="${affiliates}"><option value="${a.affiliateId}" ${affiliateFilter == a.affiliateId ? 'selected' : ''}><c:out value="${a.name}"/></option></c:forEach>
            </select>
        </label>
        <label>Status
            <select name="status">
                <option value="">All</option>
                <c:forEach var="s" items="DRAFT,CONFIRMED,CANCELLED"><option value="${s}" ${statusFilter == s ? 'selected' : ''}>${s}</option></c:forEach>
            </select>
        </label>
        <button type="submit" class="primary">Filter</button>
        <a class="btn" href="${ctx}/admin/channel-payouts">Reset</a>
        <c:set var="qe"><c:out value="${q}"/></c:set>
        <a class="btn" href="${ctx}/admin/channel-payouts?export=1&amp;q=${qe}&amp;affiliateId=${affiliateFilter}&amp;status=${statusFilter}">Export CSV</a>
    </form>
    <table>
        <thead><tr><th>Payout</th><th>Partner</th><th>Items</th><th>Total</th><th>Method</th><th>Payment reference</th><th>Payment date</th><th>Status</th><th></th></tr></thead>
        <tbody>
        <c:forEach var="p" items="${payouts.items}">
            <tr>
                <td><c:out value="${p.reference}"/></td>
                <td><c:out value="${p.affiliateName}"/></td>
                <td>${p.itemCount}</td>
                <td><b><fmt:formatNumber value="${p.totalAmount}" pattern="#,##0"/></b></td>
                <td><c:out value="${p.paymentMethod}"/></td>
                <td><c:out value="${p.paymentReference}"/></td>
                <td><c:out value="${p.paymentDate}"/></td>
                <td><span class="pill ${p.status}">${p.status}</span></td>
                <td><a href="${ctx}/admin/channel-payouts?id=${p.payoutId}">Details</a></td>
            </tr>
        </c:forEach>
        <c:if test="${empty payouts.items}"><tr><td colspan="9">No payouts found.</td></tr></c:if>
        </tbody>
    </table>
    <c:set var="result" value="${payouts}"/>
    <c:set var="pagerBase" value="${ctx}/admin/channel-payouts?q=${qe}&amp;affiliateId=${affiliateFilter}&amp;status=${statusFilter}&amp;"/>
    <%@ include file="/WEB-INF/views/common/pager.jspf" %>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
