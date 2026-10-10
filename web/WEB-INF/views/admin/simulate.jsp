<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Simulate transactions"/>
<c:set var="nav" value="simulate"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<div class="card narrow">
    <h1>Simulate an incoming transfer</h1>
    <p>Runs the same path as the SePay poller: rules, order update, alert. Put an order code such as TINKY12345 in the content to match an order.</p>
    <c:if test="${not empty error}"><p class="error"><c:out value="${error}"/></p></c:if>
    <form method="post" action="${ctx}/admin/simulate">
        <input type="hidden" name="action" value="one">
        <label>Amount (VND)
            <input type="text" name="amount" required pattern="[0-9]{1,15}" value="50000">
        </label>
        <label>Transfer content
            <input type="text" name="content" required maxlength="500" value="TINKY12345">
        </label>
        <button type="submit" class="primary">Send one</button>
    </form>
</div>
<div class="card narrow">
    <h2>Sample batch</h2>
    <p>Sends 3 transfers with no order: two identical ones (duplicate) and one unknown.</p>
    <form method="post" action="${ctx}/admin/simulate">
        <input type="hidden" name="action" value="batch">
        <button type="submit" class="primary">Send sample batch</button>
    </form>
</div>
<c:if test="${not empty result}">
    <div class="card narrow"><pre><c:out value="${result}"/></pre>
        <a href="${ctx}/alerts">Open alerts</a> | <a href="${ctx}/transactions">Open transactions</a>
    </div>
</c:if>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
