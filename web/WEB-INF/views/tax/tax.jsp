<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Tax overview"/>
<c:set var="nav" value="tax"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<c:set var="taxTab" value="estimate"/>
<%@ include file="/WEB-INF/views/common/tax-tabs.jspf" %>
<c:set var="e" value="${tax.estimate}"/>

<div class="card" style="margin-bottom: 14px;">
    <div style="display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 12px;">
        <div>
            <h1 style="font-size: 22px; font-weight: 700; margin-bottom: 4px;">Tax overview</h1>
            <p class="hint" style="margin: 0;">Focus on selling. The system helps you understand, track, and prepare tax-related tasks.</p>
        </div>
        <div style="display: flex; gap: 8px; flex-wrap: wrap;">
            <a href="${ctx}/tax/notifications" class="btn">Notifications</a>
            <a href="${ctx}/tax/assistant" class="btn primary">Ask AI assistant</a>
        </div>
    </div>
    <form method="get" action="${ctx}/tax" class="filters" style="margin-top: 14px; display: flex; align-items: center; gap: 12px; flex-wrap: wrap;">
        <label style="display: flex; align-items: center; gap: 6px; font-weight: 600;">Year
            <select name="year" onchange="this.form.submit()">
                <c:forEach var="y" begin="${thisYear - 3}" end="${thisYear}">
                    <option value="${y}" ${y == year ? 'selected' : ''}>${y}</option>
                </c:forEach>
            </select>
        </label>
        <span class="hint">Sector: <b><c:out value="${tax.sector}"/></b> &bull; Revenue counts completed orders only, without VAT added on top. Estimate only, not the amount to pay.</span>
    </form>
</div>

<c:choose>
    <c:when test="${empty e}">
        <div class="card" style="margin-bottom: 14px;">
            <div class="msg">Tax rules incomplete, ask an administrator.</div>
            <p class="hint">A required rule is missing or no longer in force for the selected sector.</p>
        </div>
    </c:when>
    <c:otherwise>
        <div class="grid g3">
            <div class="card">
                <h3><c:out value="${empty profile['name'] ? 'Your Business' : profile['name']}"/></h3>
                <div class="sub"><c:out value="${empty profile['type'] ? 'Business type not set' : (profile['type'] == 'household' ? 'Household business' : (profile['type'] == 'company' ? 'Company' : (profile['type'] == 'individual' ? 'Individual business' : 'Unregistered')))}"/> &bull; <c:out value="${empty profile['industry'] ? 'Industry not set' : profile['industry']}"/></div>
                <table style="width: 100%; margin-bottom: 12px;">
                    <tbody>
                        <tr>
                            <td class="muted" style="width: 40%; padding: 4px 0;">Tax ID (MST)</td>
                            <td style="padding: 4px 0; font-weight: 600;"><c:out value="${empty profile['tin'] ? 'Not registered' : profile['tin']}"/></td>
                        </tr>
                        <tr>
                            <td class="muted" style="padding: 4px 0;">Status</td>
                            <td style="padding: 4px 0;">
                                <span class="tpill ${profile['regStatus'] == 'registered' ? 'ok' : (profile['regStatus'] == 'progress' ? 'warn' : 'muted')}">
                                    <c:out value="${empty profile['regStatus'] ? 'Not updated' : (profile['regStatus'] == 'registered' ? 'Registered' : (profile['regStatus'] == 'progress' ? 'In progress' : 'Not registered'))}"/>
                                </span>
                            </td>
                        </tr>
                    </tbody>
                </table>
                <div>
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
                        <span class="hint">Profile completeness</span>
                        <b style="font-size: 13px;">${completion}%</b>
                    </div>
                    <div class="hb single">
                        <span class="bar"><i style="width: ${completion}%;"></i></span>
                    </div>
                    <a href="${ctx}/tax/profile" class="btn sm" style="margin-top: 10px; display: inline-block;">Continue setup</a>
                </div>
            </div>

            <div class="card">
                <h3>Important figures</h3>
                <div class="sub">Net revenue = sales minus discounts, cancellations, and returns</div>
                <ul class="list">
                    <li>
                        <span>This month net revenue</span>
                        <b><fmt:formatNumber value="${monthRevenue}" pattern="#,##0"/> ₫</b>
                    </li>
                    <li>
                        <span>Year-to-date revenue</span>
                        <b><fmt:formatNumber value="${e.revenue}" pattern="#,##0"/> ₫</b>
                    </li>
                    <li>
                        <span>vs Configured threshold</span>
                        <b style="color: ${e.percent >= 90 ? 'var(--bad)' : (e.percent >= 70 ? 'var(--warn)' : 'var(--chart-ok)')};"><fmt:formatNumber value="${e.percent}" pattern="#,##0.0"/>%</b>
                    </li>
                    <li>
                        <span>Estimated tax</span>
                        <b class="hint" style="font-weight: 600;">
                            <c:choose>
                                <c:when test="${not empty e.vat or not empty e.pit}">
                                    ~ <fmt:formatNumber value="${(empty e.vat ? 0 : e.vat) + (empty e.pit ? 0 : e.pit)}" pattern="#,##0"/> ₫
                                </c:when>
                                <c:otherwise>Not estimated</c:otherwise>
                            </c:choose>
                        </b>
                    </li>
                </ul>
                <div class="hb single" style="margin-top: 10px;">
                    <span class="bar"><i class="${e.percent >= 90 ? 'bad' : (e.percent >= 70 ? 'warn' : 'alt')}" style="width: ${e.percent > 100 ? 100 : (e.percent < 0 ? 0 : e.percent)}%;"></i></span>
                </div>
            </div>

            <div class="card ai">
                <div class="tag">AI ASSISTANT TIP</div>
                <h3>What should you note this month?</h3>
                <p style="margin: 8px 0; font-size: 13px; line-height: 1.5; color: var(--navy);">
                    <c:choose>
                        <c:when test="${e.percent >= 90}">
                            Revenue has passed <b><fmt:formatNumber value="${e.percent}" pattern="#,##0.0"/>%</b> of the threshold. You should reconcile records and prepare e-invoice registration.
                        </c:when>
                        <c:when test="${e.percent >= 70}">
                            Revenue is approaching the threshold (<b><fmt:formatNumber value="${e.percent}" pattern="#,##0.0"/>%</b>). Monitor sales channels and save all expense invoices early.
                        </c:when>
                        <c:otherwise>
                            Operations are currently within the threshold (<fmt:formatNumber value="${e.percent}" pattern="#,##0.0"/>%). Keep daily sales slips and supplier invoices organized.
                        </c:otherwise>
                    </c:choose>
                </p>
                <a href="${ctx}/tax/assistant" class="btn primary" style="margin-top: 8px; display: inline-block;">Ask AI assistant</a>
            </div>
        </div>

        <div class="grid g2" style="margin-top: 14px;">
            <div class="card">
                <h3>Action center</h3>
                <div class="sub">Compliance tasks requiring attention</div>
                <ul class="list">
                    <c:if test="${completion < 100}">
                        <li>
                            <span>Tax profile is incomplete (${completion}%).</span>
                            <a href="${ctx}/tax/profile" class="btn sm primary">Complete profile</a>
                        </li>
                    </c:if>
                    <c:if test="${empty profile['type'] or profile['type'] == 'UNKNOWN' or profile['type'] == 'unregistered'}">
                        <li>
                            <span>Verify your obligations with the checker.</span>
                            <a href="${ctx}/tax/checker" class="btn sm primary">Check obligations</a>
                        </li>
                    </c:if>
                    <c:if test="${empty profile['einv'] or profile['einv'] == 'UNKNOWN' or profile['einv'] == 'no'}">
                        <li>
                            <span>Check e-invoice requirements.</span>
                            <a href="${ctx}/invoices/manage" class="btn sm">Check</a>
                        </li>
                    </c:if>
                    <c:if test="${e.percent >= 70}">
                        <li>
                            <span>Revenue reached <fmt:formatNumber value="${e.percent}" pattern="#,##0.0"/>% of the threshold.</span>
                            <a href="${ctx}/tax/reports" class="btn sm">View reports</a>
                        </li>
                    </c:if>
                    <li>
                        <span>Review fiscal deadlines on the calendar.</span>
                        <a href="${ctx}/tax/calendar" class="btn sm">View calendar</a>
                    </li>
                </ul>
            </div>

            <div class="card">
                <h3>Upcoming deadlines</h3>
                <div class="sub">Tax declaration milestones and periodic filings</div>
                <ul class="list">
                    <c:choose>
                        <c:when test="${not empty obligations}">
                            <c:forEach var="ob" items="${obligations}" begin="0" end="3">
                                <li>
                                    <span>
                                        <b><c:out value="${ob.obligation_type}"/> &bull; <c:out value="${ob.period_label}"/></b>
                                        <div class="m">Due: <c:out value="${ob.due_date}"/></div>
                                    </span>
                                    <span class="tpill ${ob.status == 'PAID' ? 'ok' : (ob.status == 'OVERDUE' ? 'bad' : 'warn')}">
                                        <c:out value="${ob.status}"/>
                                    </span>
                                </li>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>
                            <li>
                                <span>
                                    <b>Quarterly VAT &amp; PIT declaration</b>
                                    <div class="m">Quarterly milestone</div>
                                </span>
                                <span class="tpill warn">Awaiting filing</span>
                            </li>
                            <li>
                                <span>
                                    <b>Annual revenue notice</b>
                                    <div class="m">Due 31 January ${year + 1}</div>
                                </span>
                                <span class="tpill ok">Annual</span>
                            </li>
                        </c:otherwise>
                    </c:choose>
                </ul>
            </div>
        </div>

        <div class="card" style="margin-top: 14px;">
            <h3>Affiliate module relation</h3>
            <p class="hint" style="line-height: 1.5; margin: 6px 0 0;">
                Year-to-date affiliate commission calculated: <b><fmt:formatNumber value="${affiliateCommissionTotal}" pattern="#,##0"/> ₫</b>.
                This is an operating expense reference and does not automatically deduct from taxable sales revenue.
                Payouts to external affiliates may require personal income tax withholding (simulated <b>${affiliateWithholdPct}%</b> from <b><fmt:formatNumber value="${affiliateWithholdFrom}" pattern="#,##0"/> ₫</b>).
                Confirm specific withholding rules with an accountant.
            </p>
        </div>

        <div class="card" style="margin-top: 14px;">
            <details class="ins">
                <summary>Tax calculation estimate details &amp; Legal sources</summary>
                <div style="margin-top: 10px;">
                    <table style="width: 100%; margin-bottom: 16px;">
                        <thead>
                            <tr><th>Tax</th><th>Rate</th><th>Estimate (VND)</th></tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td>VAT</td>
                                <td><fmt:formatNumber value="${tax.params.vatRatePct}" pattern="0.00"/>%<c:if test="${tax.params.vatReductionPct.signum() > 0}"> less <fmt:formatNumber value="${tax.params.vatReductionPct}" pattern="0.00"/>%</c:if></td>
                                <td class="amount-cell"><fmt:formatNumber value="${e.vat}" pattern="#,##0"/> ₫</td>
                            </tr>
                            <tr>
                                <td>Personal income tax</td>
                                <td><fmt:formatNumber value="${tax.params.pitRatePct}" pattern="0.00"/>% of revenue above threshold</td>
                                <td class="amount-cell">
                                    <c:choose>
                                        <c:when test="${empty e.pit}">Not estimated</c:when>
                                        <c:otherwise><fmt:formatNumber value="${e.pit}" pattern="#,##0"/> ₫</c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                        </tbody>
                    </table>

                    <h4 style="font-size: 13px; font-weight: 700; margin: 12px 0 6px;">Legal rules in force</h4>
                    <table style="width: 100%;">
                        <thead>
                            <tr><th>Rule</th><th>Value</th><th>Source</th></tr>
                        </thead>
                        <tbody>
                            <c:forEach var="r" items="${tax.sources}">
                                <tr>
                                    <td><c:out value="${r.kind}"/><c:if test="${not empty r.sector}"> (<c:out value="${r.sector}"/>)</c:if></td>
                                    <td><fmt:formatNumber value="${r.value}" pattern="#,##0.##"/></td>
                                    <td class="wrap"><c:out value="${r.sourceRef}"/></td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty tax.sources}">
                                <tr><td colspan="3">No rules in force.</td></tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </details>
        </div>
    </c:otherwise>
</c:choose>

<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
