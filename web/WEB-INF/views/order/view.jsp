<%--
    Document   : view
    Created on : Oct 11, 2026
    Author     : PC
--%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Order ${order.orderCode}"/>
<c:set var="nav" value="orders"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<p><a href="${ctx}/orders/new">&larr; Create another order</a></p>

<div class="card narrow center">
<div id="box"
     data-code="<c:out value='${order.orderCode}'/>"
     data-status="<c:out value='${order.status}'/>"
     data-remaining="${order.remainingSeconds}"
     data-status-url="${ctx}/orders/status">

    <h2>Order: <c:out value="${order.orderCode}"/></h2>
    <div class="amount"><fmt:formatNumber value="${order.amount}" pattern="#,##0"/> VND</div>
    <p>Created by: <strong><c:out value="${order.createdByCode}"/></strong></p>
    <p><span class="badge <c:out value='${order.status}'/>" id="statusBadge"><c:out value="${order.status}"/></span></p>

    <c:choose>
        <c:when test="${order.status == 'PENDING'}">
            <img id="qr" src="<c:out value='${order.qrContent}'/>" alt="VietQR payment code">
            <div id="countdown">--:--</div>
            <p>Scan with your banking app. The transfer content is already filled in.</p>
            <form method="post" action="${ctx}/orders/cancel"
                  onsubmit="return confirm('Cancel this order?');">
                <input type="hidden" name="code" value="<c:out value='${order.orderCode}'/>">
                <button type="submit">Cancel order (customer pays cash)</button>
            </form>
        </c:when>
        <c:when test="${order.status == 'PAID'}">
            <div class="result PAID">
                <h2>&#10004; PAYMENT RECEIVED</h2>
                <div class="big"><fmt:formatNumber value="${order.amount}" pattern="#,##0"/> VND</div>
            </div>
            <a class="next" href="${ctx}/orders/new">New order</a>
        </c:when>
        <c:when test="${order.status == 'EXPIRED'}"><div class="result EXPIRED"><h2>Order expired</h2></div></c:when>
        <c:otherwise><div class="result CANCELLED"><h2>Order cancelled</h2></div></c:otherwise>
    </c:choose>
</div>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
<script src="${ctx}/assets/js/order-view.js"></script>
</body>
</html>
