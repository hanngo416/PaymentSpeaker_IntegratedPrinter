<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Users"/>
<c:set var="nav" value="users"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<div class="card">
    <h1>Users</h1>
    <c:if test="${not empty message}">
        <div class="msg"><c:out value="${message}"/></div>
    </c:if>

    <form method="get" action="${ctx}/admin/users" class="filters">
        <label>Search
            <input type="text" name="q" maxlength="100" placeholder="user code, name or email"
                   value="<c:out value='${f.keyword}'/>">
        </label>
        <label>Role
            <select name="role">
                <option value="">All</option>
                <c:forEach var="r" items="${roles}">
                    <option value="${r}" ${f.role == r.name() ? 'selected' : ''}>${r}</option>
                </c:forEach>
            </select>
        </label>
        <label>Status
            <select name="status">
                <option value="">All</option>
                <c:forEach var="st" items="${statuses}">
                    <option value="${st}" ${f.status == st.name() ? 'selected' : ''}>${st}</option>
                </c:forEach>
            </select>
        </label>
        <input type="hidden" name="sort" value="${f.sort}">
        <input type="hidden" name="dir" value="${f.dir}">
        <button type="submit" class="primary">Filter</button>
        <a class="btn" href="${ctx}/admin/users">Reset</a>
        <c:if test="${canWrite}">
            <a class="btn primary" href="${ctx}/admin/users/edit">+ New user</a>
        </c:if>
    </form>
</div>

<div class="card">
    <table>
        <thead>
        <tr>
            <th><a href="${ctx}/admin/users?${filterQuery}sort=usercode&dir=${f.sort == 'usercode' && f.dir == 'asc' ? 'desc' : 'asc'}">User code${f.sort == 'usercode' ? (f.dir == 'asc' ? ' &#9650;' : ' &#9660;') : ''}</a></th>
            <th><a href="${ctx}/admin/users?${filterQuery}sort=name&dir=${f.sort == 'name' && f.dir == 'asc' ? 'desc' : 'asc'}">Full name${f.sort == 'name' ? (f.dir == 'asc' ? ' &#9650;' : ' &#9660;') : ''}</a></th>
            <th>Email</th>
            <th><a href="${ctx}/admin/users?${filterQuery}sort=role&dir=${f.sort == 'role' && f.dir == 'asc' ? 'desc' : 'asc'}">Role${f.sort == 'role' ? (f.dir == 'asc' ? ' &#9650;' : ' &#9660;') : ''}</a></th>
            <th><a href="${ctx}/admin/users?${filterQuery}sort=status&dir=${f.sort == 'status' && f.dir == 'asc' ? 'desc' : 'asc'}">Status${f.sort == 'status' ? (f.dir == 'asc' ? ' &#9650;' : ' &#9660;') : ''}</a></th>
            <th>Google</th>
            <c:if test="${canWrite}"><th>Actions</th></c:if>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="u" items="${result.items}">
            <tr>
                <td><c:out value="${u.userCode}"/></td>
                <td><c:out value="${u.fullName}"/></td>
                <td><c:out value="${u.email}"/></td>
                <td><c:out value="${u.role}"/></td>
                <td><span class="pill ${u.status}"><c:out value="${u.status}"/></span></td>
                <td>${empty u.googleId ? 'not linked' : 'linked'}</td>
                <c:if test="${canWrite}">
                    <td>
                        <div class="actions">
                        <c:if test="${isAdmin || u.role == 'CASHIER'}">
                        <a class="btn" href="${ctx}/admin/users/edit?id=${u.userId}">Edit</a>
                        <c:if test="${u.userId != sessionScope.loginUser.userId}">
                            <form method="post" action="${ctx}/admin/users/action">
                                <input type="hidden" name="id" value="${u.userId}">
                                <input type="hidden" name="action" value="${u.status == 'LOCKED' ? 'unlock' : 'lock'}">
                                <button type="submit">${u.status == 'LOCKED' ? 'Unlock' : 'Lock'}</button>
                            </form>
                            <c:if test="${isAdmin && not empty u.googleId}">
                                <form method="post" action="${ctx}/admin/users/action"
                                      data-name="<c:out value='${u.userCode}'/>"
                                      onsubmit="return confirm('Unlink the Google account of ' + this.dataset.name + '?');">
                                    <input type="hidden" name="id" value="${u.userId}">
                                    <input type="hidden" name="action" value="unlink">
                                    <button type="submit">Unlink Google</button>
                                </form>
                            </c:if>
                            <c:if test="${isAdmin}">
                            <form method="post" action="${ctx}/admin/users/action"
                                  data-name="<c:out value='${u.userCode}'/>"
                                  onsubmit="return confirm('Delete user ' + this.dataset.name + '? This cannot be undone.');">
                                <input type="hidden" name="id" value="${u.userId}">
                                <input type="hidden" name="action" value="delete">
                                <button type="submit" class="danger">Delete</button>
                            </form>
                            </c:if>
                        </c:if>
                        </c:if>
                        </div>
                    </td>
                </c:if>
            </tr>
        </c:forEach>
        <c:if test="${empty result.items}">
            <tr><td colspan="7">No users match the filter.</td></tr>
        </c:if>
        </tbody>
    </table>

    <p>${result.total} user(s), page ${result.page} of ${result.totalPages}</p>
    <div class="pages">
        <c:forEach begin="1" end="${result.totalPages}" var="p">
            <c:choose>
                <c:when test="${p == result.page}"><span>${p}</span></c:when>
                <c:otherwise>
                    <a href="${ctx}/admin/users?${filterQuery}sort=${f.sort}&dir=${f.dir}&page=${p}">${p}</a>
                </c:otherwise>
            </c:choose>
        </c:forEach>
    </div>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
