<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Invoices"/>
<c:set var="nav" value="invoices"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<div class="card">
    <h1>Invoices</h1>
    <c:if test="${not empty message}"><div class="msg"><c:out value="${message}"/></div></c:if>
    <c:if test="${not empty error}"><div class="alert-banner"><c:out value="${error}"/></div></c:if>
    <p class="hint">Totals of the paid orders against the money that really reached the account. Choose a day, a month or a year.</p>

    <form method="get" action="${ctx}/invoices" class="filters">
        <label>Period
            <select name="type">
                <option value="day" ${type == 'day' ? 'selected' : ''}>Day</option>
                <option value="month" ${type == 'month' ? 'selected' : ''}>Month</option>
                <option value="year" ${type == 'year' ? 'selected' : ''}>Year</option>
            </select>
        </label>
        <label>Day <input type="date" name="day" value="<c:out value='${day}'/>"></label>
        <label>Month <input type="month" name="month" value="<c:out value='${month}'/>"></label>
        <label>Year <input type="number" name="year" min="2000" max="2100" value="<c:out value='${year}'/>"></label>
        <button type="submit" class="primary">Show</button>
        <c:if test="${report != null}">
            <a class="btn" href="${ctx}/invoices?type=${type}&day=${day}&month=${month}&year=${year}&format=print" target="_blank">Print</a>
            <a class="btn" href="${ctx}/invoices?type=${type}&day=${day}&month=${month}&year=${year}&format=csv">Export CSV</a>
        </c:if>
    </form>
</div>

<c:if test="${report != null}">
    <div class="card">
        <h2><c:out value="${report.title}"/>: <c:out value="${report.period}"/></h2>
        <%@ include file="/WEB-INF/views/shift/summary.jspf" %>

        <c:if test="${not empty report.rows}">
            <table>
                <thead>
                <tr><th>${type == 'year' ? 'Month' : 'Day'}</th><th>Paid orders</th><th>Order total</th><th>Cash</th><th>Expected (QR)</th><th>Received</th></tr>
                </thead>
                <tbody>
                <c:forEach var="r" items="${report.rows}">
                    <tr>
                        <td><c:out value="${r.label}"/></td>
                        <td class="num">${r.orderCount}</td>
                        <td class="num"><fmt:formatNumber value="${r.total}" pattern="#,##0"/></td>
                        <td class="num"><fmt:formatNumber value="${r.cash}" pattern="#,##0"/></td>
                        <td class="num"><fmt:formatNumber value="${r.transfer}" pattern="#,##0"/></td>
                        <td class="num"><fmt:formatNumber value="${r.bankReceived}" pattern="#,##0"/></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:if>
    </div>
</c:if>

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
