<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Import sales"/>
<c:set var="nav" value="sales"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<div class="card narrow">
    <h1>Import online sales</h1>
    <p class="hint">Upload a CSV file. The whole file is checked first: if any line has an error, nothing is imported. Orders that already exist for the channel are skipped.</p>
    <div style="display: flex; gap: 8px; flex-wrap: wrap; margin: 12px 0 16px 0; align-items: center;">
        <a class="btn" href="${ctx}/sales/import/template?channel=SHOPEE">Download Shopee sample CSV</a>
        <a class="btn" href="${ctx}/sales/import/template?channel=TIKTOK">Download TikTok Shop sample CSV</a>
        <a href="${ctx}/sales" style="margin-left: 8px;">Back to sales book</a>
    </div>
    <c:if test="${not empty formError}"><div class="error"><c:out value="${formError}"/></div></c:if>
    <form method="post" action="${ctx}/sales/import" enctype="multipart/form-data">
        <label>Channel
            <select name="channel" required>
                <option value="SHOPEE" ${channel == 'SHOPEE' ? 'selected' : ''}>Shopee</option>
                <option value="TIKTOK" ${channel == 'TIKTOK' ? 'selected' : ''}>TikTok Shop</option>
            </select>
        </label>
        <label>CSV file (up to 2 MB and 5,000 rows)
            <input type="file" name="file" accept=".csv,text/csv" required>
        </label>
        <button type="submit" class="primary">Import</button>
    </form>
</div>

<c:if test="${done}">
    <div class="card narrow">
        <h2>Result</h2>
        <c:choose>
            <c:when test="${errorCount > 0}">
                <div class="error">Nothing was imported. Fix the ${errorCount} problem(s) below and upload the file again.</div>
                <ul>
                    <c:forEach var="e" items="${errorLines}">
                        <li><c:out value="${e}"/></li>
                    </c:forEach>
                </ul>
                <c:if test="${errorCount > errorLines.size()}">
                    <p class="hint">Showing the first ${errorLines.size()} of ${errorCount} problems.</p>
                </c:if>
            </c:when>
            <c:otherwise>
                <div class="msg">${imported} sale(s) imported, ${duplicates} duplicate(s) skipped.</div>
            </c:otherwise>
        </c:choose>
    </div>
</c:if>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
