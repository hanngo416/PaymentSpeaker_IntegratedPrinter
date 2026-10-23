<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Groups"/>
<c:set var="nav" value="products"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<div class="card">
    <h1>Groups</h1>
    <div>
        <a class="btn" href="${ctx}/admin/products">Products</a>
        <a class="btn" href="${ctx}/admin/categories">Categories</a>
        <a class="btn primary" href="${ctx}/admin/groups">Groups</a>
    </div>
    <c:if test="${not empty message}"><div class="msg"><c:out value="${message}"/></div></c:if>
    <p class="hint">A group sits inside one category and holds products. You can move a group to another category by changing its category and saving.</p>
</div>

<div class="card narrow">
    <h2>Add group</h2>
    <form method="post" action="${ctx}/admin/groups">
        <input type="hidden" name="action" value="addGroup">
        <label>Category
            <select name="categoryId" required>
                <option value="">Choose a category</option>
                <c:forEach var="cat" items="${categories}">
                    <option value="${cat.categoryId}" ${categoryFilter == cat.categoryId ? 'selected' : ''}><c:out value="${cat.categoryName}"/></option>
                </c:forEach>
            </select>
        </label>
        <label>Name
            <input type="text" name="name" required minlength="2" maxlength="50">
        </label>
        <label>Display order
            <input type="text" name="displayOrder" inputmode="numeric" maxlength="4">
        </label>
        <button type="submit" class="primary">Add</button>
    </form>
</div>

<div class="card">
    <form method="get" action="${ctx}/admin/groups" class="filters">
        <label>Search
            <input type="text" name="q" maxlength="50" placeholder="Group name" value="<c:out value='${q}'/>">
        </label>
        <label>Category
            <select name="category">
                <option value="">All categories</option>
                <c:forEach var="cat" items="${categories}">
                    <option value="${cat.categoryId}" ${categoryFilter == cat.categoryId ? 'selected' : ''}><c:out value="${cat.categoryName}"/></option>
                </c:forEach>
            </select>
        </label>
        <button type="submit" class="primary">Filter</button>
        <a class="btn" href="${ctx}/admin/groups">Reset</a>
    </form>
    <table>
        <thead>
        <tr><th>Group</th><th>Products</th><th>Action</th></tr>
        </thead>
        <tbody>
        <c:forEach var="g" items="${result.items}">
            <c:set var="productCount" value="${empty productCounts[g.groupId] ? 0 : productCounts[g.groupId]}"/>
            <tr>
                <td>
                    <form method="post" action="${ctx}/admin/groups" class="filters">
                        <input type="hidden" name="action" value="saveGroup">
                        <input type="hidden" name="id" value="${g.groupId}">
                        <select name="categoryId" aria-label="Category">
                            <c:forEach var="cat" items="${categories}">
                                <option value="${cat.categoryId}" ${cat.categoryId == g.categoryId ? 'selected' : ''}><c:out value="${cat.categoryName}"/></option>
                            </c:forEach>
                        </select>
                        <input type="text" name="name" required minlength="2" maxlength="50" value="<c:out value='${g.groupName}'/>" aria-label="Group name">
                        <input type="text" name="displayOrder" inputmode="numeric" maxlength="4" value="${g.displayOrder}" aria-label="Order" style="width:70px">
                        <button type="submit">Save</button>
                    </form>
                </td>
                <td>${productCount}</td>
                <td>
                    <form method="post" action="${ctx}/admin/groups" class="inline"
                          data-name="<c:out value='${g.groupName}'/>"
                          onsubmit="return confirm('Delete group ' + this.dataset.name + '?');">
                        <input type="hidden" name="id" value="${g.groupId}">
                        <button type="submit" name="action" value="deleteGroup" title="Works only when the group has no products">Delete</button>
                    </form>
                    <c:if test="${productCount > 0}">
                        <details>
                            <summary>Force delete</summary>
                            <div class="msg">
                                Warning: this permanently deletes the group <strong><c:out value="${g.groupName}"/></strong>
                                and its <strong>${productCount}</strong> product(s). It cannot be undone.
                                Past orders and receipts keep the product names and prices.
                                To only remove a product from sale, hide it on the Products tab instead.
                            </div>
                            <form method="post" action="${ctx}/admin/groups">
                                <input type="hidden" name="action" value="forceDeleteGroup">
                                <input type="hidden" name="id" value="${g.groupId}">
                                <label>Type the group name to confirm
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
            <tr><td colspan="3">No groups found.</td></tr>
        </c:if>
        </tbody>
    </table>
    <p>${result.total} group(s), page ${result.page} of ${result.totalPages}</p>
    <div class="pages">
        <c:if test="${result.page > 1}">
            <a href="${ctx}/admin/groups?page=${result.page - 1}&amp;q=<c:out value='${q}'/>&amp;category=${categoryFilter}" title="Previous page">&laquo;</a>
        </c:if>
        <c:forEach begin="1" end="${result.totalPages}" var="pg">
            <c:choose>
                <c:when test="${pg == result.page}"><span>${pg}</span></c:when>
                <c:otherwise><a href="${ctx}/admin/groups?page=${pg}&amp;q=<c:out value='${q}'/>&amp;category=${categoryFilter}">${pg}</a></c:otherwise>
            </c:choose>
        </c:forEach>
        <c:if test="${result.page < result.totalPages}">
            <a href="${ctx}/admin/groups?page=${result.page + 1}&amp;q=<c:out value='${q}'/>&amp;category=${categoryFilter}" title="Next page">&raquo;</a>
        </c:if>
    </div>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
