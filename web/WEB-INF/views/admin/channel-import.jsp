<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Import platform commissions"/>
<c:set var="nav" value="affiliates"/>
<c:set var="chTab" value="import"/>
<c:set var="canWrite" value="true"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<%@ include file="/WEB-INF/views/common/channel-tabs.jspf" %>
<div class="card narrow">
    <h2>Import a commission report</h2>
    <p class="hint">Steps: upload the CSV, review the preview, then confirm. The whole file is checked first: if any line has an error, nothing is imported. Records already imported are skipped.</p>
    <p class="hint">The commission amount in the file is used as reported by the platform. The file is not verified against Shopee or TikTok Shop; only the app templates were tested.</p>
    <p>Templates: <a href="${ctx}/admin/channel-commissions/template?channel=SHOPEE">Shopee</a> | <a href="${ctx}/admin/channel-commissions/template?channel=TIKTOK">TikTok Shop</a></p>
    <c:if test="${not empty formError}"><div class="error"><c:out value="${formError}"/></div></c:if>
    <form method="post" action="${ctx}/admin/channel-commissions/import" enctype="multipart/form-data">
        <input type="hidden" name="action" value="preview">
        <label>Platform
            <select name="channel" required>
                <option value="SHOPEE" ${channel == 'SHOPEE' ? 'selected' : ''}>Shopee</option>
                <option value="TIKTOK" ${channel == 'TIKTOK' ? 'selected' : ''}>TikTok Shop</option>
            </select>
        </label>
        <label>CSV file (up to 2 MB and 5,000 rows)
            <input type="file" name="file" accept=".csv,text/csv" required>
        </label>
        <button type="submit" class="primary">Preview</button>
    </form>
</div>

<c:if test="${hasPreview}">
    <div class="card">
        <h2>Preview</h2>
        <c:choose>
            <c:when test="${errorCount > 0}">
                <div class="error">Nothing can be imported yet. Fix the ${errorCount} problem(s) below and upload the file again.</div>
                <ul>
                    <c:forEach var="e" items="${errorLines}"><li><c:out value="${e}"/></li></c:forEach>
                </ul>
                <c:if test="${errorCount > errorLines.size()}"><p class="hint">Showing the first ${errorLines.size()} of ${errorCount} problems.</p></c:if>
            </c:when>
            <c:otherwise>
                <div class="msg">${previewCount} new record(s) ready to import, ${duplicates} duplicate(s) will be skipped.</div>
                <table>
                    <thead><tr><th>Platform order</th><th>Record reference</th><th>Order date</th><th>Partner</th><th>Base</th><th>Rate</th><th>Calculated</th><th>Reported by platform</th></tr></thead>
                    <tbody>
                    <c:forEach var="r" items="${previewRows}">
                        <tr>
                            <td><c:out value="${r.platformOrderId}"/></td>
                            <td><c:out value="${r.externalRef}"/></td>
                            <td><c:out value="${r.orderDate}"/></td>
                            <td><c:out value="${r.affiliateName}"/></td>
                            <td><fmt:formatNumber value="${r.baseAmount}" pattern="#,##0"/></td>
                            <td><fmt:formatNumber value="${r.commissionRate}" pattern="#,##0.##"/>%</td>
                            <td><fmt:formatNumber value="${r.calculatedAmount}" pattern="#,##0"/></td>
                            <td><b><fmt:formatNumber value="${r.reportedAmount}" pattern="#,##0"/></b></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
                <c:if test="${previewCount > previewRows.size()}"><p class="hint">Showing the first ${previewRows.size()} of ${previewCount} records.</p></c:if>
                <form method="post" action="${ctx}/admin/channel-commissions/import" class="filters">
                    <button type="submit" name="action" value="confirm" class="primary" onclick="return confirm('Import ${previewCount} commission record(s) as PENDING?');">Confirm import</button>
                    <button type="submit" name="action" value="discard">Discard</button>
                </form>
            </c:otherwise>
        </c:choose>
    </div>
</c:if>

<c:if test="${done}">
    <div class="card narrow">
        <h2>Result</h2>
        <div class="msg">${imported} commission(s) imported as PENDING, ${duplicates} duplicate(s) skipped.</div>
        <a class="btn primary" href="${ctx}/admin/channel-commissions?status=PENDING">Review pending commissions</a>
    </div>
</c:if>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
