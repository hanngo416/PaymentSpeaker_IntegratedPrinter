<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Invoice"/>
<c:set var="nav" value="einvoices"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<div class="card narrow">
    <h1><c:out value="${mode == 'edit' ? 'Edit draft invoice' : 'New draft invoice'}"/></h1>
    <div class="error">Not valid until your e-invoice provider issues it. Enter the provider's number and lookup code when you receive them.</div>
    <c:choose>
        <c:when test="${mode == 'edit'}">
            <p class="hint">Invoice <c:out value="${invoice.invoiceNo}"/>, total <fmt:formatNumber value="${invoice.totalAmount}" pattern="#,##0"/> VND, VAT <fmt:formatNumber value="${invoice.vatAmount}" pattern="#,##0"/> VND.</p>
        </c:when>
        <c:otherwise>
            <p class="hint">Source: <c:out value="${sourceLabel}"/>. The lines, total and VAT are copied from it.</p>
        </c:otherwise>
    </c:choose>
    <form method="post" action="${ctx}/invoices/new">
        <c:choose>
            <c:when test="${mode == 'edit'}">
                <input type="hidden" name="action" value="update">
                <input type="hidden" name="invoiceId" value="${invoice.invoiceId}">
            </c:when>
            <c:otherwise>
                <input type="hidden" name="action" value="create">
                <input type="hidden" name="source" value="<c:out value='${source}'/>">
                <input type="hidden" name="sourceId" value="${sourceId}">
            </c:otherwise>
        </c:choose>
        <label>Buyer name
            <input type="text" name="buyerName" required maxlength="100" value="<c:out value='${f.name}'/>">
            <c:if test="${not empty errors.name}"><div class="field-error"><c:out value="${errors.name}"/></div></c:if>
        </label>
        <label>Buyer tax code (optional)
            <input type="text" name="buyerTax" maxlength="14" value="<c:out value='${f.tax}'/>">
            <c:if test="${not empty errors.tax}"><div class="field-error"><c:out value="${errors.tax}"/></div></c:if>
        </label>
        <label>Buyer address (optional)
            <input type="text" name="buyerAddress" maxlength="200" value="<c:out value='${f.address}'/>">
            <c:if test="${not empty errors.address}"><div class="field-error"><c:out value="${errors.address}"/></div></c:if>
        </label>
        <button type="submit" class="primary"><c:out value="${mode == 'edit' ? 'Save' : 'Create draft'}"/></button>
        <a href="${ctx}/invoices/manage">Back to e-invoices</a>
    </form>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
