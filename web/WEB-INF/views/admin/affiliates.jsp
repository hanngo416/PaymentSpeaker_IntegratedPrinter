<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Affiliates"/>
<c:set var="nav" value="affiliates"/>
<c:set var="affTab" value="affiliates"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<%@ include file="/WEB-INF/views/common/affiliate-tabs.jspf" %>
<div class="card">
    <p class="hint">An affiliate sends customers to your store and earns a commission on their orders. At the counter, type the affiliate's referral code on the order screen. The code is printed on the customer's receipt, so everyone can see which code was credited.</p>
</div>

<div class="card narrow">
    <h2>New affiliate</h2>
    <form method="post" action="${ctx}/admin/affiliates">
        <input type="hidden" name="action" value="create">
            <label>Name
                <input type="text" name="name" minlength="2" maxlength="60" value="<c:out value='${form.name}'/>">
                <c:if test="${not empty errors.name}"><div class="field-error"><c:out value="${errors.name}"/></div></c:if>
            </label>
            <label>Email
                <input type="email" name="email" maxlength="100" value="<c:out value='${form.email}'/>">
                <span class="hint">An affiliate who logs in to the portal needs an account with this same email.</span>
                <c:if test="${not empty errors.email}"><div class="field-error"><c:out value="${errors.email}"/></div></c:if>
            </label>
        <label>Phone
            <input type="text" name="phone" maxlength="15" value="<c:out value='${form.phone}'/>">
            <c:if test="${not empty errors.phone}"><div class="field-error"><c:out value="${errors.phone}"/></div></c:if>
        </label>
        <label>Online-channel commission rate (%)
            <input type="text" name="rate" inputmode="decimal" maxlength="6" value="<c:out value='${formRate}'/>">
            <span class="hint">Only used for Shopee and TikTok sales in the sales book. From 0 to 100.</span>
            <c:if test="${not empty errors.rate}"><div class="field-error"><c:out value="${errors.rate}"/></div></c:if>
        </label>
        <label>Bank name <input type="text" name="bankName" maxlength="60" value="<c:out value='${form.bankName}'/>"></label>
        <label>Bank account <input type="text" name="bankAccount" maxlength="30" inputmode="numeric" value="<c:out value='${form.bankAccount}'/>"></label>
        <label>Account holder <input type="text" name="bankHolder" maxlength="100" value="<c:out value='${form.bankHolder}'/>">
            <c:if test="${not empty errors.bank}"><div class="field-error"><c:out value="${errors.bank}"/></div></c:if>
        </label>
        <button type="submit" class="primary">Create</button>
    </form>
</div>

<div class="card">
    <form method="get" action="${ctx}/admin/affiliates" class="filters">
        <label>Search
            <input type="text" name="q" maxlength="100" placeholder="Name, code, email or phone" value="<c:out value='${q}'/>">
        </label>
        <label>Status
            <select name="status">
                <option value="">All</option>
                <option value="ACTIVE" ${statusFilter == 'ACTIVE' ? 'selected' : ''}>Active</option>
                <option value="PENDING" ${statusFilter == 'PENDING' ? 'selected' : ''}>Pending</option>
                <option value="SUSPENDED" ${statusFilter == 'SUSPENDED' ? 'selected' : ''}>Suspended</option>
            </select>
        </label>
        <button type="submit" class="primary">Filter</button>
        <a class="btn" href="${ctx}/admin/affiliates">Reset</a>
        <a class="btn" href="${ctx}/admin/affiliates?export=1&amp;q=<c:out value='${q}'/>&amp;status=${statusFilter}">Export CSV</a>
    </form>
    <table>
        <thead>
        <tr><th>Code</th><th>Name</th><th>Status</th><th>Orders</th><th>Revenue</th><th>Commission</th><th>Action</th></tr>
        </thead>
        <tbody>
        <c:forEach var="a" items="${result.items}">
            <tr>
                <td><c:out value="${a.referralCode}"/></td>
                <td><a href="${ctx}/admin/affiliates?id=${a.affiliateId}"><c:out value="${a.name}"/></a></td>
                <td><span class="pill ${a.status}">${a.status}</span></td>
                <td>${a.orderCount}</td>
                <td><fmt:formatNumber value="${a.revenue}" pattern="#,##0"/> VND</td>
                <td><fmt:formatNumber value="${a.commissionTotal}" pattern="#,##0"/> VND</td>
                <td>
                    <a class="btn" href="${ctx}/admin/affiliates?id=${a.affiliateId}">View</a>
                    <form method="post" action="${ctx}/admin/affiliates" class="inline">
                        <input type="hidden" name="id" value="${a.affiliateId}">
                        <c:choose>
                            <c:when test="${a.status == 'ACTIVE'}"><button type="submit" name="action" value="suspend" onclick="return confirm('Suspend this affiliate? They cannot be credited with new orders.');">Suspend</button></c:when>
                            <c:when test="${a.status == 'PENDING'}"><button type="submit" name="action" value="approve">Approve</button></c:when>
                            <c:otherwise><button type="submit" name="action" value="activate">Activate</button></c:otherwise>
                        </c:choose>
                    </form>
                    <form method="post" action="${ctx}/admin/affiliates" class="inline"
                          data-name="<c:out value='${a.name}'/>"
                          onsubmit="return confirm('Delete affiliate ' + this.dataset.name + '? This cannot be undone.');">
                        <input type="hidden" name="id" value="${a.affiliateId}">
                        <button type="submit" name="action" value="delete">Delete</button>
                    </form>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty result.items}">
            <tr><td colspan="7">No affiliates found.</td></tr>
        </c:if>
        </tbody>
    </table>
    <c:set var="qe"><c:out value="${q}"/></c:set>
    <c:set var="pagerBase" value="${ctx}/admin/affiliates?q=${qe}&amp;status=${statusFilter}&amp;"/>
    <%@ include file="/WEB-INF/views/common/pager.jspf" %>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
