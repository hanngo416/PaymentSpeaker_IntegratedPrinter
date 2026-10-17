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

<c:set var="pendingCount" value="0"/>
<c:forEach var="o" items="${recentOrders}">
    <c:if test="${o.status == 'PENDING'}">
        <c:set var="pendingCount" value="${pendingCount + 1}"/>
    </c:if>
</c:forEach>

<div class="pos-dual-layout">
    <!-- LEFT: Frameless Small Cards Pending Orders Queue -->
    <div class="pos-pending-panel">
        <div class="pos-mini-queue-header">
            <span class="pos-mini-queue-title">Pending Orders</span>
            <span class="pos-mini-queue-badge" id="queueBadge" style="${pendingCount > 0 ? '' : 'display:none;'}">
                <span id="queueCount">${pendingCount}</span>
            </span>
        </div>

        <c:choose>
            <c:when test="${pendingCount == 0}">
                <p class="pos-mini-empty" id="emptyQueueMsg">No pending orders waiting for payment.</p>
                <div class="pos-queue-list" id="posQueueList" style="display: none;"></div>
            </c:when>
            <c:otherwise>
                <p class="pos-mini-empty" id="emptyQueueMsg" style="display: none;">No pending orders waiting for payment.</p>
                <div class="pos-queue-list" id="posQueueList">
                    <c:forEach var="o" items="${recentOrders}">
                        <c:if test="${o.status == 'PENDING'}">
                            <div class="pos-mini-card" id="card-${o.orderCode}" data-code="${o.orderCode}" data-status="PENDING">
                                <div class="pos-mini-main" onclick="window.location.href='${ctx}/orders/view?code=<c:out value='${o.orderCode}'/>'" title="Click to view order details">
                                    <div class="pos-mini-top">
                                        <span class="pos-mini-code"><c:out value="${o.orderCode}"/></span>
                                        <span class="pulse-dot"></span>
                                        <span class="pos-mini-amt"><fmt:formatNumber value="${o.amount}" pattern="#,##0"/> ₫</span>
                                    </div>
                                    <div class="pos-mini-sub">
                                        <span><fmt:formatDate value="${o.createdAt}" pattern="HH:mm"/></span>
                                        <c:choose>
                                            <c:when test="${o.cashAmount > 0}">
                                                <span>&bull; Cash: <fmt:formatNumber value="${o.cashAmount}" pattern="#,##0"/> ₫ + QR</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span>&bull; 100% VietQR</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                </div>
                                <div class="pos-mini-actions">
                                    <button type="button" class="btn-mini-qr"
                                            data-code="<c:out value='${o.orderCode}'/>"
                                            data-amount="<fmt:formatNumber value='${o.amount}' pattern='#,##0'/> ₫"
                                            data-transfer="<fmt:formatNumber value='${o.transferAmount}' pattern='#,##0'/> ₫"
                                            data-cash="<fmt:formatNumber value='${o.cashAmount}' pattern='#,##0'/> ₫"
                                            data-qr="<c:out value='${o.qrContent}'/>"
                                            data-remaining="${o.remainingSeconds}"
                                            title="Show QR code">
                                        QR
                                    </button>
                                    <form method="post" action="${ctx}/orders/cancel" class="pos-mini-cancel-form"
                                          onsubmit="return confirm('Cancel order ${o.orderCode}?');">
                                        <input type="hidden" name="code" value="<c:out value='${o.orderCode}'/>">
                                        <button type="submit" class="btn-mini-action cancel" title="Cancel order">&times;</button>
                                    </form>
                                </div>
                            </div>
                        </c:if>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </div>

    <!-- RIGHT: Compact Short Form -->
    <div class="card pos-create-panel">
        <h2 class="pos-compact-title">Create payment order</h2>

        <c:if test="${not empty error}">
            <div class="alert-banner" style="margin-bottom: 12px; padding: 8px 12px; font-size: 13px;"><c:out value="${error}"/></div>
        </c:if>

        <form method="post" action="${ctx}/orders/new" id="orderForm">
            <div class="pos-compact-label-row">
                <label for="amount" class="pos-compact-label">Amount</label>
                <span class="pos-currency-pill">VND</span>
            </div>
            <div class="pos-compact-input-wrap">
                <input id="amount" class="pos-compact-input" type="text" name="amount" inputmode="numeric" pattern="[0-9,]*" maxlength="11" autocomplete="off"
                       value="<c:out value='${amount}'/>" placeholder="0" required autofocus>
                <span class="pos-currency-symbol">₫</span>
            </div>

            <div class="pos-compact-presets">
                <button type="button" class="preset-chip" data-amount="20000">20k</button>
                <button type="button" class="preset-chip" data-amount="50000">50k</button>
                <button type="button" class="preset-chip" data-amount="100000">100k</button>
                <button type="button" class="preset-chip" data-amount="200000">200k</button>
                <button type="button" class="preset-chip" data-amount="500000">500k</button>
            </div>

            <div class="pos-compact-label-row">
                <label for="pickedCode" class="pos-compact-label">Discount code</label>
            </div>
            <div class="pos-discount-row">
                <select id="pickedCode" name="pickedCode">
                    <option value="">No discount</option>
                    <c:forEach var="d" items="${sharedCodes}">
                        <option value="<c:out value='${d.code}'/>" ${pickedCode == d.code ? 'selected' : ''}><c:out value="${d.code}"/> (<c:choose><c:when test="${d.kind == 'PERCENT'}">-<fmt:formatNumber value="${d.value}" pattern="#,##0"/>%</c:when><c:otherwise>-<fmt:formatNumber value="${d.value}" pattern="#,##0"/> VND</c:otherwise></c:choose>)</option>
                    </c:forEach>
                </select>
                <input type="text" id="typedCode" name="typedCode" maxlength="30" autocomplete="off" placeholder="Or type a code"
                       value="<c:out value='${typedCode}'/>">
            </div>

            <div class="pos-compact-methods">
                <label class="pos-seg-label">
                    <input type="checkbox" id="methodCash" name="methodCash" value="1" ${not empty cashAmount && cashAmount != '0' ? 'checked' : ''}>
                    <span class="pos-seg-btn">
                        <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <rect x="2" y="6" width="20" height="12" rx="2"></rect><circle cx="12" cy="12" r="2"></circle>
                        </svg>
                        <span>Cash</span>
                        <svg class="pos-seg-check" viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                            <polyline points="20 6 9 17 4 12"></polyline>
                        </svg>
                    </span>
                </label>
                <label class="pos-seg-label">
                    <input type="checkbox" id="methodQr" name="methodQr" value="1" ${(empty cashAmount || cashAmount == '0' || empty amount || cashAmount != amount) ? 'checked' : ''}>
                    <span class="pos-seg-btn">
                        <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect>
                            <rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect>
                        </svg>
                        <span>VietQR</span>
                        <svg class="pos-seg-check" viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                            <polyline points="20 6 9 17 4 12"></polyline>
                        </svg>
                    </span>
                </label>
            </div>

            <div id="splitSection" class="pos-compact-split" style="${not empty cashAmount && cashAmount != '0' && cashAmount != amount ? '' : 'display: none;'}">
                <div class="pos-compact-label-row">
                    <label for="cashAmount" class="pos-compact-label">Cash at counter</label>
                    <span class="pos-currency-pill">VND</span>
                </div>
                <div class="pos-compact-split-input-wrap">
                    <input id="cashAmount" class="pos-compact-split-input" type="text" name="cashAmount" inputmode="numeric" pattern="[0-9,]*" maxlength="11" autocomplete="off"
                           value="<c:out value='${cashAmount}'/>" placeholder="0">
                    <span class="pos-currency-symbol">₫</span>
                </div>
                <div class="pos-split-summary-inline">
                    <span>Cash: <span id="previewCash">0 ₫</span></span>
                    <span>QR to pay: <strong id="previewTransfer">0 ₫</strong></span>
                </div>
            </div>

            <button class="pos-compact-submit" type="submit" id="submitBtn">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect>
                    <rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect>
                </svg>
                <span id="submitBtnText">Generate QR code</span>
            </button>
        </form>
    </div>
</div>

<!-- BOTTOM: Recent Orders Table -->
<div class="card">
    <div class="head-row">
        <h2>Recent orders</h2>
        <span class="hint">Auto-synced</span>
    </div>
    <c:choose>
        <c:when test="${empty recentOrders}">
            <p>No orders yet.</p>
        </c:when>
        <c:otherwise>
            <table>
                <thead>
                <tr>
                    <th style="width: 140px;">Order</th>
                    <th style="width: 150px;">Total</th>
                    <th>Payment method</th>
                    <th style="width: 120px;">Status</th>
                    <th style="width: 140px;">Time</th>
                    <th style="width: 110px;">Created by</th>
                    <th style="width: 90px; text-align: center;">Action</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="o" items="${recentOrders}">
                    <c:set var="isPaid" value="${o.status == 'COMPLETED' || o.status == 'PAID'}"/>
                    <c:set var="isPending" value="${o.status == 'PENDING'}"/>
                    <tr>
                        <td>
                            <a href="${ctx}/orders/view?code=<c:out value='${o.orderCode}'/>">
                                <strong><c:out value="${o.orderCode}"/></strong>
                            </a>
                        </td>
                        <td class="amount-cell"><fmt:formatNumber value="${o.amount}" pattern="#,##0"/> ₫</td>
                        <td>
                            <c:choose>
                                <c:when test="${o.cashAmount > 0}">
                                    <span class="pill" style="background: rgba(190, 24, 93, 0.08); color: var(--primary); border: 1px solid rgba(190, 24, 93, 0.2);">
                                        Cash (<fmt:formatNumber value="${o.cashAmount}" pattern="#,##0"/> ₫) + QR (<fmt:formatNumber value="${o.transferAmount}" pattern="#,##0"/> ₫)
                                    </span>
                                </c:when>
                                <c:otherwise>
                                    <span class="pill" style="background: rgba(20, 163, 139, 0.1); color: var(--ok); border: 1px solid rgba(20, 163, 139, 0.25);">100% QR</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${isPaid}">
                                    <span class="pill COMPLETED">COMPLETED</span>
                                </c:when>
                                <c:when test="${isPending}">
                                    <span class="pill PENDING"><span class="pulse-dot"></span> PENDING</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="pill CANCELLED">CANCELLED</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td><fmt:formatDate value="${o.createdAt}" pattern="yyyy-MM-dd HH:mm"/></td>
                        <td><c:out value="${o.createdByCode}"/></td>
                        <td style="text-align: center; white-space: nowrap;">
                            <c:choose>
                                <c:when test="${isPending}">
                                    <button type="button" class="btn-action btn-mini-qr-table"
                                            data-code="<c:out value='${o.orderCode}'/>"
                                            data-amount="<fmt:formatNumber value='${o.amount}' pattern='#,##0'/> ₫"
                                            data-transfer="<fmt:formatNumber value='${o.transferAmount}' pattern='#,##0'/> ₫"
                                            data-cash="<fmt:formatNumber value='${o.cashAmount}' pattern='#,##0'/> ₫"
                                            data-qr="<c:out value='${o.qrContent}'/>"
                                            data-remaining="${o.remainingSeconds}"
                                            title="Show QR code">
                                        View QR
                                    </button>
                                </c:when>
                                <c:otherwise>
                                    <a class="btn-action" href="${ctx}/orders/view?code=<c:out value='${o.orderCode}'/>" title="View details">
                                        View
                                    </a>
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</div>

<!-- Modal for quick QR inspection without leaving the POS page -->
<div class="modal-backdrop" id="posQrModal" hidden>
    <div class="modal pos-qr-quick-modal" role="dialog" aria-modal="true" aria-labelledby="mQrTitle">
        <button type="button" class="modal-close" id="posQrModalClose" aria-label="Close">&times;</button>
        <p class="modal-kicker">VietQR Payment</p>
        <h2 id="mQrTitle" style="margin: 0 0 10px; font-size: 18px; color: var(--navy);"></h2>

        <div class="pos-modal-qr-frame">
            <img id="mQrImg" class="pos-modal-qr-img" src="" alt="VietQR payment code">
        </div>

        <div id="mCountdown" class="pos-modal-countdown">--:--</div>

        <div id="mAmount" class="modal-amount" style="text-align: center; margin-bottom: 4px; font-size: 24px; font-weight: 800;"></div>
        <div id="mBreakdown" style="text-align: center; font-size: 13px; color: var(--muted); margin-bottom: 16px;"></div>

        <div class="pos-modal-actions">
            <button type="button" class="btn" id="mPrintBtn">Print QR</button>
            <a id="mFullscreenLink" href="" target="_blank" class="btn">Full screen ↗</a>
            <button type="button" class="btn primary" id="mDoneBtn">Back to counter</button>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
<script>
    (function () {
        var MAX_AMOUNT = <%= util.AppConfig.MAX_AMOUNT %>;
        var amountInput = document.getElementById('amount');
        var cashInput = document.getElementById('cashAmount');
        var splitSection = document.getElementById('splitSection');
        var chkCash = document.getElementById('methodCash');
        var chkQr = document.getElementById('methodQr');
        var previewCash = document.getElementById('previewCash');
        var previewTransfer = document.getElementById('previewTransfer');
        var submitBtn = document.getElementById('submitBtn');
        var submitBtnIcon = document.getElementById('submitBtnIcon');

        // Modal elements
        var modal = document.getElementById('posQrModal');
        var modalClose = document.getElementById('posQrModalClose');
        var mDoneBtn = document.getElementById('mDoneBtn');
        var mTitle = document.getElementById('mQrTitle');
        var mImg = document.getElementById('mQrImg');
        var mCountdown = document.getElementById('mCountdown');
        var mAmount = document.getElementById('mAmount');
        var mBreakdown = document.getElementById('mBreakdown');
        var mFullscreen = document.getElementById('mFullscreenLink');
        var modalTimer = null;
        var activeModalCode = null;

        function setBtnText(text, disabled) {
            var txtSpan = document.getElementById('submitBtnText');
            if (txtSpan) {
                txtSpan.textContent = text;
            } else {
                submitBtn.textContent = text;
            }
            submitBtn.disabled = !!disabled;
        }

        function parse(val) {
            if (!val) return 0;
            var num = parseInt(String(val).replace(/[.,\s]/g, ''), 10);
            return isNaN(num) ? 0 : num;
        }

        function formatNumber(num) {
            return num.toLocaleString('en-US');
        }

        function formatDisplay(num) {
            return num.toLocaleString('en-US') + ' ₫';
        }

        function updatePreview() {
            var total = parse(amountInput.value);
            var hasCash = chkCash ? chkCash.checked : false;
            var hasQr = chkQr ? chkQr.checked : true;

            if (hasCash && !hasQr) {
                // Case 2: 100% Cash payment
                splitSection.style.display = 'none';
                cashInput.value = total > 0 ? formatNumber(total) : '';
                setBtnText(total > 0 ? 'Complete Cash Order (' + formatDisplay(total) + ')' : 'Complete Cash Order', false);
                submitBtn.classList.add('cash-only');
                if (submitBtnIcon) {
                    submitBtnIcon.innerHTML = '<polyline points="20 6 9 17 4 12"></polyline>';
                }
            } else if (!hasCash && hasQr) {
                // Case 1: 100% VietQR payment
                splitSection.style.display = 'none';
                cashInput.value = '';
                setBtnText(total > 0 ? 'Generate QR code (' + formatDisplay(total) + ')' : 'Generate QR code', false);
                submitBtn.classList.remove('cash-only');
                if (submitBtnIcon) {
                    submitBtnIcon.innerHTML = '<rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect>';
                }
            } else if (hasCash && hasQr) {
                // Case 3: Combined Cash + QR payment
                splitSection.style.display = 'block';
                submitBtn.classList.remove('cash-only');
                if (submitBtnIcon) {
                    submitBtnIcon.innerHTML = '<rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect>';
                }
                var cash = parse(cashInput.value);
                if (cash >= total && total > 0) {
                    cash = 0;
                    cashInput.value = '';
                }
                var transfer = Math.max(0, total - cash);

                if (previewCash) previewCash.textContent = formatDisplay(cash);
                if (previewTransfer) previewTransfer.textContent = formatDisplay(transfer);

                if (cash >= total && total > 0) {
                    setBtnText('Cash must be less than order total', true);
                } else {
                    setBtnText(transfer > 0 ? 'Generate QR code (' + formatDisplay(transfer) + ')' : 'Generate QR code', false);
                }
            }
        }

        function handleMethodChange(clicked) {
            if (!chkCash.checked && !chkQr.checked) {
                clicked.checked = true;
            }
            updatePreview();
        }

        if (chkCash) chkCash.addEventListener('change', function () { handleMethodChange(this); });
        if (chkQr) chkQr.addEventListener('change', function () { handleMethodChange(this); });

        document.querySelectorAll('.preset-chip').forEach(function (btn) {
            btn.addEventListener('click', function () {
                document.querySelectorAll('.preset-chip').forEach(function (c) { c.classList.remove('active'); });
                btn.classList.add('active');
                amountInput.value = formatNumber(parseInt(btn.dataset.amount, 10));
                updatePreview();
            });
        });

        // Digits only, grouped with commas, never above the maximum order amount
        function keepDigits() {
            var digits = this.value.replace(/[^0-9]/g, '').replace(/^0+/, '');
            if (digits.length > 9) { digits = digits.substring(0, 9); }
            var val = parseInt(digits || '0', 10);
            if (val > MAX_AMOUNT) { val = MAX_AMOUNT; }
            this.value = val > 0 ? formatNumber(val) : '';
            updatePreview();
        }

        amountInput.addEventListener('input', function () {
            document.querySelectorAll('.preset-chip').forEach(function (c) { c.classList.remove('active'); });
            keepDigits.call(this);
        });
        cashInput.addEventListener('input', keepDigits);

        document.getElementById('orderForm').addEventListener('submit', function (e) {
            var total = parse(amountInput.value);
            if (total < 1000 || total > MAX_AMOUNT) {
                alert('The order amount must be between 1,000 and ' + formatNumber(MAX_AMOUNT) + ' VND.');
                e.preventDefault();
                amountInput.focus();
                return;
            }
            var hasCash = chkCash ? chkCash.checked : false;
            var hasQr = chkQr ? chkQr.checked : true;
            if (!hasCash && !hasQr) {
                alert('Please select at least one payment method (Cash or VietQR).');
                e.preventDefault();
                return;
            }
            if (hasCash && !hasQr) {
                cashInput.value = total;
            } else if (!hasCash && hasQr) {
                cashInput.value = '';
            } else {
                var cash = parse(cashInput.value);
                if (cash <= 0) {
                    alert('Enter the cash collected or uncheck Cash.');
                    e.preventDefault();
                    cashInput.focus();
                    return;
                }
                if (cash >= total) {
                    alert('For split payment, cash must be less than order total. If paying 100% cash, please uncheck VietQR.');
                    e.preventDefault();
                    cashInput.focus();
                    return;
                }
                cashInput.value = cash;
            }
        });

        if (amountInput.value) {
            var initialVal = parse(amountInput.value);
            if (initialVal > 0) amountInput.value = formatNumber(initialVal);
        }
        if (cashInput.value) {
            var initialCash = parse(cashInput.value);
            if (initialCash > 0) cashInput.value = formatNumber(initialCash);
        }
        updatePreview();

        // -------------------------------------------------------------
        // QR Quick Modal Logic
        // -------------------------------------------------------------
        function openQrModal(code, amount, transfer, cash, qrUrl, remaining) {
            activeModalCode = code;
            mTitle.textContent = 'Order #' + code;
            var cleanQr = (qrUrl || '').replace('-compact2.png', '-qr_only.png').replace('-compact.png', '-qr_only.png');
            mImg.src = cleanQr;
            mAmount.textContent = transfer;
            if (cash && parse(cash) > 0) {
                mBreakdown.textContent = 'Total: ' + amount + ' (Cash: ' + cash + ' + QR: ' + transfer + ')';
            } else {
                mBreakdown.textContent = '100% VietQR payment (' + transfer + ')';
            }
            mFullscreen.href = '${ctx}/orders/view?code=' + encodeURIComponent(code);

            if (modalTimer) clearInterval(modalTimer);
            var endAt = Date.now() + (parseInt(remaining, 10) || 600) * 1000;
            function updateCountdown() {
                var left = Math.max(0, Math.round((endAt - Date.now()) / 1000));
                var m = Math.floor(left / 60);
                var s = left % 60;
                mCountdown.textContent = (m < 10 ? '0' + m : m) + ':' + (s < 10 ? '0' + s : s);
            }
            updateCountdown();
            modalTimer = setInterval(updateCountdown, 1000);

            modal.hidden = false;
        }

        function closeQrModal() {
            modal.hidden = true;
            if (modalTimer) clearInterval(modalTimer);
            activeModalCode = null;
        }

        var mPrintBtn = document.getElementById('mPrintBtn');
        if (mPrintBtn) {
            mPrintBtn.addEventListener('click', function () {
                var w = window.open('', '_blank', 'width=420,height=640');
                if (!w) {
                    alert('Allow pop-ups to print the QR code.');
                    return;
                }
                var esc = function (s) {
                    return String(s || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
                };
                w.document.write('<!doctype html><html><head><meta charset="utf-8"><title>QR ' + esc(activeModalCode) + '</title>'
                    + '<style>@page{size:80mm auto;margin:0}body{margin:0;padding:4mm;width:72mm;font-family:Arial,sans-serif;text-align:center;color:#000}'
                    + 'h1{font-size:16px;margin:0 0 2mm}img{width:60mm;height:60mm}.amt{font-size:20px;font-weight:bold;margin:2mm 0}.sm{font-size:12px}</style></head><body>'
                    + '<h1>Tinky</h1><div class="sm">Scan to pay</div>'
                    + '<img id="q" src="' + esc(mImg.src) + '" alt="VietQR">'
                    + '<div class="amt">' + esc(mAmount.textContent) + '</div>'
                    + '<div class="sm">' + esc(mTitle.textContent) + '</div>'
                    + '<div class="sm">' + esc(mBreakdown.textContent) + '</div>'
                    + '<script>var i=document.getElementById("q");function go(){window.print();window.close()}if(i.complete){go()}else{i.onload=go;i.onerror=go}<\/script>'
                    + '</body></html>');
                w.document.close();
            });
        }

        if (modalClose) modalClose.addEventListener('click', closeQrModal);
        if (mDoneBtn) mDoneBtn.addEventListener('click', closeQrModal);
        if (modal) {
            modal.addEventListener('click', function (e) {
                if (e.target === modal) closeQrModal();
            });
        }

        document.querySelectorAll('.btn-mini-qr, .btn-mini-qr-table').forEach(function (btn) {
            btn.addEventListener('click', function () {
                openQrModal(
                    this.dataset.code,
                    this.dataset.amount,
                    this.dataset.transfer,
                    this.dataset.cash,
                    this.dataset.qr,
                    this.dataset.remaining
                );
            });
        });

        // -------------------------------------------------------------
        // Live polling of pending cards
        // -------------------------------------------------------------
        var pendingCards = document.querySelectorAll('.pos-mini-card[data-status="PENDING"]');
        if (pendingCards.length > 0) {
            setInterval(function () {
                pendingCards.forEach(function (card) {
                    var code = card.dataset.code;
                    if (!code || card.dataset.status !== 'PENDING') return;

                    fetch('${ctx}/orders/status?code=' + encodeURIComponent(code), { credentials: 'same-origin' })
                        .then(function (res) { return res.ok ? res.json() : null; })
                        .then(function (data) {
                            if (data && data.status !== 'PENDING') {
                                card.dataset.status = data.status;
                                if (data.status === 'COMPLETED' || data.status === 'PAID') {
                                    card.classList.add('just-completed');
                                    var amtEl = card.querySelector('.pos-mini-amt');
                                    if (amtEl) amtEl.textContent = 'COMPLETED';
                                    if (activeModalCode === code) {
                                        mAmount.textContent = '✔ PAYMENT COMPLETED';
                                        mAmount.style.color = 'var(--ok)';
                                        setTimeout(closeQrModal, 2000);
                                    }
                                } else {
                                    var amtEl = card.querySelector('.pos-mini-amt');
                                    if (amtEl) amtEl.textContent = 'CANCELLED';
                                }
                            }
                        })
                        .catch(function () {});
                });
            }, 2500);
        }
    })();
</script>
</body>
</html>
