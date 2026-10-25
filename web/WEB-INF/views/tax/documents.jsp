<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Records and documents"/>
<c:set var="nav" value="tax"/>
<c:set var="taxTab" value="documents"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<%@ include file="/WEB-INF/views/common/tax-tabs.jspf" %>
<c:url var="base" value="/tax/documents"><c:param name="q" value="${q}"/><c:param name="category" value="${category}"/><c:param name="status" value="${status}"/></c:url>
<div class="card">
    <h1>Records and documents</h1>
    <p class="hint">Keep tax papers in one place and see what is still missing. Files up to 10 MB: PDF, PNG/JPG images, CSV, Excel and Word. Files are stored in the database and are only downloaded as attachments.</p>
</div>

<div class="kpis">
    <div class="card"><span class="kpi-label">Uploaded</span><div class="kpi-value">${counts.uploaded}</div></div>
    <div class="card"><span class="kpi-label">To be added</span><div class="kpi-value ${counts.missing > 0 ? 'bad' : ''}">${counts.missing}</div><p class="kpi-sub">No file yet</p></div>
    <div class="card"><span class="kpi-label">Needs review</span><div class="kpi-value">${counts.review}</div></div>
    <div class="card"><span class="kpi-label">Total</span><div class="kpi-value">${counts.total}</div></div>
</div>

<c:if test="${canManage}">
<div class="card narrow">
    <h2>Upload a document</h2>
    <form method="post" action="${ctx}/tax/documents" enctype="multipart/form-data">
        <input type="hidden" name="action" value="upload">
        <label>File <input type="file" name="file" required accept=".pdf,.png,.jpg,.jpeg,.csv,.xls,.xlsx,.doc,.docx"></label>
        <label>Document name <input type="text" name="name" maxlength="150" placeholder="Leave empty to use the file name"></label>
        <label>Category
            <select name="category">
                <option value="REGISTRATION">Business registration</option>
                <option value="TAX_CODE">Tax code</option>
                <option value="INVOICE">Invoice</option>
                <option value="BANK">Bank statement</option>
                <option value="EXPENSE">Expense</option>
                <option value="FILING">Tax filing and payment</option>
                <option value="OTHER">Other</option>
            </select>
        </label>
        <label>Reporting period <input type="text" name="period" maxlength="40" placeholder="Example: Q3/2026"></label>
        <button type="submit" class="primary">Upload</button>
    </form>
</div>
</c:if>

<div class="card">
    <form method="get" action="${ctx}/tax/documents" class="filters">
        <label>Search <input type="text" name="q" maxlength="100" value="<c:out value='${q}'/>" placeholder="Name, file or period"></label>
        <label>Category
            <select name="category">
                <option value="">All</option>
                <option value="REGISTRATION" ${category == 'REGISTRATION' ? 'selected' : ''}>Business registration</option>
                <option value="TAX_CODE" ${category == 'TAX_CODE' ? 'selected' : ''}>Tax code</option>
                <option value="INVOICE" ${category == 'INVOICE' ? 'selected' : ''}>Invoice</option>
                <option value="BANK" ${category == 'BANK' ? 'selected' : ''}>Bank statement</option>
                <option value="EXPENSE" ${category == 'EXPENSE' ? 'selected' : ''}>Expense</option>
                <option value="FILING" ${category == 'FILING' ? 'selected' : ''}>Tax filing and payment</option>
                <option value="OTHER" ${category == 'OTHER' ? 'selected' : ''}>Other</option>
            </select>
        </label>
        <label>Status
            <select name="status">
                <option value="">All</option>
                <option value="UPLOADED" ${status == 'UPLOADED' ? 'selected' : ''}>Uploaded</option>
                <option value="MISSING" ${status == 'MISSING' ? 'selected' : ''}>To be added</option>
                <option value="REVIEW" ${status == 'REVIEW' ? 'selected' : ''}>Needs review</option>
            </select>
        </label>
        <button type="submit" class="primary">Filter</button>
    </form>
    <table>
        <thead><tr><th>Name</th><th>Category</th><th>Period</th><th>File</th><th class="r">Size</th><th>Status</th><th>Actions</th></tr></thead>
        <tbody>
        <c:forEach var="d" items="${docs}">
            <tr>
                <td><b><c:out value="${d.doc_name}"/></b></td>
                <td><c:choose>
                    <c:when test="${d.category == 'REGISTRATION'}">Business registration</c:when>
                    <c:when test="${d.category == 'TAX_CODE'}">Tax code</c:when>
                    <c:when test="${d.category == 'INVOICE'}">Invoice</c:when>
                    <c:when test="${d.category == 'BANK'}">Bank statement</c:when>
                    <c:when test="${d.category == 'EXPENSE'}">Expense</c:when>
                    <c:when test="${d.category == 'FILING'}">Tax filing and payment</c:when>
                    <c:otherwise>Other</c:otherwise></c:choose></td>
                <td><c:out value="${d.period_label}" default="-"/></td>
                <td><c:out value="${d.file_name}" default="-"/></td>
                <td class="r"><c:choose><c:when test="${d.size_bytes > 0}"><fmt:formatNumber value="${d.size_bytes / 1024}" maxFractionDigits="0"/> KB</c:when><c:otherwise>-</c:otherwise></c:choose></td>
                <td><span class="pill ${d.status == 'UPLOADED' ? 'ACTIVE' : (d.status == 'MISSING' ? 'LOCKED' : 'PENDING')}"><c:choose><c:when test="${d.status == 'UPLOADED'}">Uploaded</c:when><c:when test="${d.status == 'MISSING'}">To be added</c:when><c:otherwise>Needs review</c:otherwise></c:choose></span></td>
                <td>
                    <c:if test="${d.status != 'MISSING'}"><a class="btn" href="${ctx}/tax/documents?download=${d.doc_id}">Download</a></c:if>
                    <c:if test="${canManage}">
                        <c:if test="${d.status == 'MISSING'}">
                            <form method="post" action="${ctx}/tax/documents" enctype="multipart/form-data" class="inline">
                                <input type="hidden" name="action" value="attach">
                                <input type="hidden" name="id" value="${d.doc_id}">
                                <input type="file" name="file" required accept=".pdf,.png,.jpg,.jpeg,.csv,.xls,.xlsx,.doc,.docx">
                                <button type="submit" class="primary">Upload</button>
                            </form>
                        </c:if>
                        <c:if test="${d.status == 'UPLOADED'}">
                            <form method="post" action="${ctx}/tax/documents" class="inline"><input type="hidden" name="action" value="review"><input type="hidden" name="id" value="${d.doc_id}"><button type="submit" class="btn">Mark for review</button></form>
                        </c:if>
                        <c:if test="${d.status == 'REVIEW'}">
                            <form method="post" action="${ctx}/tax/documents" class="inline"><input type="hidden" name="action" value="unreview"><input type="hidden" name="id" value="${d.doc_id}"><button type="submit" class="btn">Review done</button></form>
                        </c:if>
                        <c:if test="${d.status != 'MISSING'}">
                            <form method="post" action="${ctx}/tax/documents" class="inline"><input type="hidden" name="action" value="delete"><input type="hidden" name="id" value="${d.doc_id}"><button type="submit" class="btn danger" onclick="return confirm('Delete this document? This cannot be undone.');">Delete</button></form>
                        </c:if>
                    </c:if>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty docs}"><tr><td colspan="7">No matching documents.</td></tr></c:if>
        </tbody>
    </table>
    <p>Page ${page} of ${pages}</p>
    <div class="pages">
        <c:if test="${page > 1}"><a href="${base}&amp;page=${page - 1}" title="Previous page">&laquo;</a></c:if>
        <c:forEach begin="1" end="${pages}" var="pg">
            <c:choose>
                <c:when test="${pg == page}"><span>${pg}</span></c:when>
                <c:otherwise><a href="${base}&amp;page=${pg}">${pg}</a></c:otherwise>
            </c:choose>
        </c:forEach>
        <c:if test="${page < pages}"><a href="${base}&amp;page=${page + 1}" title="Next page">&raquo;</a></c:if>
    </div>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
