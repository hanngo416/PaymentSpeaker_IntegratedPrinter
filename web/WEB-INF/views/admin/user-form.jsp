<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="${creating ? 'New user' : 'Edit user'}"/>
<c:set var="nav" value="users"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<div class="card narrow">
    <h1>${creating ? 'New user' : 'Edit user'}</h1>
    <c:if test="${not empty formError}">
        <div class="error"><c:out value="${formError}"/></div>
    </c:if>

    <form method="post" action="${ctx}/admin/users/edit">
        <c:if test="${not creating}">
            <input type="hidden" name="id" value="${u.userId}">
        </c:if>

        <label>User code
            <c:choose>
                <c:when test="${creating}">
                    <input type="text" name="userCode" required minlength="3" maxlength="50"
                           pattern="[A-Za-z0-9_.]{3,50}" value="<c:out value='${u.userCode}'/>">
                    <span class="hint">Letters, digits, dot and underscore. Cannot be changed later.</span>
                </c:when>
                <c:otherwise>
                    <input type="text" value="<c:out value='${u.userCode}'/>" disabled>
                </c:otherwise>
            </c:choose>
            <c:if test="${not empty errors.userCode}"><div class="field-error"><c:out value="${errors.userCode}"/></div></c:if>
        </label>

        <label>Full name
            <input type="text" name="fullName" required maxlength="100" value="<c:out value='${u.fullName}'/>">
            <c:if test="${not empty errors.fullName}"><div class="field-error"><c:out value="${errors.fullName}"/></div></c:if>
        </label>

        <label>Email (used for OTP and Google login)
            <input type="email" name="email" required maxlength="256" value="<c:out value='${u.email}'/>">
            <c:if test="${not creating}"><span class="hint">Changing the email also unlinks the Google account.</span></c:if>
            <c:if test="${not empty errors.email}"><div class="field-error"><c:out value="${errors.email}"/></div></c:if>
        </label>

        <c:if test="${isManager}">
            <p class="hint">Managers can only create and edit CASHIER accounts. The role is set automatically.</p>
        </c:if>

        <c:if test="${not isManager}">
        <label>Role
            <select name="role" id="role" required>
                <option value="">Choose a role</option>
                <c:forEach var="r" items="${roles}">
                    <option value="${r}" ${u.role == r ? 'selected' : ''}>${r}</option>
                </c:forEach>
            </select>
            <c:if test="${not empty errors.role}"><div class="field-error"><c:out value="${errors.role}"/></div></c:if>
        </label>
        </c:if>

        <button type="submit" class="primary">Save</button>
        <a class="btn" href="${ctx}/admin/users">Cancel</a>
    </form>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
