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
    <meta name="color-scheme" content="light only">
    <title>403 - Access denied</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/base.css?v=38">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/guide.css?v=38">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/dashboard.css?v=38">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css?v=38">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/receipt.css?v=38">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/screens.css?v=38">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/tools.css?v=38">
</head>
<body class="auth">
<div class="auth-card wide">
    <h1>403</h1>
    <h2>You are not allowed to do this</h2>
    <p><c:out value="${denyReason}"/></p>
    <p><a href="${pageContext.request.contextPath}/home">Back to home</a></p>
</div>
<%@ include file="/WEB-INF/views/common/i18n.jspf" %>
</body>
</html>
