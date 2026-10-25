<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="E-invoices"/>
<c:set var="nav" value="einvoices"/>
<c:set var="taxTab" value="invoices"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<c:set var="base" value="${ctx}/invoices/manage?status=${status}"/>
<div class="card">
    <h1>E-invoices</h1>
    <c:if test="${not empty message}"><div class="msg"><c:out value="${message}"/></div></c:if>
    <div class="error">Not valid until your e-invoice provider issues it. Enter the provider's number and lookup code when you receive them.</div>
    <p class="hint">Here you prepare an invoice, download it as a CSV file and keep track of it. The real e-invoice is issued by your provider, not by this app.</p>
</div>

<div class="card">
    <h2>How to get a valid e-invoice</h2>
    <ol>
        <li>Check whether you must use e-invoices. Your revenue this year (<c:out value="${year}"/>) is
            <strong><fmt:formatNumber value="${yearRevenue}" pattern="#,##0"/> VND</strong>, compared with the
            <fmt:formatNumber value="${threshold}" pattern="#,##0"/> VND mark.
            <c:choose>
                <c:when test="${yearRevenue >= threshold}">You are above the mark.</c:when>
                <c:otherwise>You are below the mark.</c:otherwise>
            </c:choose>
            Rules differ between sources. Confirm with your tax office. Verify with your tax office.</li>
        <li>Choose an e-invoice provider, or ask the marketplace whether it issues invoices for online orders. Verify with your tax office.</li>
        <li>Register with the tax authority through the provider. Verify with your tax office.</li>
        <li>Download the CSV here and copy it into the provider's portal. Verify with your tax office.</li>
        <li>Paste the provider's invoice number and lookup code back here to mark the invoice ISSUED. Verify with your tax office.</li>
    </ol>
    <p class="hint">Official sources:
        <a href="https://xaydungchinhsach.chinhphu.vn/" target="_blank" rel="noopener">xaydungchinhsach.chinhphu.vn</a>,
        <a href="https://gdt.gov.vn/" target="_blank" rel="noopener">gdt.gov.vn</a></p>
</div>

<div class="card narrow">
    <h2>Seller details</h2>
    <p class="hint">Printed at the top of the CSV file.</p>
    <form method="post" action="${ctx}/invoices/manage">
        <input type="hidden" name="action" value="seller">
        <label>Store name
            <input type="text" name="sellerName" required maxlength="100" value="<c:out value='${seller.name}'/>">
            <c:if test="${not empty sellerErrors.name}"><div class="field-error"><c:out value="${sellerErrors.name}"/></div></c:if>
        </label>
        <label>Tax code
            <input type="text" name="sellerTax" maxlength="14" value="<c:out value='${seller.tax}'/>">
            <c:if test="${not empty sellerErrors.tax}"><div class="field-error"><c:out value="${sellerErrors.tax}"/></div></c:if>
        </label>
        <label>Address
            <input type="text" name="sellerAddress" maxlength="200" value="<c:out value='${seller.address}'/>">
            <c:if test="${not empty sellerErrors.address}"><div class="field-error"><c:out value="${sellerErrors.address}"/></div></c:if>
        </label>
        <button type="submit" class="primary">Save seller details</button>
    </form>
</div>

<div class="card narrow">
    <h2>Invoice for a counter order</h2>
    <p class="hint">For an online sale use the Create invoice link in the Sales book. For a counter order type its order code.</p>
    <form method="post" action="${ctx}/invoices/manage" class="filters">
        <input type="hidden" name="action" value="order">
        <label>Order code
            <input type="text" name="orderCode" required maxlength="10" placeholder="TINKY12345">
        </label>
        <button type="submit" class="primary">Continue</button>
    </form>
</div>

<div class="card">
    <form method="get" action="${ctx}/invoices/manage" class="filters">
        <label>Status
            <select name="status">
                <option value="" ${empty status ? 'selected' : ''}>All</option>
                <option value="DRAFT" ${status == 'DRAFT' ? 'selected' : ''}>DRAFT</option>
                <option value="SENT_TO_PROVIDER" ${status == 'SENT_TO_PROVIDER' ? 'selected' : ''}>SENT_TO_PROVIDER</option>
                <option value="ISSUED" ${status == 'ISSUED' ? 'selected' : ''}>ISSUED</option>
            </select>
        </label>
        <button type="submit" class="primary">Show</button>
    </form>
    <table>
        <thead>
        <tr>
            <th>Invoice</th>
            <th>Source</th>
            <th>Buyer</th>
            <th>Total</th>
            <th>Status</th>
            <th>Created</th>
            <th>Action</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="i" items="${invoices.items}">
            <tr>
                <td><c:out value="${i.invoiceNo}"/></td>
                <td><c:out value="${i.sourceType == 'ORDER' ? 'Counter order' : 'Online sale'}"/> #<c:out value="${i.sourceId}"/></td>
                <td><c:out value="${i.buyerName}"/></td>
                <td><fmt:formatNumber value="${i.totalAmount}" pattern="#,##0"/></td>
                <td>
                    <span class="pill ${i.status == 'ISSUED' ? 'COMPLETED' : (i.status == 'SENT_TO_PROVIDER' ? 'PENDING' : 'CANCELLED')}"><c:out value="${i.status}"/></span>
                    <c:if test="${i.status == 'ISSUED'}">
                        <div class="hint">No. <c:out value="${i.providerInvoiceNo}"/> / Code <c:out value="${i.providerLookupCode}"/></div>
                    </c:if>
                </td>
                <td><fmt:formatDate value="${i.createdAt}" pattern="yyyy-MM-dd HH:mm"/></td>
                <td>
                    <a href="${ctx}/invoices/export?id=${i.invoiceId}">Download CSV</a>
                    <c:if test="${i.status == 'DRAFT'}">
                        <a href="${ctx}/invoices/new?invoiceId=${i.invoiceId}">Edit</a>
                        <form method="post" action="${ctx}/invoices/manage" class="inline">
                            <input type="hidden" name="action" value="sent">
                            <input type="hidden" name="id" value="${i.invoiceId}">
                            <button type="submit">Mark as sent to provider</button>
                        </form>
                        <form method="post" action="${ctx}/invoices/manage" class="inline"
                              data-name="<c:out value='${i.invoiceNo}'/>"
                              onsubmit="return confirm('Delete draft ' + this.dataset.name + '?');">
                            <input type="hidden" name="action" value="delete">
                            <input type="hidden" name="id" value="${i.invoiceId}">
                            <button type="submit">Delete</button>
                        </form>
                    </c:if>
                    <c:if test="${i.status == 'SENT_TO_PROVIDER'}">
                        <form method="post" action="${ctx}/invoices/manage" class="filters">
                            <input type="hidden" name="action" value="issue">
                            <input type="hidden" name="id" value="${i.invoiceId}">
                            <input type="text" name="providerNo" required maxlength="30" placeholder="Provider invoice no." aria-label="Provider invoice number">
                            <input type="text" name="lookupCode" required maxlength="40" placeholder="Lookup code" aria-label="Lookup code">
                            <button type="submit" class="primary">Mark as issued</button>
                        </form>
                    </c:if>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty invoices.items}">
            <tr><td colspan="7">No invoices yet.</td></tr>
        </c:if>
        </tbody>
    </table>
    <p>${invoices.total} invoice(s), page ${invoices.page} of ${invoices.totalPages}</p>
    <div class="pages">
        <c:if test="${invoices.page > 1}">
            <a href="${base}&amp;page=${invoices.page - 1}" title="Previous page">&laquo;</a>
        </c:if>
        <c:forEach begin="1" end="${invoices.totalPages}" var="pg">
            <c:choose>
                <c:when test="${pg == invoices.page}"><span>${pg}</span></c:when>
                <c:otherwise><a href="${base}&amp;page=${pg}">${pg}</a></c:otherwise>
            </c:choose>
        </c:forEach>
        <c:if test="${invoices.page < invoices.totalPages}">
            <a href="${base}&amp;page=${invoices.page + 1}" title="Next page">&raquo;</a>
        </c:if>
    </div>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
