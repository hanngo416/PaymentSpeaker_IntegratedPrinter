<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Smart Reconciliation Queue"/>
<c:set var="nav" value="transactions"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>

<div class="card">
    <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 16px;">
        <div>
            <span class="pill PENDING" style="margin-bottom: 8px;">Reconciliation Queue</span>
            <h1>Smart Reconciliation</h1>
            <p class="hint">Review unassigned or flagged bank transfers. Manually link legit transfers to pending orders or close them with audit notes.</p>
        </div>
        <div style="display: flex; gap: 8px;">
            <a class="btn" href="${ctx}/transactions">Transactions Ledger</a>
            <a class="btn" href="${ctx}/alerts">Alerts Center</a>
        </div>
    </div>
    <c:if test="${not empty message}"><div class="msg"><c:out value="${message}"/></div></c:if>
</div>

<div class="card">
    <form method="get" action="${ctx}/transactions/review" class="filters">
        <label>Flagged Rule
            <select name="rule">
                <option value="">All Rules</option>
                <c:forEach var="r" items="${rules}">
                    <option value="${r}" ${rule == r ? 'selected' : ''}><c:out value="${r}"/></option>
                </c:forEach>
            </select>
        </label>
        <label>From
            <input type="date" name="from" value="<c:out value='${from}'/>">
        </label>
        <label>To
            <input type="date" name="to" value="<c:out value='${to}'/>">
        </label>
        <button type="submit" class="primary">Filter</button>
        <a class="btn" href="${ctx}/transactions/review">Reset</a>
    </form>

    <table>
        <thead>
        <tr>
            <th>Time</th>
            <th>Bank Ref</th>
            <th>Amount</th>
            <th>Transfer Note</th>
            <th>Flagged Reason</th>
            <th>Detected Order</th>
            <th style="text-align: center;">Actions</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="row" items="${result.items}">
            <tr>
                <td><fmt:formatDate value="${row.transTime}" pattern="yyyy-MM-dd HH:mm:ss"/></td>
                <td><code style="font-weight: 700; color: var(--accent-sky);"><c:out value="${row.bankTransId}"/></code></td>
                <td class="amount-cell"><fmt:formatNumber value="${row.amount}" pattern="#,##0"/> VND</td>
                <td><c:out value="${row.content}"/></td>
                <td>
                    <span class="pill CANCELLED" title="<c:out value='${row.reason}'/>">
                        <c:out value="${row.ruleCode}"/>
                    </span>
                </td>
                <td><c:out value="${empty row.orderCode ? '&mdash;' : row.orderCode}" escapeXml="false"/></td>
                <td style="text-align: center; white-space: nowrap;">
                    <button type="button" class="btn primary" onclick="openLinkModal(${row.transId}, '<c:out value="${row.bankTransId}"/>', ${row.amount}, '<c:out value="${row.content}"/>')">Link to Order</button>
                    <button type="button" class="btn" onclick="openCloseModal(${row.transId}, '<c:out value="${row.bankTransId}"/>', ${row.amount})">Close</button>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty result.items}">
            <tr><td colspan="7">No flagged transfers pending review in queue. Everything is reconciled!</td></tr>
        </c:if>
        </tbody>
    </table>

    <p>${result.total} transfer(s) awaiting review, page ${result.page} of ${result.totalPages}</p>
    <div class="pages">
        <c:if test="${result.page > 1}">
            <a href="${ctx}/transactions/review?page=${result.page - 1}&amp;rule=${rule}&amp;from=${from}&amp;to=${to}" title="Previous page">&laquo;</a>
        </c:if>
        <c:forEach begin="1" end="${result.totalPages}" var="pg">
            <c:choose>
                <c:when test="${pg == result.page}"><span>${pg}</span></c:when>
                <c:otherwise><a href="${ctx}/transactions/review?page=${pg}&amp;rule=${rule}&amp;from=${from}&amp;to=${to}">${pg}</a></c:otherwise>
            </c:choose>
        </c:forEach>
        <c:if test="${result.page < result.totalPages}">
            <a href="${ctx}/transactions/review?page=${result.page + 1}&amp;rule=${rule}&amp;from=${from}&amp;to=${to}" title="Next page">&raquo;</a>
        </c:if>
    </div>
</div>

<!-- Modal 1: Link Transfer to Order -->
<div id="linkModalBackdrop" class="modal-backdrop" hidden>
    <div class="modal">
        <button type="button" class="btn modal-close" onclick="closeLinkModal()">&times;</button>
        <p class="modal-kicker">Manual Reconciliation</p>
        <h3>Link Transfer to Pending Order</h3>
        <p class="hint">Bank Ref: <code id="linkBankRef" style="color:var(--primary);"></code> &bull; Transfer Amount: <strong id="linkAmount"></strong> VND</p>
        <p class="hint" id="linkContent" style="background:rgba(255,255,255,0.04); padding:6px 10px; border-radius:6px; margin: 8px 0;"></p>

        <form method="post" action="${ctx}/transactions/review" style="margin-top: 14px;">
            <input type="hidden" name="action" value="link">
            <input type="hidden" id="linkTransId" name="transId" value="">

            <label>Select Pending Order
                <select name="orderId" id="linkOrderId" required style="width: 100%;">
                    <option value="">Choose an order awaiting payment...</option>
                    <c:forEach var="o" items="${pendingOrders}">
                        <option value="${o.orderId}">
                            ${o.orderCode} &mdash; Needs: <fmt:formatNumber value="${o.transferAmount}" pattern="#,##0"/> VND (${o.createdByCode}, <fmt:formatDate value="${o.createdAt}" pattern="HH:mm dd/MM"/>)
                        </option>
                    </c:forEach>
                </select>
            </label>

            <label style="margin-top: 12px;">Resolution Note (required, 5-200 characters)
                <input type="text" name="note" minlength="5" maxlength="200" required placeholder="e.g. Customer typo in transfer note, verified with counter staff">
                <span class="hint">Transfer amount must be equal to or greater than the order balance. Any excess will be recorded in the note.</span>
            </label>

            <div style="display:flex; justify-content:flex-end; gap:8px; margin-top:20px;">
                <button type="button" class="btn" onclick="closeLinkModal()">Cancel</button>
                <button type="submit" class="btn primary">Confirm Link &amp; Complete Order</button>
            </div>
        </form>
    </div>
</div>

<!-- Modal 2: Close Flagged Transfer -->
<div id="closeModalBackdrop" class="modal-backdrop" hidden>
    <div class="modal">
        <button type="button" class="btn modal-close" onclick="closeCloseModal()">&times;</button>
        <p class="modal-kicker" style="color:var(--danger);">Close Flagged Transfer</p>
        <h3>Close Transfer Without Order</h3>
        <p class="hint">Bank Ref: <code id="closeBankRef" style="color:var(--primary);"></code> &bull; Amount: <strong id="closeAmount"></strong> VND</p>

        <form method="post" action="${ctx}/transactions/review" style="margin-top: 14px;">
            <input type="hidden" name="action" value="close">
            <input type="hidden" id="closeTransId" name="transId" value="">

            <label>Closure Category
                <select name="category" required style="width: 100%;">
                    <c:forEach var="cat" items="${categories}">
                        <option value="${cat}">
                            ${cat == 'REFUND_NEEDED' ? 'REFUND_NEEDED - Customer transfer to be refunded' : cat == 'WRONG_ACCOUNT_OR_SPAM' ? 'WRONG_ACCOUNT_OR_SPAM - Wrong account or spam' : cat == 'DUPLICATE_TRANSFER' ? 'DUPLICATE_TRANSFER - Accidental duplicate payment' : 'OTHER - Other reason'}
                        </option>
                    </c:forEach>
                </select>
            </label>

            <label style="margin-top: 12px;">Resolution Note (required, 5-200 characters)
                <input type="text" name="note" minlength="5" maxlength="200" required placeholder="e.g. Contacted customer, refunded via bank counter">
            </label>

            <div style="display:flex; justify-content:flex-end; gap:8px; margin-top:20px;">
                <button type="button" class="btn" onclick="closeCloseModal()">Cancel</button>
                <button type="submit" class="btn" style="background:var(--danger); border-color:var(--danger); color:#fff;">Confirm Close Transfer</button>
            </div>
        </form>
    </div>
</div>

<script>
function openLinkModal(id, bankId, amount, content) {
    document.getElementById('linkTransId').value = id;
    document.getElementById('linkBankRef').textContent = bankId;
    document.getElementById('linkAmount').textContent = Number(amount).toLocaleString();
    document.getElementById('linkContent').textContent = 'Content: ' + (content || 'None');
    document.getElementById('linkModalBackdrop').hidden = false;
}
function closeLinkModal() {
    document.getElementById('linkModalBackdrop').hidden = true;
}
function openCloseModal(id, bankId, amount) {
    document.getElementById('closeTransId').value = id;
    document.getElementById('closeBankRef').textContent = bankId;
    document.getElementById('closeAmount').textContent = Number(amount).toLocaleString();
    document.getElementById('closeModalBackdrop').hidden = false;
}
function closeCloseModal() {
    document.getElementById('closeModalBackdrop').hidden = true;
}
</script>

<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>

