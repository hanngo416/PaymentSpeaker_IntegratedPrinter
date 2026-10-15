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
            <p>Shift <strong><c:out value="${shift.id}"/></strong> started by <strong><c:out value="${shift.openedBy}"/></strong>
                at <fmt:formatDate value="${shift.openedAt}" pattern="yyyy-MM-dd HH:mm"/>.</p>
            <%@ include file="/WEB-INF/views/shift/summary.jspf" %>
            <form method="post" action="${ctx}/shift" class="filters">
                <input type="hidden" name="action" value="close">
                <label>Cash counted in the drawer (VND)
                    <input type="text" name="counted" inputmode="numeric" required placeholder="0 if the drawer is empty">
                </label>
                <button type="submit" class="primary">End shift and print</button>
            </form>
            <c:if test="${isAdmin}">
                <form method="post" action="${ctx}/shift"
                      onsubmit="return confirm('Close this shift without a cash count?');">
                    <input type="hidden" name="action" value="force">
                    <button type="submit" class="danger">Force close (admin)</button>
                    <span class="hint">For a shift that was forgotten. The drawer is not counted.</span>
                </form>
            </c:if>
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
        <tr><th>Shift</th><th>Opened</th><th>By</th><th>Closed</th><th>By</th><th>Counted cash</th><th>Summary</th></tr>
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
