<%--
    Document   : new
    Created on : Oct 11, 2026
    Author     : PC
--%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Create payment order"/>
<c:set var="nav" value="orders"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>

<c:set var="pendingCount" value="0"/>
<c:forEach var="o" items="${recentOrders}">
    <c:if test="${o.status == 'PENDING'}">
        <c:set var="pendingCount" value="${pendingCount + 1}"/>
    </c:if>
</c:forEach>

<div class="pos-omnichannel-bar">
    <div class="pos-omni-card store">
        <div class="pos-omni-meta">
            <span class="pos-omni-title">POS Store Today</span>
            <span class="pos-omni-val"><fmt:formatNumber value="${storeTodayTotal}" pattern="#,##0"/> ₫</span>
        </div>
        <div class="pos-omni-icon store" title="Store POS Counter">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path><polyline points="9 22 9 12 15 12 15 22"></polyline></svg>
        </div>
    </div>
    <c:forEach var="ch" items="${channelToday}">
        <div class="pos-omni-card ${ch.channel == 'SHOPEE' ? 'shopee' : (ch.channel == 'TIKTOK' ? 'tiktok' : 'other')}">
            <div class="pos-omni-meta">
                <span class="pos-omni-title">
                    ${ch.channel == 'SHOPEE' ? 'Shopee' : (ch.channel == 'TIKTOK' ? 'TikTok Shop' : ch.channel)} Today
                </span>
                <span class="pos-omni-val">
                    <fmt:formatNumber value="${ch.totalRevenue}" pattern="#,##0"/> ₫
                </span>
                <span class="pos-omni-sub">
                    ${ch.orderCount} order(s) &bull; Net: <fmt:formatNumber value="${ch.netRevenue}" pattern="#,##0"/> ₫
                </span>
            </div>
            <div class="pos-omni-icon ${ch.channel == 'SHOPEE' ? 'shopee' : (ch.channel == 'TIKTOK' ? 'tiktok' : 'other')}" title="${ch.channel}">
                <c:choose>
                    <c:when test="${ch.channel == 'SHOPEE'}">
                        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="21" r="1"></circle><circle cx="20" cy="21" r="1"></circle><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path></svg>
                    </c:when>
                    <c:when test="${ch.channel == 'TIKTOK'}">
                        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18V5l12-2v13"></path><circle cx="6" cy="18" r="3"></circle><circle cx="18" cy="16" r="3"></circle></svg>
                    </c:when>
                    <c:otherwise>
                        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path></svg>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </c:forEach>
</div>

<div class="pos-dual-layout">
    <div class="card pos-catalog-panel">
        <div class="pos-catalog-header">
            <div class="pos-search-wrap">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="11" cy="11" r="8"></circle>
                    <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                </svg>
                <input type="text" id="posSearchInput" placeholder="Search by name, brand, or code (e.g. Lay's, Coca, DRK01)..." autocomplete="off">
                <button type="button" id="posSearchClear" class="pos-search-clear" style="display: none;">&times;</button>
            </div>
            <button type="button" class="btn-custom-sale" id="btnCustomSale" title="Switch to custom amount sale">
                <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon>
                </svg>
                <span>Quick Custom Sale</span>
            </button>
        </div>

        <div class="pos-cat-tabs" id="posCatTabs">
            <button type="button" class="pos-cat-tab active" data-cat="all">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect>
                    <rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect>
                </svg>
                <span>All Items</span>
            </button>
            <c:forEach var="c" items="${categories}">
                <button type="button" class="pos-cat-tab" data-cat="${c.categoryId}">
                    <c:choose>
                        <c:when test="${c.categoryName == 'Beverages'}">
                            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8h1a4 4 0 0 1 0 8h-1"></path><path d="M2 8h16v9a4 4 0 0 1-4 4H6a4 4 0 0 1-4-4V8z"></path><line x1="6" y1="1" x2="6" y2="4"></line><line x1="10" y1="1" x2="10" y2="4"></line><line x1="14" y1="1" x2="14" y2="4"></line></svg>
                        </c:when>
                        <c:when test="${c.categoryName == 'Snacks'}">
                            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"></path><line x1="3" y1="6" x2="21" y2="6"></line><path d="M16 10a4 4 0 0 1-8 0"></path></svg>
                        </c:when>
                        <c:when test="${c.categoryName == 'Bakery'}">
                            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"></circle><path d="M14.8 9A2 2 0 0 0 13 8h-2a2 2 0 0 0-1.8 1"></path><path d="M8.5 13a4 4 0 0 0 7 0"></path></svg>
                        </c:when>
                        <c:when test="${c.categoryName == 'Ready to Eat'}">
                            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 10h-1.26A8 8 0 1 0 9 20h9a3 3 0 0 0 3-3v-4a3 3 0 0 0-3-3z"></path></svg>
                        </c:when>
                        <c:otherwise>
                            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path><polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline><line x1="12" y1="22.08" x2="12" y2="12"></line></svg>
                        </c:otherwise>
                    </c:choose>
                    <span><c:out value="${c.categoryName}"/></span>
                </button>
            </c:forEach>
        </div>

        <div class="pos-grp-bar" id="posGrpBar">
            <button type="button" class="pos-grp-pill active" data-grp="all" data-cat="all">All Groups</button>
            <c:forEach var="g" items="${groups}">
                <button type="button" class="pos-grp-pill" data-grp="${g.groupId}" data-cat="${g.categoryId}">
                    <c:out value="${g.groupName}"/>
                </button>
            </c:forEach>
        </div>

        <div class="pos-product-grid" id="posProductGrid">
            <c:forEach var="p" items="${products}">
                <div class="pos-product-card"
                     data-id="${p.productId}"
                     data-code="<c:out value='${p.productCode}'/>"
                     data-name="<c:out value='${p.productName}'/>"
                     data-price="${p.price}"
                     data-cat="${p.categoryId}"
                     data-grp="${p.groupId}"
                     tabindex="0"
                     role="button"
                     title="Click to add <c:out value='${p.productName}'/> to cart">
                    <div class="pos-prod-top">
                        <span class="pos-prod-code"><c:out value="${p.productCode}"/></span>
                        <span class="pos-prod-grp"><c:out value="${p.groupName}"/></span>
                    </div>
                    <div class="pos-prod-name"><c:out value="${p.productName}"/></div>
                    <c:choose>
                        <c:when test="${p.stockQuantity > 0}">
                            <div class="pos-prod-stock in-stock">&bull; Stock: <c:out value="${p.stockQuantity}"/></div>
                        </c:when>
                        <c:otherwise>
                            <div class="pos-prod-stock out-of-stock">&bull; Out of stock</div>
                        </c:otherwise>
                    </c:choose>
                    <div class="pos-prod-bottom">
                        <span class="pos-prod-price"><fmt:formatNumber value="${p.price}" pattern="#,##0"/> ₫</span>
                        <span class="pos-prod-add-btn" aria-hidden="true">
                            <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                                <line x1="12" y1="5" x2="12" y2="19"></line>
                                <line x1="5" y1="12" x2="19" y2="12"></line>
                            </svg>
                        </span>
                    </div>
                </div>
            </c:forEach>
        </div>
        <div id="posNoProducts" class="pos-no-products" style="display: none;">
            No products match your search or filter.
        </div>
    </div>

    <div class="card pos-create-panel">
        <div class="pos-panel-tabs">
            <button type="button" class="pos-panel-tab-btn active" id="tabBtnCart">
                <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="9" cy="21" r="1"></circle><circle cx="20" cy="21" r="1"></circle>
                    <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>
                </svg>
                <span>Current Cart</span>
                <span class="pos-cart-badge" id="cartCountBadge" style="display: none;">0</span>
            </button>
            <button type="button" class="pos-panel-tab-btn" id="tabBtnPending">
                <span class="pos-tab-dot ${pendingCount > 0 ? 'pulse' : ''}"></span>
                <span>Pending Orders</span>
                <span class="pos-mini-queue-badge" id="queueBadge" style="${pendingCount > 0 ? '' : 'display:none;'}">
                    <span id="queueCount">${pendingCount}</span>
                </span>
            </button>
        </div>

        <div id="panelViewCart" class="pos-panel-view">
            <c:if test="${not empty error}">
                <div class="alert-banner" style="margin-bottom: 12px; padding: 8px 12px; font-size: 13px;"><c:out value="${error}"/></div>
            </c:if>

            <form method="post" action="${ctx}/orders/new" id="orderForm">
                <input type="hidden" name="cartItems" id="cartItems" value="">

                <div class="pos-cart-section">
                    <div class="pos-cart-header">
                        <span class="pos-cart-title">Order Items</span>
                        <button type="button" class="pos-cart-clear-btn" id="btnClearCart" style="display: none;">Clear all</button>
                    </div>
                    <div class="pos-cart-list" id="posCartList"></div>
                    <div class="pos-cart-empty-state" id="posCartEmpty">
                        <svg viewBox="0 0 24 24" width="30" height="30" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" class="pos-cart-empty-icon">
                            <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"></path>
                            <line x1="3" y1="6" x2="21" y2="6"></line>
                            <path d="M16 10a4 4 0 0 1-8 0"></path>
                        </svg>
                        <p class="pos-cart-empty-title">Cart is empty</p>
                        <p class="pos-cart-empty-desc">Click items from catalog on the left to add, or enter a custom amount.</p>
                    </div>
                </div>

                <div class="pos-compact-label-row">
                    <label for="amount" class="pos-compact-label" id="amountLabel">Total Order Amount</label>
                    <span class="pos-currency-pill">VND</span>
                </div>
                <div class="pos-compact-input-wrap">
                    <input id="amount" class="pos-compact-input" type="text" name="amount" inputmode="numeric" pattern="[0-9,]*" maxlength="11" autocomplete="off"
                           value="<c:out value='${amount}'/>" placeholder="0" required autofocus>
                    <span class="pos-currency-symbol">₫</span>
                </div>

                <div class="pos-compact-presets" id="presetChipsWrap">
                    <button type="button" class="preset-chip" data-amount="20000">20k</button>
                    <button type="button" class="preset-chip" data-amount="50000">50k</button>
                    <button type="button" class="preset-chip" data-amount="100000">100k</button>
                    <button type="button" class="preset-chip" data-amount="200000">200k</button>
                    <button type="button" class="preset-chip" data-amount="500000">500k</button>
                </div>

                <details class="pos-promo-accordion" id="promoAccordion" ${not empty pickedCode || not empty discountAmount || not empty affiliateCode ? 'open' : ''}>
                    <summary class="pos-promo-summary">
                        <span class="pos-promo-summary-title">
                            <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"></path>
                                <line x1="7" y1="7" x2="7.01" y2="7"></line>
                            </svg>
                            <span>Discount & Referral code</span>
                        </span>
                        <span class="pos-promo-arrow">&#9660;</span>
                    </summary>
                    <div class="pos-promo-content">
                        <div class="pos-compact-label-row">
                            <label for="pickedCode" class="pos-compact-label">Discount Code / Amount</label>
                        </div>
                        <div class="pos-discount-row">
                            <select id="pickedCode" name="pickedCode">
                                <option value="">No discount</option>
                                <c:forEach var="d" items="${sharedCodes}">
                                    <option value="<c:out value='${d.code}'/>" ${pickedCode == d.code ? 'selected' : ''}><c:out value="${d.code}"/> (<c:choose><c:when test="${d.kind == 'PERCENT'}">-<fmt:formatNumber value="${d.value}" pattern="#,##0"/>%</c:when><c:otherwise>-<fmt:formatNumber value="${d.value}" pattern="#,##0"/> VND</c:otherwise></c:choose>)</option>
                                </c:forEach>
                            </select>
                            <input type="text" id="discountAmount" name="discountAmount" inputmode="numeric" maxlength="11" autocomplete="off" placeholder="Or enter VND"
                                   value="<c:out value='${discountAmount}'/>">
                        </div>

                        <div class="pos-compact-label-row">
                            <label for="affiliateCode" class="pos-compact-label">Affiliate referral code</label>
                        </div>
                        <div class="pos-discount-row">
                            <input type="text" id="affiliateCode" name="affiliateCode" maxlength="30" autocomplete="off" placeholder="e.g. LINH08"
                                   value="<c:out value='${affiliateCode}'/>">
                            <button type="button" id="affiliateCheck" class="pos-btn-affiliate-check">Check</button>
                        </div>
                        <div id="affiliateResult" class="hint"></div>
                    </div>
                </details>

                <div class="pos-compact-methods">
                    <label class="pos-seg-label">
                        <input type="checkbox" id="methodCash" name="methodCash" value="1" ${not empty cashAmount && cashAmount != '0' ? 'checked' : ''}>
                        <span class="pos-seg-btn">
                            <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <rect x="2" y="6" width="20" height="12" rx="2"></rect><circle cx="12" cy="12" r="2"></circle>
                            </svg>
                            <span>Cash</span>
                            <svg class="pos-seg-check" viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                                <polyline points="20 6 9 17 4 12"></polyline>
                            </svg>
                        </span>
                    </label>
                    <label class="pos-seg-label">
                        <input type="checkbox" id="methodQr" name="methodQr" value="1" ${(empty cashAmount || cashAmount == '0' || empty amount || cashAmount != amount) ? 'checked' : ''}>
                        <span class="pos-seg-btn">
                            <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                <rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect>
                                <rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect>
                            </svg>
                            <span>VietQR</span>
                            <svg class="pos-seg-check" viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                                <polyline points="20 6 9 17 4 12"></polyline>
                            </svg>
                        </span>
                    </label>
                </div>

                <div id="splitSection" class="pos-compact-split" style="${not empty cashAmount && cashAmount != '0' && cashAmount != amount ? '' : 'display: none;'}">
                    <div class="pos-compact-label-row">
                        <label for="cashAmount" class="pos-compact-label">Cash at counter</label>
                        <span class="pos-currency-pill">VND</span>
                    </div>
                    <div class="pos-compact-split-input-wrap">
                        <input id="cashAmount" class="pos-compact-split-input" type="text" name="cashAmount" inputmode="numeric" pattern="[0-9,]*" maxlength="11" autocomplete="off"
                               value="<c:out value='${cashAmount}'/>" placeholder="0">
                        <span class="pos-currency-symbol">₫</span>
                    </div>
                    <div class="pos-split-summary-inline">
                        <span>Cash: <span id="previewCash">0 ₫</span></span>
                        <span>QR to pay: <strong id="previewTransfer">0 ₫</strong></span>
                    </div>
                </div>

                <button class="pos-compact-submit" type="submit" id="submitBtn">
                    <svg id="submitBtnIcon" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect>
                        <rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect>
                    </svg>
                    <span id="submitBtnText">Generate QR code</span>
                </button>
            </form>
        </div>

        <div id="panelViewPending" class="pos-panel-view" style="display: none;">
            <div class="pos-pending-panel">
                <c:choose>
                    <c:when test="${pendingCount == 0}">
                        <p class="pos-mini-empty" id="emptyQueueMsg">No pending orders waiting for payment.</p>
                        <div class="pos-queue-list" id="posQueueList" style="display: none;"></div>
                    </c:when>
                    <c:otherwise>
                        <p class="pos-mini-empty" id="emptyQueueMsg" style="display: none;">No pending orders waiting for payment.</p>
                        <div class="pos-queue-list" id="posQueueList">
                            <c:forEach var="o" items="${recentOrders}">
                                <c:if test="${o.status == 'PENDING'}">
                                    <div class="pos-mini-card" id="card-${o.orderCode}" data-code="${o.orderCode}" data-status="PENDING">
                                        <div class="pos-mini-main" onclick="window.location.href='${ctx}/orders/view?code=<c:out value='${o.orderCode}'/>'" title="Click to view order details">
                                            <div class="pos-mini-top">
                                                <span class="pos-mini-code"><c:out value="${o.orderCode}"/></span>
                                                <span class="pulse-dot"></span>
                                                <span class="pos-mini-amt"><fmt:formatNumber value="${o.amount}" pattern="#,##0"/> ₫</span>
                                            </div>
                                            <div class="pos-mini-sub">
                                                <span><fmt:formatDate value="${o.createdAt}" pattern="HH:mm"/></span>
                                                <c:choose>
                                                    <c:when test="${o.cashAmount > 0}">
                                                        <span>&bull; Cash: <fmt:formatNumber value="${o.cashAmount}" pattern="#,##0"/> ₫ + QR</span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span>&bull; 100% VietQR</span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </div>
                                        </div>
                                        <div class="pos-mini-actions">
                                            <button type="button" class="btn-mini-qr"
                                                    data-code="<c:out value='${o.orderCode}'/>"
                                                    data-amount="<fmt:formatNumber value='${o.amount}' pattern='#,##0'/> ₫"
                                                    data-transfer="<fmt:formatNumber value='${o.transferAmount}' pattern='#,##0'/> ₫"
                                                    data-cash="<fmt:formatNumber value='${o.cashAmount}' pattern='#,##0'/> ₫"
                                                    data-qr="<c:out value='${o.qrContent}'/>"
                                                    data-remaining="${o.remainingSeconds}"
                                                    title="Show QR code">
                                                QR
                                            </button>
                                            <form method="post" action="${ctx}/orders/cancel" class="pos-mini-cancel-form"
                                                  onsubmit="return confirm('Cancel order ${o.orderCode}?');">
                                                <input type="hidden" name="code" value="<c:out value='${o.orderCode}'/>">
                                                <button type="submit" class="btn-mini-action cancel" title="Cancel order">&times;</button>
                                            </form>
                                        </div>
                                    </div>
                                </c:if>
                            </c:forEach>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>

<div class="card">
    <div class="head-row">
        <h2>Recent orders</h2>
        <span class="hint">Auto-synced</span>
    </div>
    <c:choose>
        <c:when test="${empty recentOrders}">
            <p>No orders yet.</p>
        </c:when>
        <c:otherwise>
            <table>
                <thead>
                <tr>
                    <th style="width: 140px;">Order</th>
                    <th style="width: 150px;">Total</th>
                    <th>Payment method</th>
                    <th style="width: 120px;">Status</th>
                    <th style="width: 140px;">Time</th>
                    <th style="width: 110px;">Created by</th>
                    <th style="width: 90px; text-align: center;">Action</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="o" items="${recentOrders}">
                    <c:set var="isPaid" value="${o.status == 'COMPLETED' || o.status == 'PAID'}"/>
                    <c:set var="isPending" value="${o.status == 'PENDING'}"/>
                    <tr>
                        <td>
                            <a href="${ctx}/orders/view?code=<c:out value='${o.orderCode}'/>">
                                <strong><c:out value="${o.orderCode}"/></strong>
                            </a>
                        </td>
                        <td class="amount-cell"><fmt:formatNumber value="${o.amount}" pattern="#,##0"/> ₫</td>
                        <td>
                            <c:choose>
                                <c:when test="${o.cashAmount > 0}">
                                    <span class="pill" style="background: rgba(190, 24, 93, 0.08); color: var(--primary); border: 1px solid rgba(190, 24, 93, 0.2);">
                                        Cash (<fmt:formatNumber value="${o.cashAmount}" pattern="#,##0"/> ₫) + QR (<fmt:formatNumber value="${o.transferAmount}" pattern="#,##0"/> ₫)
                                    </span>
                                </c:when>
                                <c:otherwise>
                                    <span class="pill" style="background: rgba(20, 163, 139, 0.1); color: var(--ok); border: 1px solid rgba(20, 163, 139, 0.25);">100% QR</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${isPaid}">
                                    <span class="pill COMPLETED">COMPLETED</span>
                                </c:when>
                                <c:when test="${isPending}">
                                    <span class="pill PENDING"><span class="pulse-dot"></span> PENDING</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="pill CANCELLED">CANCELLED</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td><fmt:formatDate value="${o.createdAt}" pattern="yyyy-MM-dd HH:mm"/></td>
                        <td><c:out value="${o.createdByCode}"/></td>
                        <td style="text-align: center; white-space: nowrap;">
                            <c:choose>
                                <c:when test="${isPending}">
                                    <button type="button" class="btn-action btn-mini-qr-table"
                                            data-code="<c:out value='${o.orderCode}'/>"
                                            data-amount="<fmt:formatNumber value='${o.amount}' pattern='#,##0'/> ₫"
                                            data-transfer="<fmt:formatNumber value='${o.transferAmount}' pattern='#,##0'/> ₫"
                                            data-cash="<fmt:formatNumber value='${o.cashAmount}' pattern='#,##0'/> ₫"
                                            data-qr="<c:out value='${o.qrContent}'/>"
                                            data-remaining="${o.remainingSeconds}"
                                            title="Show QR code">
                                        View QR
                                    </button>
                                </c:when>
                                <c:otherwise>
                                    <a class="btn-action" href="${ctx}/orders/view?code=<c:out value='${o.orderCode}'/>" title="View details">
                                        View
                                    </a>
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</div>

<div class="modal-backdrop" id="posQrModal" hidden>
    <div class="modal pos-qr-quick-modal" role="dialog" aria-modal="true" aria-labelledby="mQrTitle">
        <button type="button" class="modal-close" id="posQrModalClose" aria-label="Close">&times;</button>
        <p class="modal-kicker">VietQR Payment</p>
        <h2 id="mQrTitle" style="margin: 0 0 10px; font-size: 18px; color: var(--navy);"></h2>

        <div class="pos-modal-qr-frame">
            <img id="mQrImg" class="pos-modal-qr-img" src="" alt="VietQR payment code">
        </div>

        <div id="mCountdown" class="pos-modal-countdown">--:--</div>

        <div id="mAmount" class="modal-amount" style="text-align: center; margin-bottom: 4px; font-size: 24px; font-weight: 800;"></div>
        <div id="mBreakdown" style="text-align: center; font-size: 13px; color: var(--muted); margin-bottom: 16px;"></div>

        <div class="pos-modal-actions">
            <button type="button" class="btn" id="mPrintBtn">Print QR</button>
            <a id="mFullscreenLink" href="" target="_blank" class="btn">Full screen ↗</a>
            <button type="button" class="btn primary" id="mDoneBtn">Back to counter</button>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
<script>
    (function () {
        var affInput = document.getElementById('affiliateCode');
        var affBtn = document.getElementById('affiliateCheck');
        var affOut = document.getElementById('affiliateResult');
        if (affBtn) {
            affBtn.addEventListener('click', function () {
                var code = affInput.value.trim();
                if (!code) { affOut.textContent = ''; return; }
                var amount = (document.getElementById('amount').value || '0').replace(/[^0-9]/g, '');
                affOut.textContent = 'Checking...';
                fetch('${ctx}/orders/affiliate?code=' + encodeURIComponent(code) + '&amount=' + amount, { credentials: 'same-origin' })
                    .then(function (r) { return r.json(); })
                    .then(function (d) { affOut.textContent = d.message; affOut.style.color = d.ok ? '#0f766e' : '#b4233c'; })
                    .catch(function () { affOut.textContent = 'Could not check the code.'; affOut.style.color = '#b4233c'; });
            });
        }
        var MAX_AMOUNT = <%= util.AppConfig.MAX_AMOUNT %>;
        var amountInput = document.getElementById('amount');
        var cashInput = document.getElementById('cashAmount');
        var splitSection = document.getElementById('splitSection');
        var chkCash = document.getElementById('methodCash');
        var chkQr = document.getElementById('methodQr');
        var previewCash = document.getElementById('previewCash');
        var previewTransfer = document.getElementById('previewTransfer');
        var submitBtn = document.getElementById('submitBtn');
        var submitBtnIcon = document.getElementById('submitBtnIcon');

        var tabBtnCart = document.getElementById('tabBtnCart');
        var tabBtnPending = document.getElementById('tabBtnPending');
        var panelViewCart = document.getElementById('panelViewCart');
        var panelViewPending = document.getElementById('panelViewPending');

        if (tabBtnCart && tabBtnPending) {
            tabBtnCart.addEventListener('click', function () {
                tabBtnCart.classList.add('active');
                tabBtnPending.classList.remove('active');
                panelViewCart.style.display = '';
                panelViewPending.style.display = 'none';
            });
            tabBtnPending.addEventListener('click', function () {
                tabBtnPending.classList.add('active');
                tabBtnCart.classList.remove('active');
                panelViewCart.style.display = 'none';
                panelViewPending.style.display = '';
            });
        }

        var selectedCat = 'all';
        var selectedGrp = 'all';
        var searchQuery = '';
        var productCards = document.querySelectorAll('.pos-product-card');
        var grpPills = document.querySelectorAll('.pos-grp-pill');
        var catTabs = document.querySelectorAll('.pos-cat-tab');
        var posSearchInput = document.getElementById('posSearchInput');
        var posSearchClear = document.getElementById('posSearchClear');
        var posNoProducts = document.getElementById('posNoProducts');

        function applyFilters() {
            var visibleCount = 0;
            productCards.forEach(function (card) {
                var matchCat = (selectedCat === 'all' || card.dataset.cat === selectedCat);
                var matchGrp = (selectedGrp === 'all' || card.dataset.grp === selectedGrp);
                var matchSearch = true;
                if (searchQuery) {
                    var name = (card.dataset.name || '').toLowerCase();
                    var code = (card.dataset.code || '').toLowerCase();
                    matchSearch = (name.indexOf(searchQuery) !== -1 || code.indexOf(searchQuery) !== -1);
                }
                if (matchCat && matchGrp && matchSearch) {
                    card.style.display = '';
                    visibleCount++;
                } else {
                    card.style.display = 'none';
                }
            });
            if (posNoProducts) {
                posNoProducts.style.display = visibleCount === 0 ? 'block' : 'none';
            }
        }

        catTabs.forEach(function (tab) {
            tab.addEventListener('click', function () {
                catTabs.forEach(function (t) { t.classList.remove('active'); });
                tab.classList.add('active');
                selectedCat = tab.dataset.cat;
                selectedGrp = 'all';

                grpPills.forEach(function (pill) {
                    var pillCat = pill.dataset.cat;
                    if (selectedCat === 'all' || pillCat === 'all' || pillCat === selectedCat) {
                        pill.style.display = '';
                    } else {
                        pill.style.display = 'none';
                    }
                    if (pill.dataset.grp === 'all') {
                        pill.classList.add('active');
                    } else {
                        pill.classList.remove('active');
                    }
                });
                applyFilters();
            });
        });

        grpPills.forEach(function (pill) {
            pill.addEventListener('click', function () {
                grpPills.forEach(function (p) { p.classList.remove('active'); });
                pill.classList.add('active');
                selectedGrp = pill.dataset.grp;
                applyFilters();
            });
        });

        if (posSearchInput) {
            posSearchInput.addEventListener('input', function () {
                searchQuery = this.value.trim().toLowerCase();
                if (posSearchClear) {
                    posSearchClear.style.display = searchQuery ? 'inline-flex' : 'none';
                }
                applyFilters();
            });
        }

        if (posSearchClear) {
            posSearchClear.addEventListener('click', function () {
                if (posSearchInput) posSearchInput.value = '';
                searchQuery = '';
                posSearchClear.style.display = 'none';
                applyFilters();
            });
        }

        var cart = [];
        var cartList = document.getElementById('posCartList');
        var cartEmpty = document.getElementById('posCartEmpty');
        var cartBadge = document.getElementById('cartCountBadge');
        var btnClearCart = document.getElementById('btnClearCart');
        var cartItemsInput = document.getElementById('cartItems');

        function escapeHtml(str) {
            return String(str || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
        }

        function renderCart() {
            if (!cartList) return;
            cartList.innerHTML = '';
            if (cart.length === 0) {
                if (cartEmpty) cartEmpty.style.display = 'block';
                if (btnClearCart) btnClearCart.style.display = 'none';
                if (cartBadge) {
                    cartBadge.style.display = 'none';
                    cartBadge.textContent = '0';
                }
                if (cartItemsInput) cartItemsInput.value = '';
                return;
            }

            if (cartEmpty) cartEmpty.style.display = 'none';
            if (btnClearCart) btnClearCart.style.display = 'inline-block';

            var totalQty = 0;
            var totalSum = 0;

            cart.forEach(function (item, idx) {
                var subtotal = item.unitPrice * item.quantity;
                totalQty += item.quantity;
                totalSum += subtotal;

                var row = document.createElement('div');
                row.className = 'pos-cart-item';
                row.innerHTML =
                    '<div class="pos-ci-info">' +
                        '<div class="pos-ci-name">' + escapeHtml(item.productName) + '</div>' +
                        '<div class="pos-ci-price">' + formatDisplay(item.unitPrice) + '</div>' +
                    '</div>' +
                    '<div class="pos-ci-qty-wrap">' +
                        '<button type="button" class="pos-ci-btn btn-qty-dec" data-idx="' + idx + '" title="Decrease quantity">-</button>' +
                        '<span class="pos-ci-qty">' + item.quantity + '</span>' +
                        '<button type="button" class="pos-ci-btn btn-qty-inc" data-idx="' + idx + '" title="Increase quantity">+</button>' +
                    '</div>' +
                    '<div class="pos-ci-subtotal">' + formatDisplay(subtotal) + '</div>' +
                    '<button type="button" class="pos-ci-remove" data-idx="' + idx + '" title="Remove item">&times;</button>';
                cartList.appendChild(row);
            });

            if (cartBadge) {
                cartBadge.textContent = totalQty;
                cartBadge.style.display = 'inline-block';
            }

            if (cartItemsInput) {
                var payload = cart.map(function (it) {
                    return {
                        productId: it.productId,
                        productCode: it.productCode,
                        productName: it.productName,
                        unitPrice: it.unitPrice,
                        quantity: it.quantity,
                        subtotal: it.unitPrice * it.quantity
                    };
                });
                cartItemsInput.value = JSON.stringify(payload);
            }

            amountInput.value = formatNumber(totalSum);
            updatePreview();
        }

        function addToCart(card) {
            var id = parseInt(card.dataset.id, 10);
            var code = card.dataset.code;
            var name = card.dataset.name;
            var price = parseInt(card.dataset.price, 10) || 0;

            var existing = null;
            for (var i = 0; i < cart.length; i++) {
                if (cart[i].productId === id) {
                    existing = cart[i];
                    break;
                }
            }

            if (existing) {
                existing.quantity++;
            } else {
                cart.push({
                    productId: id,
                    productCode: code,
                    productName: name,
                    unitPrice: price,
                    quantity: 1
                });
            }

            if (tabBtnCart && tabBtnPending && panelViewCart && panelViewPending) {
                tabBtnCart.classList.add('active');
                tabBtnPending.classList.remove('active');
                panelViewCart.style.display = '';
                panelViewPending.style.display = 'none';
            }

            document.querySelectorAll('.preset-chip').forEach(function (c) { c.classList.remove('active'); });
            renderCart();
        }

        productCards.forEach(function (card) {
            card.addEventListener('click', function () {
                addToCart(this);
            });
        });

        if (cartList) {
            cartList.addEventListener('click', function (e) {
                var btnInc = e.target.closest('.btn-qty-inc');
                var btnDec = e.target.closest('.btn-qty-dec');
                var btnRem = e.target.closest('.pos-ci-remove');

                if (btnInc) {
                    var idx = parseInt(btnInc.dataset.idx, 10);
                    if (cart[idx]) {
                        cart[idx].quantity++;
                        renderCart();
                    }
                } else if (btnDec) {
                    var idx = parseInt(btnDec.dataset.idx, 10);
                    if (cart[idx]) {
                        cart[idx].quantity--;
                        if (cart[idx].quantity <= 0) {
                            cart.splice(idx, 1);
                        }
                        renderCart();
                    }
                } else if (btnRem) {
                    var idx = parseInt(btnRem.dataset.idx, 10);
                    if (cart[idx]) {
                        cart.splice(idx, 1);
                        renderCart();
                    }
                }
            });
        }

        if (btnClearCart) {
            btnClearCart.addEventListener('click', function () {
                cart = [];
                amountInput.value = '';
                renderCart();
                updatePreview();
            });
        }

        var btnCustomSale = document.getElementById('btnCustomSale');
        if (btnCustomSale) {
            btnCustomSale.addEventListener('click', function () {
                cart = [];
                renderCart();
                amountInput.value = '';
                amountInput.focus();
                document.querySelectorAll('.preset-chip').forEach(function (c) { c.classList.remove('active'); });
                updatePreview();
            });
        }

        var modal = document.getElementById('posQrModal');
        var modalClose = document.getElementById('posQrModalClose');
        var mDoneBtn = document.getElementById('mDoneBtn');
        var mTitle = document.getElementById('mQrTitle');
        var mImg = document.getElementById('mQrImg');
        var mCountdown = document.getElementById('mCountdown');
        var mAmount = document.getElementById('mAmount');
        var mBreakdown = document.getElementById('mBreakdown');
        var mFullscreen = document.getElementById('mFullscreenLink');
        var modalTimer = null;
        var activeModalCode = null;

        function setBtnText(text, disabled) {
            var txtSpan = document.getElementById('submitBtnText');
            if (txtSpan) {
                txtSpan.textContent = text;
            } else {
                submitBtn.textContent = text;
            }
            submitBtn.disabled = !!disabled;
        }

        function parse(val) {
            if (!val) return 0;
            var num = parseInt(String(val).replace(/[.,\s]/g, ''), 10);
            return isNaN(num) ? 0 : num;
        }

        function formatNumber(num) {
            return num.toLocaleString('en-US');
        }

        function formatDisplay(num) {
            return num.toLocaleString('en-US') + ' ₫';
        }

        function updatePreview() {
            var total = parse(amountInput.value);
            var hasCash = chkCash ? chkCash.checked : false;
            var hasQr = chkQr ? chkQr.checked : true;

            if (hasCash && !hasQr) {
                splitSection.style.display = 'none';
                cashInput.value = total > 0 ? formatNumber(total) : '';
                setBtnText(total > 0 ? 'Complete Cash Order (' + formatDisplay(total) + ')' : 'Complete Cash Order', false);
                submitBtn.classList.add('cash-only');
                if (submitBtnIcon) {
                    submitBtnIcon.innerHTML = '<polyline points="20 6 9 17 4 12"></polyline>';
                }
            } else if (!hasCash && hasQr) {
                splitSection.style.display = 'none';
                cashInput.value = '';
                setBtnText(total > 0 ? 'Generate QR code (' + formatDisplay(total) + ')' : 'Generate QR code', false);
                submitBtn.classList.remove('cash-only');
                if (submitBtnIcon) {
                    submitBtnIcon.innerHTML = '<rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect>';
                }
            } else if (hasCash && hasQr) {
                splitSection.style.display = 'block';
                submitBtn.classList.remove('cash-only');
                if (submitBtnIcon) {
                    submitBtnIcon.innerHTML = '<rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect>';
                }
                var cash = parse(cashInput.value);
                if (cash >= total && total > 0) {
                    cash = 0;
                    cashInput.value = '';
                }
                var transfer = Math.max(0, total - cash);

                if (previewCash) previewCash.textContent = formatDisplay(cash);
                if (previewTransfer) previewTransfer.textContent = formatDisplay(transfer);

                if (cash >= total && total > 0) {
                    setBtnText('Cash must be less than order total', true);
                } else {
                    setBtnText(transfer > 0 ? 'Generate QR code (' + formatDisplay(transfer) + ')' : 'Generate QR code', false);
                }
            }
        }

        function handleMethodChange(clicked) {
            if (!chkCash.checked && !chkQr.checked) {
                clicked.checked = true;
            }
            updatePreview();
        }

        if (chkCash) chkCash.addEventListener('change', function () { handleMethodChange(this); });
        if (chkQr) chkQr.addEventListener('change', function () { handleMethodChange(this); });

        document.querySelectorAll('.preset-chip').forEach(function (btn) {
            btn.addEventListener('click', function () {
                document.querySelectorAll('.preset-chip').forEach(function (c) { c.classList.remove('active'); });
                btn.classList.add('active');
                if (cart.length > 0) {
                    cart = [];
                    renderCart();
                }
                amountInput.value = formatNumber(parseInt(btn.dataset.amount, 10));
                updatePreview();
            });
        });

        function keepDigits() {
            var digits = this.value.replace(/[^0-9]/g, '').replace(/^0+/, '');
            if (digits.length > 9) { digits = digits.substring(0, 9); }
            var val = parseInt(digits || '0', 10);
            if (val > MAX_AMOUNT) { val = MAX_AMOUNT; }
            this.value = val > 0 ? formatNumber(val) : '';
            updatePreview();
        }

        amountInput.addEventListener('input', function () {
            document.querySelectorAll('.preset-chip').forEach(function (c) { c.classList.remove('active'); });
            if (cart.length > 0) {
                cart = [];
                renderCart();
            }
            keepDigits.call(this);
        });
        cashInput.addEventListener('input', keepDigits);

        document.getElementById('orderForm').addEventListener('submit', function (e) {
            var total = parse(amountInput.value);
            if (total < 1000 || total > MAX_AMOUNT) {
                alert('The order amount must be between 1,000 and ' + formatNumber(MAX_AMOUNT) + ' VND.');
                e.preventDefault();
                amountInput.focus();
                return;
            }
            var hasCash = chkCash ? chkCash.checked : false;
            var hasQr = chkQr ? chkQr.checked : true;
            if (!hasCash && !hasQr) {
                alert('Please select at least one payment method (Cash or VietQR).');
                e.preventDefault();
                return;
            }
            if (hasCash && !hasQr) {
                cashInput.value = total;
            } else if (!hasCash && hasQr) {
                cashInput.value = '';
            } else {
                var cash = parse(cashInput.value);
                if (cash <= 0) {
                    alert('Enter the cash collected or uncheck Cash.');
                    e.preventDefault();
                    cashInput.focus();
                    return;
                }
                if (cash >= total) {
                    alert('For split payment, cash must be less than order total. If paying 100% cash, please uncheck VietQR.');
                    e.preventDefault();
                    cashInput.focus();
                    return;
                }
                cashInput.value = cash;
            }
        });

        if (amountInput.value) {
            var initialVal = parse(amountInput.value);
            if (initialVal > 0) amountInput.value = formatNumber(initialVal);
        }
        if (cashInput.value) {
            var initialCash = parse(cashInput.value);
            if (initialCash > 0) cashInput.value = formatNumber(initialCash);
        }
        updatePreview();

        function openQrModal(code, amount, transfer, cash, qrUrl, remaining) {
            activeModalCode = code;
            mTitle.textContent = 'Order #' + code;
            var cleanQr = (qrUrl || '').replace('-compact2.png', '-qr_only.png').replace('-compact.png', '-qr_only.png');
            mImg.src = cleanQr;
            mAmount.textContent = transfer;
            if (cash && parse(cash) > 0) {
                mBreakdown.textContent = 'Total: ' + amount + ' (Cash: ' + cash + ' + QR: ' + transfer + ')';
            } else {
                mBreakdown.textContent = '100% VietQR payment (' + transfer + ')';
            }
            mFullscreen.href = '${ctx}/orders/view?code=' + encodeURIComponent(code);

            if (modalTimer) clearInterval(modalTimer);
            var endAt = Date.now() + (parseInt(remaining, 10) || 600) * 1000;
            function updateCountdown() {
                var left = Math.max(0, Math.round((endAt - Date.now()) / 1000));
                var m = Math.floor(left / 60);
                var s = left % 60;
                mCountdown.textContent = (m < 10 ? '0' + m : m) + ':' + (s < 10 ? '0' + s : s);
            }
            updateCountdown();
            modalTimer = setInterval(updateCountdown, 1000);

            modal.hidden = false;
        }

        function closeQrModal() {
            modal.hidden = true;
            if (modalTimer) clearInterval(modalTimer);
            activeModalCode = null;
        }

        var mPrintBtn = document.getElementById('mPrintBtn');
        if (mPrintBtn) {
            mPrintBtn.addEventListener('click', function () {
                var w = window.open('', '_blank', 'width=420,height=640');
                if (!w) {
                    alert('Allow pop-ups to print the QR code.');
                    return;
                }
                var esc = function (s) {
                    return String(s || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
                };
                w.document.write('<!doctype html><html><head><meta charset="utf-8"><title>QR ' + esc(activeModalCode) + '</title>'
                    + '<style>@page{size:80mm auto;margin:0}body{margin:0;padding:4mm;width:72mm;font-family:Arial,sans-serif;text-align:center;color:#000}'
                    + 'h1{font-size:16px;margin:0 0 2mm}img{width:60mm;height:60mm}.amt{font-size:20px;font-weight:bold;margin:2mm 0}.sm{font-size:12px}</style></head><body>'
                    + '<h1>Tinky</h1><div class="sm">Scan to pay</div>'
                    + '<img id="q" src="' + esc(mImg.src) + '" alt="VietQR">'
                    + '<div class="amt">' + esc(mAmount.textContent) + '</div>'
                    + '<div class="sm">' + esc(mTitle.textContent) + '</div>'
                    + '<div class="sm">' + esc(mBreakdown.textContent) + '</div>'
                    + '<script>var i=document.getElementById("q");function go(){window.print();window.close()}if(i.complete){go()}else{i.onload=go;i.onerror=go}<\/script>'
                    + '</body></html>');
                w.document.close();
            });
        }

        if (modalClose) modalClose.addEventListener('click', closeQrModal);
        if (mDoneBtn) mDoneBtn.addEventListener('click', closeQrModal);
        if (modal) {
            modal.addEventListener('click', function (e) {
                if (e.target === modal) closeQrModal();
            });
        }

        document.querySelectorAll('.btn-mini-qr, .btn-mini-qr-table').forEach(function (btn) {
            btn.addEventListener('click', function () {
                openQrModal(
                    this.dataset.code,
                    this.dataset.amount,
                    this.dataset.transfer,
                    this.dataset.cash,
                    this.dataset.qr,
                    this.dataset.remaining
                );
            });
        });

        var pendingCards = document.querySelectorAll('.pos-mini-card[data-status="PENDING"]');
        if (pendingCards.length > 0) {
            setInterval(function () {
                pendingCards.forEach(function (card) {
                    var code = card.dataset.code;
                    if (!code || card.dataset.status !== 'PENDING') return;

                    fetch('${ctx}/orders/status?code=' + encodeURIComponent(code), { credentials: 'same-origin' })
                        .then(function (res) { return res.ok ? res.json() : null; })
                        .then(function (data) {
                            if (data && data.status !== 'PENDING') {
                                card.dataset.status = data.status;
                                if (data.status === 'COMPLETED' || data.status === 'PAID') {
                                    card.classList.add('just-completed');
                                    var amtEl = card.querySelector('.pos-mini-amt');
                                    if (amtEl) amtEl.textContent = 'COMPLETED';
                                    if (activeModalCode === code) {
                                        mAmount.textContent = '✔ PAYMENT COMPLETED';
                                        mAmount.style.color = 'var(--ok)';
                                        setTimeout(closeQrModal, 2000);
                                    }
                                } else {
                                    var amtEl = card.querySelector('.pos-mini-amt');
                                    if (amtEl) amtEl.textContent = 'CANCELLED';
                                }
                            }
                        })
                        .catch(function () {});
                });
            }, 2500);
        }
    })();
</script>
</body>
</html>
