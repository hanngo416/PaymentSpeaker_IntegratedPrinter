<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Products"/>
<c:set var="nav" value="products"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<c:set var="editing" value="${form.productId > 0}"/>
<div class="card">
    <h1>Products</h1>
    <div>
        <a class="btn primary" href="${ctx}/admin/products">Products</a>
        <a class="btn" href="${ctx}/admin/categories">Categories</a>
        <a class="btn" href="${ctx}/admin/groups">Groups</a>
    </div>
    <c:if test="${not empty message}"><div class="msg"><c:out value="${message}"/></div></c:if>
    <p class="hint">Products appear on the cashier's order screen under their group. A hidden product is not shown to the cashier; a product that is on an order cannot be deleted.</p>
</div>

<div class="card narrow">
    <h2><c:out value="${editing ? 'Edit product' : 'Add product'}"/></h2>
    <form method="post" action="${ctx}/admin/products">
        <input type="hidden" name="action" value="save">
        <c:if test="${editing}"><input type="hidden" name="id" value="${form.productId}"></c:if>
        <label>Group
            <select name="groupId" required>
                <option value="">Choose a group</option>
                <c:forEach var="cat" items="${categories}">
                    <optgroup label="<c:out value='${cat.categoryName}'/>">
                        <c:forEach var="g" items="${groups}">
                            <c:if test="${g.categoryId == cat.categoryId}">
                                <option value="${g.groupId}" ${form.groupId == g.groupId ? 'selected' : ''}><c:out value="${g.groupName}"/></option>
                            </c:if>
                        </c:forEach>
                    </optgroup>
                </c:forEach>
            </select>
            <c:if test="${not empty errors.groupId}"><div class="field-error"><c:out value="${errors.groupId}"/></div></c:if>
        </label>
        <label>Product code
            <input type="text" name="productCode" required minlength="2" maxlength="30" value="<c:out value='${form.productCode}'/>">
            <c:if test="${not empty errors.productCode}"><div class="field-error"><c:out value="${errors.productCode}"/></div></c:if>
        </label>
        <label>Name
            <input type="text" name="productName" required minlength="2" maxlength="100" value="<c:out value='${form.productName}'/>">
            <c:if test="${not empty errors.productName}"><div class="field-error"><c:out value="${errors.productName}"/></div></c:if>
        </label>
        <label>Price (VND, VAT included)
            <input type="text" name="price" required inputmode="numeric" maxlength="11" value="<c:out value='${formPrice}'/>">
            <c:if test="${not empty errors.price}"><div class="field-error"><c:out value="${errors.price}"/></div></c:if>
        </label>
        <label>Cost price (VND)
            <input type="text" name="costPrice" inputmode="numeric" maxlength="11" value="<c:out value='${formCost}'/>">
            <span class="hint">What the product costs you. Leave empty for 0. Cannot be more than the price.</span>
            <c:if test="${not empty errors.costPrice}"><div class="field-error"><c:out value="${errors.costPrice}"/></div></c:if>
        </label>
        <label>Stock quantity
            <input type="text" name="stockQuantity" inputmode="numeric" maxlength="9" value="<c:out value='${formStock}'/>">
            <span class="hint">Units on hand, counted by hand. It does not drop when an order is paid. Leave empty for 0.</span>
            <c:if test="${not empty errors.stockQuantity}"><div class="field-error"><c:out value="${errors.stockQuantity}"/></div></c:if>
        </label>
        <label>Display order
            <input type="text" name="displayOrder" inputmode="numeric" maxlength="4" value="<c:out value='${empty formOrder ? (editing ? form.displayOrder : "") : formOrder}'/>">
            <span class="hint">Smaller numbers show first. Leave empty for 0.</span>
            <c:if test="${not empty errors.displayOrder}"><div class="field-error"><c:out value="${errors.displayOrder}"/></div></c:if>
        </label>
        <label>
            <input type="checkbox" name="active" value="1" ${(empty form or form.active) ? 'checked' : ''}> On sale
        </label>
        <button type="submit" class="primary"><c:out value="${editing ? 'Save' : 'Add'}"/></button>
        <c:if test="${editing}"><a class="btn" href="${ctx}/admin/products">Cancel</a></c:if>
    </form>
</div>

<div class="card">
    <form method="get" action="${ctx}/admin/products" class="filters">
        <label>Search
            <input type="text" name="q" maxlength="100" placeholder="Name or code" value="<c:out value='${q}'/>">
        </label>
        <label>Group
            <select name="group">
                <option value="">All groups</option>
                <c:forEach var="g" items="${groups}">
                    <option value="${g.groupId}" ${groupFilter == g.groupId ? 'selected' : ''}><c:out value="${g.groupName}"/></option>
                </c:forEach>
            </select>
        </label>
        <button type="submit" class="primary">Filter</button>
        <a class="btn" href="${ctx}/admin/products">Reset</a>
    </form>
    <table>
        <thead>
        <tr><th>Code</th><th>Name</th><th>Category / group</th><th>Price</th><th>Cost</th><th>Stock</th><th>Status</th><th>Action</th></tr>
        </thead>
        <tbody>
        <c:forEach var="p" items="${result.items}">
            <tr>
                <td><c:out value="${p.productCode}"/></td>
                <td><c:out value="${p.productName}"/></td>
                <td><c:out value="${p.categoryName}"/> / <c:out value="${p.groupName}"/></td>
                <td><fmt:formatNumber value="${p.price}" pattern="#,##0"/> VND</td>
                <td><fmt:formatNumber value="${p.costPrice}" pattern="#,##0"/> VND</td>
                <td><fmt:formatNumber value="${p.stockQuantity}" pattern="#,##0"/></td>
                <td><span class="pill ${p.active ? 'ACTIVE' : 'CANCELLED'}">${p.active ? 'ON SALE' : 'HIDDEN'}</span></td>
                <td>
                    <a class="btn" href="${ctx}/admin/products?edit=${p.productId}">Edit</a>
                    <form method="post" action="${ctx}/admin/products" class="inline">
                        <input type="hidden" name="id" value="${p.productId}">
                        <button type="submit" name="action" value="${p.active ? 'disable' : 'enable'}">${p.active ? 'Hide' : 'Show'}</button>
                    </form>
                    <form method="post" action="${ctx}/admin/products" class="inline"
                          data-name="<c:out value='${p.productName}'/>"
                          onsubmit="return confirm('Delete ' + this.dataset.name + '? This cannot be undone.');">
                        <input type="hidden" name="id" value="${p.productId}">
                        <button type="submit" name="action" value="delete">Delete</button>
                    </form>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty result.items}">
            <tr><td colspan="7">No products found.</td></tr>
        </c:if>
        </tbody>
    </table>
    <p>${result.total} product(s), page ${result.page} of ${result.totalPages}</p>
    <div class="pages">
        <c:if test="${result.page > 1}">
            <a href="${ctx}/admin/products?page=${result.page - 1}&amp;q=<c:out value='${q}'/>&amp;group=${groupFilter}" title="Previous page">&laquo;</a>
        </c:if>
        <c:forEach begin="1" end="${result.totalPages}" var="pg">
            <c:choose>
                <c:when test="${pg == result.page}"><span>${pg}</span></c:when>
                <c:otherwise><a href="${ctx}/admin/products?page=${pg}&amp;q=<c:out value='${q}'/>&amp;group=${groupFilter}">${pg}</a></c:otherwise>
            </c:choose>
        </c:forEach>
        <c:if test="${result.page < result.totalPages}">
            <a href="${ctx}/admin/products?page=${result.page + 1}&amp;q=<c:out value='${q}'/>&amp;group=${groupFilter}" title="Next page">&raquo;</a>
        </c:if>
    </div>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
