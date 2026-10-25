<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Tax obligations"/>
<c:set var="nav" value="tax"/>
<c:set var="taxTab" value="obligations"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<%@ include file="/WEB-INF/views/common/tax-tabs.jspf" %>
<div class="card">
    <h1>Tax obligations</h1>
    <p class="hint">Each obligation moves through a clear life cycle and every change is kept in its history. The system never submits a declaration or pays tax for you: you record what you did.</p>
    <h3>What may apply to you</h3>
    <c:forEach var="c" items="${assessmentChecks}">
        <div><b><c:out value="${c.label}"/></b> <span class="pill ${c.status == 'APPLICABLE' ? 'ACTIVE' : (c.status == 'NOT_APPLICABLE' ? 'EXPIRED' : 'PENDING')}">${c.status}</span> - <c:out value="${c.reason}"/></div>
    </c:forEach>
    <p class="hint">Full conditions are on the <a href="${ctx}/tax/profile">profile page</a>.</p>
</div>

<c:if test="${not empty detail}">
    <div class="card">
        <h2>${detail.kind} - <c:out value="${detail.period_label}"/> <span class="pill ${detail.status == 'PAID' ? 'PAID' : 'PENDING'}">${detail.status}</span></h2>
        <table class="kv">
            <tr><td>Due date</td><td>${detail.due_date}</td></tr>
            <tr><td>Estimated</td><td><fmt:formatNumber value="${detail.estimated_amount}" pattern="#,##0"/> VND</td></tr>
            <tr><td>Declared</td><td><c:out value="${empty detail.declared_amount ? 'not yet' : detail.declared_amount}"/></td></tr>
            <tr><td>Confirmed to pay</td><td><c:out value="${empty detail.confirmed_amount ? 'not yet' : detail.confirmed_amount}"/></td></tr>
            <tr><td>Paid so far</td><td><fmt:formatNumber value="${detail.paid_amount}" pattern="#,##0"/> VND</td></tr>
            <tr><td>Submission reference</td><td><c:out value="${detail.submission_ref}"/></td></tr>
            <tr><td>Completed on</td><td>${detail.completed_on}</td></tr>
        </table>
        <c:if test="${not empty error}"><div class="field-error"><c:out value="${error}"/></div></c:if>
        <c:if test="${canManage}">
            <h3>Update</h3>
            <form method="post" action="${ctx}/tax/obligations">
                <input type="hidden" name="id" value="${detail.obligation_id}">
                <label>Status
                    <select name="status">
                        <c:forEach var="s" items="${statuses}"><option value="${s}" ${detail.status == s ? 'selected' : ''}>${s}</option></c:forEach>
                    </select>
                </label>
                <label>Responsible
                    <select name="responsible">
                        <option value="">Nobody</option>
                        <c:forEach var="u" items="${users}"><option value="${u.user_id}" ${detail.responsible_user == u.user_id ? 'selected' : ''}><c:out value="${u.full_name}"/></option></c:forEach>
                    </select>
                </label>
                <label>Declared amount (VND) <input type="text" name="declared" inputmode="numeric" maxlength="16" value="<c:out value='${detail.declared_amount.toBigInteger()}'/>"></label>
                <label>Confirmed amount to pay (VND) <input type="text" name="confirmed" inputmode="numeric" maxlength="16" value="<c:out value='${detail.confirmed_amount.toBigInteger()}'/>"></label>
                <label>Submission reference <input type="text" name="submissionRef" maxlength="60" value="<c:out value='${detail.submission_ref}'/>"></label>
                <label>Note (required when the status changes) <input type="text" name="note" maxlength="300"></label>
                <button type="submit" class="primary">Save</button>
                <a class="btn" href="${ctx}/tax/obligations">Close</a>
            </form>
        </c:if>
        <h3>History</h3>
        <table>
            <thead><tr><th>When</th><th>Change</th><th>Note</th><th>By</th></tr></thead>
            <tbody>
            <c:forEach var="h" items="${history}">
                <tr><td><fmt:formatDate value="${h.changed_at}" pattern="yyyy-MM-dd HH:mm"/></td><td>${h.from_status} &rarr; ${h.to_status}</td><td><c:out value="${h.note}"/></td><td><c:out value="${h.changed_by}"/></td></tr>
            </c:forEach>
            <c:if test="${empty history}"><tr><td colspan="4">No status changes yet.</td></tr></c:if>
            </tbody>
        </table>
    </div>
</c:if>

<div class="card">
    <h2>Calendar and progress</h2>
    <table>
        <thead><tr><th>Obligation</th><th>Period</th><th>Due</th><th>Status</th><th>Responsible</th><th>Estimated</th><th>Confirmed</th><th>Paid</th><th></th></tr></thead>
        <tbody>
        <c:forEach var="o" items="${rows}">
            <tr>
                <td>${o.kind}</td><td><c:out value="${o.period_label}"/></td>
                <td>${o.due_date}<c:if test="${o.late == 1}"> <span class="pill HIGH">OVERDUE</span></c:if></td>
                <td><span class="pill ${o.status == 'PAID' ? 'PAID' : (o.status == 'NOT_APPLICABLE' ? 'EXPIRED' : 'PENDING')}">${o.status}</span></td>
                <td><c:out value="${o.responsible}"/></td>
                <td><fmt:formatNumber value="${o.estimated_amount}" pattern="#,##0"/></td>
                <td><c:out value="${empty o.confirmed_amount ? '-' : o.confirmed_amount}"/></td>
                <td><fmt:formatNumber value="${o.paid_amount}" pattern="#,##0"/></td>
                <td><a class="btn" href="${ctx}/tax/obligations?id=${o.obligation_id}">Open</a></td>
            </tr>
        </c:forEach>
        <c:if test="${empty rows}"><tr><td colspan="9">No obligations yet.</td></tr></c:if>
        </tbody>
    </table>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
