<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Tax calendar"/>
<c:set var="nav" value="tax"/>
<c:set var="taxTab" value="calendar"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<%@ include file="/WEB-INF/views/common/tax-tabs.jspf" %>
<div class="card">
    <h1>Tax calendar</h1>
    <p class="hint">The dates to remember come from the tax obligations list (due date and status). Check deadlines against the current regulations. To change a due date or status, open <a href="${ctx}/tax/obligations">Tax obligations</a>.</p>
    <div>
        <a class="btn ${view == 'grid' ? 'primary' : ''}" href="${ctx}/tax/calendar?view=grid&amp;m=${month}">Calendar</a>
        <a class="btn ${view == 'list' ? 'primary' : ''}" href="${ctx}/tax/calendar?view=list&amp;m=${month}">List</a>
    </div>
    <c:if test="${savedMsg}"><div class="msg">Reminder settings saved.</div></c:if>
</div>

<div class="card">
    <c:choose>
        <c:when test="${view == 'grid'}">
            <div class="cal-head">
                <a class="btn" href="${ctx}/tax/calendar?view=grid&amp;m=${prevMonth}">&lsaquo; Previous</a>
                <h2>Month ${monthLabel}</h2>
                <a class="btn" href="${ctx}/tax/calendar?view=grid&amp;m=${nextMonth}">Next &rsaquo;</a>
            </div>
            <div class="cal-grid">
                <div class="cal-dow">T2</div><div class="cal-dow">T3</div><div class="cal-dow">T4</div><div class="cal-dow">T5</div><div class="cal-dow">T6</div><div class="cal-dow">T7</div><div class="cal-dow">CN</div>
                <c:forEach var="cell" items="${cells}">
                    <c:choose>
                        <c:when test="${cell == null}"><div class="cal-day empty"></div></c:when>
                        <c:otherwise>
                            <div class="cal-day ${cell.today ? 'today' : ''}">
                                <span class="cal-num">${cell.day}<c:if test="${cell.today}"> - Today</c:if></span>
                                <c:forEach var="t" items="${cell.items}">
                                    <a class="pill ${t.status == 'PAID' || t.status == 'NOT_APPLICABLE' ? 'ACTIVE' : (t.late == 1 ? 'LOCKED' : 'PENDING')}" href="${ctx}/tax/obligations" title="<c:out value='${t.kind} - ${t.period_label}'/>"><c:out value="${t.kind}"/> <c:out value="${t.period_label}"/></a>
                                </c:forEach>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </c:forEach>
            </div>
        </c:when>
        <c:otherwise>
            <table>
                <thead><tr><th>Due date</th><th>Obligation</th><th>Period</th><th>Status</th><th>Responsible</th></tr></thead>
                <tbody>
                <c:forEach var="t" items="${tasks}">
                    <tr>
                        <td>${t.due_date}</td>
                        <td><c:out value="${t.kind}"/></td>
                        <td><c:out value="${t.period_label}"/></td>
                        <td><span class="pill ${t.status == 'PAID' || t.status == 'NOT_APPLICABLE' ? 'ACTIVE' : (t.late == 1 ? 'LOCKED' : 'PENDING')}"><c:out value="${t.status}"/></span><c:if test="${t.late == 1}"> overdue</c:if></td>
                        <td><c:out value="${t.responsible}"/></td>
                    </tr>
                </c:forEach>
                <c:if test="${empty tasks}"><tr><td colspan="5">No tax obligations yet.</td></tr></c:if>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</div>

<div class="card">
    <h2>Upcoming tasks</h2>
    <table>
        <thead><tr><th>Due date</th><th>Obligation</th><th>Period</th><th>Status</th></tr></thead>
        <tbody>
        <c:forEach var="t" items="${upcoming}">
            <tr><td>${t.due_date}</td><td><c:out value="${t.kind}"/></td><td><c:out value="${t.period_label}"/></td><td><span class="pill ${t.late == 1 ? 'LOCKED' : 'PENDING'}"><c:out value="${t.status}"/></span></td></tr>
        </c:forEach>
        <c:if test="${empty upcoming}"><tr><td colspan="4">Nothing is left to do.</td></tr></c:if>
        </tbody>
    </table>
</div>

<c:if test="${canManage}">
<div class="card narrow">
    <h2>Reminder settings</h2>
    <p class="hint">Your choices are saved. The system does not send reminder emails yet: due-date notices appear under Notifications.</p>
    <form method="post" action="${ctx}/tax/calendar">
        <input type="hidden" name="view" value="${view}">
        <label><input type="checkbox" name="d7" ${reminders.d7 ? 'checked' : ''}> Remind 7 days before</label>
        <label><input type="checkbox" name="d3" ${reminders.d3 ? 'checked' : ''}> Remind 3 days before</label>
        <label><input type="checkbox" name="d1" ${reminders.d1 ? 'checked' : ''}> Remind 1 day before</label>
        <label><input type="checkbox" name="mail" ${reminders.mail ? 'checked' : ''}> Send email</label>
        <label><input type="checkbox" name="app" ${reminders.app ? 'checked' : ''}> In-app notifications</label>
        <button type="submit" class="primary">Save settings</button>
    </form>
</div>
</c:if>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
