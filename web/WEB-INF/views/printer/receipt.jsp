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
            size: 76mm 130mm;
            margin: 0;
        }

        :root {
            color-scheme: light only;
            --paper-w: 320px;
            --paper-h: 546px; /* 76:130 aspect ratio on screen */
            --rc-font: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            --rc-mono: "SF Mono", Consolas, "Liberation Mono", Menlo, Courier, monospace;
        }

        body.receipt-screen-body {
            font-family: var(--rc-font);
            background: #e2e8f0;
            color: #000000;
            margin: 0;
            padding: 24px 16px 48px;
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            align-items: center;
            box-sizing: border-box;
            -webkit-font-smoothing: antialiased;
        }

        .receipt-screen-wrap {
            display: flex;
            flex-direction: column;
            align-items: center;
            width: 100%;
            max-width: var(--paper-w);
        }

        /* Non-printable top action toolbar */
        .receipt-toolbar {
            display: flex;
            align-items: center;
            justify-content: space-between;
            width: 100%;
            margin-bottom: 14px;
            gap: 10px;
        }

        .btn-receipt-back {
            display: inline-flex;
            align-items: center;
            gap: 5px;
            font-size: 12.5px;
            font-weight: 600;
            color: #475569;
            text-decoration: none;
            padding: 6px 12px;
            background: #ffffff;
            border: 1px solid #cbd5e1;
            border-radius: 6px;
            transition: all 0.15s ease;
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
        }
        .btn-receipt-back:hover {
            background: #f8fafc;
            color: #0f172a;
            border-color: #94a3b8;
        }

        .receipt-toolbar-meta {
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .paper-badge {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            font-size: 11px;
            font-weight: 700;
            letter-spacing: 0.03em;
            color: #047857;
            background: #ecfdf5;
            border: 1px solid #a7f3d0;
            padding: 4px 8px;
            border-radius: 6px;
        }

        .btn-receipt-print {
            display: inline-flex;
            align-items: center;
            gap: 5px;
            font-size: 12.5px;
            font-weight: 600;
            color: #ffffff;
            background: #be185d;
            border: none;
            border-radius: 6px;
            padding: 6px 14px;
            cursor: pointer;
            box-shadow: 0 2px 6px rgba(190, 24, 93, 0.25);
            transition: all 0.15s ease;
        }
        .btn-receipt-print:hover {
            filter: brightness(1.1);
            transform: translateY(-1px);
        }

        /* The 76x130mm Thermal Receipt Slip (Full-bleed distributed height) */
        .receipt-slip {
            width: var(--paper-w);
            min-height: var(--paper-h);
            background: #ffffff;
            padding: 22px 18px 20px;
            border-radius: 4px;
            box-sizing: border-box;
            box-shadow: 0 16px 40px rgba(0, 0, 0, 0.12), 0 2px 6px rgba(0, 0, 0, 0.04);
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            font-family: var(--rc-font);
            font-size: 11px;
            color: #000000;
            position: relative;
        }

        /* Brand Header */
        .receipt-brand-block {
            text-align: center;
        }

        .receipt-brand-title {
            font-size: 22px;
            font-weight: 900;
            letter-spacing: 0.08em;
            text-transform: uppercase;
            line-height: 1.1;
            margin-bottom: 4px;
        }

        .receipt-brand-sub {
            font-size: 10px;
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            color: #334155;
            margin: 0 0 3px;
        }

        .receipt-brand-contact {
            font-size: 9.5px;
            color: #64748b;
            margin: 0;
        }

        /* Separators */
        .rc-line-solid {
            width: 100%;
            height: 0;
            border-top: 1.5px solid #000000;
            margin: 10px 0 8px;
        }

        .rc-line-thin {
            width: 100%;
            height: 0;
            border-top: 1px solid #000000;
            margin: 8px 0;
        }

        /* Metadata */
        .receipt-meta-block {
            display: flex;
            flex-direction: column;
            gap: 4px;
            font-size: 11px;
        }

        .meta-headline {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 2px;
        }

        .order-title {
            font-size: 12px;
            font-weight: 800;
            letter-spacing: 0.08em;
            text-transform: uppercase;
        }

        .order-code-badge {
            font-size: 12.5px;
            font-weight: 800;
            font-family: var(--rc-mono);
            letter-spacing: 0.04em;
        }

        .reprint-badge {
            font-size: 9px;
            font-weight: 800;
            color: #dc2626;
            border: 1px solid #dc2626;
            border-radius: 3px;
            padding: 1px 4px;
            margin-left: 6px;
            letter-spacing: 0.04em;
        }

        .meta-row {
            display: flex;
            justify-content: space-between;
            align-items: baseline;
            line-height: 1.35;
        }

        .meta-label {
            color: #475569;
            font-size: 10.5px;
        }

        .meta-val {
            font-weight: 600;
            text-align: right;
        }

        .meta-val-mono {
            font-family: var(--rc-mono);
            font-size: 10px;
        }

        /* Items Table */
        .receipt-table-block {
            margin: 2px 0;
        }

        .items-table {
            width: 100%;
            border-collapse: collapse;
        }

        .items-table th {
            font-size: 10px;
            font-weight: 800;
            text-transform: uppercase;
            letter-spacing: 0.05em;
            padding: 5px 0;
            border-top: 1px solid #000000;
            border-bottom: 1px solid #000000;
        }

        .items-table td {
            padding: 5px 0;
            vertical-align: top;
            font-size: 11px;
            line-height: 1.3;
        }

        .col-name {
            text-align: left;
            padding-right: 6px;
            word-break: break-word;
        }

        .col-qty {
            text-align: center;
            width: 30px;
            font-weight: 700;
        }

        .col-price {
            text-align: right;
            width: 60px;
            font-variant-numeric: tabular-nums;
            color: #334155;
        }

        .col-total {
            text-align: right;
            width: 65px;
            font-weight: 700;
            font-variant-numeric: tabular-nums;
        }

        /* Totals Block */
        .receipt-totals-block {
            display: flex;
            flex-direction: column;
            gap: 4px;
            font-size: 11px;
        }

        .total-sub-row {
            display: flex;
            justify-content: space-between;
            color: #475569;
            font-size: 10.5px;
            padding-left: 6px;
        }

        .total-grand-row {
            display: flex;
            justify-content: space-between;
            align-items: baseline;
            margin-top: 2px;
        }

        .total-grand-label {
            font-size: 15px;
            font-weight: 900;
            letter-spacing: 0.03em;
            text-transform: uppercase;
        }

        .total-grand-val {
            font-size: 18.5px;
            font-weight: 900;
            font-variant-numeric: tabular-nums;
            letter-spacing: -0.01em;
        }

        /* Status Stamp Box */
        .receipt-status-stamp {
            border: 1.5px solid #000000;
            padding: 6px 10px;
            text-align: center;
            font-size: 10.5px;
            font-weight: 800;
            letter-spacing: 0.08em;
            text-transform: uppercase;
            margin: 6px 0 2px;
            background: #fafafa;
        }

        /* Footer */
        .receipt-footer-block {
            text-align: center;
            line-height: 1.4;
        }

        .footer-thank {
            font-size: 11px;
            font-weight: 700;
            margin-bottom: 2px;
        }

        .footer-policy {
            font-size: 9px;
            color: #64748b;
            margin-bottom: 4px;
        }

        .footer-brandmark {
            font-size: 8.5px;
            font-weight: 700;
            letter-spacing: 0.08em;
            text-transform: uppercase;
            color: #94a3b8;
            margin: 0;
        }

        /* Print Media Styles strictly for 76mm x 130mm Thermal Printing */
        @media print {
            @page {
                size: 76mm 130mm;
                margin: 0;
            }

            html, body, body.receipt-screen-body {
                width: 100% !important;
                height: 100% !important;
                margin: 0 !important;
                padding: 0 !important;
                background: #ffffff !important;
                color: #000000 !important;
            }

            .no-print {
                display: none !important;
            }

            .receipt-screen-wrap {
                padding: 0 !important;
                margin: 0 !important;
                max-width: 100% !important;
                background: transparent !important;
                display: block !important;
            }

            .receipt-slip {
                width: 72mm !important;
                height: 126mm !important;
                max-width: 72mm !important;
                max-height: 126mm !important;
                margin: 0 auto !important;
                padding: 4mm 3mm 4mm !important;
                box-shadow: none !important;
                border: none !important;
                border-radius: 0 !important;
                background: #ffffff !important;
                display: flex !important;
                flex-direction: column !important;
                justify-content: space-between !important;
                page-break-inside: avoid !important;
                break-inside: avoid !important;
            }

            .receipt-brand-title, .order-title, .total-grand-label, .total-grand-val, .footer-thank {
                color: #000000 !important;
            }

            .receipt-brand-sub, .receipt-brand-contact, .meta-label, .col-price, .total-sub-row, .footer-policy, .footer-brandmark {
                color: #000000 !important;
            }

            .rc-line-solid, .items-table th {
                border-color: #000000 !important;
            }

            .receipt-status-stamp {
                background: transparent !important;
                border-color: #000000 !important;
                color: #000000 !important;
            }

            .reprint-badge {
                border-color: #000000 !important;
                color: #000000 !important;
            }
        }
    </style>
</head>
<body class="receipt-screen-body">
<div class="receipt-screen-wrap">
    <!-- Non-printable top action toolbar -->
    <div class="receipt-toolbar no-print">
        <a href="${ctx}/transactions" class="btn-receipt-back">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="19" y1="12" x2="5" y2="12"></line>
                <polyline points="12 19 5 12 12 5"></polyline>
            </svg>
            Back
        </a>
        <div class="receipt-toolbar-meta">
            <span class="paper-badge">
                <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="6 9 6 2 18 2 18 9"></polyline>
                    <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"></path>
                    <rect x="6" y="14" width="12" height="8"></rect>
                </svg>
                <span id="paper-label">POS 76×130mm</span>
            </span>
            <button type="button" id="print-again" class="btn-receipt-print">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="6 9 6 2 18 2 18 9"></polyline>
                    <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"></path>
                    <rect x="6" y="14" width="12" height="8"></rect>
                </svg>
                Print
            </button>
        </div>
    </div>

    <!-- The 76x130mm Thermal Receipt Slip -->
    <div class="receipt-slip">
        <!-- 1. Brand Header -->
        <div class="receipt-brand-block">
            <div class="receipt-brand-title">TINKY POS</div>
            <p class="receipt-brand-sub">Smart Payment Speaker &amp; Cashier Register</p>
            <p class="receipt-brand-contact">Hotline: 1900 8888 &bull; store.tinky.vn</p>
        </div>

        <div class="rc-line-solid"></div>

        <!-- 2. Order Metadata -->
        <div class="receipt-meta-block">
            <div class="meta-headline">
                <span class="order-title">
                    SALES RECEIPT
                    <c:if test="${reprint}">
                        <span class="reprint-badge">REPRINT</span>
                    </c:if>
                </span>
                <span class="order-code-badge"><c:out value="${receipt.orderCode}"/></span>
            </div>
            <div class="meta-row">
                <span class="meta-label">Date &amp; Time:</span>
                <span class="meta-val"><fmt:formatDate value="${receipt.transTime}" pattern="yyyy-MM-dd HH:mm:ss"/></span>
            </div>
            <div class="meta-row">
                <span class="meta-label">Cashier:</span>
                <span class="meta-val"><c:out value="${receipt.cashierCode}"/></span>
            </div>
            <div class="meta-row">
                <span class="meta-label">Payment Method:</span>
                <span class="meta-val">
                    <c:choose>
                        <c:when test="${receipt.cashAmount > 0 && receipt.amount > 0}">Split (Cash + VietQR)</c:when>
                        <c:when test="${receipt.cashAmount > 0}">Cash at counter</c:when>
                        <c:otherwise>VietQR Dynamic</c:otherwise>
                    </c:choose>
                </span>
            </div>
            <c:if test="${not empty receipt.bankTransId}">
                <div class="meta-row">
                    <span class="meta-label">Bank Ref / Trace:</span>
                    <span class="meta-val meta-val-mono"><c:out value="${receipt.bankTransId}"/></span>
                </div>
            </c:if>
        </div>

        <!-- 3. Itemized Products Table -->
        <c:if test="${not empty orderItems}">
            <div class="receipt-table-block">
                <table class="items-table">
                    <thead>
                        <tr>
                            <th class="col-name">ITEM</th>
                            <th class="col-qty">QTY</th>
                            <th class="col-price">PRICE</th>
                            <th class="col-total">TOTAL</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="item" items="${orderItems}">
                            <tr>
                                <td class="col-name"><c:out value="${item.productName}"/></td>
                                <td class="col-qty"><c:out value="${item.quantity}"/></td>
                                <td class="col-price"><fmt:formatNumber value="${item.unitPrice}" pattern="#,##0"/></td>
                                <td class="col-total"><fmt:formatNumber value="${item.subtotal}" pattern="#,##0"/></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </c:if>

        <!-- 4. Amounts & Totals -->
        <div class="receipt-totals-block">
            <div class="meta-row" style="font-weight: 600;">
                <span>Subtotal</span>
                <span class="meta-val"><fmt:formatNumber value="${receipt.orderAmount}" pattern="#,##0"/> ₫</span>
            </div>
            <c:if test="${receipt.cashAmount > 0}">
                <div class="total-sub-row">
                    <span>&bull; Cash at counter</span>
                    <span><fmt:formatNumber value="${receipt.cashAmount}" pattern="#,##0"/> ₫</span>
                </div>
                <c:if test="${receipt.amount > 0}">
                    <div class="total-sub-row">
                        <span>&bull; VietQR transfer</span>
                        <span><fmt:formatNumber value="${receipt.amount}" pattern="#,##0"/> ₫</span>
                    </div>
                </c:if>
            </c:if>
            <c:if test="${receipt.vatAmount.signum() > 0}">
                <div class="total-sub-row">
                    <span>&bull; VAT <fmt:formatNumber value="${receipt.vatRate}" pattern="0.##"/>% included</span>
                    <span><fmt:formatNumber value="${receipt.vatAmount}" pattern="#,##0"/> ₫</span>
                </div>
            </c:if>
            <div class="rc-line-solid"></div>
            <div class="total-grand-row">
                <span class="total-grand-label">TOTAL PAID</span>
                <span class="total-grand-val"><fmt:formatNumber value="${receipt.orderAmount}" pattern="#,##0"/> ₫</span>
            </div>
        </div>

        <!-- 5. Status Confirmation Stamp -->
        <div class="receipt-status-stamp">
            &bull; TRANSACTION COMPLETED &bull;
        </div>

        <!-- 6. Footer -->
        <div class="receipt-footer-block">
            <div class="footer-thank">Thank you for your purchase!</div>
            <div class="footer-policy">Please keep this receipt for returns within 7 days</div>
            <p class="footer-brandmark">Tinky Smart Payment Speaker POS</p>
        </div>
    </div>

    <!-- Return link on screen -->
    <div class="no-print" style="margin-top: 16px; text-align: center;">
        <a href="${ctx}/transactions" class="btn-receipt-back" style="box-shadow: 0 1px 4px rgba(0,0,0,0.06); padding: 7px 18px;">
            &larr; Return to Transactions
        </a>
    </div>
</div>
<script src="${ctx}/assets/js/receipt.js"></script>
</body>
</html>
