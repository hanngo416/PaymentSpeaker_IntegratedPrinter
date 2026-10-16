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
<div class="card home-welcome-card">
    <div class="home-welcome-header">
        <div>
            <div class="home-welcome-role"><c:out value="${sessionScope.loginUser.role}"/></div>
            <h1 class="home-welcome-title">Welcome, <c:out value="${sessionScope.loginUser.fullName}"/></h1>
            <p class="home-welcome-sub">User Code: <code><c:out value="${sessionScope.loginUser.userCode}"/></code></p>
        </div>
    </div>

    <div class="home-quick-actions">
        <span class="home-quick-heading">Quick Actions</span>
        <div class="quick-nav-grid">
            <c:if test="${role == 'CASHIER' || role == 'ADMIN'}">
                <a href="${ctx}/orders/new" class="quick-nav-item primary">
                    <span class="quick-nav-icon">
                        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <line x1="12" y1="5" x2="12" y2="19"></line>
                            <line x1="5" y1="12" x2="19" y2="12"></line>
                        </svg>
                    </span>
                    <span class="quick-nav-content">
                        <strong>New Order</strong>
                        <small>Generate VietQR</small>
                    </span>
                </a>
            </c:if>

            <c:if test="${role == 'CASHIER' || role == 'STORE_MANAGER' || role == 'ADMIN'}">
                <a href="${ctx}/shift" class="quick-nav-item">
                    <span class="quick-nav-icon">
                        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <circle cx="12" cy="12" r="10"></circle>
                            <polyline points="12 6 12 12 16 14"></polyline>
                        </svg>
                    </span>
                    <span class="quick-nav-content">
                        <strong>Shift &amp; Cash</strong>
                        <small>Open / end shift</small>
                    </span>
                </a>
            </c:if>

            <c:if test="${role != 'TECHNICIAN'}">
                <a href="${ctx}/transactions" class="quick-nav-item">
                    <span class="quick-nav-icon">
                        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <line x1="12" y1="1" x2="12" y2="23"></line>
                            <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path>
                        </svg>
                    </span>
                    <span class="quick-nav-content">
                        <strong>Transactions</strong>
                        <small>Ledger history</small>
                    </span>
                </a>
            </c:if>

            <c:if test="${role == 'ADMIN' || role == 'STORE_MANAGER' || role == 'AUDITOR'}">
                <a href="${ctx}/alerts" class="quick-nav-item ${newAlerts > 0 ? 'has-alert' : ''}">
                    <span class="quick-nav-icon alert-icon">
                        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
                            <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
                        </svg>
                    </span>
                    <span class="quick-nav-content">
                        <strong>Alert Center</strong>
                        <small><c:choose><c:when test="${newAlerts > 0}"><span class="quick-alert-count">${newAlerts} new</span></c:when><c:otherwise>All clear</c:otherwise></c:choose></small>
                    </span>
                </a>
            </c:if>

            <c:if test="${role == 'ADMIN' || role == 'STORE_MANAGER'}">
                <a href="${ctx}/admin/users" class="quick-nav-item">
                    <span class="quick-nav-icon">
                        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                            <circle cx="9" cy="7" r="4"></circle>
                            <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                            <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                        </svg>
                    </span>
                    <span class="quick-nav-content">
                        <strong>Users</strong>
                        <small>Manage staff</small>
                    </span>
                </a>
            </c:if>

            <c:if test="${role == 'ADMIN' || role == 'AUDITOR'}">
                <a href="${ctx}/invoices" class="quick-nav-item">
                    <span class="quick-nav-icon">
                        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                            <polyline points="14 2 14 8 20 8"></polyline>
                            <line x1="16" y1="13" x2="8" y2="13"></line>
                            <line x1="16" y1="17" x2="8" y2="17"></line>
                        </svg>
                    </span>
                    <span class="quick-nav-content">
                        <strong>Invoices</strong>
                        <small>Period reports</small>
                    </span>
                </a>
            </c:if>

            <c:if test="${role == 'ADMIN' || role == 'AUDITOR'}">
                <a href="${ctx}/admin/audit" class="quick-nav-item">
                    <span class="quick-nav-icon">
                        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
                            <polyline points="9 12 11 14 15 10"></polyline>
                        </svg>
                    </span>
                    <span class="quick-nav-content">
                        <strong>Audit Log</strong>
                        <small>Hash chain</small>
                    </span>
                </a>
            </c:if>

            <c:if test="${role == 'ADMIN' || role == 'STORE_MANAGER' || role == 'CASHIER' || role == 'TECHNICIAN'}">
                <a href="${ctx}/settings" class="quick-nav-item">
                    <span class="quick-nav-icon">
                        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <circle cx="12" cy="12" r="3"></circle>
                            <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
                        </svg>
                    </span>
                    <span class="quick-nav-content">
                        <strong>Settings</strong>
                        <small>Audio &amp; devices</small>
                    </span>
                </a>
            </c:if>
        </div>
    </div>
</div>

<c:if test="${canTransactions}">
    <div class="kpis">
        <div class="card">
            <span class="kpi-label">Valid amount today</span>
            <div class="kpi-value accent">${validToday} <span class="kpi-unit">VND</span></div>
            <p class="kpi-sub">COMPLETED transactions</p>
        </div>
        <div class="card">
            <span class="kpi-label">Transactions today</span>
            <div class="kpi-value">${countToday}</div>
            <p class="kpi-sub">all statuses</p>
        </div>
        <div class="card">
            <span class="kpi-label">Flagged amount today</span>
            <div class="kpi-value ${flaggedToday == '0' ? '' : 'bad'}">${flaggedToday} <span class="kpi-unit">VND</span></div>
            <p class="kpi-sub">any status other than COMPLETED</p>
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
        <div class="head-row">
            <h2>Latest transactions</h2>
            <a href="${ctx}/transactions">View all transactions</a>
        </div>
        <table>
            <thead>
            <tr>
                <th style="width: 170px;">Time</th><th style="width: 130px;">Order</th><th style="width: 140px;">Amount (VND)</th><th>Content</th><th style="width: 100px;">Status</th><th style="width: 90px; text-align: center;">Action</th>
            </tr>
            </thead>
            <tbody>
            <c:set var="canPrintRole" value="${sessionScope.loginUser.role == 'ADMIN' || sessionScope.loginUser.role == 'STORE_MANAGER' || sessionScope.loginUser.role == 'CASHIER'}"/>
            <c:forEach var="t" items="${recent}">
                <c:set var="isCompleted" value="${t.status == 'LEGIT' || t.status == 'COMPLETED'}"/>
                <c:set var="displayStatus" value="${isCompleted ? 'COMPLETED' : 'CANCELLED'}"/>
                <c:set var="canPrintRecent" value="${canPrintRole && isCompleted && not empty t.orderCode && (sessionScope.loginUser.role != 'CASHIER' || t.createdBy == sessionScope.loginUser.userId)}"/>
                <tr>
                    <td><fmt:formatDate value="${t.transTime}" pattern="yyyy-MM-dd HH:mm:ss"/></td>
                    <td><c:out value="${empty t.orderCode ? '-' : t.orderCode}"/></td>
                    <td class="amount-cell"><fmt:formatNumber value="${t.amount}" pattern="#,##0"/></td>
                    <td><c:out value="${t.content}"/></td>
                    <td><span class="pill ${displayStatus}"><c:out value="${displayStatus}"/></span></td>
                    <td style="text-align: center; white-space: nowrap;">
                        <c:if test="${canPrintRecent}">
                            <form method="post" action="${ctx}/printer/print" target="_blank" class="inline">
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
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty recent}">
                <tr><td colspan="6">No transactions in the last 7 days.</td></tr>
            </c:if>
            </tbody>
        </table>
    </div>
</c:if>

<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
<script src="${ctx}/assets/js/chart-tip.js?v=2"></script>
</body>
</html>
