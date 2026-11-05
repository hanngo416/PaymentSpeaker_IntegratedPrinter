<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Stock History"/>
<c:set var="nav" value="products"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>

<div class="card">
    <h1>Stock Movements</h1>
    <div>
        <a class="btn" href="${ctx}/admin/products">Products</a>
        <a class="btn" href="${ctx}/admin/categories">Categories</a>
        <a class="btn" href="${ctx}/admin/groups">Groups</a>
        <a class="btn primary" href="${ctx}/admin/stock-history">Stock history</a>
    </div>
    <p class="hint">Full immutable audit trail of inventory movements. Every sale, opening balance, restock and manual physical count adjustment is recorded here.</p>
</div>

<div class="card">
    <form method="get" action="${ctx}/admin/stock-history" class="filters">
        <label>Product
            <select name="productId">
                <option value="">All products</option>
                <c:forEach var="p" items="${products}">
                    <option value="${p.productId}" ${productId == p.productId ? 'selected' : ''}>
                        <c:out value="${p.productCode}"/> - <c:out value="${p.productName}"/>
                    </option>
                </c:forEach>
            </select>
        </label>
        <label>Reason
            <select name="reason">
                <option value="">All reasons</option>
                <option value="INITIAL" ${reason == 'INITIAL' ? 'selected' : ''}>INITIAL - Opening Balance</option>
                <option value="SALE" ${reason == 'SALE' ? 'selected' : ''}>SALE - Order Checkout</option>
                <option value="RESTOCK" ${reason == 'RESTOCK' ? 'selected' : ''}>RESTOCK - Stock In</option>
                <option value="ADJUST" ${reason == 'ADJUST' ? 'selected' : ''}>ADJUST - Audit Adjustment</option>
            </select>
        </label>
        <label>From
            <input type="date" name="from" value="<c:out value='${from}'/>">
        </label>
        <label>To
            <input type="date" name="to" value="<c:out value='${to}'/>">
        </label>
        <button type="submit" class="primary">Filter</button>
        <a class="btn" href="${ctx}/admin/stock-history">Reset</a>
    </form>

    <table>
        <thead>
        <tr>
            <th>ID</th>
            <th>Date &amp; Time</th>
            <th>Product</th>
            <th>Change</th>
            <th>Stock After</th>
            <th>Reason</th>
            <th>Order Code</th>
            <th>Note</th>
            <th>User</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="m" items="${result.items}">
            <tr>
                <td>#${m.movementId}</td>
                <td><fmt:formatDate value="${m.createdAt}" pattern="yyyy-MM-dd HH:mm:ss"/></td>
                <td>
                    <strong><c:out value="${m.productCode}"/></strong><br>
                    <small style="color:var(--text-muted);"><c:out value="${m.productName}"/></small>
                </td>
                <td>
                    <c:choose>
                        <c:when test="${m.quantityChange > 0}">
                            <strong style="color:var(--ok);">+${m.quantityChange}</strong>
                        </c:when>
                        <c:when test="${m.quantityChange < 0}">
                            <strong style="color:var(--danger);">${m.quantityChange}</strong>
                        </c:when>
                        <c:otherwise>
                            <span>0</span>
                        </c:otherwise>
                    </c:choose>
                </td>
                <td><strong>${m.quantityAfter}</strong></td>
                <td>
                    <span class="pill ${m.reason == 'SALE' ? 'CANCELLED' : m.reason == 'RESTOCK' ? 'ACTIVE' : m.reason == 'ADJUST' ? 'PENDING' : 'LOCKED'}">
                        ${m.reason}
                    </span>
                </td>
                <td>
                    <c:choose>
                        <c:when test="${not empty m.orderCode}">
                            <a href="${ctx}/orders/view?code=${m.orderCode}"><c:out value="${m.orderCode}"/></a>
                        </c:when>
                        <c:otherwise>&mdash;</c:otherwise>
                    </c:choose>
                </td>
                <td><c:out value="${empty m.note ? '&mdash;' : m.note}" escapeXml="false"/></td>
                <td><c:out value="${empty m.createdByCode ? 'system' : m.createdByCode}"/></td>
            </tr>
        </c:forEach>
        <c:if test="${empty result.items}">
            <tr><td colspan="9">No stock movements found matching criteria.</td></tr>
        </c:if>
        </tbody>
    </table>

    <p>${result.total} movement(s), page ${result.page} of ${result.totalPages}</p>
    <div class="pages">
        <c:if test="${result.page > 1}">
            <a href="${ctx}/admin/stock-history?page=${result.page - 1}&amp;productId=${productId}&amp;reason=${reason}&amp;from=${from}&amp;to=${to}" title="Previous page">&laquo;</a>
        </c:if>
        <c:forEach begin="1" end="${result.totalPages}" var="pg">
            <c:choose>
                <c:when test="${pg == result.page}"><span>${pg}</span></c:when>
                <c:otherwise><a href="${ctx}/admin/stock-history?page=${pg}&amp;productId=${productId}&amp;reason=${reason}&amp;from=${from}&amp;to=${to}">${pg}</a></c:otherwise>
            </c:choose>
        </c:forEach>
        <c:if test="${result.page < result.totalPages}">
            <a href="${ctx}/admin/stock-history?page=${result.page + 1}&amp;productId=${productId}&amp;reason=${reason}&amp;from=${from}&amp;to=${to}" title="Next page">&raquo;</a>
        </c:if>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>

