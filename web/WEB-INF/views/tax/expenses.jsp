<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Expenses"/>
<c:set var="nav" value="tax"/>
<c:set var="taxTab" value="expenses"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<%@ include file="/WEB-INF/views/common/tax-tabs.jspf" %>
<div class="card">
    <h1>Expenses and purchases</h1>
    <p class="hint">Keep your purchases and costs with their documents. A cost is never counted as deductible automatically: you decide, and "may count" needs a supporting document. Whether it really reduces tax depends on the rules that apply to you.</p>
</div>
<div class="kpis">
    <div class="card"><span class="kpi-label">Shown expenses</span><div class="kpi-value"><fmt:formatNumber value="${total}" pattern="#,##0"/></div><p class="kpi-sub">VND, with the filters below</p></div>
    <div class="card"><span class="kpi-label">Missing documents</span><div class="kpi-value ${missing > 0 ? 'bad' : ''}">${missing}</div><p class="kpi-sub">Attach them before claiming</p></div>
    <div class="card"><span class="kpi-label">Revenue, last 30 days</span><div class="kpi-value"><fmt:formatNumber value="${revenue30}" pattern="#,##0"/></div></div>
    <div class="card"><span class="kpi-label">Expenses, last 30 days</span><div class="kpi-value"><fmt:formatNumber value="${expense30}" pattern="#,##0"/></div></div>
</div>

<c:if test="${canManage}">
<div class="card narrow">
    <h2>Add an expense</h2>
    <form method="post" action="${ctx}/tax/expenses">
        <input type="hidden" name="action" value="create">
        <label>Date <input type="date" name="spentOn" required value="${param.spentOn}">
            <c:if test="${not empty errors.spentOn}"><div class="field-error"><c:out value="${errors.spentOn}"/></div></c:if></label>
        <label>Supplier <input type="text" name="supplier" required maxlength="100" value="<c:out value='${param.supplier}'/>">
            <c:if test="${not empty errors.supplier}"><div class="field-error"><c:out value="${errors.supplier}"/></div></c:if></label>
        <label>Supplier tax code <input type="text" name="taxCode" maxlength="14" value="<c:out value='${param.taxCode}'/>">
            <c:if test="${not empty errors.taxCode}"><div class="field-error"><c:out value="${errors.taxCode}"/></div></c:if></label>
        <label>Category
            <select name="category"><c:forEach var="k" items="${categories}"><option ${param.category == k ? 'selected' : ''}>${k}</option></c:forEach></select>
        </label>
        <label>Amount (VND) <input type="text" name="amount" required inputmode="numeric" maxlength="16" value="<c:out value='${param.amount}'/>">
            <c:if test="${not empty errors.amount}"><div class="field-error"><c:out value="${errors.amount}"/></div></c:if></label>
        <label>Payment method
            <select name="method"><option>Bank transfer</option><option>Cash</option><option>Card</option></select>
        </label>
        <label>Purchase invoice number <input type="text" name="invoiceNo" maxlength="40" value="<c:out value='${param.invoiceNo}'/>"></label>
        <label><input type="checkbox" name="hasDocument" value="1"> I have the supporting document</label>
        <button type="submit" class="primary">Add expense</button>
    </form>
</div>
</c:if>

<div class="card">
    <h2>Expenses by month</h2>
    <table>
        <thead><tr><th>Month</th><th>Expenses</th></tr></thead>
        <tbody>
        <c:forEach var="m" items="${monthly}"><tr><td>${m.month}</td><td><fmt:formatNumber value="${m.total}" pattern="#,##0"/> VND</td></tr></c:forEach>
        <c:if test="${empty monthly}"><tr><td colspan="2">No expenses yet.</td></tr></c:if>
        </tbody>
    </table>
</div>

<div class="card">
    <form method="get" action="${ctx}/tax/expenses" class="filters">
        <label>Search <input type="text" name="q" maxlength="60" value="<c:out value='${q}'/>"></label>
        <label>Category
            <select name="category"><option value="">All</option><c:forEach var="k" items="${categories}"><option ${catFilter == k ? 'selected' : ''}>${k}</option></c:forEach></select>
        </label>
        <label>Document
            <select name="doc"><option value="">All</option><option value="missing" ${docFilter == 'missing' ? 'selected' : ''}>Missing</option><option value="ok" ${docFilter == 'ok' ? 'selected' : ''}>Attached</option></select>
        </label>
        <label>From <input type="date" name="from" value="${from}"></label>
        <label>To <input type="date" name="to" value="${to}"></label>
        <button type="submit" class="primary">Filter</button>
        <a class="btn" href="${ctx}/tax/expenses">Reset</a>
        <a class="btn" href="${ctx}/tax/expenses?export=1&amp;category=${catFilter}&amp;doc=${docFilter}&amp;from=${from}&amp;to=${to}">Export CSV</a>
    </form>
    <table>
        <thead><tr><th>Date</th><th>Supplier</th><th>Category</th><th>Amount</th><th>Payment</th><th>Invoice</th><th>Document</th><th>Tax treatment</th><th></th></tr></thead>
        <tbody>
        <c:forEach var="e" items="${result.items}">
            <tr>
                <td>${e.spent_on}</td><td><c:out value="${e.supplier}"/></td><td>${e.category}</td>
                <td><fmt:formatNumber value="${e.amount}" pattern="#,##0"/></td><td>${e.payment_method}</td><td><c:out value="${e.invoice_no}"/></td>
                <td><c:choose><c:when test="${e.has_document}"><span class="pill ACTIVE">ATTACHED</span></c:when><c:otherwise><span class="pill HIGH">MISSING</span></c:otherwise></c:choose></td>
                <td>
                    <c:choose>
                        <c:when test="${canManage}">
                            <form method="post" action="${ctx}/tax/expenses" class="inline">
                                <input type="hidden" name="action" value="treatment"><input type="hidden" name="id" value="${e.expense_id}">
                                <select name="value" onchange="this.form.submit()">
                                    <option value="UNCLASSIFIED" ${e.tax_treatment == 'UNCLASSIFIED' ? 'selected' : ''}>Unclassified</option>
                                    <option value="MAY_COUNT" ${e.tax_treatment == 'MAY_COUNT' ? 'selected' : ''}>May count (needs document)</option>
                                    <option value="NOT_COUNTED" ${e.tax_treatment == 'NOT_COUNTED' ? 'selected' : ''}>Not counted</option>
                                </select>
                            </form>
                        </c:when>
                        <c:otherwise>${e.tax_treatment}</c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <c:if test="${canManage}">
                        <form method="post" action="${ctx}/tax/expenses" class="inline">
                            <input type="hidden" name="id" value="${e.expense_id}">
                            <input type="hidden" name="value" value="${e.has_document ? '0' : '1'}">
                            <button type="submit" name="action" value="document">${e.has_document ? 'Mark missing' : 'Mark attached'}</button>
                            <button type="submit" name="action" value="delete" onclick="return confirm('Delete this expense?');">Delete</button>
                        </form>
                    </c:if>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty result.items}"><tr><td colspan="9">No expenses found.</td></tr></c:if>
        </tbody>
    </table>
    <c:set var="qe"><c:out value="${q}"/></c:set>
    <c:set var="pagerBase" value="${ctx}/tax/expenses?q=${qe}&amp;category=${catFilter}&amp;doc=${docFilter}&amp;from=${from}&amp;to=${to}&amp;"/>
    <%@ include file="/WEB-INF/views/common/pager.jspf" %>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
