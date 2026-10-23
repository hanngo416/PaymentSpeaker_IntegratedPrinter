<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Discount codes"/>
<c:set var="nav" value="discounts"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<div class="card">
    <h1>Discount codes</h1>
    <c:if test="${not empty message}"><div class="msg"><c:out value="${message}"/></div></c:if>
    <p class="hint">A shared code (no holder) appears in the cashier's discount dropdown on the POS counter. A personal code is assigned to a specific holder.</p>
</div>

<div class="card narrow">
    <h2>New code</h2>
    <form method="post" action="${ctx}/admin/discounts">
        <input type="hidden" name="action" value="create">
        <label>Code
            <input type="text" name="code" required minlength="3" maxlength="30" value="<c:out value='${form.code}'/>">
            <c:if test="${not empty errors.code}"><div class="field-error"><c:out value="${errors.code}"/></div></c:if>
        </label>
        <label>Type
            <select name="kind" required>
                <option value="PERCENT" ${form.kind == 'PERCENT' ? 'selected' : ''}>Percent of the order</option>
                <option value="FIXED" ${form.kind == 'FIXED' ? 'selected' : ''}>Fixed amount (VND)</option>
            </select>
            <c:if test="${not empty errors.kind}"><div class="field-error"><c:out value="${errors.kind}"/></div></c:if>
        </label>
        <label>Value
            <input type="text" name="value" required inputmode="numeric" maxlength="11" value="<c:out value='${formValue}'/>">
            <c:if test="${not empty errors.value}"><div class="field-error"><c:out value="${errors.value}"/></div></c:if>
        </label>
        <label>Holder (personal code only)
            <input type="text" name="holder" maxlength="100" value="<c:out value='${form.holder}'/>">
            <span class="hint">Leave empty for a shared code that appears on the POS counter.</span>
            <c:if test="${not empty errors.holder}"><div class="field-error"><c:out value="${errors.holder}"/></div></c:if>
        </label>
        <label>Maximum uses (shared code)
            <input type="text" name="maxUses" inputmode="numeric" maxlength="6" value="<c:out value='${formMaxUses}'/>">
            <span class="hint">Leave empty for unlimited.</span>
            <c:if test="${not empty errors.maxUses}"><div class="field-error"><c:out value="${errors.maxUses}"/></div></c:if>
        </label>
        <label>Valid from
            <input type="date" name="validFrom" required value="<c:out value='${formFrom}'/>">
        </label>
        <label>Valid to
            <input type="date" name="validTo" required value="<c:out value='${formTo}'/>">
            <c:if test="${not empty errors.validTo}"><div class="field-error"><c:out value="${errors.validTo}"/></div></c:if>
        </label>
        <button type="submit" class="primary">Create</button>
    </form>
</div>

<div class="card">
    <table>
        <thead>
        <tr><th>Code</th><th>Discount</th><th>Holder</th><th>Used</th><th>Valid</th><th>Status</th><th>Action</th></tr>
        </thead>
        <tbody>
        <c:forEach var="d" items="${codes}">
            <tr>
                <td><c:out value="${d.code}"/></td>
                <td>
                    <c:choose>
                        <c:when test="${d.kind == 'PERCENT'}"><fmt:formatNumber value="${d.value}" pattern="#,##0"/>%</c:when>
                        <c:otherwise><fmt:formatNumber value="${d.value}" pattern="#,##0"/> VND</c:otherwise>
                    </c:choose>
                </td>
                <td><c:out value="${empty d.holder ? 'Shared' : d.holder}"/></td>
                <td>${d.usedCount} / <c:out value="${empty d.maxUses ? 'unlimited' : d.maxUses}"/></td>
                <td><fmt:formatDate value="${d.validFrom}" pattern="yyyy-MM-dd"/> to <fmt:formatDate value="${d.validTo}" pattern="yyyy-MM-dd"/></td>
                <td><span class="pill ${d.available ? 'ACTIVE' : 'CANCELLED'}">${d.available ? 'USABLE' : (d.active ? 'ENDED' : 'DISABLED')}</span></td>
                <td>
                    <form method="post" action="${ctx}/admin/discounts" class="inline">
                        <input type="hidden" name="id" value="${d.codeId}">
                        <button type="submit" name="action" value="${d.active ? 'disable' : 'enable'}">${d.active ? 'Disable' : 'Enable'}</button>
                    </form>
                    <form method="post" action="${ctx}/admin/discounts" class="inline"
                          data-name="<c:out value='${d.code}'/>"
                          onsubmit="return confirm('Delete code ' + this.dataset.name + '? This cannot be undone.');">
                        <input type="hidden" name="id" value="${d.codeId}">
                        <button type="submit" name="action" value="delete">Delete</button>
                    </form>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
