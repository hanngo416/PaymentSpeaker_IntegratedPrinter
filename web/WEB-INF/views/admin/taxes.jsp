<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Tax rules"/>
<c:set var="nav" value="taxes"/>
<c:set var="taxTab" value="rules"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<div class="card">
    <h1>Tax rules</h1>
    <c:if test="${not empty message}"><div class="msg"><c:out value="${message}"/></div></c:if>
    <p class="hint">Every legal number used by the tax estimate lives here. Estimate only, not the amount to pay.</p>
</div>

<div class="card narrow">
    <h2>Business sector</h2>
    <p class="hint">The VAT and personal income tax rates of this sector are used by the tax estimate and the assistant. Current sector: <strong><c:out value="${currentSector}"/></strong>.</p>
    <c:if test="${canManage}">
        <form method="post" action="${ctx}/admin/taxes">
            <input type="hidden" name="action" value="sector">
            <label>Sector
                <select name="sector">
                    <c:forEach var="s" items="${sectors}">
                        <option value="${s}" ${currentSector == s ? 'selected' : ''}>${s}</option>
                    </c:forEach>
                </select>
            </label>
            <button type="submit" class="primary">Change sector</button>
        </form>
    </c:if>
</div>

<c:if test="${canManage}">
    <div class="card narrow">
        <h2>${empty form || form.ruleId == 0 ? 'New rule' : 'Edit rule'}</h2>
        <form method="post" action="${ctx}/admin/taxes">
            <input type="hidden" name="action" value="save">
            <input type="hidden" name="id" value="${empty form ? 0 : form.ruleId}">
            <label>Kind
                <select name="kind" required>
                    <c:forEach var="k" items="${kinds}">
                        <option value="${k}" ${form.kind == k ? 'selected' : ''}>${k}</option>
                    </c:forEach>
                </select>
                <c:if test="${not empty errors.kind}"><div class="field-error"><c:out value="${errors.kind}"/></div></c:if>
            </label>
            <label>Sector (VAT_RATE and PIT_RATE only)
                <select name="sector">
                    <option value="">None</option>
                    <c:forEach var="s" items="${sectors}">
                        <option value="${s}" ${form.sector == s ? 'selected' : ''}>${s}</option>
                    </c:forEach>
                </select>
                <c:if test="${not empty errors.sector}"><div class="field-error"><c:out value="${errors.sector}"/></div></c:if>
            </label>
            <label>Value (VND for threshold and ceiling, percent for the others)
                <input type="text" name="value" required inputmode="decimal" maxlength="24" value="<c:out value='${formValue}'/>">
                <c:if test="${not empty errors.value}"><div class="field-error"><c:out value="${errors.value}"/></div></c:if>
            </label>
            <label>Effective from
                <input type="date" name="effectiveFrom" required value="<c:out value='${formFrom}'/>">
                <c:if test="${not empty errors.effectiveFrom}"><div class="field-error"><c:out value="${errors.effectiveFrom}"/></div></c:if>
            </label>
            <label>Effective to (optional)
                <input type="date" name="effectiveTo" value="<c:out value='${formTo}'/>">
                <c:if test="${not empty errors.effectiveTo}"><div class="field-error"><c:out value="${errors.effectiveTo}"/></div></c:if>
            </label>
            <label>Source
                <input type="text" name="sourceRef" required maxlength="200" value="<c:out value='${form.sourceRef}'/>">
                <c:if test="${not empty errors.sourceRef}"><div class="field-error"><c:out value="${errors.sourceRef}"/></div></c:if>
            </label>
            <label>
                <input type="checkbox" name="verified" value="1" ${form.verified ? 'checked' : ''}> Verified against the source
            </label>
            <button type="submit" class="primary">Save</button>
            <c:if test="${not empty form && form.ruleId != 0}"><a class="btn" href="${ctx}/admin/taxes">Cancel</a></c:if>
        </form>
    </div>
</c:if>

<div class="card">
    <form method="get" action="${ctx}/admin/taxes" class="filters">
        <label>Search
            <input type="text" name="q" value="<c:out value='${q}'/>" placeholder="Search source or sector..." maxlength="100">
        </label>
        <label>Kind
            <select name="kind">
                <option value="">All kinds</option>
                <c:forEach var="k" items="${kinds}">
                    <option value="${k}" ${kindFilter == k ? 'selected' : ''}>${k}</option>
                </c:forEach>
            </select>
        </label>
        <button type="submit" class="primary">Filter</button>
        <a class="btn" href="${ctx}/admin/taxes">Reset</a>
    </form>
    <table>
        <thead>
        <tr><th>Kind</th><th>Sector</th><th>Value</th><th>Valid</th><th>Source</th><th>Status</th><c:if test="${canManage}"><th>Action</th></c:if></tr>
        </thead>
        <tbody>
        <c:forEach var="r" items="${result.items}">
            <tr>
                <td><c:out value="${r.kind}"/></td>
                <td><c:out value="${empty r.sector ? '-' : r.sector}"/></td>
                <td><fmt:formatNumber value="${r.value}" pattern="#,##0.##"/></td>
                <td><fmt:formatDate value="${r.effectiveFrom}" pattern="yyyy-MM-dd"/> to <c:choose><c:when test="${empty r.effectiveTo}">open</c:when><c:otherwise><fmt:formatDate value="${r.effectiveTo}" pattern="yyyy-MM-dd"/></c:otherwise></c:choose></td>
                <td class="wrap"><c:out value="${r.sourceRef}"/></td>
                <td>
                    <span class="pill ${r.active ? 'ACTIVE' : 'CANCELLED'}">${r.active ? 'ACTIVE' : 'DISABLED'}</span>                </td>
                <c:if test="${canManage}">
                    <td>
                        <a class="btn" href="${ctx}/admin/taxes?edit=${r.ruleId}">Edit</a>
                        <form method="post" action="${ctx}/admin/taxes" class="inline">
                            <input type="hidden" name="id" value="${r.ruleId}">
                            <button type="submit" name="action" value="toggle">${r.active ? 'Disable' : 'Enable'}</button>
                        </form>
                        <form method="post" action="${ctx}/admin/taxes" class="inline"
                              data-name="<c:out value='${r.kind} ${r.sector}'/>"
                              onsubmit="return confirm('Delete rule ' + this.dataset.name + '? This cannot be undone.');">
                            <input type="hidden" name="id" value="${r.ruleId}">
                            <button type="submit" name="action" value="delete">Delete</button>
                        </form>
                    </td>
                </c:if>
            </tr>
        </c:forEach>
        <c:if test="${empty result.items}">
            <tr><td colspan="7">No tax rules found.</td></tr>
        </c:if>
        </tbody>
    </table>
    <p>${result.total} rule(s), page ${result.page} of ${result.totalPages}</p>
    <div class="pages">
        <c:if test="${result.page > 1}">
            <a href="${ctx}/admin/taxes?page=${result.page - 1}&amp;q=<c:out value='${q}'/>&amp;kind=<c:out value='${kindFilter}'/>" title="Previous page">&laquo;</a>
        </c:if>
        <c:forEach begin="1" end="${result.totalPages}" var="p">
            <c:choose>
                <c:when test="${p == result.page}"><span>${p}</span></c:when>
                <c:otherwise><a href="${ctx}/admin/taxes?page=${p}&amp;q=<c:out value='${q}'/>&amp;kind=<c:out value='${kindFilter}'/>">${p}</a></c:otherwise>
            </c:choose>
        </c:forEach>
        <c:if test="${result.page < result.totalPages}">
            <a href="${ctx}/admin/taxes?page=${result.page + 1}&amp;q=<c:out value='${q}'/>&amp;kind=<c:out value='${kindFilter}'/>" title="Next page">&raquo;</a>
        </c:if>
    </div>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
