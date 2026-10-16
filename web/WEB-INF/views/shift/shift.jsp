<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Shift"/>
<c:set var="nav" value="shift"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<div class="card">
    <h1>Shift</h1>
    <c:if test="${not empty message}"><div class="msg"><c:out value="${message}"/></div></c:if>

    <c:choose>
        <c:when test="${shift == null}">
            <div class="alert-banner">No shift is open. Orders can only be created while a shift is open.</div>
            <p class="hint">Press Start shift when you take over the counter. The shift lasts until someone presses End shift and prints the summary.</p>
            <form method="post" action="${ctx}/shift">
                <input type="hidden" name="action" value="open">
                <button type="submit" class="primary">Start shift</button>
            </form>
        </c:when>
        <c:otherwise>
            <div class="report-header" style="margin-top: 10px;">
                <div>
                    <span class="report-kicker">ACTIVE SHIFT</span>
                    <h2 class="report-title">Shift <c:out value="${shift.id}"/></h2>
                    <p class="hint" style="margin: 4px 0 0;">Started by <strong><c:out value="${shift.openedBy}"/></strong> at <fmt:formatDate value="${shift.openedAt}" pattern="yyyy-MM-dd HH:mm"/>.</p>
                </div>
                <span class="pill NEW" style="font-size: var(--fs-xs); padding: 4px 10px;">IN PROGRESS</span>
            </div>
            <%@ include file="/WEB-INF/views/shift/summary.jspf" %>
            <div class="shift-close-section">
                <div class="shift-close-header">
                    <div>
                        <h3 class="shift-close-title">
                            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <rect x="2" y="6" width="20" height="12" rx="2"></rect>
                                <circle cx="12" cy="12" r="2"></circle>
                                <path d="M6 12h.01M18 12h.01"></path>
                            </svg>
                            End Shift &amp; Cash Reconciliation
                        </h3>
                        <p class="shift-close-subtitle">Count the physical currency in the cash drawer to close out the shift and generate the print receipt.</p>
                    </div>
                </div>

                <form method="post" action="${ctx}/shift" class="shift-close-form">
                    <input type="hidden" name="action" value="close">
                    <div class="shift-close-row">
                        <div class="shift-input-col">
                            <label for="cashCounted" class="pos-field-label">Actual cash counted in drawer</label>
                            <div class="shift-amount-input-wrap">
                                <input id="cashCounted" class="shift-amount-input" type="text" name="counted" inputmode="numeric"
                                       placeholder="0" ${isAdmin ? '' : 'required'} autocomplete="off">
                                <span class="shift-currency-suffix">VND</span>
                            </div>
                            <span class="hint">Type 0 if the drawer is empty or all orders were paid via VietQR.<c:if test="${isAdmin}"> Admin may leave this empty.</c:if></span>
                        </div>
                        <div class="shift-btn-col">
                            <button type="submit" class="btn-end-shift">
                                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <polyline points="6 9 6 2 18 2 18 9"></polyline>
                                    <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"></path>
                                    <rect x="6" y="14" width="12" height="8"></rect>
                                </svg>
                                <span>End shift and print</span>
                            </button>
                        </div>
                    </div>
                </form>

                <c:if test="${isAdmin}">
                    <div class="shift-force-bar">
                        <div class="force-text">
                            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                                <line x1="12" y1="9" x2="12" y2="13"></line>
                                <line x1="12" y1="17" x2="12.01" y2="17"></line>
                            </svg>
                            <span>Forgotten shift? Close immediately without drawer cash audit.</span>
                        </div>
                        <form method="post" action="${ctx}/shift" onsubmit="return confirm('Close this shift without a cash count?');" class="inline">
                            <input type="hidden" name="action" value="force">
                            <button type="submit" class="btn-action btn-danger-soft">Force close (admin)</button>
                        </form>
                    </div>
                </c:if>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<div class="card">
    <h2>Print a whole day</h2>
    <form method="get" action="${ctx}/shift/print" target="_blank" class="filters">
        <label>Day
            <input type="date" name="date" value="${today}" required>
        </label>
        <button type="submit">Print day summary</button>
    </form>
    <p class="hint">Adds up every shift of that day.</p>
</div>

<div class="card">
    <h2>Recent shifts</h2>
    <table>
        <thead>
        <tr><th>Shift</th><th>Opened</th><th>By</th><th>Closed</th><th>By</th><th class="num" style="text-align: right;">Counted cash</th><th>Summary</th></tr>
        </thead>
        <tbody>
        <c:forEach var="s" items="${recent}">
            <tr>
                <td><c:out value="${s.id}"/></td>
                <td><fmt:formatDate value="${s.openedAt}" pattern="yyyy-MM-dd HH:mm"/></td>
                <td><c:out value="${s.openedBy}"/></td>
                <td><c:choose>
                    <c:when test="${s.open}"><span class="pill NEW">OPEN</span></c:when>
                    <c:otherwise><fmt:formatDate value="${s.closedAt}" pattern="yyyy-MM-dd HH:mm"/></c:otherwise>
                </c:choose></td>
                <td><c:out value="${s.closedBy}"/></td>
                <td class="num"><c:choose>
                    <c:when test="${s.countedCash != null}"><fmt:formatNumber value="${s.countedCash}" pattern="#,##0"/></c:when>
                    <c:otherwise>-</c:otherwise>
                </c:choose></td>
                <td><a class="btn" href="${ctx}/shift/print?id=${s.id}" target="_blank">Print</a></td>
            </tr>
        </c:forEach>
        <c:if test="${empty recent}">
            <tr><td colspan="7">No shift yet.</td></tr>
        </c:if>
        </tbody>
    </table>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
