<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Obligation check"/>
<c:set var="nav" value="tax"/>
<c:set var="taxTab" value="checker"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<%@ include file="/WEB-INF/views/common/tax-tabs.jspf" %>

<div class="card" style="margin-bottom: 14px;">
    <h1>Tax obligation check</h1>
    <p class="hint">A result is a guide, not a legal decision: check it with the tax office or an accountant. The checker never decides from revenue alone: it also needs your business type and registration details.</p>
    <div style="margin-top: 10px;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
            <span class="hint">Profile completeness: <b>${completion}%</b></span>
            <a href="${ctx}/tax/profile" class="btn sm">Update profile</a>
        </div>
        <div class="hb single">
            <span class="bar"><i style="width: ${completion}%;"></i></span>
        </div>
    </div>
</div>

<div class="steps">
    <div class="done"><b>1</b>Basics</div>
    <div class="done"><b>2</b>Information</div>
    <div class="done"><b>3</b>Rule analysis</div>
    <div class="on"><b>4</b>Obligations</div>
    <div class="on"><b>5</b>Reasons</div>
    <div><b>6</b>Missing info</div>
    <div><b>7</b>Confirm</div>
</div>

<div class="card" style="margin-bottom: 14px;">
    <h2>Eligibility check</h2>
    <p class="hint">Assessment date: <b>today</b>, applicable period: <b>this calendar year</b>.</p>
    
    <div style="margin-top: 14px;">
        <c:forEach var="c" items="${checks}">
            <details class="ins" open>
                <summary>
                    <c:out value="${c.label}"/>
                    <span class="tpill ${c.status == 'APPLICABLE' ? 'ok' : (c.status == 'NOT_APPLICABLE' ? 'muted' : 'warn')}" style="margin-left: 8px;">
                        <c:choose>
                            <c:when test="${c.status == 'APPLICABLE'}">Applicable</c:when>
                            <c:when test="${c.status == 'NOT_APPLICABLE'}">Not applicable</c:when>
                            <c:otherwise>Needs verification</c:otherwise>
                        </c:choose>
                    </span>
                </summary>
                <div>
                    <p style="margin: 6px 0 10px; font-weight: 500;"><c:out value="${c.reason}"/></p>
                    <c:forEach var="cond" items="${c.conds}">
                        <div class="check ${cond[1] == false ? 'no' : ''}">
                            <i><c:choose><c:when test="${cond[1] == true}">&#10003;</c:when><c:when test="${cond[1] == false}">&#10005;</c:when><c:otherwise>?</c:otherwise></c:choose></i>
                            <span><c:out value="${cond[0]}"/></span>
                        </div>
                    </c:forEach>
                    <c:if test="${not empty c.missing}">
                        <div class="alert-line" style="margin-top: 8px;">
                            <b>Missing:</b> <c:forEach var="m" items="${c.missing}" varStatus="st"><c:out value="${m}"/><c:if test="${!st.last}">, </c:if></c:forEach>
                        </div>
                    </c:if>
                    <p class="hint" style="margin-top: 8px;">
                        Basis: <c:out value="${c.basis}"/> &bull; rules ${c.verified ? 'verified' : 'not verified'} &bull; taken from <a href="${ctx}/admin/taxes">Tax rules</a>.
                    </p>
                </div>
            </details>
        </c:forEach>
    </div>

    <c:if test="${canManage}">
        <form method="post" action="${ctx}/tax/checker" style="margin-top: 16px;">
            <input type="hidden" name="action" value="confirm">
            <button type="submit" class="primary" onclick="return confirm('Save this assessment with today\'s date?');">Confirm and save this assessment</button>
        </form>
    </c:if>
</div>

<div class="card">
    <h2>Assessment history</h2>
    <ul class="list" style="margin-top: 10px;">
        <c:forEach var="a" items="${assessments}">
            <li>
                <span>
                    <b><fmt:formatDate value="${a.assessed_on}" pattern="dd/MM/yyyy"/></b> &bull; <c:out value="${a.period_label}"/>
                    <div class="m"><c:out value="${a.summary}"/> &bull; Saved by <c:out value="${a.created_name}"/></div>
                </span>
                <span class="tpill ${a.verification == 'VERIFIED' ? 'ok' : 'warn'}">
                    <c:out value="${a.verification}"/>
                </span>
            </li>
        </c:forEach>
        <c:if test="${empty assessments}">
            <li class="muted">No assessment has been confirmed yet.</li>
        </c:if>
    </ul>
</div>

<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
