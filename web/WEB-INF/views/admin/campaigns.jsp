<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Campaigns"/>
<c:set var="nav" value="affiliates"/>
<c:set var="affTab" value="campaigns"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<%@ include file="/WEB-INF/views/common/affiliate-tabs.jspf" %>
<c:set var="editing" value="${form.campaignId > 0}"/>
<div class="card">
    <p class="hint">A campaign decides who earns a commission and how much. The rule in force when an order is saved is copied onto its commission, so later changes never touch old orders. Only one campaign counts per order (the one paying the most).</p>
</div>

<div class="card narrow">
    <h2><c:out value="${editing ? 'Edit campaign' : 'New campaign'}"/></h2>
    <form method="post" action="${ctx}/admin/campaigns">
        <input type="hidden" name="action" value="save">
        <c:if test="${editing}"><input type="hidden" name="id" value="${form.campaignId}"></c:if>
        <label>Name <input type="text" name="name" required minlength="3" maxlength="80" value="<c:out value='${form.name}'/>">
            <c:if test="${not empty errors.name}"><div class="field-error"><c:out value="${errors.name}"/></div></c:if>
        </label>
        <label>Description <input type="text" name="description" maxlength="300" value="<c:out value='${form.description}'/>">
            <c:if test="${not empty errors.description}"><div class="field-error"><c:out value="${errors.description}"/></div></c:if>
        </label>
        <label>Commission type
            <select name="commissionType">
                <option value="PERCENT" ${form.commissionType == 'PERCENT' || empty form ? 'selected' : ''}>Percentage of the order</option>
                <option value="FIXED" ${form.commissionType == 'FIXED' ? 'selected' : ''}>Fixed amount per order (VND)</option>
            </select>
            <c:if test="${not empty errors.commissionType}"><div class="field-error"><c:out value="${errors.commissionType}"/></div></c:if>
        </label>
        <label>Value (percent or VND)
            <input type="text" name="commissionValue" inputmode="decimal" maxlength="12" value="<c:out value='${empty formValue ? (editing ? form.commissionValue : "") : formValue}'/>">
            <span class="hint">A percentage is at most 50. A fixed amount is paid once per order.</span>
            <c:if test="${not empty errors.commissionValue}"><div class="field-error"><c:out value="${errors.commissionValue}"/></div></c:if>
        </label>
        <label>Minimum order (VND)
            <input type="text" name="minOrder" inputmode="numeric" maxlength="11" value="<c:out value='${empty formMin ? (editing ? form.minOrder.toBigInteger() : "") : formMin}'/>">
            <span class="hint">The amount after the discount and before VAT. Leave empty for no minimum.</span>
            <c:if test="${not empty errors.minOrder}"><div class="field-error"><c:out value="${errors.minOrder}"/></div></c:if>
        </label>
        <label>Start date <input type="date" name="startDate" required value="${form.startDate}">
            <c:if test="${not empty errors.startDate}"><div class="field-error"><c:out value="${errors.startDate}"/></div></c:if>
        </label>
        <label>End date <input type="date" name="endDate" required value="${form.endDate}">
            <c:if test="${not empty errors.endDate}"><div class="field-error"><c:out value="${errors.endDate}"/></div></c:if>
        </label>
        <button type="submit" class="primary"><c:out value="${editing ? 'Save' : 'Create draft'}"/></button>
        <c:if test="${editing}"><a class="btn" href="${ctx}/admin/campaigns">Cancel</a></c:if>
    </form>
    <p class="hint">New campaigns start as a draft. Publish them from the list below.</p>
</div>

<div class="card">
    <form method="get" action="${ctx}/admin/campaigns" class="filters">
        <label>Search <input type="text" name="q" maxlength="80" value="<c:out value='${q}'/>"></label>
        <label>Status
            <select name="status">
                <option value="">All</option>
                <option value="DRAFT" ${statusFilter == 'DRAFT' ? 'selected' : ''}>Draft</option>
                <option value="ACTIVE" ${statusFilter == 'ACTIVE' ? 'selected' : ''}>Active</option>
                <option value="PAUSED" ${statusFilter == 'PAUSED' ? 'selected' : ''}>Paused</option>
                <option value="ENDED" ${statusFilter == 'ENDED' ? 'selected' : ''}>Ended</option>
            </select>
        </label>
        <button type="submit" class="primary">Filter</button>
        <a class="btn" href="${ctx}/admin/campaigns">Reset</a>
    </form>
    <table>
        <thead><tr><th>Name</th><th>Commission</th><th>Min order</th><th>Dates</th><th>Orders</th><th>Status</th><th>Action</th></tr></thead>
        <tbody>
        <c:forEach var="c" items="${result.items}">
            <tr>
                <td><b><c:out value="${c.name}"/></b><br><span class="hint"><c:out value="${c.description}"/></span></td>
                <td>
                    <c:choose>
                        <c:when test="${c.commissionType == 'PERCENT'}"><fmt:formatNumber value="${c.commissionValue}" pattern="0.##"/>% of the order</c:when>
                        <c:otherwise><fmt:formatNumber value="${c.commissionValue}" pattern="#,##0"/> VND per order</c:otherwise>
                    </c:choose>
                </td>
                <td><fmt:formatNumber value="${c.minOrder}" pattern="#,##0"/></td>
                <td>${c.startDate} to ${c.endDate}</td>
                <td>${c.orderCount}</td>
                <td><span class="pill ${c.status}">${c.status}</span></td>
                <td>
                    <c:if test="${c.status != 'ENDED'}"><a class="btn" href="${ctx}/admin/campaigns?edit=${c.campaignId}">Edit</a></c:if>
                    <form method="post" action="${ctx}/admin/campaigns" class="inline">
                        <input type="hidden" name="id" value="${c.campaignId}">
                        <c:if test="${c.status == 'DRAFT'}"><button type="submit" name="action" value="publish">Publish</button></c:if>
                        <c:if test="${c.status == 'PAUSED'}"><button type="submit" name="action" value="resume">Resume</button></c:if>
                        <c:if test="${c.status == 'ACTIVE'}"><button type="submit" name="action" value="pause">Pause</button></c:if>
                        <button type="submit" name="action" value="duplicate">Duplicate</button>
                        <c:if test="${c.status != 'ENDED'}"><button type="submit" name="action" value="end" onclick="return confirm('End this campaign? Orders saved from now on will not earn from it.');">End</button></c:if>
                        <button type="submit" name="action" value="delete" onclick="return confirm('Delete this campaign? It only works when no order uses it.');">Delete</button>
                    </form>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty result.items}"><tr><td colspan="7">No campaigns found.</td></tr></c:if>
        </tbody>
    </table>
    <c:set var="qe"><c:out value="${q}"/></c:set>
    <c:set var="pagerBase" value="${ctx}/admin/campaigns?q=${qe}&amp;status=${statusFilter}&amp;"/>
    <%@ include file="/WEB-INF/views/common/pager.jspf" %>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
