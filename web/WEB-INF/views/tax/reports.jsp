<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Revenue and reports"/>
<c:set var="nav" value="tax"/>
<c:set var="taxTab" value="reports"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<%@ include file="/WEB-INF/views/common/tax-tabs.jspf" %>
<div class="card">
    <h1>Revenue and tax reports</h1>
    <p class="hint">Summed up automatically from counter orders and imported marketplace orders. This revenue is not necessarily the revenue used for tax by law: see how it is calculated at the bottom of the page and check with an accountant.</p>
</div>

<div class="card">
    <form method="get" action="${ctx}/tax/reports" class="filters">
        <label>Reporting period
            <select name="range" id="rangeSelect">
                <option value="ytd" ${range == 'ytd' ? 'selected' : ''}>Year to date</option>
                <option value="m0" ${range == 'm0' ? 'selected' : ''}>This month</option>
                <option value="m1" ${range == 'm1' ? 'selected' : ''}>Last month</option>
                <option value="q" ${range == 'q' ? 'selected' : ''}>This quarter</option>
                <option value="custom" ${range == 'custom' ? 'selected' : ''}>Custom</option>
            </select>
        </label>
        <label>From <input type="date" name="from" value="${from}" id="fromInput" ${range == 'custom' ? '' : 'disabled'}></label>
        <label>To <input type="date" name="to" value="${to}" id="toInput" ${range == 'custom' ? '' : 'disabled'}></label>
        <button type="submit" class="primary">View</button>
        <c:if test="${empty error}"><a class="btn" href="${ctx}/tax/reports?range=${range}&amp;from=${from}&amp;to=${to}&amp;export=csv">Export CSV</a></c:if>
    </form>
    <c:if test="${not empty error}"><div class="alert-line"><c:out value="${error}"/></div></c:if>
    <p class="hint">Reporting period: ${from} to ${to}</p>
</div>

<div class="kpis">
    <div class="card"><span class="kpi-label">Gross revenue</span><div class="kpi-value"><fmt:formatNumber value="${report.gross}" pattern="#,##0"/></div><p class="kpi-sub">VND, before discounts</p></div>
    <div class="card"><span class="kpi-label">Discounts</span><div class="kpi-value">-<fmt:formatNumber value="${report.discounts}" pattern="#,##0"/></div><p class="kpi-sub">Discount codes and typed amounts</p></div>
    <div class="card"><span class="kpi-label">Cancelled and returned orders</span><div class="kpi-value"><fmt:formatNumber value="${report.removed}" pattern="#,##0"/></div><p class="kpi-sub">Already excluded from revenue</p></div>
    <div class="card"><span class="kpi-label">Revenue used for tax</span><div class="kpi-value"><fmt:formatNumber value="${report.taxable}" pattern="#,##0"/></div><p class="kpi-sub">Net revenue, please review it</p></div>
</div>

<div class="card">
    <h2>By sales channel</h2>
    <table>
        <thead><tr><th>Channel</th><th class="r">Net revenue</th><th>Share</th></tr></thead>
        <tbody>
        <tr><td>Store (POS)</td><td class="r"><fmt:formatNumber value="${report.store}" pattern="#,##0"/></td><td><progress class="bar" value="${report.store}" max="${report.taxable > 0 ? report.taxable : 1}"></progress></td></tr>
        <tr><td>Shopee</td><td class="r"><fmt:formatNumber value="${report.shopee}" pattern="#,##0"/></td><td><progress class="bar" value="${report.shopee}" max="${report.taxable > 0 ? report.taxable : 1}"></progress></td></tr>
        <tr><td>TikTok</td><td class="r"><fmt:formatNumber value="${report.tiktok}" pattern="#,##0"/></td><td><progress class="bar" value="${report.tiktok}" max="${report.taxable > 0 ? report.taxable : 1}"></progress></td></tr>
        </tbody>
    </table>
</div>

<div class="card">
    <h2>Monthly detail</h2>
    <table>
        <thead><tr><th>Month</th><th class="r">Store</th><th class="r">Shopee</th><th class="r">TikTok</th><th class="r">Discounts</th><th class="r">Cancelled / returned</th><th class="r">Taxable revenue</th><th>Compare</th></tr></thead>
        <tbody>
        <c:forEach var="m" items="${report.months}">
            <tr>
                <td>${m.ym}</td>
                <td class="r"><fmt:formatNumber value="${m.store}" pattern="#,##0"/></td>
                <td class="r"><fmt:formatNumber value="${m.shopee}" pattern="#,##0"/></td>
                <td class="r"><fmt:formatNumber value="${m.tiktok}" pattern="#,##0"/></td>
                <td class="r">-<fmt:formatNumber value="${m.discounts}" pattern="#,##0"/></td>
                <td class="r"><fmt:formatNumber value="${m.removed}" pattern="#,##0"/></td>
                <td class="r"><b><fmt:formatNumber value="${m.taxable}" pattern="#,##0"/></b></td>
                <td><progress class="bar" value="${m.taxable}" max="${maxMonth}"></progress></td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
    <p class="hint">Amounts are in VND. Cancelled and returned orders are for reference only and are not part of the taxable revenue.</p>
</div>

<div class="card">
    <h2>How it is calculated</h2>
    <ul class="plain">
        <li>Data source: completed counter orders (without VAT added on top) and imported Shopee and TikTok orders that are completed.</li>
        <li>Revenue used for tax = net store revenue + marketplace revenue.</li>
        <li>Gross revenue = revenue used for tax + discounts.</li>
        <li>Excluded: cancelled orders and cancelled or returned marketplace orders. The system has no refunds for counter orders yet.</li>
        <li>This summary is for reference. It is not a tax return and the system does not file anything for you.</li>
    </ul>
    <c:forEach var="w" items="${warnings}"><div class="alert-line"><c:out value="${w}"/></div></c:forEach>
</div>
<script>
    (function () {
        var sel = document.getElementById('rangeSelect');
        sel.addEventListener('change', function () {
            var custom = sel.value === 'custom';
            document.getElementById('fromInput').disabled = !custom;
            document.getElementById('toInput').disabled = !custom;
        });
    })();
</script>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
