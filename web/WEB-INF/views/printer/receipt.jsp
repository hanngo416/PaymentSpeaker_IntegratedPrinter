<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="color-scheme" content="light only">
    <title>Receipt <c:out value="${receipt.orderCode}"/></title>
    <link rel="stylesheet" href="${ctx}/assets/css/app.css">
    <style>
        @page {
            size: 80mm auto;
            margin: 0;
        }
    </style>
</head>
<body class="receipt-screen-body">
<div class="receipt-screen-wrap">
    <!-- Non-printable top action toolbar -->
    <div class="receipt-toolbar no-print">
        <a href="${ctx}/transactions" class="btn-receipt-back">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="19" y1="12" x2="5" y2="12"></line>
                <polyline points="12 19 5 12 12 5"></polyline>
            </svg>
            Back to Transactions
        </a>
        <div class="receipt-toolbar-meta">
            <span class="paper-badge">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="6 9 6 2 18 2 18 9"></polyline>
                    <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"></path>
                    <rect x="6" y="14" width="12" height="8"></rect>
                </svg>
                POS 80mm
            </span>
            <button type="button" id="print-again" class="btn-receipt-print">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="6 9 6 2 18 2 18 9"></polyline>
                    <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"></path>
                    <rect x="6" y="14" width="12" height="8"></rect>
                </svg>
                Print Receipt
            </button>
        </div>
    </div>

    <!-- The Thermal Receipt Slip -->
    <div class="receipt-slip">
        <div class="receipt-header">
            <div class="receipt-brand">
                <span class="receipt-logo-mark">T</span>
                <span class="receipt-brand-name">TINKY POS</span>
            </div>
            <p class="receipt-store-desc">Smart Payment Speaker &amp; Cashier Register</p>
            <p class="receipt-store-hotline">Hotline: 1900 8888 &bull; store.tinky.vn</p>
            
            <div class="receipt-type-banner">
                <span class="receipt-type-title">PAYMENT RECEIPT</span>
                <c:if test="${reprint}">
                    <span class="receipt-copy-pill">&bull; REPRINT COPY &bull;</span>
                </c:if>
            </div>
        </div>

        <div class="receipt-dashed-line"></div>

        <div class="receipt-meta-grid">
            <div class="receipt-row">
                <span class="r-label">Order Code:</span>
                <span class="r-val r-bold"><c:out value="${receipt.orderCode}"/></span>
            </div>
            <div class="receipt-row">
                <span class="r-label">Date &amp; Time:</span>
                <span class="r-val"><fmt:formatDate value="${receipt.transTime}" pattern="yyyy-MM-dd HH:mm:ss"/></span>
            </div>
            <div class="receipt-row">
                <span class="r-label">Cashier:</span>
                <span class="r-val"><c:out value="${receipt.cashierCode}"/></span>
            </div>
            <c:if test="${not empty receipt.bankTransId}">
                <div class="receipt-row">
                    <span class="r-label">Bank Ref / Trace:</span>
                    <span class="r-val r-mono"><c:out value="${receipt.bankTransId}"/></span>
                </div>
            </c:if>
            <div class="receipt-row">
                <span class="r-label">Payment Method:</span>
                <span class="r-val">
                    <c:choose>
                        <c:when test="${receipt.cashAmount > 0}">Split (Cash + VietQR)</c:when>
                        <c:otherwise>VietQR Dynamic</c:otherwise>
                    </c:choose>
                </span>
            </div>
        </div>

        <div class="receipt-dashed-line"></div>

        <div class="receipt-amount-block">
            <div class="receipt-row r-sub">
                <span class="r-label">Subtotal</span>
                <span class="r-val"><fmt:formatNumber value="${receipt.orderAmount}" pattern="#,##0"/> ₫</span>
            </div>
            <c:if test="${receipt.cashAmount > 0}">
                <div class="receipt-row r-detail">
                    <span class="r-label">&bull; Cash at counter</span>
                    <span class="r-val"><fmt:formatNumber value="${receipt.cashAmount}" pattern="#,##0"/> ₫</span>
                </div>
                <div class="receipt-row r-detail">
                    <span class="r-label">&bull; VietQR transfer</span>
                    <span class="r-val"><fmt:formatNumber value="${receipt.amount}" pattern="#,##0"/> ₫</span>
                </div>
            </c:if>
            <div class="receipt-solid-line"></div>
            <div class="receipt-row r-total-row">
                <span class="r-total-label">TOTAL PAID</span>
                <span class="r-total-val"><fmt:formatNumber value="${receipt.orderAmount}" pattern="#,##0"/> ₫</span>
            </div>
        </div>

        <div class="receipt-status-box">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                <polyline points="22 4 12 14.01 9 11.01"></polyline>
            </svg>
            <span>TRANSACTION COMPLETED</span>
        </div>

        <div class="receipt-footer">
            <p class="r-thank">Thank you for your purchase!</p>
            <p class="r-engine">Tinky Payment Speaker &bull; Thermal Print Engine</p>
        </div>
    </div>

    <div class="receipt-screen-actions no-print" style="margin-top: 18px; text-align: center;">
        <a href="${ctx}/transactions" class="btn-receipt-back" style="box-shadow: 0 2px 8px rgba(0,0,0,0.08); padding: 9px 18px; font-size: 13px;">
            &larr; Return to Transactions
        </a>
    </div>
</div>
<script src="${ctx}/assets/js/receipt.js"></script>
</body>
</html>
