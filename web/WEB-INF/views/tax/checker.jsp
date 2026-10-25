<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Obligation check"/>
<c:set var="nav" value="tax"/>
<c:set var="taxTab" value="checker"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<%@ include file="/WEB-INF/views/common/tax-tabs.jspf" %>
<div class="card">
    <h1>Tax obligation check</h1>
    <p class="hint">A result is a guide, not a legal decision: check it with the tax office or an accountant. The checker never decides from revenue alone: it also needs your business type and registration details, and it says what is still missing.</p>
    <p>Profile completeness: <b>${completion}%</b> - <a href="${ctx}/tax/profile">update the business profile</a></p>
</div>

<div class="card">
    <h2>Eligibility check</h2>
    <p class="hint">Assessment date: <b>today</b>, applicable period: <b>this calendar year</b>.</p>
    <c:forEach var="c" items="${checks}">
        <div class="card">
            <h3><c:out value="${c.label}"/> <span class="pill ${c.status == 'APPLICABLE' ? 'ACTIVE' : (c.status == 'NOT_APPLICABLE' ? 'EXPIRED' : 'PENDING')}"><c:choose><c:when test="${c.status == 'APPLICABLE'}">Applicable</c:when><c:when test="${c.status == 'NOT_APPLICABLE'}">Not applicable</c:when><c:otherwise>Needs verification</c:otherwise></c:choose></span></h3>
            <p><c:out value="${c.reason}"/></p>
            <c:forEach var="cond" items="${c.conds}">
                <div class="hint"><c:choose><c:when test="${cond[1] == true}">&#10003;</c:when><c:when test="${cond[1] == false}">&#10007;</c:when><c:otherwise>?</c:otherwise></c:choose> <c:out value="${cond[0]}"/></div>
            </c:forEach>
            <c:if test="${not empty c.missing}"><div class="alert-line">Missing: <c:forEach var="m" items="${c.missing}" varStatus="st"><c:out value="${m}"/><c:if test="${!st.last}">, </c:if></c:forEach></div></c:if>
            <p class="hint">Basis: <c:out value="${c.basis}"/> - rules ${c.verified ? 'verified' : 'not verified'} - taken from <a href="${ctx}/admin/taxes">Tax rules</a>.</p>
        </div>
    </c:forEach>
    <c:if test="${canManage}">
        <form method="post" action="${ctx}/tax/checker">
            <input type="hidden" name="action" value="confirm">
            <button type="submit" class="primary" onclick="return confirm('Save this assessment with today's date?');">Confirm and save this assessment</button>
        </form>
    </c:if>
</div>

<div class="card">
    <h2>Assessment history</h2>
    <table>
        <thead><tr><th>Date</th><th>Period</th><th>Result</th><th>Verification</th><th>Saved by</th></tr></thead>
        <tbody>
        <c:forEach var="a" items="${assessments}">
            <tr><td>${a.assessed_on}</td><td><c:out value="${a.period_label}"/></td><td><c:out value="${a.summary}"/></td><td>${a.verification}</td><td><c:out value="${a.created_name}"/></td></tr>
        </c:forEach>
        <c:if test="${empty assessments}"><tr><td colspan="5">No assessment has been saved yet.</td></tr></c:if>
        </tbody>
    </table>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
