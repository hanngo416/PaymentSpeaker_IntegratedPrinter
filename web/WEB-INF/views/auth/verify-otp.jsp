<%--
    Document   : verify-otp
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
    <title>Enter code</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css">
</head>
<body class="auth">
<div class="auth-card">
    <div class="brand">
        <div class="brand-mark">T</div>
        <span class="brand-name">Tinky</span>
    </div>
    <h1>Enter your code</h1>
    <p>If the email is registered, we sent a 6-digit code to <strong><c:out value="${sessionScope.otpEmail}"/></strong>. It expires in 5 minutes.</p>
    <c:if test="${not empty error}">
        <p class="error"><c:out value="${error}"/></p>
    </c:if>
    <form method="post" action="${pageContext.request.contextPath}/login/verify">
        <input class="code-input" type="text" name="code" inputmode="numeric" maxlength="6" pattern="\d{6}" autocomplete="one-time-code" required autofocus>
        <button type="submit" class="primary">Login</button>
    </form>
    <form method="post" action="${pageContext.request.contextPath}/login/otp">
        <input type="hidden" name="email" value="<c:out value='${sessionScope.otpEmail}'/>">
        <button type="submit" id="resendBtn" class="secondary">Resend code</button>
    </form>
    <p class="hint">You can ask for a new code once every 60 seconds.</p>
    <p><a href="${pageContext.request.contextPath}/login">Use a different email</a></p>
</div>
<script>
    (function () {
        var KEY = 'otpResendAt', WAIT = 60000;
        var btn = document.getElementById('resendBtn');
        var form = btn.form;
        function get() { try { return parseInt(sessionStorage.getItem(KEY), 10) || 0; } catch (e) { return 0; } }
        function set(v) { try { sessionStorage.setItem(KEY, String(v)); } catch (e) { } }
        if (!get()) { set(Date.now() + WAIT); }
        form.addEventListener('submit', function () { set(Date.now() + WAIT); });
        function tick() {
            var left = Math.ceil((get() - Date.now()) / 1000);
            if (left > 0) {
                btn.disabled = true;
                btn.textContent = 'Resend code (' + left + 's)';
                setTimeout(tick, 250);
            } else {
                btn.disabled = false;
                btn.textContent = 'Resend code';
            }
        }
        tick();
    })();
</script>
</body>
</html>
