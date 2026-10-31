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

                    <div style="margin: 10px 0 6px 0;">
                        <div style="display:flex; justify-content:space-between; font-size:11px; margin-bottom:4px;">
                            <span class="hint">Profit Margin:</span>
                            <span class="margin-badge ${m.profitMarginPct >= 25 ? 'margin-high' : (m.profitMarginPct >= 10 ? 'margin-mid' : 'margin-low')}">${m.profitMarginPct}%</span>
                        </div>
                        <div class="waterfall-bar-track" style="height:6px;">
                            <div class="waterfall-fill" style="width: ${m.profitMarginPct < 0 ? 0 : (m.profitMarginPct > 100 ? 100 : m.profitMarginPct)}%; background: ${m.profitMarginPct >= 25 ? '#10b981' : (m.profitMarginPct >= 10 ? '#f59e0b' : '#ef4444')};"></div>
                        </div>
                    </div>

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
            <div class="chart-header">
                <h2>Daily Revenue & Profit Trend</h2>
                <div class="chart-legend">
                    <span class="legend-item"><span class="legend-dot" style="background:#ea580c;"></span> Shopee</span>
                    <span class="legend-item"><span class="legend-dot" style="background:#0f172a;"></span> TikTok Shop</span>
                    <span class="legend-item"><span class="legend-dot" style="background:#10b981;"></span> Net Profit</span>
                </div>
            </div>

            <c:set var="maxTrendRev" value="1"/>
            <c:forEach var="t" items="${bundle.dailyTrends}">
                <c:set var="dayTotal" value="${t.shopeeRevenue + t.tiktokRevenue}"/>
                <c:if test="${dayTotal > maxTrendRev}">
                    <c:set var="maxTrendRev" value="${dayTotal}"/>
                </c:if>
            </c:forEach>

            <c:if test="${not empty bundle.dailyTrends}">
                <div class="daily-trend-chart" style="margin-bottom: 20px;">
                    <c:forEach var="t" items="${bundle.dailyTrends}">
                        <c:set var="hShopee" value="${t.shopeeRevenue * 100 / maxTrendRev}"/>
                        <c:set var="hTiktok" value="${t.tiktokRevenue * 100 / maxTrendRev}"/>
                        <c:set var="hProfit" value="${t.totalProfit > 0 ? (t.totalProfit * 100 / maxTrendRev) : 0}"/>
                        <div class="daily-col-item" style="max-width: 54px;"
                             title="${t.date} | Shopee: <fmt:formatNumber value='${t.shopeeRevenue}' pattern='#,##0'/> VND | TikTok: <fmt:formatNumber value='${t.tiktokRevenue}' pattern='#,##0'/> VND | Profit: <fmt:formatNumber value='${t.totalProfit}' pattern='#,##0'/> VND">
                            <div class="daily-bars-group">
                                <div style="width: 10px; background: #ea580c; border-radius: 3px 3px 0 0; height: ${hShopee < 3 ? 3 : hShopee}%;"></div>
                                <div style="width: 10px; background: #0f172a; border-radius: 3px 3px 0 0; height: ${hTiktok < 3 ? 3 : hTiktok}%;"></div>
                                <div style="width: 8px; background: ${t.totalProfit >= 0 ? '#10b981' : '#ef4444'}; border-radius: 3px 3px 0 0; height: ${hProfit < 2 ? 2 : hProfit}%;"></div>
                            </div>
                            <div class="daily-col-label">${t.date.length() > 5 ? t.date.substring(5) : t.date}</div>
                        </div>
                    </c:forEach>
                </div>
            </c:if>

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
        <div class="alert-banner" style="margin-bottom: 16px; background: #eff6ff; border-color: #3b82f6; color: #1e40af;">
            <strong>Tax Accounting Principle:</strong> 
            <strong>Doanh thu chịu thuế (Total Revenue)</strong> = <em>Subtotal after seller discounts</em> (Tiền hàng sau khi trừ voucher shop). 
            Các khoản chi phí sàn (Phí giao dịch, Phí hoa hồng sàn, Phí vận chuyển, Phí tiếp thị) được khấu trừ vào tiền quyết toán về ví (<strong>Total settlement amount</strong>), nhưng theo quy định thuế <u>không được giảm trừ</u> khỏi doanh thu tính thuế GTGT/TNCN.
        </div>
        <div class="kpi-grid" style="grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));">
            <!-- Shopee Waterfall -->
            <div class="card">
                <div class="chart-header">
                    <h2>Shopee Financial Waterfall</h2>
                    <span class="tpill warn">Shopee Mall &amp; Standard</span>
                </div>
                <c:set var="spGross" value="${bundle.shopeeWaterfall.grossRevenue > 0 ? bundle.shopeeWaterfall.grossRevenue : 1}"/>
                <div class="waterfall-container">
                    <div class="waterfall-row">
                        <div class="waterfall-meta">
                            <strong>1. Total Revenue (Doanh thu chịu thuế)</strong>
                            <strong><fmt:formatNumber value="${bundle.shopeeWaterfall.grossRevenue}" pattern="#,##0"/> VND</strong>
                        </div>
                        <div class="waterfall-bar-track">
                            <div class="waterfall-fill" style="width: 100%; background: linear-gradient(90deg, #ea580c, #f97316);"></div>
                        </div>
                    </div>

                    <div class="waterfall-row" style="padding-left: 12px; border-left: 2px dashed #fca5a5;">
                        <div class="waterfall-meta" style="color: #dc2626;">
                            <span>- Fixed Platform Commission</span>
                            <span>-<fmt:formatNumber value="${bundle.shopeeWaterfall.fixedFee}" pattern="#,##0"/> VND (<fmt:formatNumber value="${bundle.shopeeWaterfall.fixedFee * 100 / spGross}" pattern="0.0"/>%)</span>
                        </div>
                        <div class="waterfall-bar-track" style="height: 6px;">
                            <div class="waterfall-fill" style="width: ${bundle.shopeeWaterfall.fixedFee * 100 / spGross}%; background: #dc2626;"></div>
                        </div>
                    </div>

                    <div class="waterfall-row" style="padding-left: 12px; border-left: 2px dashed #fca5a5;">
                        <div class="waterfall-meta" style="color: #dc2626;">
                            <span>- Service Package Fee (Xtra)</span>
                            <span>-<fmt:formatNumber value="${bundle.shopeeWaterfall.serviceFee}" pattern="#,##0"/> VND (<fmt:formatNumber value="${bundle.shopeeWaterfall.serviceFee * 100 / spGross}" pattern="0.0"/>%)</span>
                        </div>
                        <div class="waterfall-bar-track" style="height: 6px;">
                            <div class="waterfall-fill" style="width: ${bundle.shopeeWaterfall.serviceFee * 100 / spGross}%; background: #dc2626;"></div>
                        </div>
                    </div>

                    <div class="waterfall-row" style="padding-left: 12px; border-left: 2px dashed #fca5a5;">
                        <div class="waterfall-meta" style="color: #dc2626;">
                            <span>- Payment Processing Fee</span>
                            <span>-<fmt:formatNumber value="${bundle.shopeeWaterfall.transactionFee}" pattern="#,##0"/> VND (<fmt:formatNumber value="${bundle.shopeeWaterfall.transactionFee * 100 / spGross}" pattern="0.0"/>%)</span>
                        </div>
                        <div class="waterfall-bar-track" style="height: 6px;">
                            <div class="waterfall-fill" style="width: ${bundle.shopeeWaterfall.transactionFee * 100 / spGross}%; background: #dc2626;"></div>
                        </div>
                    </div>

                    <div class="waterfall-row" style="padding-left: 12px; border-left: 2px dashed #fca5a5;">
                        <div class="waterfall-meta" style="color: #dc2626;">
                            <span>- Seller Shipping Subsidy</span>
                            <span>-<fmt:formatNumber value="${bundle.shopeeWaterfall.shippingFee}" pattern="#,##0"/> VND (<fmt:formatNumber value="${bundle.shopeeWaterfall.shippingFee * 100 / spGross}" pattern="0.0"/>%)</span>
                        </div>
                        <div class="waterfall-bar-track" style="height: 6px;">
                            <div class="waterfall-fill" style="width: ${bundle.shopeeWaterfall.shippingFee * 100 / spGross}%; background: #dc2626;"></div>
                        </div>
                    </div>

                    <div class="waterfall-row" style="padding-left: 12px; border-left: 2px dashed #fca5a5;">
                        <div class="waterfall-meta" style="color: #dc2626;">
                            <span>- Affiliate / AMS Fee</span>
                            <span>-<fmt:formatNumber value="${bundle.shopeeWaterfall.affiliateFee}" pattern="#,##0"/> VND (<fmt:formatNumber value="${bundle.shopeeWaterfall.affiliateFee * 100 / spGross}" pattern="0.0"/>%)</span>
                        </div>
                        <div class="waterfall-bar-track" style="height: 6px;">
                            <div class="waterfall-fill" style="width: ${bundle.shopeeWaterfall.affiliateFee * 100 / spGross}%; background: #dc2626;"></div>
                        </div>
                    </div>

                    <div class="waterfall-row">
                        <div class="waterfall-meta">
                            <strong>2. Total Settlement (Tiền quyết toán thực nhận)</strong>
                            <strong><fmt:formatNumber value="${bundle.shopeeWaterfall.netRevenue}" pattern="#,##0"/> VND (<fmt:formatNumber value="${bundle.shopeeWaterfall.netRevenue * 100 / spGross}" pattern="0.0"/>%)</strong>
                        </div>
                        <div class="waterfall-bar-track">
                            <div class="waterfall-fill" style="width: ${bundle.shopeeWaterfall.netRevenue * 100 / spGross}%; background: #2563eb;"></div>
                        </div>
                    </div>

                    <div class="waterfall-row" style="padding-left: 12px; border-left: 2px dashed #cbd5e1;">
                        <div class="waterfall-meta">
                            <span>- Cost of Goods Sold (COGS)</span>
                            <span>-<fmt:formatNumber value="${bundle.shopeeWaterfall.cogs}" pattern="#,##0"/> VND (<fmt:formatNumber value="${bundle.shopeeWaterfall.cogs * 100 / spGross}" pattern="0.0"/>%)</span>
                        </div>
                        <div class="waterfall-bar-track" style="height: 6px;">
                            <div class="waterfall-fill" style="width: ${bundle.shopeeWaterfall.cogs * 100 / spGross}%; background: #64748b;"></div>
                        </div>
                    </div>

                    <div class="waterfall-row">
                        <div class="waterfall-meta">
                            <strong>3. Net Profit</strong>
                            <strong class="accent"><fmt:formatNumber value="${bundle.shopeeWaterfall.netProfit}" pattern="#,##0"/> VND</strong>
                        </div>
                        <div class="waterfall-bar-track">
                            <div class="waterfall-fill" style="width: ${bundle.shopeeWaterfall.netProfit > 0 ? (bundle.shopeeWaterfall.netProfit * 100 / spGross) : 0}%; background: #10b981;"></div>
                        </div>
                    </div>
                </div>

                <div class="wallet-meter-box">
                    <c:set var="spTotalWallet" value="${bundle.shopeeWaterfall.settledAmount + bundle.shopeeWaterfall.pendingEscrow}"/>
                    <c:set var="spSettledPct" value="${spTotalWallet > 0 ? (bundle.shopeeWaterfall.settledAmount * 100 / spTotalWallet) : 0}"/>
                    <c:set var="spEscrowPct" value="${spTotalWallet > 0 ? (bundle.shopeeWaterfall.pendingEscrow * 100 / spTotalWallet) : 0}"/>
                    <div class="chart-header" style="margin-bottom: 4px;">
                        <span style="font-weight: 700; font-size: 13px;">Settlement &amp; Escrow Pipeline</span>
                        <span style="font-size: 11px; color: var(--gray);">Pipeline: <strong><fmt:formatNumber value="${spTotalWallet}" pattern="#,##0"/> VND</strong></span>
                    </div>
                    <div class="meter-track">
                        <div class="meter-settled" style="width: ${spSettledPct}%;" title="Settled to Wallet: <fmt:formatNumber value='${bundle.shopeeWaterfall.settledAmount}' pattern='#,##0'/> VND (<fmt:formatNumber value='${spSettledPct}' pattern='0.0'/>%)"></div>
                        <div class="meter-escrow" style="width: ${spEscrowPct}%;" title="Pending in Escrow: <fmt:formatNumber value='${bundle.shopeeWaterfall.pendingEscrow}' pattern='#,##0'/> VND (<fmt:formatNumber value='${spEscrowPct}' pattern='0.0'/>%)"></div>
                    </div>
                    <div style="display: flex; justify-content: space-between; font-size: 11px; margin-top: 4px;">
                        <span><span class="legend-dot" style="background:#10b981;"></span> Settled: <strong><fmt:formatNumber value="${bundle.shopeeWaterfall.settledAmount}" pattern="#,##0"/> VND</strong></span>
                        <span><span class="legend-dot" style="background:#d97706;"></span> Pending Escrow: <strong style="color: #d97706;"><fmt:formatNumber value="${bundle.shopeeWaterfall.pendingEscrow}" pattern="#,##0"/> VND</strong></span>
                    </div>
                </div>
            </div>

            <!-- TikTok Waterfall -->
            <div class="card">
                <div class="chart-header">
                    <h2>TikTok Shop Financial Waterfall</h2>
                    <span class="tpill info">TikTok Shop Global</span>
                </div>
                <c:set var="ttGross" value="${bundle.tiktokWaterfall.grossRevenue > 0 ? bundle.tiktokWaterfall.grossRevenue : 1}"/>
                <div class="waterfall-container">
                    <div class="waterfall-row">
                        <div class="waterfall-meta">
                            <strong>1. Total Revenue (Doanh thu chịu thuế)</strong>
                            <strong><fmt:formatNumber value="${bundle.tiktokWaterfall.grossRevenue}" pattern="#,##0"/> VND</strong>
                        </div>
                        <div class="waterfall-bar-track">
                            <div class="waterfall-fill" style="width: 100%; background: linear-gradient(90deg, #0f172a, #334155);"></div>
                        </div>
                    </div>

                    <div class="waterfall-row" style="padding-left: 12px; border-left: 2px dashed #fca5a5;">
                        <div class="waterfall-meta" style="color: #dc2626;">
                            <span>- TikTok Shop Commission Fee</span>
                            <span>-<fmt:formatNumber value="${bundle.tiktokWaterfall.fixedFee}" pattern="#,##0"/> VND (<fmt:formatNumber value="${bundle.tiktokWaterfall.fixedFee * 100 / ttGross}" pattern="0.0"/>%)</span>
                        </div>
                        <div class="waterfall-bar-track" style="height: 6px;">
                            <div class="waterfall-fill" style="width: ${bundle.tiktokWaterfall.fixedFee * 100 / ttGross}%; background: #dc2626;"></div>
                        </div>
                    </div>

                    <div class="waterfall-row" style="padding-left: 12px; border-left: 2px dashed #fca5a5;">
                        <div class="waterfall-meta" style="color: #dc2626;">
                            <span>- Transaction Fee</span>
                            <span>-<fmt:formatNumber value="${bundle.tiktokWaterfall.transactionFee}" pattern="#,##0"/> VND (<fmt:formatNumber value="${bundle.tiktokWaterfall.transactionFee * 100 / ttGross}" pattern="0.0"/>%)</span>
                        </div>
                        <div class="waterfall-bar-track" style="height: 6px;">
                            <div class="waterfall-fill" style="width: ${bundle.tiktokWaterfall.transactionFee * 100 / ttGross}%; background: #dc2626;"></div>
                        </div>
                    </div>

                    <div class="waterfall-row" style="padding-left: 12px; border-left: 2px dashed #fca5a5;">
                        <div class="waterfall-meta" style="color: #dc2626;">
                            <span>- Seller Shipping Subsidy</span>
                            <span>-<fmt:formatNumber value="${bundle.tiktokWaterfall.shippingFee}" pattern="#,##0"/> VND (<fmt:formatNumber value="${bundle.tiktokWaterfall.shippingFee * 100 / ttGross}" pattern="0.0"/>%)</span>
                        </div>
                        <div class="waterfall-bar-track" style="height: 6px;">
                            <div class="waterfall-fill" style="width: ${bundle.tiktokWaterfall.shippingFee * 100 / ttGross}%; background: #dc2626;"></div>
                        </div>
                    </div>

                    <div class="waterfall-row" style="padding-left: 12px; border-left: 2px dashed #fca5a5;">
                        <div class="waterfall-meta" style="color: #dc2626;">
                            <span>- Creator Commission</span>
                            <span>-<fmt:formatNumber value="${bundle.tiktokWaterfall.affiliateFee}" pattern="#,##0"/> VND (<fmt:formatNumber value="${bundle.tiktokWaterfall.affiliateFee * 100 / ttGross}" pattern="0.0"/>%)</span>
                        </div>
                        <div class="waterfall-bar-track" style="height: 6px;">
                            <div class="waterfall-fill" style="width: ${bundle.tiktokWaterfall.affiliateFee * 100 / ttGross}%; background: #dc2626;"></div>
                        </div>
                    </div>

                    <div class="waterfall-row">
                        <div class="waterfall-meta">
                            <strong>2. Total Settlement (Tiền quyết toán thực nhận)</strong>
                            <strong><fmt:formatNumber value="${bundle.tiktokWaterfall.netRevenue}" pattern="#,##0"/> VND (<fmt:formatNumber value="${bundle.tiktokWaterfall.netRevenue * 100 / ttGross}" pattern="0.0"/>%)</strong>
                        </div>
                        <div class="waterfall-bar-track">
                            <div class="waterfall-fill" style="width: ${bundle.tiktokWaterfall.netRevenue * 100 / ttGross}%; background: #2563eb;"></div>
                        </div>
                    </div>

                    <div class="waterfall-row" style="padding-left: 12px; border-left: 2px dashed #cbd5e1;">
                        <div class="waterfall-meta">
                            <span>- Cost of Goods Sold (COGS)</span>
                            <span>-<fmt:formatNumber value="${bundle.tiktokWaterfall.cogs}" pattern="#,##0"/> VND (<fmt:formatNumber value="${bundle.tiktokWaterfall.cogs * 100 / ttGross}" pattern="0.0"/>%)</span>
                        </div>
                        <div class="waterfall-bar-track" style="height: 6px;">
                            <div class="waterfall-fill" style="width: ${bundle.tiktokWaterfall.cogs * 100 / ttGross}%; background: #64748b;"></div>
                        </div>
                    </div>

                    <div class="waterfall-row">
                        <div class="waterfall-meta">
                            <strong>3. Net Profit</strong>
                            <strong class="accent"><fmt:formatNumber value="${bundle.tiktokWaterfall.netProfit}" pattern="#,##0"/> VND</strong>
                        </div>
                        <div class="waterfall-bar-track">
                            <div class="waterfall-fill" style="width: ${bundle.tiktokWaterfall.netProfit > 0 ? (bundle.tiktokWaterfall.netProfit * 100 / ttGross) : 0}%; background: #10b981;"></div>
                        </div>
                    </div>
                </div>

                <div class="wallet-meter-box">
                    <c:set var="ttTotalWallet" value="${bundle.tiktokWaterfall.settledAmount + bundle.tiktokWaterfall.pendingEscrow}"/>
                    <c:set var="ttSettledPct" value="${ttTotalWallet > 0 ? (bundle.tiktokWaterfall.settledAmount * 100 / ttTotalWallet) : 0}"/>
                    <c:set var="ttEscrowPct" value="${ttTotalWallet > 0 ? (bundle.tiktokWaterfall.pendingEscrow * 100 / ttTotalWallet) : 0}"/>
                    <div class="chart-header" style="margin-bottom: 4px;">
                        <span style="font-weight: 700; font-size: 13px;">Settlement &amp; Escrow Pipeline</span>
                        <span style="font-size: 11px; color: var(--gray);">Pipeline: <strong><fmt:formatNumber value="${ttTotalWallet}" pattern="#,##0"/> VND</strong></span>
                    </div>
                    <div class="meter-track">
                        <div class="meter-settled" style="width: ${ttSettledPct}%;" title="Settled to Wallet: <fmt:formatNumber value='${bundle.tiktokWaterfall.settledAmount}' pattern='#,##0'/> VND (<fmt:formatNumber value='${ttSettledPct}' pattern='0.0'/>%)"></div>
                        <div class="meter-escrow" style="width: ${ttEscrowPct}%;" title="Pending in Escrow: <fmt:formatNumber value='${bundle.tiktokWaterfall.pendingEscrow}' pattern='#,##0'/> VND (<fmt:formatNumber value='${ttEscrowPct}' pattern='0.0'/>%)"></div>
                    </div>
                    <div style="display: flex; justify-content: space-between; font-size: 11px; margin-top: 4px;">
                        <span><span class="legend-dot" style="background:#10b981;"></span> Settled: <strong><fmt:formatNumber value="${bundle.tiktokWaterfall.settledAmount}" pattern="#,##0"/> VND</strong></span>
                        <span><span class="legend-dot" style="background:#d97706;"></span> Pending Escrow: <strong style="color: #d97706;"><fmt:formatNumber value="${bundle.tiktokWaterfall.pendingEscrow}" pattern="#,##0"/> VND</strong></span>
                    </div>
                </div>
            </div>
        </div>
    </c:when>

    <c:when test="${activeTab == 'skus'}">
        <div class="card">
            <div class="chart-header">
                <h2>Product &amp; SKU Performance Matrix</h2>
                <c:if test="${bundle.availability.hasItemData && not empty bundle.topSkus}">
                    <c:set var="maxSkuRev" value="1"/>
                    <c:set var="totalSkuUnits" value="0"/>
                    <c:set var="bestSkuName" value=""/>
                    <c:set var="bestSkuUnits" value="0"/>
                    <c:forEach var="s" items="${bundle.topSkus}">
                        <c:set var="totalSkuUnits" value="${totalSkuUnits + s.unitsSold}"/>
                        <c:if test="${s.revenue > maxSkuRev}"><c:set var="maxSkuRev" value="${s.revenue}"/></c:if>
                        <c:if test="${s.unitsSold > bestSkuUnits}">
                            <c:set var="bestSkuUnits" value="${s.unitsSold}"/>
                            <c:set var="bestSkuName" value="${s.productName}"/>
                        </c:if>
                    </c:forEach>
                    <div class="chart-legend">
                        <span class="legend-item">🏆 Best-seller: <strong><c:out value="${bestSkuName}"/></strong> (${bestSkuUnits} sold)</span>
                        <span class="legend-item">📦 Total Volume: <strong>${totalSkuUnits} units</strong></span>
                    </div>
                </c:if>
            </div>

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
                            <th style="text-align: right; min-width: 140px;">Revenue Share</th>
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
                                <td style="text-align: right;"><strong>${s.unitsSold}</strong></td>
                                <td style="text-align: right;">
                                    <div><fmt:formatNumber value="${s.revenue}" pattern="#,##0"/></div>
                                    <div class="sku-progress-bar" style="margin-top: 3px;" title="Relative volume: <fmt:formatNumber value='${s.revenue * 100 / maxSkuRev}' pattern='0.0'/>%">
                                        <div class="sku-progress-fill" style="width: ${s.revenue * 100 / maxSkuRev}%;"></div>
                                    </div>
                                </td>
                                <td style="text-align: right;"><fmt:formatNumber value="${s.cost}" pattern="#,##0"/></td>
                                <td style="text-align: right;" class="accent"><strong><fmt:formatNumber value="${s.profit}" pattern="#,##0"/></strong></td>
                                <td style="text-align: right;">
                                    <span class="margin-badge ${s.marginPct >= 25 ? 'margin-high' : (s.marginPct >= 10 ? 'margin-mid' : 'margin-low')}">${s.marginPct}%</span>
                                </td>
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
            <h2>Content &amp; Traffic Source Attribution</h2>
            <c:choose>
                <c:when test="${not bundle.availability.hasTrafficData}">
                    <div class="alert-banner" style="margin: 12px 0;">
                        <strong>Dimensional Notice:</strong> Traffic attribution (LIVE, Short Video, Showcase, Creator Affiliate) is not present in the currently imported reports. The analytics engine will populate this view when reports with traffic source columns are uploaded.
                    </div>
                </c:when>
                <c:otherwise>
                    <!-- Stacked Traffic Distribution Bar -->
                    <div style="margin: 14px 0 20px 0; padding: 14px; border-radius: 10px; background: rgba(255, 255, 255, 0.65); border: 1px solid var(--line);">
                        <div class="chart-header">
                            <span style="font-weight: 700; font-size: 13px;">Traffic Revenue Distribution</span>
                            <div class="chart-legend">
                                <span class="legend-item"><span class="legend-dot" style="background:#ef4444;"></span> LIVE</span>
                                <span class="legend-item"><span class="legend-dot" style="background:#a855f7;"></span> Short Video</span>
                                <span class="legend-item"><span class="legend-dot" style="background:#3b82f6;"></span> Showcase</span>
                                <span class="legend-item"><span class="legend-dot" style="background:#10b981;"></span> Creator Affiliate</span>
                            </div>
                        </div>
                        <div class="stacked-dist-bar" style="height: 22px;">
                            <c:forEach var="tr" items="${bundle.trafficMetrics}">
                                <c:if test="${tr.sharePct > 0}">
                                    <c:set var="trColor" value="${tr.source == 'LIVE' ? '#ef4444' : (tr.source == 'SHORT_VIDEO' || tr.source == 'VIDEO' ? '#a855f7' : (tr.source == 'SHOWCASE' ? '#3b82f6' : '#10b981'))}"/>
                                    <div class="dist-seg" style="width: ${tr.sharePct}%; background: ${trColor};"
                                         title="${tr.source}: <fmt:formatNumber value='${tr.revenue}' pattern='#,##0'/> VND (${tr.sharePct}%, ${tr.orderCount} orders)">
                                        <c:if test="${tr.sharePct >= 10}">
                                            ${tr.source} ${tr.sharePct}%
                                        </c:if>
                                    </div>
                                </c:if>
                            </c:forEach>
                        </div>
                    </div>

                    <table>
                        <thead>
                        <tr>
                            <th>Channel Source / Attribution</th>
                            <th style="text-align: right;">Orders Generated</th>
                            <th style="text-align: right;">Revenue</th>
                            <th style="text-align: right; min-width: 140px;">Revenue Share %</th>
                        </tr>
                        </thead>
                        <tbody>
                        <c:forEach var="tr" items="${bundle.trafficMetrics}">
                            <c:set var="trColor" value="${tr.source == 'LIVE' ? '#ef4444' : (tr.source == 'SHORT_VIDEO' || tr.source == 'VIDEO' ? '#a855f7' : (tr.source == 'SHOWCASE' ? '#3b82f6' : '#10b981'))}"/>
                            <tr>
                                <td><strong><c:out value="${tr.source}"/></strong></td>
                                <td style="text-align: right;">${tr.orderCount}</td>
                                <td style="text-align: right;"><fmt:formatNumber value="${tr.revenue}" pattern="#,##0"/></td>
                                <td style="text-align: right;">
                                    <div><strong>${tr.sharePct}%</strong></div>
                                    <div class="sku-progress-bar" style="margin-top: 3px;" title="Share: ${tr.sharePct}%">
                                        <div class="sku-progress-fill" style="width: ${tr.sharePct}%; background: ${trColor};"></div>
                                    </div>
                                </td>
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
