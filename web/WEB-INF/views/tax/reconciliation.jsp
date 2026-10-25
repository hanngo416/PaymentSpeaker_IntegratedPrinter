<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Reconciliation"/>
<c:set var="nav" value="tax"/>
<c:set var="taxTab" value="reconciliation"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<%@ include file="/WEB-INF/views/common/tax-tabs.jspf" %>
<div class="card">
    <h1>Reconciliation</h1>
    <p class="hint">Compares completed store orders of the last 90 days with the invoices made from them. Reconciliation never changes an order: you only add an explanation and mark the item resolved.</p>
    <div class="kpis">
        <div class="card"><span class="kpi-label">Matched</span><div class="kpi-value accent">${empty counts.MATCHED ? 0 : counts.MATCHED}</div></div>
        <div class="card"><span class="kpi-label">Missing invoice</span><div class="kpi-value">${empty counts.MISSING_INVOICE ? 0 : counts.MISSING_INVOICE}</div></div>
        <div class="card"><span class="kpi-label">Amount difference</span><div class="kpi-value">${empty counts.AMOUNT_DIFFERENCE ? 0 : counts.AMOUNT_DIFFERENCE}</div></div>
        <div class="card"><span class="kpi-label">Invoice not issued</span><div class="kpi-value">${empty counts.INVOICE_NOT_ISSUED ? 0 : counts.INVOICE_NOT_ISSUED}</div></div>
        <div class="card"><span class="kpi-label">Possible duplicate</span><div class="kpi-value">${empty counts.POSSIBLE_DUPLICATE ? 0 : counts.POSSIBLE_DUPLICATE}</div></div>
        <div class="card"><span class="kpi-label">Invoice without order</span><div class="kpi-value">${empty counts.UNMATCHED_INVOICE ? 0 : counts.UNMATCHED_INVOICE}</div></div>
    </div>
</div>
<div class="card">
    <form method="get" action="${ctx}/tax/reconciliation" class="filters">
        <label>Result
            <select name="kind">
                <option value="">All</option>
                <option value="MISSING_INVOICE" ${kindFilter == 'MISSING_INVOICE' ? 'selected' : ''}>Missing invoice</option>
                <option value="AMOUNT_DIFFERENCE" ${kindFilter == 'AMOUNT_DIFFERENCE' ? 'selected' : ''}>Amount difference</option>
                <option value="INVOICE_NOT_ISSUED" ${kindFilter == 'INVOICE_NOT_ISSUED' ? 'selected' : ''}>Invoice not issued</option>
                <option value="POSSIBLE_DUPLICATE" ${kindFilter == 'POSSIBLE_DUPLICATE' ? 'selected' : ''}>Possible duplicate</option>
                <option value="UNMATCHED_INVOICE" ${kindFilter == 'UNMATCHED_INVOICE' ? 'selected' : ''}>Invoice without order</option>
                <option value="MATCHED" ${kindFilter == 'MATCHED' ? 'selected' : ''}>Matched</option>
            </select>
        </label>
        <label>Show
            <select name="state">
                <option value="open" ${stateFilter == 'open' ? 'selected' : ''}>Needs review</option>
                <option value="resolved" ${stateFilter == 'resolved' ? 'selected' : ''}>Resolved</option>
                <option value="" ${empty stateFilter ? 'selected' : ''}>Everything</option>
            </select>
        </label>
        <button type="submit" class="primary">Filter</button>
        <a class="btn" href="${ctx}/tax/reconciliation?export=1">Export CSV</a>
    </form>
    <table>
        <thead><tr><th>Reference</th><th>Result</th><th>POS amount</th><th>Invoice amount</th><th>Difference</th><th>Explanation</th></tr></thead>
        <tbody>
        <c:forEach var="r" items="${result.items}">
            <tr>
                <td><c:out value="${r.ref}"/><br><span class="hint"><fmt:formatDate value="${r.date}" pattern="yyyy-MM-dd"/> <c:out value="${r.invoiceNo}"/></span></td>
                <td><span class="pill ${r.kind == 'MATCHED' ? 'ACTIVE' : (r.resolved ? 'EXPIRED' : 'PENDING')}">${r.kind}</span><c:if test="${r.resolved}"> <span class="pill RESOLVED">RESOLVED</span></c:if></td>
                <td><fmt:formatNumber value="${r.posAmount}" pattern="#,##0"/></td>
                <td><fmt:formatNumber value="${r.invoiceAmount}" pattern="#,##0"/></td>
                <td><fmt:formatNumber value="${r.diff}" pattern="#,##0"/></td>
                <td>
                    <c:if test="${r.kind != 'MATCHED'}">
                        <c:choose>
                            <c:when test="${canManage}">
                                <form method="post" action="${ctx}/tax/reconciliation">
                                    <input type="hidden" name="key" value="${r.key}">
                                    <input type="text" name="note" required minlength="3" maxlength="300" placeholder="Why does it differ?" value="<c:out value='${r.note}'/>">
                                    <button type="submit" name="resolved" value="0">Save note</button>
                                    <button type="submit" name="resolved" value="1">Mark resolved</button>
                                </form>
                            </c:when>
                            <c:otherwise><c:out value="${r.note}"/></c:otherwise>
                        </c:choose>
                    </c:if>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty result.items}"><tr><td colspan="6">Nothing to show.</td></tr></c:if>
        </tbody>
    </table>
    <c:set var="pagerBase" value="${ctx}/tax/reconciliation?kind=${kindFilter}&amp;state=${stateFilter}&amp;"/>
    <%@ include file="/WEB-INF/views/common/pager.jspf" %>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
