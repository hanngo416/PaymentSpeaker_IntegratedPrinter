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
    <c:if test="${isAdmin && pendingCount > 0}">
        <div class="msg">
            ${pendingCount} account(s) signed in with Google and are awaiting approval. They cannot enter until you
            choose a role and press Approve (or remove them with Reject).
            <a href="${ctx}/admin/users?status=PENDING">Show only awaiting approval</a>
        </div>
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
                    <option value="${st}" ${f.status == st.name() ? 'selected' : ''}>${st == 'PENDING' ? 'Awaiting approval' : st}</option>
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
            <c:if test="${canWrite}"><th style="width: 1%; white-space: nowrap;">Actions</th></c:if>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="u" items="${result.items}">
            <c:set var="isSelf" value="${u.userId == sessionScope.loginUser.userId}"/>
            <tr>
                <td>
                    <c:out value="${u.userCode}"/>
                    <c:if test="${isSelf}">
                        <span class="badge-self" title="This is your logged-in account">You</span>
                    </c:if>
                </td>
                <td><c:out value="${u.fullName}"/></td>
                <td><c:out value="${u.email}"/></td>
                <td>${u.status == 'PENDING' ? '' : u.role}<c:if test="${u.status == 'PENDING'}"><span class="hint">Choose a role</span></c:if></td>
                <td><span class="pill ${u.status}"><c:out value="${u.status == 'PENDING' ? 'Awaiting approval' : u.status}"/></span></td>
                <td>${empty u.googleId ? 'not linked' : 'linked'}</td>
                <c:if test="${canWrite}">
                    <td>
                        <div class="actions">
                        <c:if test="${isAdmin && u.status == 'PENDING'}">
                            <form method="post" action="${ctx}/admin/users/action">
                                <input type="hidden" name="id" value="${u.userId}">
                                <input type="hidden" name="action" value="approve">
                                <select name="role" aria-label="Role to give" class="select-role-compact">
                                    <c:forEach var="r" items="${roles}">
                                        <option value="${r}" ${r == 'CASHIER' ? 'selected' : ''}>${r}</option>
                                    </c:forEach>
                                </select>
                                <button type="submit" class="btn-action btn-approve" title="Give this role and open the account">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="20 6 9 17 4 12"/></svg>
                                    <span>Approve</span>
                                </button>
                            </form>
                            <form method="post" action="${ctx}/admin/users/action"
                                  data-name="<c:out value='${u.userCode}'/>"
                                  onsubmit="return confirm('Reject and delete request of ' + this.dataset.name + '?');">
                                <input type="hidden" name="id" value="${u.userId}">
                                <input type="hidden" name="action" value="delete">
                                <button type="submit" class="btn-action btn-danger" title="Delete this account request">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                                    <span>Reject</span>
                                </button>
                            </form>
                        </c:if>
                        <c:if test="${u.status != 'PENDING' && (isAdmin || u.role == 'CASHIER')}">
                            <a class="btn-action btn-edit" href="${ctx}/admin/users/edit?id=${u.userId}" title="Edit profile and role">
                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                                <span>Edit</span>
                            </a>
                            <c:choose>
                                <c:when test="${isSelf}">
                                    <button type="button" class="btn-action btn-lock" disabled title="You cannot lock your own account">
                                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                                        <span>Lock</span>
                                    </button>
                                    <c:if test="${isAdmin}">
                                        <button type="button" class="btn-action btn-danger" disabled title="You cannot delete your own account">
                                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/></svg>
                                            <span>Delete</span>
                                        </button>
                                    </c:if>
                                </c:when>
                                <c:otherwise>
                                    <form method="post" action="${ctx}/admin/users/action"
                                          data-name="<c:out value='${u.userCode}'/>"
                                          onsubmit="return confirm('${u.status == 'LOCKED' ? 'Unlock account' : 'Lock account'} ' + this.dataset.name + '?');">
                                        <input type="hidden" name="id" value="${u.userId}">
                                        <input type="hidden" name="action" value="${u.status == 'LOCKED' ? 'unlock' : 'lock'}">
                                        <c:choose>
                                            <c:when test="${u.status == 'LOCKED'}">
                                                <button type="submit" class="btn-action btn-unlock" title="Unlock account to restore login access">
                                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 9.9-1"/></svg>
                                                    <span>Unlock</span>
                                                </button>
                                            </c:when>
                                            <c:otherwise>
                                                <button type="submit" class="btn-action btn-lock" title="Lock account to prevent login">
                                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                                                    <span>Lock</span>
                                                </button>
                                            </c:otherwise>
                                        </c:choose>
                                    </form>
                                    <c:if test="${isAdmin && not empty u.googleId}">
                                        <form method="post" action="${ctx}/admin/users/action"
                                              data-name="<c:out value='${u.userCode}'/>"
                                              onsubmit="return confirm('Unlink the Google account of ' + this.dataset.name + '?');">
                                            <input type="hidden" name="id" value="${u.userId}">
                                            <input type="hidden" name="action" value="unlink">
                                            <button type="submit" class="btn-action btn-unlink" title="Unlink connected Google account">
                                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/><line x1="2" y1="2" x2="22" y2="22"/></svg>
                                                <span>Unlink</span>
                                            </button>
                                        </form>
                                    </c:if>
                                    <c:if test="${isAdmin}">
                                        <form method="post" action="${ctx}/admin/users/action"
                                              data-name="<c:out value='${u.userCode}'/>"
                                              onsubmit="return confirm('Delete user ' + this.dataset.name + '? This cannot be undone.');">
                                            <input type="hidden" name="id" value="${u.userId}">
                                            <input type="hidden" name="action" value="delete">
                                            <button type="submit" class="btn-action btn-danger" title="Permanently delete user">
                                                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/></svg>
                                                <span>Delete</span>
                                            </button>
                                        </form>
                                    </c:if>
                                </c:otherwise>
                            </c:choose>
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
