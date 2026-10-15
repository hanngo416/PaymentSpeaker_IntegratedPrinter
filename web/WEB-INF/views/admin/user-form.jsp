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

        <c:if test="${not creating}">
            <label>User code
                <input type="text" value="<c:out value='${u.userCode}'/>" disabled>
            </label>
        </c:if>

        <label>Full name
            <input type="text" name="fullName" id="fullName" required minlength="2" maxlength="100" value="<c:out value='${u.fullName}'/>">
            <span class="hint">Letters, spaces, dot, apostrophe and hyphen only.</span>
            <c:if test="${not empty errors.fullName}"><div class="field-error"><c:out value="${errors.fullName}"/></div></c:if>
        </label>

        <label>Email (used for OTP and Google login)
            <input type="email" name="email" required maxlength="256" value="<c:out value='${u.email}'/>">
            <span class="hint"><c:choose><c:when test="${creating}">The user code is made from the part before the @.</c:when><c:otherwise>Changing the email also unlinks the Google account.</c:otherwise></c:choose></span>
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
<script>
    (function () {
        var name = document.getElementById('fullName');
        var rule = /^[\p{L}][\p{L} .'-]{1,99}$/u;
        function check() {
            name.setCustomValidity(rule.test(name.value.trim()) ? '' : 'Use 2-100 letters, spaces, dot, apostrophe or hyphen (no digits).');
        }
        name.addEventListener('input', check);
        check();
    })();
</script>
</body>
</html>
