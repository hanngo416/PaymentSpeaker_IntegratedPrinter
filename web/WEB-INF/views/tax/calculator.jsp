<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Tax calculator"/>
<c:set var="nav" value="tax"/>
<c:set var="taxTab" value="calculator"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<%@ include file="/WEB-INF/views/common/tax-tabs.jspf" %>
<div class="card">
    <h1>Tax calculator</h1>
    <p class="hint">The numbers come from the tax rules in force (see Tax rules), never from the AI assistant. Every result is an estimate. Each saved calculation keeps its inputs and the exact rules used, so you can recalculate when a rule changes.</p>
    <p class="hint">Sector: <b><c:out value="${sector}"/></b>. Change it on the profile page.</p>
</div>

<div class="card narrow">
    <form method="post" action="${ctx}/tax/calculator">
        <label>Period
            <select name="period">
                <c:forEach var="p" items="${periods}"><option value="${p[0]}" ${param.period == p[0] ? 'selected' : ''}><c:out value="${p[1]}"/></option></c:forEach>
            </select>
        </label>
        <label>Revenue for the period (VND)
            <input type="text" name="revenue" inputmode="numeric" maxlength="16" value="<c:out value='${param.revenue}'/>" placeholder="Leave empty to use completed sales">
            <span class="hint">Empty means store orders plus online sales for that period.</span>
        </label>
        <label>Adjustment to deduct (VND)
            <input type="text" name="adjustment" inputmode="numeric" maxlength="16" value="<c:out value='${param.adjustment}'/>" placeholder="0">
            <span class="hint">Only if a rule allows it and you have documents. It cannot exceed the revenue.</span>
        </label>
        <c:if test="${not empty error}"><div class="field-error"><c:out value="${error}"/></div></c:if>
        <button type="submit" name="action" value="run" class="primary">Calculate</button>
    </form>
</div>

<c:if test="${not empty result}">
    <div class="card">
        <h2>Result <span class="pill PENDING">ESTIMATE</span></h2>
        <table class="kv">
            <tr><td>Revenue in the period</td><td><fmt:formatNumber value="${result.revenue}" pattern="#,##0"/> VND</td></tr>
            <tr><td>Adjustment</td><td><fmt:formatNumber value="${result.adjustment}" pattern="#,##0"/> VND</td></tr>
            <tr><td>Revenue earlier this year</td><td><fmt:formatNumber value="${result.prior}" pattern="#,##0"/> VND</td></tr>
            <tr><td>Revenue this year after the period</td><td><fmt:formatNumber value="${result.cumulative}" pattern="#,##0"/> VND (threshold <fmt:formatNumber value="${result.threshold}" pattern="#,##0"/>)</td></tr>
            <tr><td>VAT at <fmt:formatNumber value="${result.vatRate}" pattern="0.##"/>%</td><td><fmt:formatNumber value="${result.vat}" pattern="#,##0"/> VND</td></tr>
            <tr><td>PIT at <fmt:formatNumber value="${result.pitRate}" pattern="0.##"/>%</td><td><c:choose><c:when test="${result.ceiling}">Not estimated: revenue is above the ceiling, an income-based method is required.</c:when><c:otherwise><fmt:formatNumber value="${result.pit}" pattern="#,##0"/> VND</c:otherwise></c:choose></td></tr>
            <tr><td><b>Estimated total</b></td><td><b><fmt:formatNumber value="${result.vat + result.pit}" pattern="#,##0"/> VND</b></td></tr>
        </table>
        <p class="hint"><b>How it is worked out:</b> the tax is taken on this year's revenue up to the end of the period, minus what the same rules give for the revenue before the period. Rules used: <c:out value="${result.stamp}"/>. They are <b>${result.verified ? 'verified' : 'not all verified'}</b>.</p>
        <c:if test="${canManage}">
            <form method="post" action="${ctx}/tax/calculator" class="filters">
                <input type="hidden" name="period" value="${param.period}">
                <input type="hidden" name="revenue" value="<c:out value='${param.revenue}'/>">
                <input type="hidden" name="adjustment" value="<c:out value='${param.adjustment}'/>">
                <label>Note <input type="text" name="note" maxlength="200"></label>
                <button type="submit" name="action" value="save" class="primary">Save this calculation</button>
            </form>
        </c:if>
    </div>
</c:if>

<div class="card">
    <h2>Saved calculations</h2>
    <form method="get" action="${ctx}/tax/calculator" class="filters">
        <label>Compare A
            <select name="a"><option value="">-</option><c:forEach var="k" items="${calcs}"><option value="${k.calc_id}" ${param.a == k.calc_id ? 'selected' : ''}>#${k.calc_id} ${k.calc_date}</option></c:forEach></select>
        </label>
        <label>with B
            <select name="b"><option value="">-</option><c:forEach var="k" items="${calcs}"><option value="${k.calc_id}" ${param.b == k.calc_id ? 'selected' : ''}>#${k.calc_id} ${k.calc_date}</option></c:forEach></select>
        </label>
        <button type="submit">Compare</button>
    </form>
    <c:if test="${not empty cmpA && not empty cmpB}">
        <table>
            <thead><tr><th></th><th>#${cmpA.calc_id}</th><th>#${cmpB.calc_id}</th><th>Difference</th></tr></thead>
            <tbody>
            <tr><td>Revenue</td><td><fmt:formatNumber value="${cmpA.revenue}" pattern="#,##0"/></td><td><fmt:formatNumber value="${cmpB.revenue}" pattern="#,##0"/></td><td><fmt:formatNumber value="${cmpB.revenue - cmpA.revenue}" pattern="#,##0"/></td></tr>
            <tr><td>VAT</td><td><fmt:formatNumber value="${cmpA.vat_amount}" pattern="#,##0"/></td><td><fmt:formatNumber value="${cmpB.vat_amount}" pattern="#,##0"/></td><td><fmt:formatNumber value="${cmpB.vat_amount - cmpA.vat_amount}" pattern="#,##0"/></td></tr>
            <tr><td>PIT</td><td><fmt:formatNumber value="${cmpA.pit_amount}" pattern="#,##0"/></td><td><fmt:formatNumber value="${cmpB.pit_amount}" pattern="#,##0"/></td><td><fmt:formatNumber value="${cmpB.pit_amount - cmpA.pit_amount}" pattern="#,##0"/></td></tr>
            <tr><td><b>Total</b></td><td><fmt:formatNumber value="${cmpA.total_amount}" pattern="#,##0"/></td><td><fmt:formatNumber value="${cmpB.total_amount}" pattern="#,##0"/></td><td><fmt:formatNumber value="${cmpB.total_amount - cmpA.total_amount}" pattern="#,##0"/></td></tr>
            </tbody>
        </table>
    </c:if>
    <table>
        <thead><tr><th>#</th><th>Date</th><th>Period</th><th>Revenue</th><th>Total (estimate)</th><th>Rules used</th><th>Note</th><th></th></tr></thead>
        <tbody>
        <c:forEach var="k" items="${calcs}">
            <tr>
                <td>${k.calc_id}</td><td>${k.calc_date}</td><td><c:out value="${k.period_label}"/></td>
                <td><fmt:formatNumber value="${k.revenue}" pattern="#,##0"/></td>
                <td><fmt:formatNumber value="${k.total_amount}" pattern="#,##0"/></td>
                <td><span class="hint"><c:out value="${k.rule_stamp}"/></span></td>
                <td><c:out value="${k.note}"/></td>
                <td><c:if test="${canManage}"><form method="post" action="${ctx}/tax/calculator" class="inline"><input type="hidden" name="id" value="${k.calc_id}"><button type="submit" name="action" value="recalc">Recalculate</button></form></c:if></td>
            </tr>
        </c:forEach>
        <c:if test="${empty calcs}"><tr><td colspan="8">No saved calculations.</td></tr></c:if>
        </tbody>
    </table>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
