<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Tax payments"/>
<c:set var="nav" value="tax"/>
<c:set var="taxTab" value="payments"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<%@ include file="/WEB-INF/views/common/tax-tabs.jspf" %>
<div class="card">
    <h1>Tax payments</h1>
    <p class="hint">Four different numbers are kept apart: <b>estimated</b> (our guess), <b>declared</b> (what you filed), <b>confirmed</b> (what the tax office says you owe) and <b>paid</b> (backed by a bank reference or receipt). An obligation is only marked paid when the confirmed amount is covered by payments with evidence.</p>
</div>
<div class="kpis">
    <div class="card"><span class="kpi-label">Estimated</span><div class="kpi-value"><fmt:formatNumber value="${sumEst}" pattern="#,##0"/></div></div>
    <div class="card"><span class="kpi-label">Declared</span><div class="kpi-value"><fmt:formatNumber value="${sumDecl}" pattern="#,##0"/></div></div>
    <div class="card"><span class="kpi-label">Confirmed to pay</span><div class="kpi-value"><fmt:formatNumber value="${sumConf}" pattern="#,##0"/></div></div>
    <div class="card"><span class="kpi-label">Paid</span><div class="kpi-value accent"><fmt:formatNumber value="${sumPaid}" pattern="#,##0"/></div></div>
    <div class="card"><span class="kpi-label">Still owed</span><div class="kpi-value ${sumOut > 0 ? 'bad' : ''}"><fmt:formatNumber value="${sumOut}" pattern="#,##0"/></div></div>
</div>

<c:if test="${canManage}">
<div class="card narrow">
    <h2>Record a payment</h2>
    <c:if test="${not empty error}"><div class="field-error"><c:out value="${error}"/></div></c:if>
    <form method="post" action="${ctx}/tax/payments">
        <label>Obligation
            <select name="obligationId" required>
                <option value="">Choose...</option>
                <c:forEach var="o" items="${rows}"><c:if test="${o.outstanding > 0}"><option value="${o.obligation_id}">${o.kind} <c:out value="${o.period_label}"/> - owed <fmt:formatNumber value="${o.outstanding}" pattern="#,##0"/></option></c:if></c:forEach>
            </select>
        </label>
        <label>Amount (VND) <input type="text" name="amount" required inputmode="numeric" maxlength="16"><span class="hint">Part payments are fine; it cannot exceed what is still owed.</span></label>
        <label>Date paid <input type="date" name="paidOn" required></label>
        <label>Method
            <select name="method"><option>Bank transfer</option><option>Treasury counter</option><option>E-wallet</option></select>
        </label>
        <label>Bank reference <input type="text" name="reference" maxlength="60"></label>
        <label>Receipt name <input type="text" name="receipt" maxlength="100" placeholder="e.g. receipt-0731.jpg"><span class="hint">Give a bank reference or a receipt name. Without evidence the payment is refused. The file itself is not stored here.</span></label>
        <button type="submit" class="primary" onclick="return confirm('Record this payment? It will count towards the confirmed amount.');">Record payment</button>
    </form>
</div>
</c:if>

<div class="card">
    <h2>By obligation</h2>
    <table>
        <thead><tr><th>Obligation</th><th>Due</th><th>Estimated</th><th>Declared</th><th>Confirmed</th><th>Paid</th><th>Outstanding</th><th>Status</th></tr></thead>
        <tbody>
        <c:forEach var="o" items="${rows}">
            <tr>
                <td>${o.kind} <c:out value="${o.period_label}"/></td><td>${o.due_date}</td>
                <td><fmt:formatNumber value="${o.estimated_amount}" pattern="#,##0"/></td>
                <td><c:out value="${empty o.declared_amount ? '-' : o.declared_amount}"/></td>
                <td><c:out value="${empty o.confirmed_amount ? '-' : o.confirmed_amount}"/></td>
                <td><fmt:formatNumber value="${o.paid_amount}" pattern="#,##0"/></td>
                <td><b><fmt:formatNumber value="${o.outstanding}" pattern="#,##0"/></b></td>
                <td><span class="pill ${o.status == 'PAID' ? 'PAID' : 'PENDING'}">${o.status}</span></td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</div>
<div class="card">
    <h2>Payment history</h2>
    <table>
        <thead><tr><th>Date</th><th>Obligation</th><th>Amount</th><th>Method</th><th>Reference</th><th>Receipt</th><th>Recorded by</th></tr></thead>
        <tbody>
        <c:forEach var="p" items="${payments}">
            <tr><td>${p.paid_on}</td><td>${p.kind} <c:out value="${p.period_label}"/></td><td><fmt:formatNumber value="${p.amount}" pattern="#,##0"/></td><td><c:out value="${p.method}"/></td><td><c:out value="${p.reference}"/></td><td><c:out value="${p.receipt_name}"/></td><td><c:out value="${p.recorded_by}"/></td></tr>
        </c:forEach>
        <c:if test="${empty payments}"><tr><td colspan="7">No payments recorded.</td></tr></c:if>
        </tbody>
    </table>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
