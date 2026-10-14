<%--
    Document   : home
    Created on : Oct 10, 2026, 9:00:00 AM
    Author     : PC
--%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Home"/>
<c:set var="nav" value="home"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<div class="card">
    <h1>Welcome, <c:out value="${sessionScope.loginUser.fullName}"/> (<c:out value="${sessionScope.loginUser.userCode}"/>)</h1>
    <p>Role: <strong><c:out value="${sessionScope.loginUser.role}"/></strong></p>
</div>

<c:if test="${canTransactions}">
    <div class="kpis">
        <div class="card">
            <span class="kpi-label">Valid amount today</span>
            <div class="kpi-value accent">${validToday} <span class="kpi-unit">VND</span></div>
            <p class="kpi-sub">LEGIT transactions</p>
        </div>
        <div class="card">
            <span class="kpi-label">Transactions today</span>
            <div class="kpi-value">${countToday}</div>
            <p class="kpi-sub">all labels</p>
        </div>
        <div class="card">
            <span class="kpi-label">Flagged amount today</span>
            <div class="kpi-value ${flaggedToday == '0' ? '' : 'bad'}">${flaggedToday} <span class="kpi-unit">VND</span></div>
            <p class="kpi-sub">any label other than LEGIT</p>
        </div>
        <c:if test="${canAlerts}">
            <div class="card">
                <span class="kpi-label">New alerts</span>
                <div class="kpi-value ${newAlerts == 0 ? '' : 'bad'}">${newAlerts}</div>
                <p class="kpi-sub"><a href="${ctx}/alerts?status=NEW">Open alert center</a></p>
            </div>
        </c:if>
    </div>

    <div class="card">
        <div class="head-row">
            <h2>Daily amount received</h2>
            <a href="${ctx}/transactions">View all transactions</a>
        </div>
        <c:choose>
            <c:when test="${empty bars}">
                <p>No transactions in the last 7 days.</p>
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
        <div class="head-row">
            <h2>Latest transactions</h2>
            <a href="${ctx}/transactions">View all transactions</a>
        </div>
        <table>
            <thead>
            <tr>
                <th>Time</th><th>Order</th><th>Amount (VND)</th><th>Content</th><th>Label</th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="t" items="${recent}">
                <tr>
                    <td><fmt:formatDate value="${t.transTime}" pattern="yyyy-MM-dd HH:mm:ss"/></td>
                    <td><c:out value="${empty t.orderCode ? '-' : t.orderCode}"/></td>
                    <td class="num"><fmt:formatNumber value="${t.amount}" pattern="#,##0"/></td>
                    <td><c:out value="${t.content}"/></td>
                    <td><span class="pill ${t.status}"><c:out value="${t.status}"/></span></td>
                </tr>
            </c:forEach>
            <c:if test="${empty recent}">
                <tr><td colspan="5">No transactions in the last 7 days.</td></tr>
            </c:if>
            </tbody>
        </table>
    </div>
</c:if>

<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
<script src="${ctx}/assets/js/chart-tip.js"></script>
</body>
</html>
