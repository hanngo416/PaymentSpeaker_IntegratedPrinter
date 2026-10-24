<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Sales book"/>
<c:set var="nav" value="sales"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<c:set var="base" value="${ctx}/sales?from=${from}&amp;to=${to}&amp;channel=${channel}"/>
<div class="card">
    <h1>Sales book</h1>
    <c:if test="${not empty message}"><div class="msg"><c:out value="${message}"/></div></c:if>
    <c:if test="${not empty rangeError}"><div class="error"><c:out value="${rangeError}"/></div></c:if>
    <p class="hint"><strong>What to do next:</strong> <c:out value="${nextStep}"/></p>
    <form method="get" action="${ctx}/sales" class="filters">
        <label>From
            <input type="date" name="from" value="<c:out value='${from}'/>" required>
        </label>
        <label>To
            <input type="date" name="to" value="<c:out value='${to}'/>" required>
        </label>
        <label>Channel
            <select name="channel">
                <option value="" ${empty channel ? 'selected' : ''}>All</option>
                <option value="STORE" ${channel == 'STORE' ? 'selected' : ''}>Store</option>
                <option value="SHOPEE" ${channel == 'SHOPEE' ? 'selected' : ''}>Shopee</option>
                <option value="TIKTOK" ${channel == 'TIKTOK' ? 'selected' : ''}>TikTok Shop</option>
            </select>
        </label>
        <button type="submit" class="primary">Show</button>
    </form>
    <p class="hint">Profit is an estimate. It includes an estimated tax reserve, so it is not the amount to pay.</p>
</div>

<div class="kpis">
    <c:forEach var="c" items="${cards}">
        <div class="card">
            <span class="kpi-label"><c:out value="${channelNames[c.channel]}"/></span>
            <div class="kpi-value accent"><fmt:formatNumber value="${c.revenue}" pattern="#,##0"/> <span class="kpi-unit">VND</span></div>
            <p class="kpi-sub">Revenue, ${c.orders} sale(s)</p>
            <p class="kpi-sub ${c.profit < 0 ? 'loss' : ''}">Profit <fmt:formatNumber value="${c.profit}" pattern="#,##0"/> VND (estimate)</p>
        </div>
    </c:forEach>
</div>

<div class="card">
    <h2>Days</h2>
    <table>
        <thead>
        <tr>
            <th>Day</th>
            <th>Revenue <span class="help" title="Money from goods sold, before any cost.">?</span></th>
            <th>Costs <span class="help" title="Platform fee, shipping, commission, cost of goods and the estimated tax.">?</span></th>
            <th>Profit <span class="help" title="What you keep after fees, shipping, commission, cost and the estimated tax.">?</span></th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="d" items="${days}">
            <tr>
                <td><a href="${base}&amp;day=${d.day}"><c:out value="${d.day}"/></a></td>
                <td><fmt:formatNumber value="${d.revenue}" pattern="#,##0"/></td>
                <td><fmt:formatNumber value="${d.platformFee + d.shippingFee + d.commission + d.cost + d.taxReserve}" pattern="#,##0"/></td>
                <td class="${d.profit < 0 ? 'loss' : ''}"><fmt:formatNumber value="${d.profit}" pattern="#,##0"/></td>
            </tr>
        </c:forEach>
        <c:if test="${empty days}">
            <tr><td colspan="4">No sales in this period.</td></tr>
        </c:if>
        </tbody>
    </table>
</div>

<c:if test="${not empty selected}">
    <div class="card">
        <h2>Day <c:out value="${selected.day}"/></h2>
        <table>
            <thead>
            <tr>
                <th>Revenue</th>
                <th>Platform fee <span class="help" title="The fee the marketplace takes from each sale.">?</span></th>
                <th>Shipping <span class="help" title="Shipping paid by the shop.">?</span></th>
                <th>Commission <span class="help" title="Money paid to affiliates for the sale.">?</span></th>
                <th>Cost <span class="help" title="What the goods cost you to buy or make.">?</span></th>
                <th>Tax reserve <span class="help" title="An estimate of the tax to set aside. Not the amount to pay.">?</span></th>
                <th>Profit</th>
            </tr>
            </thead>
            <tbody>
            <tr>
                <td><fmt:formatNumber value="${selected.revenue}" pattern="#,##0"/></td>
                <td><fmt:formatNumber value="${selected.platformFee}" pattern="#,##0"/></td>
                <td><fmt:formatNumber value="${selected.shippingFee}" pattern="#,##0"/></td>
                <td><fmt:formatNumber value="${selected.commission}" pattern="#,##0"/></td>
                <td><fmt:formatNumber value="${selected.cost}" pattern="#,##0"/></td>
                <td><fmt:formatNumber value="${selected.taxReserve}" pattern="#,##0"/></td>
                <td class="${selected.profit < 0 ? 'loss' : ''}"><fmt:formatNumber value="${selected.profit}" pattern="#,##0"/></td>
            </tr>
            </tbody>
        </table>
        <c:choose>
            <c:when test="${channel == 'STORE'}">
                <p class="hint">Store sales are in the Transactions page.</p>
            </c:when>
            <c:otherwise>
                <h2>Online sales of the day</h2>
                <table>
                    <thead>
                    <tr>
                        <th>Channel</th>
                        <th>Order ref</th>
                        <th>Status</th>
                        <th>Goods</th>
                        <th>Fees</th>
                        <th>Affiliate</th>
                        <th>Commission</th>
                        <th>Cost</th>
                        <th>Action</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="s" items="${sales.items}">
                        <tr>
                            <td><c:out value="${channelNames[s.channel]}"/></td>
                            <td><c:out value="${s.externalRef}"/></td>
                            <td><span class="pill ${s.status == 'COMPLETED' ? 'COMPLETED' : 'CANCELLED'}"><c:out value="${s.status}"/></span></td>
                            <td><fmt:formatNumber value="${s.goodsAmount}" pattern="#,##0"/></td>
                            <td><fmt:formatNumber value="${s.platformFee + s.shippingFee}" pattern="#,##0"/></td>
                            <td><c:out value="${empty s.affiliateName ? '-' : s.affiliateName}"/></td>
                            <td><fmt:formatNumber value="${s.affiliateCommission}" pattern="#,##0"/></td>
                            <td><fmt:formatNumber value="${s.costAmount}" pattern="#,##0"/></td>
                            <td>
                                <c:if test="${s.status == 'COMPLETED'}">
                                    <a href="${ctx}/invoices/new?source=CHANNEL_SALE&amp;id=${s.saleId}">Create invoice</a>
                                    <c:forEach var="st" items="${['CANCELLED', 'RETURNED']}">
                                        <form method="post" action="${ctx}/sales" class="inline"
                                              data-name="<c:out value='${s.externalRef}'/>"
                                              onsubmit="return confirm('Mark ' + this.dataset.name + ' as ${st}?');">
                                            <input type="hidden" name="action" value="status">
                                            <input type="hidden" name="id" value="${s.saleId}">
                                            <input type="hidden" name="status" value="${st}">
                                            <input type="hidden" name="from" value="<c:out value='${from}'/>">
                                            <input type="hidden" name="to" value="<c:out value='${to}'/>">
                                            <input type="hidden" name="channel" value="<c:out value='${channel}'/>">
                                            <input type="hidden" name="day" value="<c:out value='${selected.day}'/>">
                                            <button type="submit">${st == 'CANCELLED' ? 'Cancel' : 'Return'}</button>
                                        </form>
                                    </c:forEach>
                                </c:if>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty sales.items}">
                        <tr><td colspan="9">No online sales on this day.</td></tr>
                    </c:if>
                    </tbody>
                </table>
                <p>${sales.total} sale(s), page ${sales.page} of ${sales.totalPages}</p>
                <div class="pages">
                    <c:if test="${sales.page > 1}">
                        <a href="${base}&amp;day=${selected.day}&amp;page=${sales.page - 1}" title="Previous page">&laquo;</a>
                    </c:if>
                    <c:forEach begin="1" end="${sales.totalPages}" var="pg">
                        <c:choose>
                            <c:when test="${pg == sales.page}"><span>${pg}</span></c:when>
                            <c:otherwise><a href="${base}&amp;day=${selected.day}&amp;page=${pg}">${pg}</a></c:otherwise>
                        </c:choose>
                    </c:forEach>
                    <c:if test="${sales.page < sales.totalPages}">
                        <a href="${base}&amp;day=${selected.day}&amp;page=${sales.page + 1}" title="Next page">&raquo;</a>
                    </c:if>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</c:if>

<div class="card narrow">
    <h2>Add an online sale</h2>
    <p><a href="${ctx}/sales/import">Import from file</a></p>
    <p class="hint">Amounts are whole VND, from 0 to 50,000,000. Leave a cost or fee empty for 0.</p>
    <form method="post" action="${ctx}/sales">
        <input type="hidden" name="action" value="add">
        <label>Channel
            <select name="channel" required>
                <option value="SHOPEE" ${f.channel == 'SHOPEE' ? 'selected' : ''}>Shopee</option>
                <option value="TIKTOK" ${f.channel == 'TIKTOK' ? 'selected' : ''}>TikTok Shop</option>
            </select>
            <c:if test="${not empty errors.channel}"><div class="field-error"><c:out value="${errors.channel}"/></div></c:if>
        </label>
        <label>Order reference
            <input type="text" name="ref" required maxlength="60" value="<c:out value='${f.ref}'/>">
            <c:if test="${not empty errors.ref}"><div class="field-error"><c:out value="${errors.ref}"/></div></c:if>
        </label>
        <label>Sold date
            <input type="date" name="soldOn" required max="<c:out value='${today}'/>" value="<c:out value='${empty f.soldOn ? today : f.soldOn}'/>">
            <c:if test="${not empty errors.soldOn}"><div class="field-error"><c:out value="${errors.soldOn}"/></div></c:if>
        </label>
        <label>Status
            <select name="status" required>
                <option value="COMPLETED" ${empty f.status || f.status == 'COMPLETED' ? 'selected' : ''}>Completed</option>
                <option value="CANCELLED" ${f.status == 'CANCELLED' ? 'selected' : ''}>Cancelled</option>
                <option value="RETURNED" ${f.status == 'RETURNED' ? 'selected' : ''}>Returned</option>
            </select>
            <c:if test="${not empty errors.status}"><div class="field-error"><c:out value="${errors.status}"/></div></c:if>
        </label>
        <label>Goods amount (VND)
            <input type="text" name="goods" required inputmode="numeric" maxlength="11" value="<c:out value='${f.goods}'/>">
            <c:if test="${not empty errors.goods}"><div class="field-error"><c:out value="${errors.goods}"/></div></c:if>
        </label>
        <label>Platform fee (VND)
            <input type="text" name="platformFee" inputmode="numeric" maxlength="11" value="<c:out value='${f.platformFee}'/>">
            <c:if test="${not empty errors.platformFee}"><div class="field-error"><c:out value="${errors.platformFee}"/></div></c:if>
        </label>
        <label>Shipping fee (VND)
            <input type="text" name="shippingFee" inputmode="numeric" maxlength="11" value="<c:out value='${f.shippingFee}'/>">
            <c:if test="${not empty errors.shippingFee}"><div class="field-error"><c:out value="${errors.shippingFee}"/></div></c:if>
        </label>
        <label>Affiliate
            <select name="affiliate">
                <option value="">None</option>
                <c:forEach var="a" items="${affiliates}">
                    <option value="${a.affiliateId}" ${selectedAffiliate == a.affiliateId ? 'selected' : ''}><c:out value="${a.name}"/> (<fmt:formatNumber value="${a.rate}" pattern="0.00"/>%)</option>
                </c:forEach>
            </select>
            <c:if test="${not empty errors.affiliate}"><div class="field-error"><c:out value="${errors.affiliate}"/></div></c:if>
        </label>
        <label>Commission (VND, empty = affiliate rate)
            <input type="text" name="commission" inputmode="numeric" maxlength="11" value="<c:out value='${f.commission}'/>">
            <c:if test="${not empty errors.commission}"><div class="field-error"><c:out value="${errors.commission}"/></div></c:if>
        </label>
        <label>Cost of goods (VND)
            <input type="text" name="cost" inputmode="numeric" maxlength="11" value="<c:out value='${f.cost}'/>">
            <c:if test="${not empty errors.cost}"><div class="field-error"><c:out value="${errors.cost}"/></div></c:if>
        </label>
        <label>Note
            <input type="text" name="note" maxlength="200" value="<c:out value='${f.note}'/>">
            <c:if test="${not empty errors.note}"><div class="field-error"><c:out value="${errors.note}"/></div></c:if>
        </label>
        <button type="submit" class="primary">Add sale</button>
    </form>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
