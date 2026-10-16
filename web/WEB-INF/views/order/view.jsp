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

<div class="order-view-wrapper">
    <div class="order-view-nav">
        <a href="${ctx}/orders/new" class="order-back-link">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="15 18 9 12 15 6"></polyline>
            </svg>
            Back to POS Counter
        </a>
        <span class="order-nav-code">#<c:out value="${order.orderCode}"/></span>
    </div>

    <c:if test="${param.cancel == 'failed'}">
        <div class="error" style="margin-bottom: 16px;">
            The order could not be cancelled: it is no longer waiting for payment.
        </div>
    </c:if>

    <div class="order-receipt-card" id="box"
         data-code="<c:out value='${order.orderCode}'/>"
         data-status="<c:out value='${order.status}'/>"
         data-remaining="${order.remainingSeconds}"
         data-status-url="${ctx}/orders/status">

        <c:choose>
            <%-- STATE 1: COMPLETED / PAID --%>
            <c:when test="${order.status == 'COMPLETED' || order.status == 'PAID'}">
                <div class="order-hero-banner success">
                    <div class="order-status-icon success">
                        <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                            <polyline points="20 6 9 17 4 12"></polyline>
                        </svg>
                    </div>
                    <h2 class="order-hero-title">Payment Completed</h2>
                    <p class="order-hero-subtitle">Order successfully verified and settled</p>
                    <div class="order-hero-amount">
                        <fmt:formatNumber value="${order.amount}" pattern="#,##0"/><span class="currency-unit">₫</span>
                    </div>
                    <div>
                        <span class="pill COMPLETED">COMPLETED</span>
                    </div>
                </div>

                <div class="order-receipt-body">
                    <div class="order-summary-box">
                        <div class="summary-row">
                            <span class="label">Order Code</span>
                            <span class="val mono"><c:out value="${order.orderCode}"/></span>
                        </div>
                        <div class="summary-row">
                            <span class="label">Payment Method</span>
                            <span class="val">
                                <c:choose>
                                    <c:when test="${order.cashAmount >= order.amount}">
                                        💵 100% Cash
                                    </c:when>
                                    <c:when test="${order.cashAmount > 0}">
                                        ⚡ Split (Cash + VietQR)
                                    </c:when>
                                    <c:otherwise>
                                        📱 100% VietQR Transfer
                                    </c:otherwise>
                                </c:choose>
                            </span>
                        </div>
                        <c:if test="${order.cashAmount > 0}">
                            <div class="summary-row sub-row">
                                <span class="label">&bull; Cash Collected</span>
                                <span class="val"><fmt:formatNumber value="${order.cashAmount}" pattern="#,##0"/> ₫</span>
                            </div>
                            <div class="summary-row sub-row">
                                <span class="label">&bull; QR Transfer</span>
                                <span class="val"><fmt:formatNumber value="${order.transferAmount}" pattern="#,##0"/> ₫</span>
                            </div>
                        </c:if>
                        <div class="summary-row">
                            <span class="label">Cashier</span>
                            <span class="val"><strong><c:out value="${order.createdByCode}"/></strong></span>
                        </div>
                        <c:if test="${not empty order.createdAt}">
                            <div class="summary-row">
                                <span class="label">Created At</span>
                                <span class="val"><fmt:formatDate value="${order.createdAt}" pattern="dd/MM/yyyy HH:mm:ss"/></span>
                            </div>
                        </c:if>
                    </div>

                    <div class="order-card-actions">
                        <a href="${ctx}/orders/new" class="btn primary btn-pos">
                            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                                <line x1="12" y1="5" x2="12" y2="19"></line>
                                <line x1="5" y1="12" x2="19" y2="12"></line>
                            </svg>
                            Create New Order
                        </a>
                        <a href="${ctx}/transactions" class="btn secondary">
                            View All Transactions
                        </a>
                    </div>
                </div>
            </c:when>

            <%-- STATE 2: PENDING --%>
            <c:when test="${order.status == 'PENDING'}">
                <div class="order-hero-banner pending">
                    <div class="order-status-icon pending">
                        <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                            <circle cx="12" cy="12" r="10"></circle>
                            <polyline points="12 6 12 12 16 14"></polyline>
                        </svg>
                    </div>
                    <h2 class="order-hero-title">Waiting for Payment</h2>
                    <p class="order-hero-subtitle">Customer scans the QR code to transfer</p>
                    <div class="order-hero-amount">
                        <fmt:formatNumber value="${order.transferAmount > 0 ? order.transferAmount : order.amount}" pattern="#,##0"/><span class="currency-unit">₫</span>
                    </div>
                    <div class="countdown-pill">
                        <span class="pulse-dot"></span>
                        <span>Expires in:</span>
                        <strong id="countdown">--:--</strong>
                    </div>
                </div>

                <div class="order-receipt-body">
                    <c:if test="${order.cashAmount > 0}">
                        <div class="split-info-pill-bar">
                            <span>Total: <strong><fmt:formatNumber value="${order.amount}" pattern="#,##0"/> ₫</strong></span>
                            <span class="split-sep">&bull;</span>
                            <span>Cash received: <strong><fmt:formatNumber value="${order.cashAmount}" pattern="#,##0"/> ₫</strong></span>
                        </div>
                    </c:if>

                    <div class="order-qr-frame">
                        <img id="qr" src="<c:out value='${order.qrContent}'/>" alt="VietQR payment code">
                        <p class="qr-guide-text">Scan with banking app. Transfer note: <strong><c:out value="${order.orderCode}"/></strong></p>
                    </div>

                    <div class="order-summary-box">
                        <div class="summary-row">
                            <span class="label">Order Code</span>
                            <span class="val mono"><c:out value="${order.orderCode}"/></span>
                        </div>
                        <div class="summary-row">
                            <span class="label">Cashier</span>
                            <span class="val"><strong><c:out value="${order.createdByCode}"/></strong></span>
                        </div>
                    </div>

                    <div class="order-card-actions">
                        <form method="post" action="${ctx}/orders/cancel" style="width: 100%;"
                              onsubmit="return confirm('Cancel this order?\n\nIf the customer has just transferred, the money may still be on its way. Only cancel if nothing was transferred.');">
                            <input type="hidden" name="code" value="<c:out value='${order.orderCode}'/>">
                            <button type="submit" class="btn btn-outline-danger">Cancel Order</button>
                        </form>
                    </div>
                </div>
            </c:when>

            <%-- STATE 3: CANCELLED / EXPIRED --%>
            <c:otherwise>
                <div class="order-hero-banner cancelled">
                    <div class="order-status-icon cancelled">
                        <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="15" y1="9" x2="9" y2="15"></line>
                            <line x1="9" y1="9" x2="15" y2="15"></line>
                        </svg>
                    </div>
                    <h2 class="order-hero-title">Order Cancelled</h2>
                    <p class="order-hero-subtitle">
                        <c:choose>
                            <c:when test="${order.status == 'EXPIRED'}">Payment window timed out</c:when>
                            <c:otherwise>Order was cancelled by cashier</c:otherwise>
                        </c:choose>
                    </p>
                    <div class="order-hero-amount cancelled-amount">
                        <fmt:formatNumber value="${order.amount}" pattern="#,##0"/><span class="currency-unit">₫</span>
                    </div>
                    <div>
                        <span class="pill CANCELLED">CANCELLED</span>
                    </div>
                </div>

                <div class="order-receipt-body">
                    <div class="order-summary-box">
                        <div class="summary-row">
                            <span class="label">Order Code</span>
                            <span class="val mono"><c:out value="${order.orderCode}"/></span>
                        </div>
                        <div class="summary-row">
                            <span class="label">Cashier</span>
                            <span class="val"><strong><c:out value="${order.createdByCode}"/></strong></span>
                        </div>
                    </div>

                    <div class="order-card-actions">
                        <a href="${ctx}/orders/new" class="btn primary btn-pos">
                            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                                <line x1="12" y1="5" x2="12" y2="19"></line>
                                <line x1="5" y1="12" x2="19" y2="12"></line>
                            </svg>
                            Create New Order
                        </a>
                        <a href="${ctx}/home" class="btn secondary">
                            Back to Home
                        </a>
                    </div>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
<script src="${ctx}/assets/js/order-view.js"></script>
</body>
</html>
