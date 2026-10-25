<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Tax estimate"/>
<c:set var="nav" value="tax"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<c:set var="taxTab" value="estimate"/>
<%@ include file="/WEB-INF/views/common/tax-tabs.jspf" %>
<c:set var="e" value="${tax.estimate}"/>
<div class="card">
    <h1>Tax estimate ${year}</h1>
    <form method="get" action="${ctx}/tax" class="filters">
        <label>Year
            <select name="year">
                <c:forEach var="y" begin="${thisYear - 3}" end="${thisYear}">
                    <option value="${y}" ${y == year ? 'selected' : ''}>${y}</option>
                </c:forEach>
            </select>
        </label>
        <button type="submit" class="primary">Show</button>
    </form>
    <p class="hint">Sector: <c:out value="${tax.sector}"/>. Revenue counts completed orders only, without VAT added on top. Estimate only, not the amount to pay.</p>
</div>

<c:choose>
    <c:when test="${empty e}">
        <div class="card">
            <div class="msg">Tax rules incomplete, ask an administrator.</div>
            <p class="hint">A required rule is missing or no longer in force for the selected sector.</p>
        </div>
    </c:when>
    <c:otherwise>
        <div class="kpis">
            <div class="card">
                <span class="kpi-label">Revenue ${year}</span>
                <div class="kpi-value accent"><fmt:formatNumber value="${e.revenue}" pattern="#,##0"/> <span class="kpi-unit">VND</span></div>
                <p class="kpi-sub"><fmt:formatNumber value="${e.percent}" pattern="#,##0.00"/>% of the threshold</p>
            </div>
            <div class="card">
                <span class="kpi-label">Left before the threshold</span>
                <div class="kpi-value"><fmt:formatNumber value="${e.remaining}" pattern="#,##0"/> <span class="kpi-unit">VND</span></div>
                <p class="kpi-sub">Threshold <fmt:formatNumber value="${tax.params.threshold}" pattern="#,##0"/> VND</p>
            </div>
            <div class="card">
                <span class="kpi-label">Status</span>
                <div class="kpi-value ${e.status == 'UNDER' ? '' : 'bad'}">${e.status == 'UNDER' ? 'Tax free' : (e.status == 'OVER' ? 'Over threshold' : 'Over ceiling')}</div>
                <p class="kpi-sub">Warning level ${e.level} of 3</p>
            </div>
        </div>

        <div class="card">
            <h2>Progress to the threshold</h2>
            <progress class="tax-progress level${e.level}" value="${e.percent > 100 ? 100 : e.percent}" max="100"></progress>
            <p class="hint"><fmt:formatNumber value="${e.percent}" pattern="#,##0.00"/>%. Warning levels at <fmt:formatNumber value="${tax.params.warn1}" pattern="0.00"/>%, <fmt:formatNumber value="${tax.params.warn2}" pattern="0.00"/>% and <fmt:formatNumber value="${tax.params.warn3}" pattern="0.00"/>%.</p>
        </div>

        <div class="card">
            <h2>Estimate</h2>
            <table>
                <thead>
                <tr><th>Tax</th><th>Rate</th><th>Estimate (VND)</th></tr>
                </thead>
                <tbody>
                <tr>
                    <td>VAT</td>
                    <td><fmt:formatNumber value="${tax.params.vatRatePct}" pattern="0.00"/>%<c:if test="${tax.params.vatReductionPct.signum() > 0}"> less <fmt:formatNumber value="${tax.params.vatReductionPct}" pattern="0.00"/>%</c:if></td>
                    <td class="amount-cell"><fmt:formatNumber value="${e.vat}" pattern="#,##0"/></td>
                </tr>
                <tr>
                    <td>Personal income tax</td>
                    <td><fmt:formatNumber value="${tax.params.pitRatePct}" pattern="0.00"/>% of revenue above the threshold</td>
                    <td class="amount-cell">
                        <c:choose>
                            <c:when test="${empty e.pit}">Not estimated</c:when>
                            <c:otherwise><fmt:formatNumber value="${e.pit}" pattern="#,##0"/></c:otherwise>
                        </c:choose>
                    </td>
                </tr>
                </tbody>
            </table>
            <p class="hint">Estimate only, not the amount to pay.</p>
        </div>

        <div class="card" id="todo">
            <h2>What to do</h2>
            <ul>
                <c:forEach var="t" items="${tax.todo}">
                    <li><c:out value="${t}"/></li>
                </c:forEach>
            </ul>
        </div>
    </c:otherwise>
</c:choose>

<div class="card">
    <h2>Sources</h2>
    <table>
        <thead>
        <tr><th>Rule</th><th>Value</th><th>Source</th></tr>
        </thead>
        <tbody>
        <c:forEach var="r" items="${tax.sources}">
            <tr>
                <td><c:out value="${r.kind}"/><c:if test="${not empty r.sector}"> (<c:out value="${r.sector}"/>)</c:if></td>
                <td><fmt:formatNumber value="${r.value}" pattern="#,##0.##"/></td>
                <td class="wrap">
                    <c:out value="${r.sourceRef}"/>                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty tax.sources}">
            <tr><td colspan="3">No rules in force.</td></tr>
        </c:if>
        </tbody>
    </table>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
