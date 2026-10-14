<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Receipt <c:out value="${receipt.orderCode}"/></title>
    <link rel="stylesheet" href="${ctx}/assets/css/app.css">
</head>
<body class="receipt-page">
<div class="receipt">
    <h1>TINKY</h1>
    <p class="receipt-sub">Payment receipt<c:if test="${reprint}"> (copy)</c:if></p>
    <hr>
    <dl>
        <dt>Order</dt><dd><c:out value="${receipt.orderCode}"/></dd>
        <dt>Time</dt><dd><fmt:formatDate value="${receipt.transTime}" pattern="yyyy-MM-dd HH:mm:ss"/></dd>
        <dt>Cashier</dt><dd><c:out value="${receipt.cashierCode}"/></dd>
        <dt>Bank ref</dt><dd><c:out value="${receipt.bankTransId}"/></dd>
    </dl>
    <hr>
    <dl>
        <dt>Order total</dt><dd><fmt:formatNumber value="${receipt.orderAmount}" pattern="#,##0"/> VND</dd>
        <c:if test="${receipt.cashAmount > 0}">
            <dt>Cash paid</dt><dd><fmt:formatNumber value="${receipt.cashAmount}" pattern="#,##0"/> VND</dd>
            <dt>Bank transfer (QR)</dt><dd><fmt:formatNumber value="${receipt.amount}" pattern="#,##0"/> VND</dd>
        </c:if>
        <c:if test="${receipt.cashAmount == 0}">
            <dt>Payment (QR)</dt><dd><fmt:formatNumber value="${receipt.amount}" pattern="#,##0"/> VND</dd>
        </c:if>
        <dt>Total paid</dt><dd><fmt:formatNumber value="${receipt.orderPaid}" pattern="#,##0"/> VND</dd>
    </dl>
    <hr>
    <p class="receipt-sub">Thank you!</p>
    <button type="button" id="print-again" class="no-print">Print</button>
</div>
<script src="${ctx}/assets/js/receipt.js"></script>
</body>
</html>
