<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Refunds"/>
<c:set var="nav" value="refunds"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<div class="card">
    <h1>Refunds</h1>
    <c:if test="${not empty message}"><div class="msg"><c:out value="${message}"/></div></c:if>
    <p class="hint">Money that reached the account but is not kept for an order. Send it back to the customer by bank transfer, then enter the bank reference and mark it done.</p>
    <form method="get" action="${ctx}/refunds" class="filters">
        <label>Status
            <select name="status">
                <option value="">All</option>
                <c:forEach var="s" items="${statuses}">
                    <option value="${s}" ${status == s ? 'selected' : ''}>${s}</option>
                </c:forEach>
            </select>
        </label>
        <button type="submit" class="primary">Filter</button>
    </form>
</div>

<div class="card">
    <table>
        <thead>
        <tr>
            <th>Time</th><th>Order</th><th>Amount (VND)</th><th>Reason</th><th>Status</th><th>Action</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="r" items="${refunds}">
            <tr>
                <td><fmt:formatDate value="${r.createdAt}" pattern="yyyy-MM-dd HH:mm:ss"/></td>
                <td><a href="${ctx}/orders/view?code=${r.orderCode}"><c:out value="${r.orderCode}"/></a><br><small><c:out value="${r.cashierCode}"/></small></td>
                <td class="num"><fmt:formatNumber value="${r.amount}" pattern="#,##0"/></td>
                <td><c:out value="${r.reason}"/></td>
                <td><span class="pill ${r.status == 'DONE' ? 'RESOLVED' : 'PENDING'}"><c:out value="${r.status}"/></span></td>
                <td class="wrap">
                    <c:choose>
                        <c:when test="${r.status == 'PENDING'}">
                            <form method="post" action="${ctx}/refunds" class="inline">
                                <input type="hidden" name="id" value="${r.refundId}">
                                <input type="text" name="note" maxlength="500" placeholder="bank reference of the refund" required>
                                <button type="submit" class="primary">Mark as sent back</button>
                            </form>
                        </c:when>
                        <c:otherwise>
                            <c:out value="${r.handledBy}"/>: <c:out value="${r.note}"/>
                        </c:otherwise>
                    </c:choose>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty refunds}">
            <tr><td colspan="6">No refunds match the filter.</td></tr>
        </c:if>
        </tbody>
    </table>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
