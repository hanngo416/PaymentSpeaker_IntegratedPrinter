<%--
    Document   : error
    Created on : Oct 12, 2026
    Author     : PC
--%>

<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="color-scheme" content="light only">
    <title>Error</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/base.css?v=42">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/guide.css?v=42">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/dashboard.css?v=42">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css?v=42">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/receipt.css?v=42">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/screens.css?v=42">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/tools.css?v=42">
</head>
<body class="auth">
<div class="auth-card wide">
    <c:choose>
        <c:when test="${pageContext.errorData.statusCode == 404}">
            <h1>404</h1>
            <h2>Page not found</h2>
            <p>The page you asked for does not exist.</p>
        </c:when>
        <c:when test="${pageContext.errorData.statusCode == 400}">
            <h1>400</h1>
            <h2>Invalid Request</h2>
            <p>The request parameters are invalid or malformed.</p>
        </c:when>
        <c:otherwise>
            <h1>Something went wrong</h1>
            <p>The system had a problem. Please try again later.</p>
        </c:otherwise>
    </c:choose>
    <p><a href="${pageContext.request.contextPath}/home">Back to home</a></p>
</div>
<%@ include file="/WEB-INF/views/common/i18n.jspf" %>
</body>
</html>
