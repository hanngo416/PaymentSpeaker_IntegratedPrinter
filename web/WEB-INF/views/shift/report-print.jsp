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
    <title><c:out value="${report.title}"/></title>
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
    <div class="receipt-toolbar no-print">
        <c:choose>
            <c:when test="${sessionScope.loginUser.role == 'AUDITOR'}">
                <a href="${ctx}/invoices" class="btn-receipt-back">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <line x1="19" y1="12" x2="5" y2="12"></line>
                        <polyline points="12 19 5 12 12 5"></polyline>
                    </svg>
                    Back to Invoices
                </a>
            </c:when>
            <c:otherwise>
                <a href="${ctx}/shift" class="btn-receipt-back">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <line x1="19" y1="12" x2="5" y2="12"></line>
                        <polyline points="12 19 5 12 12 5"></polyline>
                    </svg>
                    Back to Shifts
                </a>
            </c:otherwise>
        </c:choose>
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
                Print Report
            </button>
        </div>
    </div>

    <div class="receipt-slip">
        <div class="receipt-header">
            <div class="receipt-brand">
                <span class="receipt-logo-mark">T</span>
                <span class="receipt-brand-name">TINKY POS</span>
            </div>
            <p class="receipt-store-desc">Smart Payment Speaker &amp; Cashier Register</p>
            <p class="receipt-store-hotline">Hotline: 1900 8888 &bull; store.tinky.vn</p>
            
            <div class="receipt-type-banner">
                <span class="receipt-type-title"><c:out value="${report.title}"/></span>
                <span class="receipt-period-text"><c:out value="${report.period}"/></span>
            </div>
        </div>

        <div class="receipt-dashed-line"></div>

        <div class="receipt-meta-grid">
            <div class="receipt-row">
                <span class="r-label">Printed by:</span>
                <span class="r-val r-bold"><c:out value="${printedBy}"/></span>
            </div>
            <div class="receipt-row">
                <span class="r-label">Printed at:</span>
                <span class="r-val"><fmt:formatDate value="${printedAt}" pattern="yyyy-MM-dd HH:mm"/></span>
            </div>
            <c:if test="${report.shift != null}">
                <div class="receipt-row">
                    <span class="r-label">Shift ID:</span>
                    <span class="r-val r-mono"><c:out value="${report.shift.id}"/></span>
                </div>
                <div class="receipt-row">
                    <span class="r-label">Opened by:</span>
                    <span class="r-val"><c:out value="${report.shift.openedBy}"/></span>
                </div>
                <c:if test="${not report.shift.open}">
                    <div class="receipt-row">
                        <span class="r-label">Closed by:</span>
                        <span class="r-val"><c:out value="${report.shift.closedBy}"/></span>
                    </div>
                </c:if>
                <div class="receipt-row">
                    <span class="r-label">Shift Status:</span>
                    <span class="r-val ${report.shift.open ? 'r-bold' : ''}">
                        <c:choose>
                            <c:when test="${report.shift.open}">OPEN (ACTIVE)</c:when>
                            <c:otherwise>CLOSED</c:otherwise>
                        </c:choose>
                    </span>
                </div>
            </c:if>
        </div>

        <div class="receipt-dashed-line"></div>

        <div class="receipt-section-header">&bull; SALES SUMMARY &bull;</div>
        <div class="receipt-amount-block">
            <div class="receipt-row r-sub">
                <span class="r-label">Completed Orders</span>
                <span class="r-val r-bold">${report.summary.orderCount}</span>
            </div>
            <div class="receipt-row r-sub">
                <span class="r-label">Discounts Given</span>
                <span class="r-val"><fmt:formatNumber value="${report.summary.discount}" pattern="#,##0"/> ₫</span>
            </div>
            <div class="receipt-solid-line"></div>
            <div class="receipt-row r-total-row">
                <span class="r-total-label">GROSS REVENUE</span>
                <span class="r-total-val"><fmt:formatNumber value="${report.summary.total}" pattern="#,##0"/> ₫</span>
            </div>
        </div>

        <div class="receipt-dashed-line"></div>

        <div class="receipt-section-header">&bull; CASH DRAWER AUDIT &bull;</div>
        <div class="receipt-meta-grid">
            <div class="receipt-row">
                <span class="r-label">Expected Cash:</span>
                <span class="r-val"><fmt:formatNumber value="${report.summary.cash}" pattern="#,##0"/> ₫</span>
            </div>
            <c:if test="${report.shift != null || not empty report.shifts}">
                <div class="receipt-row">
                    <span class="r-label">Drawer Counted:</span>
                    <span class="r-val r-bold">
                        <c:choose>
                            <c:when test="${report.countedCash != null}">
                                <fmt:formatNumber value="${report.countedCash}" pattern="#,##0"/> ₫
                            </c:when>
                            <c:otherwise>-</c:otherwise>
                        </c:choose>
                    </span>
                </div>
                <c:if test="${report.cashDiff != null}">
                    <div class="receipt-row">
                        <span class="r-label">Difference:</span>
                        <span class="r-val r-bold ${report.cashDiff.signum() != 0 ? 'r-diff-mismatch' : ''}">
                            <fmt:formatNumber value="${report.cashDiff}" pattern="#,##0"/> ₫
                            <c:if test="${report.cashDiff.signum() != 0}"> [CHECK]</c:if>
                        </span>
                    </div>
                </c:if>
            </c:if>
        </div>

        <div class="receipt-dashed-line"></div>

        <div class="receipt-section-header">&bull; VIETQR BANK AUDIT &bull;</div>
        <div class="receipt-meta-grid">
            <div class="receipt-row">
                <span class="r-label">Expected QR:</span>
                <span class="r-val"><fmt:formatNumber value="${report.summary.transfer}" pattern="#,##0"/> ₫</span>
            </div>
            <div class="receipt-row">
                <span class="r-label">Bank Received:</span>
                <span class="r-val"><fmt:formatNumber value="${report.summary.bankReceived}" pattern="#,##0"/> ₫</span>
            </div>
            <div class="receipt-row">
                <span class="r-label">Difference:</span>
                <span class="r-val r-bold ${report.summary.bankDiff.signum() != 0 ? 'r-diff-mismatch' : ''}">
                    <fmt:formatNumber value="${report.summary.bankDiff}" pattern="#,##0"/> ₫
                    <c:if test="${report.summary.bankDiff.signum() != 0}"> [CHECK]</c:if>
                </span>
            </div>
        </div>

        <c:if test="${report.summary.pendingOrders > 0}">
            <div class="receipt-dashed-line"></div>
            <div class="receipt-notice-box">
                * ${report.summary.pendingOrders} order(s) still waiting, not counted
            </div>
        </c:if>

        <c:if test="${not empty report.shifts}">
            <div class="receipt-dashed-line"></div>
            <div class="receipt-section-header">&bull; SHIFTS OF THE DAY &bull;</div>
            <div class="receipt-meta-grid">
                <c:forEach var="s" items="${report.shifts}">
                    <div class="receipt-row">
                        <span class="r-label">
                            <fmt:formatDate value="${s.openedAt}" pattern="HH:mm"/> -
                            <c:choose>
                                <c:when test="${s.open}">now</c:when>
                                <c:otherwise><fmt:formatDate value="${s.closedAt}" pattern="HH:mm"/></c:otherwise>
                            </c:choose>
                            (<c:out value="${s.openedBy}"/>)
                        </span>
                        <span class="r-val">
                            <c:choose>
                                <c:when test="${s.countedCash != null}"><fmt:formatNumber value="${s.countedCash}" pattern="#,##0"/> ₫</c:when>
                                <c:otherwise>-</c:otherwise>
                            </c:choose>
                        </span>
                    </div>
                </c:forEach>
            </div>
        </c:if>

        <c:if test="${not empty report.rows}">
            <div class="receipt-dashed-line"></div>
            <div class="receipt-section-header">&bull; PERIOD BREAKDOWN &bull;</div>
            <div class="receipt-meta-grid">
                <c:forEach var="r" items="${report.rows}">
                    <div class="receipt-row">
                        <span class="r-label"><c:out value="${r.label}"/> (${r.orderCount})</span>
                        <span class="r-val r-bold"><fmt:formatNumber value="${r.total}" pattern="#,##0"/> ₫</span>
                    </div>
                </c:forEach>
            </div>
        </c:if>

        <div class="receipt-status-box">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="16" y1="13" x2="8" y2="13"></line>
                <line x1="16" y1="17" x2="8" y2="17"></line>
                <polyline points="10 9 9 9 8 9"></polyline>
            </svg>
            <span>OFFICIAL FINANCIAL AUDIT REPORT</span>
        </div>

        <div class="receipt-signatures">
            <div class="sig-col">
                <div class="sig-title">Prepared by</div>
                <div class="sig-line"></div>
                <div class="sig-sub"><c:out value="${printedBy}"/></div>
            </div>
            <div class="sig-col">
                <div class="sig-title">Store Manager</div>
                <div class="sig-line"></div>
                <div class="sig-sub">(Signed &amp; Verified)</div>
            </div>
        </div>

        <div class="receipt-footer">
            <p class="r-thank">Tinky Financial Management System</p>
            <p class="r-engine">Thermal Print Engine &bull; All Amounts in VND</p>
        </div>
    </div>

    <div class="receipt-screen-actions no-print" style="margin-top: 18px; text-align: center;">
        <c:choose>
            <c:when test="${sessionScope.loginUser.role == 'AUDITOR'}">
                <a href="${ctx}/invoices" class="btn-receipt-back" style="box-shadow: 0 2px 8px rgba(0,0,0,0.08); padding: 9px 18px; font-size: 13px;">
                    &larr; Return to Invoices
                </a>
            </c:when>
            <c:otherwise>
                <a href="${ctx}/shift" class="btn-receipt-back" style="box-shadow: 0 2px 8px rgba(0,0,0,0.08); padding: 9px 18px; font-size: 13px;">
                    &larr; Return to Shifts
                </a>
            </c:otherwise>
        </c:choose>
    </div>
</div>
<script src="${ctx}/assets/js/receipt.js"></script>
</body>
</html>
