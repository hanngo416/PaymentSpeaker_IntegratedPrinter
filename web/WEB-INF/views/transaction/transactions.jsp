<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Transactions"/>
<c:set var="nav" value="transactions"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<div class="kpis">
    <div class="card">
        <span class="kpi-label">Valid amount in this range</span>
        <div class="kpi-value accent">${sumValid} <span class="kpi-unit">VND</span></div>
        <p class="kpi-sub">COMPLETED transactions</p>
    </div>
    <div class="card">
        <span class="kpi-label">Flagged amount in this range</span>
        <div class="kpi-value ${sumFlagged == '0' ? '' : 'bad'}">${sumFlagged} <span class="kpi-unit">VND</span></div>
        <p class="kpi-sub">any status other than COMPLETED</p>
    </div>
    <div class="card">
        <span class="kpi-label">Transactions</span>
        <div class="kpi-value">${result.total}</div>
        <p class="kpi-sub">matching the filter</p>
    </div>
</div>

<div class="card">
    <h1>Transactions</h1>
    <form method="get" action="${ctx}/transactions" class="filters">
        <label>From
            <input type="date" name="from" value="${fromText}">
        </label>
        <label>To
            <input type="date" name="to" value="${toText}">
        </label>
        <label>Status
            <select name="status">
                <option value="">All</option>
                <c:forEach var="l" items="${labels}">
                    <option value="${l}" ${f.label == l ? 'selected' : ''}>${l}</option>
                </c:forEach>
            </select>
        </label>
        <button type="submit" class="primary">Filter</button>
        <a class="btn" href="${ctx}/transactions">Reset</a>
    </form>
</div>

<div class="card">
    <h2>Daily amount received</h2>
    <c:choose>
        <c:when test="${empty bars}">
            <p>No transactions in this range.</p>
        </c:when>
        <c:otherwise>
            <svg class="chart" viewBox="0 0 ${chartWidth} ${chartTop + chartHeight + 36}" role="img" aria-label="Daily amount received">
                <c:forEach var="t" items="${ticks}">
                    <c:if test="${t.y < chartTop + chartHeight}">
                        <line class="grid" x1="${chartLeft}" y1="${t.y}" x2="${chartWidth}" y2="${t.y}" stroke="var(--chart-axis)" stroke-dasharray="3,3" opacity="0.6"/>
                    </c:if>
                    <text x="${chartLeft - 8}" y="${t.y + 4}" text-anchor="end"><c:out value="${t.label}"/></text>
                </c:forEach>
                <line class="axis" x1="${chartLeft}" y1="${chartTop + chartHeight}" x2="${chartWidth}" y2="${chartTop + chartHeight}"/>
                <line class="axis" x1="${chartLeft}" y1="${chartTop}" x2="${chartLeft}" y2="${chartTop + chartHeight}"/>
                <c:forEach var="b" items="${bars}">
                    <g class="bar" data-tip="<c:out value='${b.tip}'/>">
                        <rect class="hit" x="${b.hitX}" y="${chartTop}" width="${b.hitWidth}" height="${chartHeight}"/>
                        <c:if test="${b.legitHeight > 0}">
                            <rect class="ok" x="${b.legitX}" y="${b.legitY}" width="${b.barWidth}" height="${b.legitHeight}" rx="3"/>
                        </c:if>
                        <c:if test="${b.flaggedHeight > 0}">
                            <rect class="flag" x="${b.flaggedX}" y="${b.flaggedY}" width="${b.barWidth}" height="${b.flaggedHeight}" rx="3"/>
                        </c:if>
                    </g>
                    <text x="${b.labelX}" y="${chartTop + chartHeight + 18}" text-anchor="middle"><c:out value="${b.label}"/></text>
                </c:forEach>
            </svg>
            <div id="tip"></div>
            <div class="legend">
                <span><i class="ok"></i>Valid (COMPLETED)</span>
                <span><i class="flag"></i>Flagged (CANCELLED)</span>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<div class="card">
    <table>
        <thead>
        <tr>
            <th style="width: 170px;">Time</th><th style="width: 120px;">Order</th><th style="width: 130px;">Amount (VND)</th><th>Content</th><th style="width: 110px;">Status</th><th style="width: 130px;">Bank ID</th><th style="width: 85px; text-align: center;">Action</th>
        </tr>
        </thead>
        <tbody>
        <c:set var="canPrintRole" value="${sessionScope.loginUser.role == 'ADMIN' || sessionScope.loginUser.role == 'STORE_MANAGER' || sessionScope.loginUser.role == 'CASHIER'}"/>
        <c:forEach var="t" items="${result.items}">
            <fmt:formatDate var="when" value="${t.transTime}" pattern="yyyy-MM-dd HH:mm:ss"/>
            <fmt:formatNumber var="amt" value="${t.amount}" pattern="#,##0"/>
            <c:set var="isCompleted" value="${t.status == 'LEGIT' || t.status == 'COMPLETED'}"/>
            <c:set var="displayStatus" value="${isCompleted ? 'COMPLETED' : 'CANCELLED'}"/>
            <c:set var="canPrintRow" value="${canPrintRole && isCompleted && not empty t.orderCode && (sessionScope.loginUser.role != 'CASHIER' || t.createdBy == sessionScope.loginUser.userId)}"/>
            <tr class="tx-row" tabindex="0"
                data-id="${t.transId}"
                data-can-print="${canPrintRow ? '1' : '0'}"
                data-amount="<c:out value='${amt}'/>"
                data-bank-id="<c:out value='${t.bankTransId}'/>"
                data-time="<c:out value='${when}'/>"
                data-order="<c:out value='${empty t.orderCode ? "-" : t.orderCode}'/>"
                data-content="<c:out value='${t.content}'/>"
                data-status="<c:out value='${displayStatus}'/>"
                data-label="<c:out value='${displayStatus}'/>">
                <td><c:out value="${when}"/></td>
                <td><c:out value="${empty t.orderCode ? '-' : t.orderCode}"/></td>
                <td class="amount-cell"><c:out value="${amt}"/></td>
                <td><c:out value="${t.content}"/></td>
                <td><span class="pill ${displayStatus}"><c:out value="${displayStatus}"/></span></td>
                <td><c:out value="${t.bankTransId}"/></td>
                <td style="text-align: center; white-space: nowrap;" onclick="event.stopPropagation()">
                    <c:if test="${canPrintRow}">
                        <form method="post" action="${ctx}/printer/print" target="_blank" class="inline" onclick="event.stopPropagation()">
                            <input type="hidden" name="id" value="${t.transId}">
                            <button type="submit" class="btn-action btn-print" title="Print receipt for ${t.orderCode}">
                                <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" style="margin-right: 4px; vertical-align: -2px;">
                                    <polyline points="6 9 6 2 18 2 18 9"></polyline>
                                    <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"></path>
                                    <rect x="6" y="14" width="12" height="8"></rect>
                                </svg>Print
                            </button>
                        </form>
                    </c:if>
                    <c:if test="${!canPrintRow}"><small class="hint">No receipt</small></c:if>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty result.items}">
            <tr><td colspan="7">No transactions match the filter.</td></tr>
        </c:if>
        </tbody>
    </table>
    <p>${result.total} transaction(s), page ${result.page} of ${result.totalPages}</p>
    <div class="pages">
        <c:forEach begin="1" end="${result.totalPages}" var="p">
            <c:choose>
                <c:when test="${p == result.page}"><span>${p}</span></c:when>
                <c:otherwise><a href="${ctx}/transactions?${filterQuery}page=${p}">${p}</a></c:otherwise>
            </c:choose>
        </c:forEach>
    </div>
</div>

<div class="modal-backdrop" id="txModal" hidden>
    <div class="modal" role="dialog" aria-modal="true" aria-labelledby="mTitle">
        <button type="button" class="modal-close" id="txModalClose" aria-label="Close">&times;</button>
        <p class="modal-kicker" id="mTitle">Transaction details</p>
        <div class="modal-amount" id="mAmount"></div>
        <p><span id="mStatus" class="pill"></span></p>
        <dl>
            <dt>Bank ID</dt><dd id="mBankId"></dd>
            <dt>Time</dt><dd id="mTime"></dd>
            <dt>Order</dt><dd id="mOrder"></dd>
            <dt>Content</dt><dd id="mContent"></dd>
        </dl>
        <div id="mPrintWrapper" style="margin-top: 16px; padding-top: 14px; border-top: 1px solid var(--line); display: none; justify-content: flex-end;">
            <form id="modalPrintForm" method="post" action="${ctx}/printer/print" target="_blank" class="inline">
                <input type="hidden" name="id" id="modalPrintTransId" value="">
                <button type="submit" class="btn-action btn-print" style="padding: 6px 14px; font-size: var(--fs-sm);">
                    <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" style="margin-right: 6px; vertical-align: -2px;">
                        <polyline points="6 9 6 2 18 2 18 9"></polyline>
                        <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"></path>
                        <rect x="6" y="14" width="12" height="8"></rect>
                    </svg>Print receipt
                </button>
            </form>
        </div>
    </div>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
<script src="${ctx}/assets/js/chart-tip.js?v=2"></script>
<script src="${ctx}/assets/js/tx-detail.js"></script>
</body>
</html>
