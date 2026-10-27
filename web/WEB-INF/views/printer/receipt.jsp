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
    <link rel="stylesheet" href="${ctx}/assets/css/base.css?v=42">
    <link rel="stylesheet" href="${ctx}/assets/css/guide.css?v=42">
    <link rel="stylesheet" href="${ctx}/assets/css/dashboard.css?v=42">
    <link rel="stylesheet" href="${ctx}/assets/css/auth.css?v=42">
    <link rel="stylesheet" href="${ctx}/assets/css/receipt.css?v=42">
    <link rel="stylesheet" href="${ctx}/assets/css/screens.css?v=42">
    <link rel="stylesheet" href="${ctx}/assets/css/tools.css?v=42">
    <style>
        @page {
            size: 76mm 130mm;
            margin: 0;
        }

        :root {
            color-scheme: light only;
            --paper-w: 295px;
            --paper-h: 510px;
            --rc-font: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            --rc-mono: Consolas, "Courier New", Courier, monospace;
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

        .receipt-toolbar {
            display: flex;
            align-items: center;
            justify-content: space-between;
            width: 100%;
            margin-bottom: 12px;
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

        /* Authentic Physical Slip */
        .receipt-slip {
            width: var(--paper-w);
            min-height: var(--paper-h);
            background: #ffffff;
            padding: 16px 14px 14px;
            border-radius: 2px;
            box-sizing: border-box;
            box-shadow: 0 12px 30px rgba(0, 0, 0, 0.12), 0 1px 4px rgba(0, 0, 0, 0.04);
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            font-family: var(--rc-font);
            font-size: 11px;
            line-height: 1.35;
            color: #000000;
            position: relative;
        }

        .text-center { text-align: center; }
        .text-right { text-align: right; }
        .bold { font-weight: 700; }

        .store-title {
            font-size: 16px;
            font-weight: 800;
            letter-spacing: 0.06em;
            text-transform: uppercase;
            margin-bottom: 2px;
            line-height: 1.2;
        }

        .store-address {
            font-size: 9.5px;
            line-height: 1.35;
            color: #000000;
        }

        .dashed-sep {
            border: none;
            border-top: 1px dashed #000000;
            margin: 7px 0;
            width: 100%;
        }

        .receipt-type {
            font-size: 13.5px;
            font-weight: 800;
            letter-spacing: 0.05em;
            text-transform: uppercase;
            margin-top: 1px;
        }

        .receipt-no {
            font-size: 10.5px;
            font-weight: 700;
            margin-bottom: 3px;
        }

        .meta-row {
            display: flex;
            justify-content: space-between;
            font-size: 9.5px;
            line-height: 1.35;
        }

        .reprint-badge {
            font-size: 8px;
            font-weight: 800;
            color: #dc2626;
            border: 1px solid #dc2626;
            border-radius: 2px;
            padding: 0 3px;
            margin-left: 3px;
        }

        /* 4-column Items Table */
        .items-table {
            width: 100%;
            border-collapse: collapse;
            font-size: 10px;
            margin: 2px 0;
        }

        .items-table th {
            font-weight: 700;
            padding: 3px 0;
            color: #000000;
        }

        .items-table td {
            padding: 3px 0;
            vertical-align: baseline;
            color: #000000;
        }

        .col-item { text-align: left; width: 44%; word-break: break-word; }
        .col-qty { text-align: center; width: 12%; }
        .col-price { text-align: right; width: 22%; font-variant-numeric: tabular-nums; }
        .col-amt { text-align: right; width: 22%; font-variant-numeric: tabular-nums; font-weight: 600; }

        /* Summary Section */
        .summary-wrap {
            display: flex;
            flex-direction: column;
            gap: 2.5px;
            font-size: 10.5px;
            margin-top: 2px;
        }

        .sum-row {
            display: flex;
            justify-content: space-between;
            align-items: baseline;
        }

        .sum-val {
            font-variant-numeric: tabular-nums;
        }

        .total-row {
            display: flex;
            justify-content: space-between;
            align-items: baseline;
            margin: 1px 0;
        }

        .total-lbl {
            font-size: 13.5px;
            font-weight: 800;
            letter-spacing: 0.02em;
        }

        .total-val {
            font-size: 14.5px;
            font-weight: 800;
            font-variant-numeric: tabular-nums;
        }

        /* Footer Section */
        .footer-wrap {
            margin-top: 4px;
            display: flex;
            flex-direction: column;
            align-items: center;
        }

        .thanks-msg {
            font-size: 10px;
            font-weight: 600;
            line-height: 1.35;
            margin-bottom: 5px;
        }

        .codes-row {
            display: flex;
            align-items: center;
            justify-content: space-between;
            width: 100%;
            padding: 0 4px;
            gap: 10px;
            margin-top: 2px;
        }

        .barcode-wrap {
            flex: 1;
            display: flex;
            flex-direction: column;
            align-items: center;
        }

        .barcode-svg {
            width: 100%;
            max-width: 175px;
            height: 38px;
        }

        .barcode-digits {
            font-family: var(--rc-mono);
            font-size: 8px;
            letter-spacing: 1px;
            margin-top: 1px;
        }

        .qr-wrap {
            width: 44px;
            height: 44px;
            flex-shrink: 0;
            display: flex;
            align-items: center;
            justify-content: center;
        }

        .qr-svg {
            width: 44px;
            height: 44px;
        }

        .internal-note {
            font-size: 8px;
            font-weight: 700;
            letter-spacing: 0.06em;
            text-transform: uppercase;
            margin-top: 5px;
            color: #000000;
        }

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
                min-height: 124mm !important;
                max-width: 72mm !important;
                margin: 0 auto !important;
                padding: 2.5mm 3mm 2.5mm !important;
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

            .dashed-sep {
                border-color: #000000 !important;
            }
        }
    </style>
</head>
<body class="receipt-screen-body">
<div class="receipt-screen-wrap">
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

    <div class="receipt-slip">
        <div>
            <!-- Store Header -->
            <div class="text-center">
                <div class="store-title">TINKY</div>
                <div class="store-address">
                    Address: 123 Hoang huu Nam,<br>
                    Thu Duc, Ho Chi Minh City<br>
                    Phone: (+84) 28 1234 5678
                </div>
            </div>

            <div class="dashed-sep"></div>

            <!-- Receipt Meta -->
            <div class="text-center">
                <div class="receipt-type">PAYMENT RECEIPT</div>
                <div class="receipt-no">
                    Receipt No: #<c:out value="${receipt.orderCode}"/>
                    <c:if test="${reprint}"><span class="reprint-badge">COPY</span></c:if>
                </div>
            </div>

            <div class="meta-row">
                <span>Date: <fmt:formatDate value="${receipt.transTime}" pattern="dd/MM/yyyy"/></span>
                <span>Time: <fmt:formatDate value="${receipt.transTime}" pattern="HH:mm:ss"/></span>
            </div>
            <div class="meta-row">
                <span>Cashier: <c:out value="${receipt.cashierCode}"/></span>
                <span>Method:
                    <c:choose>
                        <c:when test="${receipt.cashAmount > 0 && receipt.amount > 0}">Split (Cash+QR)</c:when>
                        <c:when test="${receipt.cashAmount > 0}">Cash</c:when>
                        <c:otherwise>VietQR Dynamic</c:otherwise>
                    </c:choose>
                </span>
            </div>

            <div class="dashed-sep"></div>

            <!-- Items Table (4 Columns: Item Name, Qty, Price, Total) -->
            <table class="items-table">
                <thead>
                    <tr>
                        <th class="col-item">Item Name</th>
                        <th class="col-qty">Qty</th>
                        <th class="col-price">Price</th>
                        <th class="col-amt">Total</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td colspan="4" style="padding:0;"><div class="dashed-sep" style="margin:2px 0;"></div></td>
                    </tr>
                    <c:choose>
                        <c:when test="${not empty orderItems}">
                            <c:forEach var="item" items="${orderItems}">
                                <tr>
                                    <td class="col-item"><c:out value="${item.productName}"/></td>
                                    <td class="col-qty"><c:out value="${item.quantity}"/></td>
                                    <td class="col-price"><fmt:formatNumber value="${item.unitPrice}" pattern="#,##0"/></td>
                                    <td class="col-amt"><fmt:formatNumber value="${item.subtotal}" pattern="#,##0"/></td>
                                </tr>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>
                            <tr>
                                <td class="col-item">Quick Custom Sale</td>
                                <td class="col-qty">1</td>
                                <td class="col-price"><fmt:formatNumber value="${receipt.orderAmount}" pattern="#,##0"/></td>
                                <td class="col-amt"><fmt:formatNumber value="${receipt.orderAmount}" pattern="#,##0"/></td>
                            </tr>
                        </c:otherwise>
                    </c:choose>
                </tbody>
            </table>

            <div class="dashed-sep"></div>

            <!-- Summary / Totals -->
            <div class="summary-wrap">
                <div class="sum-row">
                    <span>Subtotal:</span>
                    <span class="sum-val"><fmt:formatNumber value="${receipt.subtotal}" pattern="#,##0"/> VND</span>
                </div>
                <c:if test="${receipt.discountAmount.signum() > 0}">
                    <div class="sum-row">
                        <span>Discount (<c:out value="${receipt.discountCode}"/>):</span>
                        <span class="sum-val">-<fmt:formatNumber value="${receipt.discountAmount}" pattern="#,##0"/> VND</span>
                    </div>
                </c:if>
                <c:if test="${not empty receipt.affiliateCode}">
                    <div class="sum-row">
                        <span>Referral code:</span>
                        <span class="sum-val"><c:out value="${receipt.affiliateCode}"/></span>
                    </div>
                </c:if>
                <c:if test="${receipt.vatAmount.signum() > 0}">
                    <div class="sum-row">
                        <span>VAT (<fmt:formatNumber value="${receipt.vatRate}" pattern="0.##"/>%):</span>
                        <span class="sum-val"><fmt:formatNumber value="${receipt.vatAmount}" pattern="#,##0"/> VND</span>
                    </div>
                </c:if>

                <div class="dashed-sep"></div>

                <div class="total-row">
                    <span class="total-lbl">TOTAL:</span>
                    <span class="total-val"><fmt:formatNumber value="${receipt.orderAmount}" pattern="#,##0"/> VND</span>
                </div>

                <c:if test="${receipt.cashAmount > 0}">
                    <div class="sum-row" style="margin-top: 2px;">
                        <span>Cash:</span>
                        <span class="sum-val"><fmt:formatNumber value="${receipt.cashAmount}" pattern="#,##0"/> VND</span>
                    </div>
                    <div class="sum-row">
                        <span>Change:</span>
                        <span class="sum-val">0 VND</span>
                    </div>
                </c:if>
                <c:if test="${receipt.amount > 0}">
                    <div class="sum-row" style="margin-top: 2px;">
                        <span>VietQR:</span>
                        <span class="sum-val"><fmt:formatNumber value="${receipt.amount}" pattern="#,##0"/> VND</span>
                    </div>
                    <c:if test="${not empty receipt.bankTransId}">
                        <div class="sum-row" style="font-size: 9px; color: #475569;">
                            <span>Trace No:</span>
                            <span class="sum-val" style="font-family: var(--rc-mono);"><c:out value="${receipt.bankTransId}"/></span>
                        </div>
                    </c:if>
                </c:if>
            </div>
        </div>

        <!-- Footer & Codes -->
        <div class="footer-wrap">
            <div class="dashed-sep"></div>
            <div class="thanks-msg text-center">
                Thank you &amp; See you again!
            </div>

            <!-- Barcode & QR Code Side-by-Side matching photo -->
            <div class="codes-row">
                <div class="barcode-wrap">
                    <svg class="barcode-svg" viewBox="0 0 160 36" fill="#000">
                        <!-- Code128 pattern -->
                        <rect x="0" y="0" width="2" height="36"/>
                        <rect x="4" y="0" width="1" height="36"/>
                        <rect x="6" y="0" width="3" height="36"/>
                        <rect x="11" y="0" width="1" height="36"/>
                        <rect x="14" y="0" width="2" height="36"/>
                        <rect x="18" y="0" width="3" height="36"/>
                        <rect x="23" y="0" width="1" height="36"/>
                        <rect x="26" y="0" width="1" height="36"/>
                        <rect x="29" y="0" width="2" height="36"/>
                        <rect x="33" y="0" width="4" height="36"/>
                        <rect x="39" y="0" width="1" height="36"/>
                        <rect x="42" y="0" width="2" height="36"/>
                        <rect x="46" y="0" width="3" height="36"/>
                        <rect x="51" y="0" width="1" height="36"/>
                        <rect x="54" y="0" width="3" height="36"/>
                        <rect x="59" y="0" width="2" height="36"/>
                        <rect x="63" y="0" width="1" height="36"/>
                        <rect x="66" y="0" width="4" height="36"/>
                        <rect x="72" y="0" width="1" height="36"/>
                        <rect x="75" y="0" width="2" height="36"/>
                        <rect x="79" y="0" width="2" height="36"/>
                        <rect x="83" y="0" width="1" height="36"/>
                        <rect x="86" y="0" width="3" height="36"/>
                        <rect x="91" y="0" width="2" height="36"/>
                        <rect x="95" y="0" width="1" height="36"/>
                        <rect x="98" y="0" width="3" height="36"/>
                        <rect x="103" y="0" width="1" height="36"/>
                        <rect x="106" y="0" width="4" height="36"/>
                        <rect x="112" y="0" width="2" height="36"/>
                        <rect x="116" y="0" width="1" height="36"/>
                        <rect x="119" y="0" width="3" height="36"/>
                        <rect x="124" y="0" width="2" height="36"/>
                        <rect x="128" y="0" width="1" height="36"/>
                        <rect x="131" y="0" width="3" height="36"/>
                        <rect x="136" y="0" width="1" height="36"/>
                        <rect x="139" y="0" width="2" height="36"/>
                        <rect x="143" y="0" width="4" height="36"/>
                        <rect x="149" y="0" width="1" height="36"/>
                        <rect x="152" y="0" width="2" height="36"/>
                        <rect x="156" y="0" width="2" height="36"/>
                    </svg>
                    <div class="barcode-digits">
                        <c:choose>
                            <c:when test="${not empty receipt.bankTransId}">003009000000<c:out value="${receipt.orderId}"/></c:when>
                            <c:otherwise>00300900000010700799</c:otherwise>
                        </c:choose>
                    </div>
                </div>

                <!-- Clean QR Code SVG -->
                <div class="qr-wrap">
                    <svg class="qr-svg" viewBox="0 0 29 29" shape-rendering="crispEdges">
                        <path fill="#fff" d="M0 0h29v29H0z"/>
                        <path fill="#000" d="M0 0h7v7H0zm1 1v5h5V1zM8 0h1v1H8zm3 0h1v2h-1zm3 0h1v1h-1zm2 0h1v1h-1zm2 0h1v1h-1zm4 0h7v7h-7zm1 1v5h5V1zM2 2h3v3H2zm20 0h3v3h-3zM9 1h1v1H9zm4 0h1v2h-1zm-4 2h2v1H9zm3 0h1v1h-1zm4 0h2v1h-2zm-6 1h1v1h-1zm2 0h1v1h-1zm2 0h2v1h-2zm-5 1h1v1H9zm3 0h1v1h-1zm-4 1h1v1H8zm2 0h1v1h-1zm4 0h2v1h-2zm-5 1h2v1H9zm3 0h2v1h-2zm0 1h1v1h-1zm-4 1h1v1H8zm2 0h1v1h-1zm3 0h1v1h-1zm-3 1h2v1H9zm-8 1h1v1H1zm2 0h1v1H3zm3 0h2v1H6zm4 0h1v1h-1zm2 0h1v1h-1zm-9 1h1v1H2zm3 0h1v1H5zm2 0h1v1H7zm4 0h1v1h-1zm-9 1h2v1H2zm4 0h1v1H6zm2 0h1v1H8zm2 0h2v1h-2zm-9 1h1v1H1zm4 0h1v1H5zm2 0h2v1H7zm3 0h1v1h-1zm-9 1h1v1H1zm2 0h2v1H3zm3 0h1v1H6zm3 0h1v1H9zm-7 1h1v1H2zm2 0h1v1H4zm3 0h1v1H7zm2 0h1v1H9zm11-13h1v1h-1zm3 0h1v1h-1zm-2 1h1v2h-1zm3 0h1v1h-1zm-1 2h1v1h-1zm3 0h1v1h-1zm-2 1h1v1h-1zm-1 1h1v1h-1zm2 0h2v1h-2zm-2 1h1v1h-1zm-1 1h1v2h-1zm2 0h1v1h-1zm2 0h1v1h-1zm-2 2h1v1h-1zm0 1h2v1h-2zm-18 2h7v7H0zm1 1v5h5v-5zM2 24h3v3H2zm6-2h1v1H8zm2 0h1v1h-1zm-3 1h1v2H7zm3 0h1v1h-1zm-2 1h1v1H8zm3 0h1v1h-1zm-3 1h1v1H8zm2 0h2v1h-2zm-3 1h1v1H7zm3 0h1v1h-1zm1 1h1v1h-1zm-3 1h1v1H8zm2 0h2v1h-2zm8-9h1v1h-1zm2 0h2v1h-2zm-1 1h1v1h-1zm-2 1h1v1h-1zm2 0h1v1h-1zm1 0h1v1h-1zm-2 1h1v1h-1zm2 0h1v1h-1zm-3 1h1v1h-1zm1 0h1v1h-1zm-2 1h1v1h-1zm2 0h1v1h-1zm-1 1h1v1h-1zm-1 1h1v1h-1zm2 0h1v1h-1zm-1 1h1v1h-1zm2 0h1v1h-1z"/>
                    </svg>
                </div>
            </div>

            <div class="internal-note text-center">
                FOR TINKY PROJECT INTERNAL USE
            </div>
        </div>
    </div>

    <div class="no-print" style="margin-top: 14px; text-align: center;">
        <a href="${ctx}/transactions" class="btn-receipt-back" style="box-shadow: 0 1px 4px rgba(0,0,0,0.06); padding: 7px 18px;">
            &larr; Return to Transactions
        </a>
    </div>
</div>
<script src="${ctx}/assets/js/receipt.js"></script>
</body>
</html>
