<%--
    Document   : login
    Created on : Oct 10, 2026, 9:00:00 AM
    Author     : PC
--%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="color-scheme" content="light only">
    <title>Login</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/base.css?v=38">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/guide.css?v=38">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/dashboard.css?v=38">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/auth.css?v=38">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/receipt.css?v=38">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/screens.css?v=38">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/tools.css?v=38">
</head>
<body class="auth">
<div class="auth-card">
    <div class="brand">
        <div class="brand-mark">T</div>
        <span class="brand-name">Tinky</span>
    </div>
    <h1>Login</h1>
    <c:if test="${not empty error}">
        <p class="error"><c:out value="${error}"/></p>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/login/otp">
        <label>Email
            <input type="email" name="email" value="<c:out value='${email}'/>" required autofocus>
        </label>
        <button type="submit" class="primary">Send me a login code</button>
    </form>

    <p class="or">or</p>

    <a class="btn" href="${pageContext.request.contextPath}/login/google">Sign in with Google</a>
</div>
</body>
</html>
