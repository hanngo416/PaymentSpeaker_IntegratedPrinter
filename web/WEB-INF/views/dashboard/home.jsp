<%--
    Document   : home
    Created on : Oct 10, 2026, 9:00:00 AM
    Author     : PC
--%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Home"/>
<c:set var="nav" value="home"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<div class="card">
    <h1>Welcome, <c:out value="${sessionScope.loginUser.fullName}"/> (<c:out value="${sessionScope.loginUser.userCode}"/>)</h1>
    <p>Role: <strong><c:out value="${sessionScope.loginUser.role}"/></strong></p>
    <div class="tiles">
        <c:if test="${role == 'CASHIER'}">
            <a class="tile" href="${ctx}/orders/new">Create payment order</a>
        </c:if>
        <c:if test="${role == 'ADMIN' || role == 'STORE_MANAGER'}">
            <a class="tile" href="${ctx}/admin/users">Users</a>
        </c:if>
        <c:if test="${role != 'TECHNICIAN'}">
            <a class="tile" href="${ctx}/transactions">Transactions</a>
        </c:if>
        <c:if test="${role == 'ADMIN' || role == 'STORE_MANAGER' || role == 'AUDITOR'}">
            <a class="tile" href="${ctx}/alerts">Alert center</a>
        </c:if>
        <c:if test="${role == 'ADMIN'}">
            <a class="tile" href="${ctx}/admin/simulate">Simulate transactions</a>
        </c:if>
        <a class="tile" href="${ctx}/profile">Profile</a>
    </div>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
