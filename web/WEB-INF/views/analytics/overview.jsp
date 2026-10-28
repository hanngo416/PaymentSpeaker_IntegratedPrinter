<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Channel Analytics"/>
<c:set var="nav" value="analytics"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>

<div class="card">
    <div style="display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 12px;">
        <div>
            <h1>Channel Analytics & Business Intelligence</h1>
            <p class="hint">Comprehensive multi-dimensional performance analysis across Shopee, TikTok Shop, and In-Store POS sales.</p>
        </div>
        <div style="display: flex; gap: 6px; flex-wrap: wrap;">
            <a class="btn ${bundle.period == 'today' ? 'primary' : ''}" href="${ctx}/analytics?tab=${activeTab}&period=today&channel=${channel}">Today</a>
            <a class="btn ${bundle.period == '7d' ? 'primary' : ''}" href="${ctx}/analytics?tab=${activeTab}&period=7d&channel=${channel}">Last 7D</a>
            <a class="btn ${bundle.period == '30d' ? 'primary' : ''}" href="${ctx}/analytics?tab=${activeTab}&period=30d&channel=${channel}">Last 30D</a>
            <a class="btn ${bundle.period == 'month' ? 'primary' : ''}" href="${ctx}/analytics?tab=${activeTab}&period=month&channel=${channel}">This Month</a>
            <a class="btn ${bundle.period == 'all' ? 'primary' : ''}" href="${ctx}/analytics?tab=${activeTab}&period=all&channel=${channel}">All Time</a>
        </div>
    </div>
    <div style="margin-top: 14px; display: flex; gap: 8px; border-bottom: 1px solid var(--line); padding-bottom: 10px;">
        <a class="btn ${activeTab == 'overview' ? 'primary' : ''}" href="${ctx}/analytics?tab=overview&period=${period}&channel=${channel}">Overview & Benchmark</a>
        <a class="btn ${activeTab == 'fees' ? 'primary' : ''}" href="${ctx}/analytics?tab=fees&period=${period}&channel=${channel}">Fee Waterfall & Escrow</a>
        <a class="btn ${activeTab == 'skus' ? 'primary' : ''}" href="${ctx}/analytics?tab=skus&period=${period}&channel=${channel}">SKU Performance</a>
        <a class="btn ${activeTab == 'traffic' ? 'primary' : ''}" href="${ctx}/analytics?tab=traffic&period=${period}&channel=${channel}">Traffic Attribution</a>
    </div>
</div>

<c:choose>
    <c:when test="${activeTab == 'overview'}">
        <div class="kpi-grid" style="grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));">
            <c:forEach var="m" items="${bundle.channelMetrics}">
                <div class="card">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                        <span class="pill ${m.channel == 'STORE' ? 'ACTIVE' : (m.channel == 'SHOPEE' ? 'PENDING' : 'OFFLINE')}">${m.channel}</span>
                        <small class="hint">${m.completedOrders} completed / ${m.totalOrders} total</small>
                    </div>
                    <div class="kpi-label">Gross Revenue (Tiền hàng)</div>
                    <div class="kpi-value"><fmt:formatNumber value="${m.grossRevenue}" pattern="#,##0"/> <span class="kpi-unit">VND</span></div>
                    
                    <div style="margin-top: 10px; font-size: var(--fs-sm); display: flex; flex-direction: column; gap: 4px;">
                        <div style="display: flex; justify-content: space-between;">
                            <span class="hint">GMV (Buyer spend):</span>
                            <strong><fmt:formatNumber value="${m.gmv}" pattern="#,##0"/> VND</strong>
                        </div>
                        <div style="display: flex; justify-content: space-between;">
                            <span class="hint">Total Deductions:</span>
                            <span style="color: #dc2626;">-<fmt:formatNumber value="${m.totalDeductions}" pattern="#,##0"/> VND</span>
                        </div>
                        <div style="display: flex; justify-content: space-between;">
                            <span class="hint">Net Revenue (Thực thu):</span>
                            <strong><fmt:formatNumber value="${m.netRevenue}" pattern="#,##0"/> VND</strong>
                        </div>
                        <div style="display: flex; justify-content: space-between;">
                            <span class="hint">Cost of Goods (COGS):</span>
                            <span><fmt:formatNumber value="${m.cogs}" pattern="#,##0"/> VND</span>
                        </div>
                        <div style="display: flex; justify-content: space-between; padding-top: 4px; border-top: 1px dashed var(--line);">
                            <strong>Net Profit:</strong>
                            <strong class="${m.netProfit < 0 ? 'loss' : 'accent'}"><fmt:formatNumber value="${m.netProfit}" pattern="#,##0"/> VND (${m.profitMarginPct}%)</strong>
                        </div>
                        <div style="display: flex; justify-content: space-between; margin-top: 4px;">
                            <span class="hint">AOV: <fmt:formatNumber value="${m.averageOrderValue}" pattern="#,##0"/> VND</span>
                            <span class="hint">Cancel: ${m.cancelRatePct}% | Return: ${m.returnRatePct}%</span>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </div>

        <div class="card">
            <h2>Daily Revenue & Profit Trend</h2>
            <table>
                <thead>
                <tr>
                    <th>Date</th>
                    <th style="text-align: right;">Shopee Revenue</th>
                    <th style="text-align: right;">TikTok Revenue</th>
                    <th style="text-align: right;">Total Net Profit</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="t" items="${bundle.dailyTrends}">
                    <tr>
                        <td><c:out value="${t.date}"/></td>
                        <td style="text-align: right;"><fmt:formatNumber value="${t.shopeeRevenue}" pattern="#,##0"/></td>
                        <td style="text-align: right;"><fmt:formatNumber value="${t.tiktokRevenue}" pattern="#,##0"/></td>
                        <td style="text-align: right;" class="${t.totalProfit < 0 ? 'loss' : 'accent'}">
                            <strong><fmt:formatNumber value="${t.totalProfit}" pattern="#,##0"/></strong>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty bundle.dailyTrends}">
                    <tr><td colspan="4" class="hint">No marketplace data in this period.</td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </c:when>

    <c:when test="${activeTab == 'fees'}">
        <div class="kpi-grid" style="grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));">
            <div class="card">
                <h2>Shopee Financial Waterfall</h2>
                <div style="display: flex; flex-direction: column; gap: 8px; margin-top: 12px;">
                    <div style="display: flex; justify-content: space-between;"><span>1. Gross Revenue (Tiền hàng):</span><strong><fmt:formatNumber value="${bundle.shopeeWaterfall.grossRevenue}" pattern="#,##0"/> VND</strong></div>
                    <div style="display: flex; justify-content: space-between; color: #dc2626;"><span>- Fixed Platform Commission:</span><span>-<fmt:formatNumber value="${bundle.shopeeWaterfall.fixedFee}" pattern="#,##0"/> VND</span></div>
                    <div style="display: flex; justify-content: space-between; color: #dc2626;"><span>- Service Package Fee (Xtra):</span><span>-<fmt:formatNumber value="${bundle.shopeeWaterfall.serviceFee}" pattern="#,##0"/> VND</span></div>
                    <div style="display: flex; justify-content: space-between; color: #dc2626;"><span>- Payment Processing Fee:</span><span>-<fmt:formatNumber value="${bundle.shopeeWaterfall.transactionFee}" pattern="#,##0"/> VND</span></div>
                    <div style="display: flex; justify-content: space-between; color: #dc2626;"><span>- Seller Shipping Subsidy:</span><span>-<fmt:formatNumber value="${bundle.shopeeWaterfall.shippingFee}" pattern="#,##0"/> VND</span></div>
                    <div style="display: flex; justify-content: space-between; color: #dc2626;"><span>- Affiliate / AMS Fee:</span><span>-<fmt:formatNumber value="${bundle.shopeeWaterfall.affiliateFee}" pattern="#,##0"/> VND</span></div>
                    <div style="display: flex; justify-content: space-between; padding-top: 6px; border-top: 1px solid var(--line);"><strong>2. Net Revenue (Thực thu sàn):</strong><strong><fmt:formatNumber value="${bundle.shopeeWaterfall.netRevenue}" pattern="#,##0"/> VND</strong></div>
                    <div style="display: flex; justify-content: space-between;"><span>- Cost of Goods Sold (COGS):</span><span>-<fmt:formatNumber value="${bundle.shopeeWaterfall.cogs}" pattern="#,##0"/> VND</span></div>
                    <div style="display: flex; justify-content: space-between; padding-top: 6px; border-top: 1px solid var(--line);"><strong>3. Net Profit:</strong><strong class="accent"><fmt:formatNumber value="${bundle.shopeeWaterfall.netProfit}" pattern="#,##0"/> VND</strong></div>
                    <div style="margin-top: 10px; padding: 10px; background: #f8fafc; border-radius: 8px;">
                        <div style="display: flex; justify-content: space-between;"><span class="hint">Settled to Wallet:</span><strong><fmt:formatNumber value="${bundle.shopeeWaterfall.settledAmount}" pattern="#,##0"/> VND</strong></div>
                        <div style="display: flex; justify-content: space-between; margin-top: 4px;"><span class="hint">Pending in Escrow:</span><span style="color: #d97706;"><fmt:formatNumber value="${bundle.shopeeWaterfall.pendingEscrow}" pattern="#,##0"/> VND</span></div>
                    </div>
                </div>
            </div>

            <div class="card">
                <h2>TikTok Shop Financial Waterfall</h2>
                <div style="display: flex; flex-direction: column; gap: 8px; margin-top: 12px;">
                    <div style="display: flex; justify-content: space-between;"><span>1. Gross Revenue (Tiền hàng):</span><strong><fmt:formatNumber value="${bundle.tiktokWaterfall.grossRevenue}" pattern="#,##0"/> VND</strong></div>
                    <div style="display: flex; justify-content: space-between; color: #dc2626;"><span>- Platform Commission:</span><span>-<fmt:formatNumber value="${bundle.tiktokWaterfall.fixedFee}" pattern="#,##0"/> VND</span></div>
                    <div style="display: flex; justify-content: space-between; color: #dc2626;"><span>- Payment Platform Fee:</span><span>-<fmt:formatNumber value="${bundle.tiktokWaterfall.transactionFee}" pattern="#,##0"/> VND</span></div>
                    <div style="display: flex; justify-content: space-between; color: #dc2626;"><span>- Seller Shipping Subsidy:</span><span>-<fmt:formatNumber value="${bundle.tiktokWaterfall.shippingFee}" pattern="#,##0"/> VND</span></div>
                    <div style="display: flex; justify-content: space-between; color: #dc2626;"><span>- Creator Commission:</span><span>-<fmt:formatNumber value="${bundle.tiktokWaterfall.affiliateFee}" pattern="#,##0"/> VND</span></div>
                    <div style="display: flex; justify-content: space-between; padding-top: 6px; border-top: 1px solid var(--line);"><strong>2. Net Revenue (Thực thu sàn):</strong><strong><fmt:formatNumber value="${bundle.tiktokWaterfall.netRevenue}" pattern="#,##0"/> VND</strong></div>
                    <div style="display: flex; justify-content: space-between;"><span>- Cost of Goods Sold (COGS):</span><span>-<fmt:formatNumber value="${bundle.tiktokWaterfall.cogs}" pattern="#,##0"/> VND</span></div>
                    <div style="display: flex; justify-content: space-between; padding-top: 6px; border-top: 1px solid var(--line);"><strong>3. Net Profit:</strong><strong class="accent"><fmt:formatNumber value="${bundle.tiktokWaterfall.netProfit}" pattern="#,##0"/> VND</strong></div>
                    <div style="margin-top: 10px; padding: 10px; background: #f8fafc; border-radius: 8px;">
                        <div style="display: flex; justify-content: space-between;"><span class="hint">Settled to Wallet:</span><strong><fmt:formatNumber value="${bundle.tiktokWaterfall.settledAmount}" pattern="#,##0"/> VND</strong></div>
                        <div style="display: flex; justify-content: space-between; margin-top: 4px;"><span class="hint">Pending in Escrow:</span><span style="color: #d97706;"><fmt:formatNumber value="${bundle.tiktokWaterfall.pendingEscrow}" pattern="#,##0"/> VND</span></div>
                    </div>
                </div>
            </div>
        </div>
    </c:when>

    <c:when test="${activeTab == 'skus'}">
        <div class="card">
            <h2>Product & SKU Performance Matrix</h2>
            <c:choose>
                <c:when test="${not bundle.availability.hasItemData}">
                    <div class="alert-banner" style="margin: 12px 0;">
                        <strong>Dimensional Notice:</strong> Item/SKU variation details are not included in the currently imported files. To activate SKU-level profitability and best-seller matrix, upload a Shopee or TikTok Order Report containing product columns (Mã SKU, Tên sản phẩm, Số lượng).
                    </div>
                </c:when>
                <c:otherwise>
                    <table>
                        <thead>
                        <tr>
                            <th>SKU Code</th>
                            <th>Product Name</th>
                            <th>Variation</th>
                            <th style="text-align: right;">Units Sold</th>
                            <th style="text-align: right;">Revenue</th>
                            <th style="text-align: right;">COGS</th>
                            <th style="text-align: right;">Profit</th>
                            <th style="text-align: right;">Margin %</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="s" items="${bundle.topSkus}">
                            <tr>
                                <td><code><c:out value="${s.skuCode}"/></code></td>
                                <td><c:out value="${s.productName}"/></td>
                                <td><c:out value="${s.variationName != null ? s.variationName : '-'}"/></td>
                                <td style="text-align: right;">${s.unitsSold}</td>
                                <td style="text-align: right;"><fmt:formatNumber value="${s.revenue}" pattern="#,##0"/></td>
                                <td style="text-align: right;"><fmt:formatNumber value="${s.cost}" pattern="#,##0"/></td>
                                <td style="text-align: right;" class="accent"><strong><fmt:formatNumber value="${s.profit}" pattern="#,##0"/></strong></td>
                                <td style="text-align: right;">${s.marginPct}%</td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty bundle.topSkus}">
                            <tr><td colspan="8" class="hint">No SKU rows found in the selected date range.</td></tr>
                        </c:if>
                        </tbody>
                    </table>
                </c:otherwise>
            </c:choose>
        </div>
    </c:when>

    <c:when test="${activeTab == 'traffic'}">
        <div class="card">
            <h2>Content & Traffic Source Attribution</h2>
            <c:choose>
                <c:when test="${not bundle.availability.hasTrafficData}">
                    <div class="alert-banner" style="margin: 12px 0;">
                        <strong>Dimensional Notice:</strong> Traffic attribution (LIVE, Short Video, Showcase, Creator Affiliate) is not present in the currently imported reports. The analytics engine will populate this view when reports with traffic source columns are uploaded.
                    </div>
                </c:when>
                <c:otherwise>
                    <table>
                        <thead>
                        <tr>
                            <th>Channel Source / Attribution</th>
                            <th style="text-align: right;">Orders Generated</th>
                            <th style="text-align: right;">Revenue</th>
                            <th style="text-align: right;">Revenue Share %</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="tr" items="${bundle.trafficMetrics}">
                            <tr>
                                <td><strong><c:out value="${tr.source}"/></strong></td>
                                <td style="text-align: right;">${tr.orderCount}</td>
                                <td style="text-align: right;"><fmt:formatNumber value="${tr.revenue}" pattern="#,##0"/></td>
                                <td style="text-align: right;">${tr.sharePct}%</td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty bundle.trafficMetrics}">
                            <tr><td colspan="4" class="hint">No attribution data recorded.</td></tr>
                        </c:if>
                        </tbody>
                    </table>
                </c:otherwise>
            </c:choose>
        </div>
    </c:when>
</c:choose>

<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>

