<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:out value="${report.title}"/></title>
    <link rel="stylesheet" href="${ctx}/assets/css/app.css">
</head>
<body class="receipt-page">
<div class="receipt">
    <h1>TINKY</h1>
    <p class="receipt-sub"><c:out value="${report.title}"/></p>
    <p class="receipt-sub"><c:out value="${report.period}"/></p>
    <hr>
    <dl>
        <dt>Printed by</dt><dd><c:out value="${printedBy}"/></dd>
        <dt>Printed at</dt><dd><fmt:formatDate value="${printedAt}" pattern="yyyy-MM-dd HH:mm"/></dd>
        <c:if test="${report.shift != null}">
            <dt>Opened by</dt><dd><c:out value="${report.shift.openedBy}"/></dd>
            <c:if test="${not report.shift.open}">
                <dt>Closed by</dt><dd><c:out value="${report.shift.closedBy}"/></dd>
            </c:if>
            <c:if test="${report.shift.open}"><dt>Status</dt><dd>NOT CLOSED YET</dd></c:if>
        </c:if>
    </dl>
    <hr>
    <dl>
        <dt>Paid orders</dt><dd>${report.summary.orderCount}</dd>
        <dt>Order total</dt><dd><fmt:formatNumber value="${report.summary.total}" pattern="#,##0"/></dd>
    </dl>
    <hr>
    <dl>
        <dt>Cash expected</dt><dd><fmt:formatNumber value="${report.summary.cash}" pattern="#,##0"/></dd>
        <c:if test="${report.shift != null || not empty report.shifts}">
            <dt>Cash counted</dt>
            <dd><c:choose>
                <c:when test="${report.countedCash != null}"><fmt:formatNumber value="${report.countedCash}" pattern="#,##0"/></c:when>
                <c:otherwise>-</c:otherwise>
            </c:choose></dd>
            <c:if test="${report.cashDiff != null}">
                <dt>Difference</dt>
                <dd><fmt:formatNumber value="${report.cashDiff}" pattern="#,##0"/><c:if test="${report.cashDiff.signum() != 0}"> (CHECK)</c:if></dd>
            </c:if>
        </c:if>
    </dl>
    <hr>
    <dl>
        <dt>QR expected</dt><dd><fmt:formatNumber value="${report.summary.transfer}" pattern="#,##0"/></dd>
        <dt>Account received</dt><dd><fmt:formatNumber value="${report.summary.bankReceived}" pattern="#,##0"/></dd>
        <dt>Difference</dt>
        <dd><fmt:formatNumber value="${report.summary.bankDiff}" pattern="#,##0"/><c:if test="${report.summary.bankDiff.signum() != 0}"> (CHECK)</c:if></dd>
    </dl>
    <c:if test="${report.summary.pendingOrders > 0}">
        <p class="receipt-sub">${report.summary.pendingOrders} order(s) still waiting, not counted</p>
    </c:if>
    <c:if test="${not empty report.shifts}">
        <hr>
        <p class="receipt-sub">Shifts of the day</p>
        <dl>
            <c:forEach var="s" items="${report.shifts}">
                <dt><fmt:formatDate value="${s.openedAt}" pattern="HH:mm"/>-<c:choose><c:when test="${s.open}">now</c:when><c:otherwise><fmt:formatDate value="${s.closedAt}" pattern="HH:mm"/></c:otherwise></c:choose> <c:out value="${s.openedBy}"/></dt>
                <dd><c:choose>
                    <c:when test="${s.countedCash != null}"><fmt:formatNumber value="${s.countedCash}" pattern="#,##0"/></c:when>
                    <c:otherwise>-</c:otherwise>
                </c:choose></dd>
            </c:forEach>
        </dl>
    </c:if>
    <c:if test="${not empty report.rows}">
        <hr>
        <dl>
            <c:forEach var="r" items="${report.rows}">
                <dt><c:out value="${r.label}"/> (${r.orderCount})</dt>
                <dd><fmt:formatNumber value="${r.total}" pattern="#,##0"/></dd>
            </c:forEach>
        </dl>
    </c:if>
    <hr>
    <p class="receipt-sub">Amounts in VND</p>
    <button type="button" id="print-again" class="no-print">Print</button>
</div>
<script src="${ctx}/assets/js/receipt.js"></script>
</body>
</html>
