<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Store Setup Wizard"/>
<c:set var="nav" value="settings"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>

<div class="card" style="max-width: 860px; margin: 0 auto 20px;">
    <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 16px;">
        <div>
            <span class="pill ACTIVE" style="margin-bottom: 8px;">Store Onboarding</span>
            <h1>Store Setup Wizard</h1>
            <p class="hint">Tailor Tinky to your business model. Pick a quick preset or customize the 4 modules below. Core features (POS counter checkout, bank QR ingestion, inventory tracking and shifts) are always active.</p>
        </div>
        <c:if test="${setupDone}">
            <a class="btn" href="${ctx}/home">Return to Home</a>
        </c:if>
    </div>
    <c:if test="${not empty message}"><div class="msg alert" style="color:var(--danger);">${message}</div></c:if>
</div>

<div class="card" style="max-width: 860px; margin: 0 auto 20px;">
    <h2>1. Quick Presets (1-Click Model Selection)</h2>
    <p class="hint">Click a template below to automatically configure the recommended module switches.</p>

    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 14px; margin-top: 14px;">
        <!-- Preset 1: Local Store -->
        <div class="preset-card card" style="cursor: pointer; padding: 16px; border: 2px solid var(--card-border); transition: all 0.2s;" onclick="applyPreset('LOCAL', this)">
            <div style="font-size: 24px; margin-bottom: 6px;">☕</div>
            <strong style="font-size: 15px; color: var(--text-primary);">Local Store</strong>
            <p class="hint" style="font-size: 12px; margin: 6px 0 10px;">Cafes, bakeries, restaurants and counter-only retail.</p>
            <div style="font-size: 11.5px; color: var(--text-secondary); line-height: 1.5;">
                <div>&bull; POS + SePay + Inventory: <strong>ON</strong></div>
                <div>&bull; Tax Governance: <strong>ON</strong></div>
                <div>&bull; Online &amp; Affiliate: <strong style="color: var(--text-muted);">OFF</strong></div>
                <div>&bull; AI Copilot: <strong>ON</strong></div>
            </div>
        </div>

        <!-- Preset 2: Omnichannel Retail -->
        <div class="preset-card card" style="cursor: pointer; padding: 16px; border: 2px solid var(--card-border); transition: all 0.2s;" onclick="applyPreset('OMNI', this)">
            <div style="font-size: 24px; margin-bottom: 6px;">🛍️</div>
            <strong style="font-size: 15px; color: var(--text-primary);">Omnichannel Retail</strong>
            <p class="hint" style="font-size: 12px; margin: 6px 0 10px;">Fashion, cosmetics, retail selling in-store and on Shopee / TikTok.</p>
            <div style="font-size: 11.5px; color: var(--text-secondary); line-height: 1.5;">
                <div>&bull; POS + SePay + Inventory: <strong>ON</strong></div>
                <div>&bull; Shopee / TikTok Sync: <strong>ON</strong></div>
                <div>&bull; CTV Affiliate Program: <strong>ON</strong></div>
                <div>&bull; Tax &amp; AI Copilot: <strong>ON</strong></div>
            </div>
        </div>

        <!-- Preset 3: Full Enterprise Suite -->
        <div class="preset-card card" style="cursor: pointer; padding: 16px; border: 2px solid var(--primary); background: rgba(190, 24, 93, 0.04); transition: all 0.2s;" onclick="applyPreset('FULL', this)">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
                <span style="font-size: 24px;">🏢</span>
                <span class="pill ACTIVE" style="font-size: 10px;">DEFAULT</span>
            </div>
            <strong style="font-size: 15px; color: var(--text-primary);">Full Enterprise Suite</strong>
            <p class="hint" style="font-size: 12px; margin: 6px 0 10px;">Complete feature suite with all advanced tools enabled.</p>
            <div style="font-size: 11.5px; color: var(--text-secondary); line-height: 1.5;">
                <div>&bull; All 4 modules active</div>
                <div>&bull; Comprehensive tax compliance</div>
                <div>&bull; Multi-channel sales book</div>
                <div>&bull; 14-tool Gemini AI assistant</div>
            </div>
        </div>
    </div>
</div>

<div class="card" style="max-width: 860px; margin: 0 auto;">
    <h2>2. Module Customization</h2>
    <p class="hint">Fine-tune each individual module according to your store operations.</p>

    <form method="post" action="${ctx}/setup" id="setupForm" style="margin-top: 16px;">
        <!-- Question 1: Tax -->
        <div class="setting-item" style="padding: 16px 0; border-bottom: 1px solid var(--card-border);">
            <div class="setting-info">
                <strong>1. Tax Governance &amp; E-Invoices</strong>
                <p class="hint">Track annual revenue towards the 1,000,000,000 VND statutory threshold (Decree 141/2026/ND-CP), estimate flat VAT/PIT, and stage electronic invoices.</p>
                <div id="sectorGroup" style="margin-top: 10px;">
                    <label style="font-size: 12.5px; font-weight: 600;">Business Sector:
                        <select name="taxSector" id="taxSectorSelect" style="margin-left: 6px; padding: 4px 8px; font-size: 12.5px;">
                            <c:forEach var="sec" items="${sectors}">
                                <option value="${sec}" ${taxSector == sec ? 'selected' : ''}>
                                    ${sec == 'GOODS' ? 'GOODS - Retail & Consumer Products (1% VAT, 0.5% PIT)' : sec == 'SERVICE' ? 'SERVICE - Food & Beverage, Hospitality (5% VAT, 2% PIT)' : sec == 'PRODUCTION' ? 'PRODUCTION - Manufacturing & Transport (3% VAT, 1.5% PIT)' : 'OTHER - Other Business Lines (2% VAT, 1% PIT)'}
                                </option>
                            </c:forEach>
                        </select>
                    </label>
                </div>
            </div>
            <div class="setting-control">
                <label class="toggle-switch" title="Toggle Tax module">
                    <input type="checkbox" id="taxToggle" name="featureTax" value="1" checked onchange="toggleSectorSelect(this)">
                    <span class="toggle-slider"></span>
                </label>
            </div>
        </div>

        <!-- Question 2: Online Omnichannel -->
        <div class="setting-item" style="padding: 16px 0; border-bottom: 1px solid var(--card-border);">
            <div class="setting-info">
                <strong>2. Omnichannel Retail &amp; Marketplace Sync</strong>
                <p class="hint">Import Shopee &amp; TikTok Shop order CSV files, reconcile marketplace commissions and platform service fees, and compute net margin waterfall.</p>
            </div>
            <div class="setting-control">
                <label class="toggle-switch" title="Toggle Omnichannel module">
                    <input type="checkbox" id="onlineToggle" name="featureOnline" value="1" checked>
                    <span class="toggle-slider"></span>
                </label>
            </div>
        </div>

        <!-- Question 3: Affiliate -->
        <div class="setting-item" style="padding: 16px 0; border-bottom: 1px solid var(--card-border);">
            <div class="setting-info">
                <strong>3. Affiliate Referral Program &amp; Collaborator Portal</strong>
                <p class="hint">Manage referral partners, attribute cashier referrals at POS checkout, guard against staff fraud on thermal receipts, and provide self-service affiliate portal (/affiliate/portal).</p>
            </div>
            <div class="setting-control">
                <label class="toggle-switch" title="Toggle Affiliate module">
                    <input type="checkbox" id="affiliateToggle" name="featureAffiliate" value="1" checked>
                    <span class="toggle-slider"></span>
                </label>
            </div>
        </div>

        <!-- Question 4: AI Copilot -->
        <div class="setting-item" style="padding: 16px 0; border-bottom: 1px solid var(--card-border);">
            <div class="setting-info">
                <strong>4. Gemini Agentic AI Copilot</strong>
                <p class="hint">Enable interactive chat assistant powered by Gemini function calling with 14 store ledger tools and 18 official government legal tax guide documents.</p>
            </div>
            <div class="setting-control">
                <label class="toggle-switch" title="Toggle AI module">
                    <input type="checkbox" id="aiToggle" name="featureAi" value="1" checked>
                    <span class="toggle-slider"></span>
                </label>
            </div>
        </div>

        <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 24px; padding-top: 16px;">
            <button type="submit" name="skip" value="1" class="btn" style="color: var(--text-secondary);">
                Skip &amp; Enable All Modules
            </button>
            <div style="display: flex; gap: 10px;">
                <button type="submit" class="btn primary" style="font-weight: 700; padding: 10px 24px;">
                    Complete Setup &amp; Save
                </button>
            </div>
        </div>
    </form>
</div>

<script>
function toggleSectorSelect(taxCb) {
    var sg = document.getElementById('sectorGroup');
    if (sg) {
        sg.style.opacity = taxCb.checked ? '1' : '0.4';
        var sel = document.getElementById('taxSectorSelect');
        if (sel) sel.disabled = !taxCb.checked;
    }
}

function applyPreset(name, el) {
    document.querySelectorAll('.preset-card').forEach(function(c) {
        c.style.borderColor = 'var(--card-border)';
        c.style.background = 'transparent';
    });
    el.style.borderColor = 'var(--primary)';
    el.style.background = 'rgba(190, 24, 93, 0.04)';

    var tax = document.getElementById('taxToggle');
    var online = document.getElementById('onlineToggle');
    var aff = document.getElementById('affiliateToggle');
    var ai = document.getElementById('aiToggle');

    if (name === 'LOCAL') {
        tax.checked = true;
        online.checked = false;
        aff.checked = false;
        ai.checked = true;
    } else if (name === 'OMNI') {
        tax.checked = true;
        online.checked = true;
        aff.checked = true;
        ai.checked = true;
    } else if (name === 'FULL') {
        tax.checked = true;
        online.checked = true;
        aff.checked = true;
        ai.checked = true;
    }
    toggleSectorSelect(tax);
}
</script>

<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>

