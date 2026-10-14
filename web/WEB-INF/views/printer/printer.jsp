<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Printer"/>
<c:set var="nav" value="printer"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<div class="card">
    <h1>Receipt printer</h1>
    <c:if test="${not empty message}"><div class="msg"><c:out value="${message}"/></div></c:if>
    <c:if test="${maintenance}">
        <div class="alert-banner">This printer is in maintenance and cannot print until a technician puts it back to work.</div>
    </c:if>
    <p class="hint">Money received for orders, newest first. Print opens a receipt in a new tab and sends it to the printer. A receipt that was already printed can be printed again.</p>
</div>

<div class="card">
    <table>
        <thead>
        <tr><th>Time</th><th>Order</th><th>Cashier</th><th>Received (VND)</th><th>Receipt</th></tr>
        </thead>
        <tbody>
        <c:forEach var="r" items="${receipts}">
            <tr>
                <td><fmt:formatDate value="${r.transTime}" pattern="yyyy-MM-dd HH:mm:ss"/></td>
                <td><c:out value="${r.orderCode}"/></td>
                <td><c:out value="${r.cashierCode}"/></td>
                <td class="num"><fmt:formatNumber value="${r.amount}" pattern="#,##0"/></td>
                <td>
                    <form method="post" action="${ctx}/printer/print" target="_blank" class="inline">
                        <input type="hidden" name="id" value="${r.transId}">
                        <button type="submit" class="${r.printed ? '' : 'primary'}" ${maintenance ? 'disabled' : ''}>${r.printed ? 'Print again' : 'Print'}</button>
                        <c:if test="${r.printed}"><span class="pill RESOLVED">PRINTED</span></c:if>
                    </form>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty receipts}">
            <tr><td colspan="5">No payments received yet.</td></tr>
        </c:if>
        </tbody>
    </table>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
