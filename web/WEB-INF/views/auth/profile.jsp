<%--
    Document   : profile
    Created on : Oct 10, 2026, 9:00:00 AM
    Author     : PC
--%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Profile"/>
<c:set var="nav" value="profile"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<div class="card narrow">
    <h1>Profile</h1>
    <dl>
        <dt>User code</dt><dd><c:out value="${sessionScope.loginUser.userCode}"/></dd>
        <dt>Full name</dt><dd><c:out value="${sessionScope.loginUser.fullName}"/></dd>
        <dt>Role</dt><dd><c:out value="${sessionScope.loginUser.role}"/></dd>
        <dt>Status</dt><dd><c:out value="${sessionScope.loginUser.status}"/></dd>
    </dl>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
