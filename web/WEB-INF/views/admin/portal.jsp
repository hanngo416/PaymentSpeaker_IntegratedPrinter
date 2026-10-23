<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="My affiliate page"/>
<c:set var="nav" value="portal"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<div class="card">
    <h1>My affiliate page</h1>
    <c:if test="${not empty message}"><div class="msg"><c:out value="${message}"/></div></c:if>
    <c:choose>
        <c:when test="${empty me}"><p>No affiliate profile is linked to your account. Ask the store owner to add you as an affiliate with this email address.</p></c:when>
        <c:otherwise>
            <p>
                <b><c:out value="${me.name}"/></b> <span class="pill ${me.status}">${me.status}</span>
                - referral code <b><c:out value="${me.referralCode}"/></b>
            </p>
            <div>
                <a class="btn ${tab == 'dashboard' ? 'primary' : ''}" href="${ctx}/affiliate/portal?tab=dashboard">Dashboard</a>
                <a class="btn ${tab == 'links' ? 'primary' : ''}" href="${ctx}/affiliate/portal?tab=links">My referral code</a>
                <a class="btn ${tab == 'catalog' ? 'primary' : ''}" href="${ctx}/affiliate/portal?tab=catalog">Product catalog</a>
                <a class="btn ${tab == 'orders' ? 'primary' : ''}" href="${ctx}/affiliate/portal?tab=orders">My orders</a>
                <a class="btn ${tab == 'commissions' ? 'primary' : ''}" href="${ctx}/affiliate/portal?tab=commissions">My commissions</a>
                <a class="btn ${tab == 'wallet' ? 'primary' : ''}" href="${ctx}/affiliate/portal?tab=wallet">My wallet</a>
                <a class="btn ${tab == 'profile' ? 'primary' : ''}" href="${ctx}/affiliate/portal?tab=profile">Profile</a>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<c:if test="${not empty me}">
    <c:if test="${me.status != 'ACTIVE'}">
        <div class="card"><div class="alert-line">Your account is <b>${me.status}</b>. New orders are not credited to you. Your earlier commissions stay visible.</div></div>
    </c:if>

    <c:if test="${tab == 'dashboard'}">
        <div class="kpis">
            <div class="card"><span class="kpi-label">Referred orders</span><div class="kpi-value">${me.orderCount}</div><p class="kpi-sub">Completed</p></div>
            <div class="card"><span class="kpi-label">Generated revenue</span><div class="kpi-value"><fmt:formatNumber value="${me.revenue}" pattern="#,##0"/></div><p class="kpi-sub">VND</p></div>
            <div class="card"><span class="kpi-label">Pending commission</span><div class="kpi-value"><fmt:formatNumber value="${wallet.pending}" pattern="#,##0"/></div><p class="kpi-sub">Waiting for approval or the return window</p></div>
            <div class="card"><span class="kpi-label">Available balance</span><div class="kpi-value accent"><fmt:formatNumber value="${wallet.available}" pattern="#,##0"/></div><p class="kpi-sub">Ready to withdraw</p></div>
            <div class="card"><span class="kpi-label">Total earnings</span><div class="kpi-value"><fmt:formatNumber value="${wallet.pending + wallet.available + wallet.paid}" pattern="#,##0"/></div><p class="kpi-sub">Pending + available + paid</p></div>
        </div>
        <div class="card">
            <h2>Recent activity</h2>
            <table>
                <tbody>
                <c:forEach var="c" items="${commissions}" end="4">
                    <tr><td><fmt:formatDate value="${c.createdAt}" pattern="yyyy-MM-dd"/></td><td><c:out value="${c.orderCode}"/></td><td><fmt:formatNumber value="${c.amount}" pattern="#,##0"/> VND</td><td><span class="pill ${c.status}">${c.status}</span></td></tr>
                </c:forEach>
                <c:if test="${empty commissions}"><tr><td>No referred orders yet. Share your code to get started.</td></tr></c:if>
                </tbody>
            </table>
        </div>
    </c:if>

    <c:if test="${tab == 'links'}">
        <div class="card narrow">
            <h2>Your referral code</h2>
            <p style="font-size: 2rem; font-weight: 800;"><c:out value="${me.referralCode}"/></p>
            <button type="button" onclick="navigator.clipboard && navigator.clipboard.writeText('<c:out value="${me.referralCode}"/>'); this.textContent = 'Copied';">Copy code</button>
            <p class="hint">Give this code to your customers. They (or the cashier) type it on the order screen so the sale is credited to you. Staff affiliates cannot be credited for orders they sell themselves.</p>
        </div>
    </c:if>

    <c:if test="${tab == 'catalog'}">
        <div class="card">
            <h2>Products you can promote</h2>
            <table>
                <thead><tr><th>Product</th><th>Price</th><th>Commission rule</th><th>Estimated earning</th></tr></thead>
                <tbody>
                <c:forEach var="row" items="${catalog}">
                    <tr>
                        <td><c:out value="${row[0].productName}"/></td>
                        <td><fmt:formatNumber value="${row[0].price}" pattern="#,##0"/> VND</td>
                        <td><c:out value="${empty row[2] ? 'Not in a running campaign' : row[2]}"/></td>
                        <td><fmt:formatNumber value="${row[1]}" pattern="#,##0"/> VND</td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
            <p class="hint">Estimates are for one unit. The final commission is worked out when the order is saved and approved.</p>
        </div>
    </c:if>

    <c:if test="${tab == 'orders' || tab == 'commissions'}">
        <c:if test="${tab == 'commissions'}">
            <div class="kpis">
                <div class="card"><span class="kpi-label">Pending</span><div class="kpi-value"><fmt:formatNumber value="${totals.PENDING}" pattern="#,##0"/></div></div>
                <div class="card"><span class="kpi-label">Approved</span><div class="kpi-value"><fmt:formatNumber value="${totals.APPROVED}" pattern="#,##0"/></div></div>
                <div class="card"><span class="kpi-label">Available</span><div class="kpi-value accent"><fmt:formatNumber value="${totals.AVAILABLE}" pattern="#,##0"/></div></div>
                <div class="card"><span class="kpi-label">Paid</span><div class="kpi-value"><fmt:formatNumber value="${totals.PAID}" pattern="#,##0"/></div></div>
                <div class="card"><span class="kpi-label">Reversed</span><div class="kpi-value"><fmt:formatNumber value="${totals.REVERSED}" pattern="#,##0"/></div></div>
            </div>
        </c:if>
        <div class="card">
            <h2><c:out value="${tab == 'orders' ? 'My referred orders' : 'Commission history'}"/></h2>
            <table>
                <thead><tr><th>Date</th><th>Order</th><th>Order value</th><th>Commission</th><th>Status</th><th>Note</th></tr></thead>
                <tbody>
                <c:forEach var="c" items="${commissions}">
                    <tr>
                        <td><fmt:formatDate value="${c.createdAt}" pattern="yyyy-MM-dd"/></td>
                        <td><c:out value="${c.orderCode}"/></td>
                        <td><fmt:formatNumber value="${c.orderAmount}" pattern="#,##0"/></td>
                        <td><fmt:formatNumber value="${c.amount}" pattern="#,##0"/></td>
                        <td><span class="pill ${c.status == 'ON_HOLD' ? 'PENDING' : c.status}">${c.status == 'ON_HOLD' ? 'PENDING' : c.status}</span></td>
                        <td><c:if test="${c.status == 'REJECTED' || c.status == 'REVERSED'}"><c:out value="${c.reason}"/></c:if></td>
                    </tr>
                </c:forEach>
                <c:if test="${empty commissions}"><tr><td colspan="6">Nothing yet.</td></tr></c:if>
                </tbody>
            </table>
            <p class="hint">You only see your own referred orders. Customer details are never shown.</p>
        </div>
    </c:if>

    <c:if test="${tab == 'wallet'}">
        <div class="kpis">
            <div class="card"><span class="kpi-label">Available balance</span><div class="kpi-value accent"><fmt:formatNumber value="${wallet.available}" pattern="#,##0"/></div><p class="kpi-sub">Minimum to withdraw: <fmt:formatNumber value="${payoutMin}" pattern="#,##0"/> VND</p></div>
            <div class="card"><span class="kpi-label">Pending balance</span><div class="kpi-value"><fmt:formatNumber value="${wallet.pending}" pattern="#,##0"/></div></div>
            <div class="card"><span class="kpi-label">Total paid</span><div class="kpi-value"><fmt:formatNumber value="${wallet.paid}" pattern="#,##0"/></div></div>
        </div>
        <div class="two-col">
            <div class="card">
                <h2>Bank information</h2>
                <form method="post" action="${ctx}/affiliate/portal">
                    <input type="hidden" name="action" value="bank">
                    <label>Bank name <input type="text" name="bankName" required maxlength="60" value="<c:out value='${me.bankName}'/>"></label>
                    <label>Account number <input type="text" name="bankAccount" required maxlength="30" inputmode="numeric" value="<c:out value='${me.bankAccount}'/>"></label>
                    <label>Account holder <input type="text" name="bankHolder" required maxlength="100" value="<c:out value='${me.bankHolder}'/>"></label>
                    <button type="submit" class="primary">Save bank details</button>
                </form>
                <form method="post" action="${ctx}/affiliate/portal" style="margin-top: 16px;">
                    <input type="hidden" name="action" value="payout">
                    <button type="submit" class="primary" ${openRequest || wallet.available < payoutMin || empty me.bankAccount ? 'disabled' : ''}
                            onclick="return confirm('Request a payout of the available balance?');">Request payout</button>
                    <c:if test="${openRequest}"><span class="hint">A request is already waiting for the store.</span></c:if>
                </form>
            </div>
            <div class="card">
                <h2>Payout history</h2>
                <table>
                    <thead><tr><th>Date</th><th>Net</th><th>Reference</th><th>Status</th></tr></thead>
                    <tbody>
                    <c:forEach var="p" items="${payouts}">
                        <tr>
                            <td><fmt:formatDate value="${empty p.paidAt ? p.requestedAt : p.paidAt}" pattern="yyyy-MM-dd"/></td>
                            <td><fmt:formatNumber value="${p.net}" pattern="#,##0"/></td>
                            <td><c:out value="${p.reference}"/></td>
                            <td><span class="pill ${p.status}">${p.status}</span></td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty payouts}"><tr><td colspan="4">No payouts yet.</td></tr></c:if>
                    </tbody>
                </table>
                <p class="hint">Deductions such as tax withholding are shown by the store when the payout is made.</p>
            </div>
        </div>
    </c:if>

    <c:if test="${tab == 'profile'}">
        <div class="card narrow">
            <h2>Profile</h2>
            <table class="kv">
                <tr><td>Name</td><td><c:out value="${me.name}"/></td></tr>
                <tr><td>Phone</td><td><c:out value="${me.phone}"/></td></tr>
                <tr><td>Email</td><td><c:out value="${me.email}"/></td></tr>
                <tr><td>Bank account</td><td><c:out value="${me.bankName}"/> <c:out value="${me.bankAccount}"/></td></tr>
                <tr><td>Status</td><td><span class="pill ${me.status}">${me.status}</span></td></tr>
                <tr><td>Registered</td><td><fmt:formatDate value="${me.createdAt}" pattern="yyyy-MM-dd"/></td></tr>
            </table>
            <p class="hint">To change your name or phone, ask the store owner.</p>
        </div>
    </c:if>
</c:if>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
