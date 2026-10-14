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

    <form method="post" action="${ctx}/orders/new" id="orderForm">
        <label for="amount">Tổng tiền đơn hàng (VND)</label>
        <input id="amount" class="amount-input" type="text" name="amount" inputmode="numeric" autocomplete="off"
               value="<c:out value='${amount}'/>" placeholder="Ví dụ: 100000" required autofocus>
        <div class="quick">
            <button type="button" data-amount="20000">20.000</button>
            <button type="button" data-amount="50000">50.000</button>
            <button type="button" data-amount="100000">100.000</button>
            <button type="button" data-amount="200000">200.000</button>
            <button type="button" data-amount="500000">500.000</button>
        </div>

        <div class="payment-method-group">
            <label class="group-title">Phương thức thanh toán</label>
            <div class="method-options">
                <label class="method-option">
                    <input type="radio" name="payType" value="FULL_QR" ${empty cashAmount || cashAmount == '0' ? 'checked' : ''}>
                    <span class="method-box">
                        <span class="method-icon">📱</span>
                        <span class="method-text">
                            <strong>100% Chuyển khoản QR</strong>
                            <small>Quét mã thanh toán toàn bộ</small>
                        </span>
                    </span>
                </label>
                <label class="method-option">
                    <input type="radio" name="payType" value="SPLIT" ${not empty cashAmount && cashAmount != '0' ? 'checked' : ''}>
                    <span class="method-box">
                        <span class="method-icon">💵+📱</span>
                        <span class="method-text">
                            <strong>Tiền mặt + Chuyển khoản</strong>
                            <small>Khách trả 1 phần tiền mặt</small>
                        </span>
                    </span>
                </label>
            </div>
        </div>

        <div id="splitSection" class="split-section" style="${not empty cashAmount && cashAmount != '0' ? '' : 'display: none;'}">
            <label for="cashAmount">Tiền mặt khách đưa tại quầy (VND)</label>
            <input id="cashAmount" type="text" name="cashAmount" inputmode="numeric" autocomplete="off"
                   value="<c:out value='${cashAmount}'/>" placeholder="Ví dụ: 40000">
            <div class="split-preview">
                <div class="preview-item">
                    <span>Tổng đơn hàng</span>
                    <strong id="previewTotal">0 VND</strong>
                </div>
                <div class="preview-item">
                    <span>Tiền mặt thu</span>
                    <strong id="previewCash">0 VND</strong>
                </div>
                <div class="preview-item highlight">
                    <span>Cần quét QR</span>
                    <strong id="previewTransfer">0 VND</strong>
                </div>
            </div>
            <p class="hint">Mã VietQR sẽ được tạo với số tiền chuyển khoản này.</p>
        </div>

        <button class="go primary" type="submit" id="submitBtn">Generate QR code</button>
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
                <tr>
                    <th>Order</th>
                    <th>Total</th>
                    <th>Payment method</th>
                    <th>Status</th>
                    <th>Created by</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="o" items="${recentOrders}">
                    <tr>
                        <td><a href="${ctx}/orders/view?code=<c:out value='${o.orderCode}'/>"><c:out value="${o.orderCode}"/></a></td>
                        <td><strong><fmt:formatNumber value="${o.amount}" pattern="#,##0"/> VND</strong></td>
                        <td>
                            <c:choose>
                                <c:when test="${o.cashAmount > 0}">
                                    <span class="pill" style="background: rgba(190, 24, 93, 0.1); color: var(--primary);">
                                        Tiền mặt (<fmt:formatNumber value="${o.cashAmount}" pattern="#,##0"/>) + QR (<fmt:formatNumber value="${o.transferAmount}" pattern="#,##0"/>)
                                    </span>
                                </c:when>
                                <c:otherwise>
                                    <span class="pill" style="background: rgba(20, 163, 139, 0.1); color: var(--ok);">100% QR</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
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
    (function () {
        var amountInput = document.getElementById('amount');
        var cashInput = document.getElementById('cashAmount');
        var splitSection = document.getElementById('splitSection');
        var radios = document.querySelectorAll('input[name="payType"]');
        var previewTotal = document.getElementById('previewTotal');
        var previewCash = document.getElementById('previewCash');
        var previewTransfer = document.getElementById('previewTransfer');
        var submitBtn = document.getElementById('submitBtn');

        function parse(val) {
            if (!val) return 0;
            var num = parseInt(val.replace(/[.,\s]/g, ''), 10);
            return isNaN(num) ? 0 : num;
        }

        function format(num) {
            return num.toLocaleString('vi-VN') + ' VND';
        }

        function updatePreview() {
            var total = parse(amountInput.value);
            var isSplit = document.querySelector('input[name="payType"]:checked').value === 'SPLIT';

            if (!isSplit) {
                cashInput.value = '';
                splitSection.style.display = 'none';
                submitBtn.textContent = total > 0 ? 'Tạo mã QR (' + format(total) + ')' : 'Generate QR code';
                return;
            }

            splitSection.style.display = 'block';
            var cash = parse(cashInput.value);
            var transfer = Math.max(0, total - cash);

            previewTotal.textContent = format(total);
            previewCash.textContent = format(cash);
            previewTransfer.textContent = format(transfer);

            if (cash >= total && total > 0) {
                previewTransfer.textContent = '0 VND (Khách trả 100% tiền mặt)';
                submitBtn.textContent = 'Tiền mặt phải nhỏ hơn tổng đơn';
                submitBtn.disabled = true;
            } else {
                submitBtn.disabled = false;
                submitBtn.textContent = transfer > 0 ? 'Tạo mã QR chuyển khoản (' + format(transfer) + ')' : 'Generate QR code';
            }
        }

        document.querySelectorAll('.quick button').forEach(function (btn) {
            btn.addEventListener('click', function () {
                amountInput.value = btn.dataset.amount;
                updatePreview();
            });
        });

        amountInput.addEventListener('input', updatePreview);
        cashInput.addEventListener('input', updatePreview);
        radios.forEach(function (r) {
            r.addEventListener('change', updatePreview);
        });

        document.getElementById('orderForm').addEventListener('submit', function (e) {
            var total = parse(amountInput.value);
            var isSplit = document.querySelector('input[name="payType"]:checked').value === 'SPLIT';
            if (isSplit) {
                var cash = parse(cashInput.value);
                if (cash <= 0) {
                    alert('Vui lòng nhập số tiền mặt khách trả hoặc chọn 100% Chuyển khoản QR.');
                    e.preventDefault();
                    cashInput.focus();
                    return;
                }
                if (cash >= total) {
                    alert('Tiền mặt khách trả phải nhỏ hơn tổng tiền đơn hàng. Nếu khách trả 100% tiền mặt thì không cần tạo mã QR thanh toán ngân hàng.');
                    e.preventDefault();
                    cashInput.focus();
                    return;
                }
            }
        });

        updatePreview();
    })();
</script>
</body>
</html>
