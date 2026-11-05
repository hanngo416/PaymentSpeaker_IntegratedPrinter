<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Categories"/>
<c:set var="nav" value="products"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<div class="card">
    <h1>Categories</h1>
    <div>
        <a class="btn" href="${ctx}/admin/products">Products</a>
        <a class="btn primary" href="${ctx}/admin/categories">Categories</a>
        <a class="btn" href="${ctx}/admin/groups">Groups</a>
        <a class="btn" href="${ctx}/admin/stock-history">Stock history</a>
    </div>
    <c:if test="${not empty message}"><div class="msg"><c:out value="${message}"/></div></c:if>
    <p class="hint">A category holds groups and a group holds products, as the cashier sees them on the order screen. A smaller order number shows first.</p>
</div>

<div class="card narrow">
    <h2>Add category</h2>
    <form method="post" action="${ctx}/admin/categories">
        <input type="hidden" name="action" value="addCategory">
        <label>Name
            <input type="text" name="name" required minlength="2" maxlength="50" placeholder="Personal care">
        </label>
        <label>Display order
            <input type="text" name="displayOrder" inputmode="numeric" maxlength="4">
        </label>
        <button type="submit" class="primary">Add</button>
    </form>
</div>

<div class="card">
    <form method="get" action="${ctx}/admin/categories" class="filters">
        <label>Search
            <input type="text" name="q" maxlength="50" placeholder="Category name" value="<c:out value='${q}'/>">
        </label>
        <button type="submit" class="primary">Filter</button>
        <a class="btn" href="${ctx}/admin/categories">Reset</a>
    </form>
    <table>
        <thead>
        <tr><th>Category</th><th>Groups</th><th>Products</th><th>Action</th></tr>
        </thead>
        <tbody>
        <c:forEach var="cat" items="${result.items}">
            <c:set var="groupCount" value="${empty groupCounts[cat.categoryId] ? 0 : groupCounts[cat.categoryId]}"/>
            <c:set var="productCount" value="${empty categoryProductCounts[cat.categoryId] ? 0 : categoryProductCounts[cat.categoryId]}"/>
            <tr>
                <td>
                    <form method="post" action="${ctx}/admin/categories" class="filters">
                        <input type="hidden" name="action" value="saveCategory">
                        <input type="hidden" name="id" value="${cat.categoryId}">
                        <input type="text" name="name" required minlength="2" maxlength="50" value="<c:out value='${cat.categoryName}'/>" aria-label="Category name">
                        <input type="text" name="displayOrder" inputmode="numeric" maxlength="4" value="${cat.displayOrder}" aria-label="Order" style="width:70px">
                        <button type="submit">Save</button>
                    </form>
                </td>
                <td>${groupCount}</td>
                <td>${productCount}</td>
                <td>
                    <form method="post" action="${ctx}/admin/categories" class="inline"
                          data-name="<c:out value='${cat.categoryName}'/>"
                          onsubmit="return confirm('Delete category ' + this.dataset.name + '?');">
                        <input type="hidden" name="id" value="${cat.categoryId}">
                        <button type="submit" name="action" value="deleteCategory" title="Works only when the category has no groups">Delete</button>
                    </form>
                    <c:if test="${groupCount > 0}">
                        <details>
                            <summary>Force delete</summary>
                            <div class="msg">
                                Warning: this permanently deletes the category <strong><c:out value="${cat.categoryName}"/></strong>,
                                its <strong>${groupCount}</strong> group(s) and its <strong>${productCount}</strong> product(s).
                                It cannot be undone. Past orders and receipts keep the product names and prices,
                                but the products disappear from the cashier screen and from the Products tab.
                                To only remove a product from sale, hide it on the Products tab instead.
                            </div>
                            <form method="post" action="${ctx}/admin/categories">
                                <input type="hidden" name="action" value="forceDeleteCategory">
                                <input type="hidden" name="id" value="${cat.categoryId}">
                                <label>Type the category name to confirm
                                    <input type="text" name="confirmName" required autocomplete="off" maxlength="50">
                                </label>
                                <button type="submit" class="danger">Force delete</button>
                            </form>
                        </details>
                    </c:if>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty result.items}">
            <tr><td colspan="4">No categories found.</td></tr>
        </c:if>
        </tbody>
    </table>
    <p>${result.total} categor(ies), page ${result.page} of ${result.totalPages}</p>
    <div class="pages">
        <c:if test="${result.page > 1}">
            <a href="${ctx}/admin/categories?page=${result.page - 1}&amp;q=<c:out value='${q}'/>" title="Previous page">&laquo;</a>
        </c:if>
        <c:forEach begin="1" end="${result.totalPages}" var="pg">
            <c:choose>
                <c:when test="${pg == result.page}"><span>${pg}</span></c:when>
                <c:otherwise><a href="${ctx}/admin/categories?page=${pg}&amp;q=<c:out value='${q}'/>">${pg}</a></c:otherwise>
            </c:choose>
        </c:forEach>
        <c:if test="${result.page < result.totalPages}">
            <a href="${ctx}/admin/categories?page=${result.page + 1}&amp;q=<c:out value='${q}'/>" title="Next page">&raquo;</a>
        </c:if>
    </div>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
