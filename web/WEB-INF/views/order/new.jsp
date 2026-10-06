<%--
    Document   : new
    Created on : Oct 11, 2026
    Author     : PC
--%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Create payment order"/>
<c:set var="nav" value="orders"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<div class="card narrow">
    <h1>Create payment order</h1>

    <c:if test="${not empty error}">
        <p class="error"><c:out value="${error}"/></p>
    </c:if>

    <form method="post" action="${ctx}/orders/new">
        <label for="amount">Amount (VND)</label>
        <input id="amount" class="amount-input" type="text" name="amount" inputmode="numeric" autocomplete="off"
               value="<c:out value='${amount}'/>" placeholder="e.g. 50000" required autofocus>
        <div class="quick">
            <button type="button" data-amount="20000">20.000</button>
            <button type="button" data-amount="50000">50.000</button>
            <button type="button" data-amount="100000">100.000</button>
            <button type="button" data-amount="200000">200.000</button>
        </div>
        <button class="go primary" type="submit">Generate QR code</button>
    </form>
</div>

<div class="card">
    <h2>Recent orders</h2>
    <c:choose>
        <c:when test="${empty recentOrders}">
            <p>No orders yet.</p>
        </c:when>
        <c:otherwise>
            <table>
                <thead>
                <tr><th>Order</th><th>Amount</th><th>Status</th><th>Created by</th></tr>
                </thead>
                <tbody>
                <c:forEach var="o" items="${recentOrders}">
                    <tr>
                        <td><a href="${ctx}/orders/view?code=<c:out value='${o.orderCode}'/>"><c:out value="${o.orderCode}"/></a></td>
                        <td><fmt:formatNumber value="${o.amount}" pattern="#,##0"/> VND</td>
                        <td><span class="pill ${o.status}"><c:out value="${o.status}"/></span></td>
                        <td><c:out value="${o.createdByCode}"/></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
<script>
    document.querySelectorAll('.quick button').forEach(function (btn) {
        btn.addEventListener('click', function () {
            document.getElementById('amount').value = btn.dataset.amount;
        });
    });
</script>
</body>
</html>
