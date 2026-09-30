<%--
    Document   : error403
    Created on : Oct 11, 2026
    Author     : PC
--%>

<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>403 - Access denied</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css">
</head>
<body class="auth">
<div class="auth-card wide">
    <h1>403</h1>
    <h2>You are not allowed to do this</h2>
    <p><c:out value="${denyReason}"/></p>
    <p><a href="${pageContext.request.contextPath}/dashboard">Back to home</a></p>
</div>
</body>
</html>
