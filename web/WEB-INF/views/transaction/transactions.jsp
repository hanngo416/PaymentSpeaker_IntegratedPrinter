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
        <p class="kpi-sub">LEGIT transactions</p>
    </div>
    <div class="card">
        <span class="kpi-label">Flagged amount in this range</span>
        <div class="kpi-value ${sumFlagged == '0' ? '' : 'bad'}">${sumFlagged} <span class="kpi-unit">VND</span></div>
        <p class="kpi-sub">any label other than LEGIT</p>
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
        <label>Device
            <select name="device">
                <option value="">All</option>
                <c:forEach var="dv" items="${devices}">
                    <option value="${dv.deviceId}" ${f.deviceId == dv.deviceId ? 'selected' : ''}><c:out value="${dv.deviceCode}"/></option>
                </c:forEach>
            </select>
        </label>
        <label>Label
            <select name="label">
                <option value="">All</option>
                <c:forEach var="l" items="${labels}">
                    <option value="${l}" ${f.label == l.name() ? 'selected' : ''}>${l}</option>
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
            <svg class="chart" viewBox="0 0 ${chartWidth} ${chartHeight + 36}" role="img" aria-label="Daily amount received">
                <line class="axis" x1="${chartLeft}" y1="${chartHeight}" x2="${chartWidth}" y2="${chartHeight}"/>
                <line class="axis" x1="${chartLeft}" y1="0" x2="${chartLeft}" y2="${chartHeight}"/>
                <text x="${chartLeft - 6}" y="12" text-anchor="end">${chartMax}</text>
                <text x="${chartLeft - 6}" y="${chartHeight}" text-anchor="end">0</text>
                <c:forEach var="b" items="${bars}">
                    <g class="bar" data-tip="<c:out value='${b.tip}'/>">
                        <rect class="hit" x="${b.x - 4}" y="0" width="${b.width + 8}" height="${chartHeight}"/>
                        <rect class="ok" x="${b.x}" y="${b.legitY}" width="${b.width}" height="${b.legitHeight}" rx="2"/>
                        <rect class="flag" x="${b.x}" y="${b.flaggedY}" width="${b.width}" height="${b.flaggedHeight}" rx="2"/>
                    </g>
                    <text x="${b.x + b.width / 2}" y="${chartHeight + 16}" text-anchor="middle"><c:out value="${b.label}"/></text>
                </c:forEach>
            </svg>
            <div id="tip"></div>
            <div class="legend">
                <span><i class="ok"></i>Valid (LEGIT)</span>
                <span><i class="flag"></i>Flagged (any other label)</span>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<div class="card">
    <table>
        <thead>
        <tr>
            <th>Time</th><th>Device</th><th>Order</th><th>Amount (VND)</th><th>Content</th><th>Label</th><th>Bank ID</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="t" items="${result.items}">
            <fmt:formatDate var="when" value="${t.transTime}" pattern="yyyy-MM-dd HH:mm:ss"/>
            <fmt:formatNumber var="amt" value="${t.amount}" pattern="#,##0"/>
            <tr class="tx-row" tabindex="0"
                data-amount="<c:out value='${amt}'/>"
                data-bank-id="<c:out value='${t.bankTransId}'/>"
                data-time="<c:out value='${when}'/>"
                data-device="<c:out value='${empty t.deviceCode ? "-" : t.deviceCode}'/>"
                data-order="<c:out value='${empty t.orderCode ? "-" : t.orderCode}'/>"
                data-content="<c:out value='${t.content}'/>"
                data-label="<c:out value='${t.status}'/>">
                <td><c:out value="${when}"/></td>
                <td><c:out value="${empty t.deviceCode ? '-' : t.deviceCode}"/></td>
                <td><c:out value="${empty t.orderCode ? '-' : t.orderCode}"/></td>
                <td class="amount-cell"><c:out value="${amt}"/></td>
                <td><c:out value="${t.content}"/></td>
                <td><span class="pill ${t.status}"><c:out value="${t.status}"/></span></td>
                <td><c:out value="${t.bankTransId}"/></td>
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
        <p><span id="mLabel" class="pill"></span></p>
        <dl>
            <dt>Bank ID</dt><dd id="mBankId"></dd>
            <dt>Time</dt><dd id="mTime"></dd>
            <dt>Device</dt><dd id="mDevice"></dd>
            <dt>Order</dt><dd id="mOrder"></dd>
            <dt>Content</dt><dd id="mContent"></dd>
        </dl>
    </div>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
<script src="${ctx}/assets/js/chart-tip.js"></script>
<script src="${ctx}/assets/js/tx-detail.js"></script>
</body>
</html>
