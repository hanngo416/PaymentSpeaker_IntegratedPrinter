<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Settings"/>
<c:set var="nav" value="settings"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>

<div class="card">
    <h1>Settings</h1>
    <p class="hint">Configure counter speaker announcements, receipt printer automation, and hardware device status.</p>
    <c:if test="${not empty message}"><div class="msg"><c:out value="${message}"/></div></c:if>
</div>

<div class="card">
    <h2>Language</h2>
    <div class="setting-item">
        <div class="setting-info">
            <strong>Interface language</strong>
            <p class="hint">Switch the whole application between English and Vietnamese. The choice is saved in this browser. Receipts, emails and the Starter Guide articles keep their own language.</p>
        </div>
        <div class="setting-control">
            <select id="langSelect" aria-label="Interface language">
                <option value="en">English</option>
                <option value="vi">Tiếng Việt</option>
            </select>
        </div>
    </div>
</div>

<div class="card">
    <h2>Speaker & Sound Notification</h2>
    <div class="setting-item">
        <div class="setting-info">
            <div class="setting-title-row">
                <strong>Payment voice announcement</strong>
                <span id="soundStatus" class="pill ACTIVE">Sound is enabled</span>
            </div>
            <p class="hint">Play an audible chime and read the received amount aloud when customer transfer arrives.</p>
        </div>
        <div class="setting-control">
            <label class="toggle-switch" title="Toggle sound announcement">
                <input type="checkbox" id="soundToggle" checked>
                <span class="toggle-slider"></span>
            </label>
        </div>
    </div>

    <div class="setting-item">
        <div class="setting-info">
            <strong>Voice synthesizer engine</strong>
            <p id="speakerVoice" class="hint">Checking voices...</p>
        </div>
        <div class="setting-control">
            <button id="speakerTest" type="button" class="btn">Test voice</button>
        </div>
    </div>
</div>

<div class="card">
    <h2>Receipt Printing & Automation</h2>
    <div class="setting-item">
        <div class="setting-info">
            <div class="setting-title-row">
                <strong>Print receipt when OK is pressed</strong>
                <span id="autoprintStatus" class="pill">Print on OK is off</span>
            </div>
            <p class="hint">When a payment popup appears, pressing OK also prints the receipt of that order.</p>
        </div>
        <div class="setting-control">
            <label class="toggle-switch" title="Toggle automatic receipt printing">
                <input type="checkbox" id="autoprintToggle">
                <span class="toggle-slider"></span>
            </label>
        </div>
    </div>

    <div class="setting-item">
        <div class="setting-info">
            <strong>Receipt paper size</strong>
            <p class="hint">Match the paper loaded in the printer on this computer so the receipt fits the roll or sheet.</p>
        </div>
        <div class="setting-control">
            <select id="paperSize" aria-label="Receipt paper size">
                <option value="58">Thermal roll 58 mm</option>
                <option value="80">Thermal roll 80 mm / 76×130 mm</option>
                <option value="76">Thermal slip 76×130 mm</option>
                <option value="a4">A4 sheet</option>
            </select>
        </div>
    </div>
</div>

<div class="card">
    <h2>Tax (VAT)</h2>
    <div class="setting-item">
        <div class="setting-info">
            <strong>VAT rate included in prices</strong>
            <p class="hint">Choose whether the prices in the catalog already contain VAT (the receipt then shows the VAT part of the total) or VAT is added on top (the total paid is the goods plus VAT, rounded to the nearest dong). 8% applies until 31/12/2026, then 10% unless the law is extended. Use 0 to hide the VAT line. Each order keeps the rate and the way it was created with.</p>
        </div>
        <div class="setting-control">
            <c:choose>
                <c:when test="${isAdmin}">
                    <form method="post" action="${ctx}/settings" class="filters">
                        <input type="hidden" name="action" value="vat">
                        <input type="text" name="vatRate" value="${vatRate}" inputmode="decimal" maxlength="5" aria-label="VAT rate in percent" style="width:80px">
                        <span>%</span>
                        <select name="vatIncluded" aria-label="How VAT is applied">
                            <option value="0" ${vatIncluded ? '' : 'selected'}>VAT is added on top of the price</option>
                            <option value="1" ${vatIncluded ? 'selected' : ''}>Price already includes VAT</option>
                        </select>
                        <button type="submit" class="btn">Save</button>
                    </form>
                </c:when>
                <c:otherwise><strong><c:out value="${vatRate}"/>%</strong></c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<div class="card">
    <h2>Inventory &amp; Low Stock Alert</h2>
    <div class="setting-item">
        <div class="setting-info">
            <strong>Low stock alert threshold</strong>
            <p class="hint">When a product's on-hand quantity falls at or below this number, it shows a Low stock warning badge on products and order screens.</p>
        </div>
        <div class="setting-control">
            <c:choose>
                <c:when test="${isAdmin}">
                    <form method="post" action="${ctx}/settings" class="filters">
                        <input type="hidden" name="action" value="stock">
                        <input type="number" name="stockLowThreshold" min="0" max="9999" value="${stockLowThreshold}" aria-label="Low stock threshold" style="width:80px">
                        <span>units</span>
                        <button type="submit" class="btn">Save</button>
                    </form>
                </c:when>
                <c:otherwise><strong><c:out value="${stockLowThreshold}"/> units</strong></c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<c:if test="${isAdmin}">
<div class="card">
    <h2>Store Modules (Feature Toggles)</h2>
    <p class="hint">Enable or disable advanced system modules. Core functions (POS, automated bank polling, inventory, shifts, audit logs) remain permanently active. <a href="${ctx}/setup" style="margin-left: 8px;">Run setup wizard again &rarr;</a></p>
    
    <form id="featuresForm" method="post" action="${ctx}/settings">
        <input type="hidden" name="action" value="features">
        
        <div class="setting-item">
            <div class="setting-info">
                <div class="setting-title-row">
                    <strong>Tax Governance &amp; E-Invoices</strong>
                    <span class="pill ${features.tax ? 'ACTIVE' : 'LOCKED'}">${features.tax ? 'ENABLED' : 'DISABLED'}</span>
                </div>
                <p class="hint">1 Billion VND revenue threshold tracker, deterministic tax estimation, e-invoice staging, and statutory tax alerts.</p>
            </div>
            <div class="setting-control">
                <label class="toggle-switch" title="Toggle Tax module">
                    <input type="checkbox" id="featureTaxInput" name="featureTax" value="1" ${features.tax ? 'checked' : ''} onchange="handleTaxToggle(this)">
                    <span class="toggle-slider"></span>
                </label>
            </div>
        </div>

        <div class="setting-item">
            <div class="setting-info">
                <div class="setting-title-row">
                    <strong>Omnichannel Retail &amp; Marketplace Sync</strong>
                    <span class="pill ${features.online ? 'ACTIVE' : 'LOCKED'}">${features.online ? 'ENABLED' : 'DISABLED'}</span>
                </div>
                <p class="hint">Shopee and TikTok Shop CSV sales book import, platform commissions, shipping fees, and waterfall net margin calculation.</p>
            </div>
            <div class="setting-control">
                <label class="toggle-switch" title="Toggle Omnichannel module">
                    <input type="checkbox" name="featureOnline" value="1" ${features.online ? 'checked' : ''}>
                    <span class="toggle-slider"></span>
                </label>
            </div>
        </div>

        <div class="setting-item">
            <div class="setting-info">
                <div class="setting-title-row">
                    <strong>Affiliate Referral Program</strong>
                    <span class="pill ${features.affiliate ? 'ACTIVE' : 'LOCKED'}">${features.affiliate ? 'ENABLED' : 'DISABLED'}</span>
                </div>
                <p class="hint">Collaborator referral campaigns, cashier attribution at POS, receipt fraud shield, and self-service affiliate portal (/affiliate/portal).</p>
            </div>
            <div class="setting-control">
                <label class="toggle-switch" title="Toggle Affiliate module">
                    <input type="checkbox" name="featureAffiliate" value="1" ${features.affiliate ? 'checked' : ''}>
                    <span class="toggle-slider"></span>
                </label>
            </div>
        </div>

        <div class="setting-item">
            <div class="setting-info">
                <div class="setting-title-row">
                    <strong>Gemini Agentic AI Copilot</strong>
                    <span class="pill ${features.ai ? 'ACTIVE' : 'LOCKED'}">${features.ai ? 'ENABLED' : 'DISABLED'}</span>
                </div>
                <p class="hint">Interactive natural language assistant chat with 14 role-gated business tools and 18-document official legal tax knowledge RAG.</p>
            </div>
            <div class="setting-control">
                <label class="toggle-switch" title="Toggle AI Copilot module">
                    <input type="checkbox" name="featureAi" value="1" ${features.ai ? 'checked' : ''}>
                    <span class="toggle-slider"></span>
                </label>
            </div>
        </div>

        <div style="margin-top: 16px;">
            <button type="submit" class="btn primary">Save Module Settings</button>
        </div>
    </form>
</div>

<div id="taxDisableModal" class="modal-backdrop" hidden>
    <div class="modal">
        <p class="modal-kicker" style="color:var(--danger);">Legal Compliance Notice</p>
        <h3>Disabling Tax Governance Module?</h3>
        <p style="font-size:13.5px; line-height:1.5; color:var(--text-secondary); margin:12px 0;">
            Turning off this module will stop Tinky from tracking your <strong>1 Billion VND statutory threshold</strong> and will disable proactive threshold warning notifications.
        </p>
        <p style="font-size:12.5px; line-height:1.45; background:rgba(239, 68, 68, 0.1); border:1px solid rgba(239, 68, 68, 0.25); padding:10px 14px; border-radius:8px; color:var(--danger);">
            <strong>Important:</strong> Disabling this module inside Tinky does <em>not</em> relieve your business of its statutory tax declaration and payment obligations under Decree 141/2026/ND-CP and Circular 40/2021/TT-BTC.
        </p>
        <div style="display:flex; justify-content:flex-end; gap:8px; margin-top:20px;">
            <button type="button" class="btn" onclick="cancelTaxDisable()">Cancel</button>
            <button type="button" class="btn" style="background:var(--danger); border-color:var(--danger); color:#fff;" onclick="confirmTaxDisable()">I understand, disable Tax</button>
        </div>
    </div>
</div>
</c:if>

<div class="card">
    <h2>Hardware Devices</h2>
    <c:forEach var="d" items="${devices}">
        <div class="setting-item">
            <div class="setting-info">
                <div class="setting-title-row">
                    <strong><c:out value="${d.modelName}"/> (<c:out value="${d.deviceCode}"/>)</strong>
                    <span class="pill ${d.status == 'ONLINE' ? 'ACTIVE' : d.status == 'MAINTENANCE' ? 'PENDING' : 'LOCKED'}"><c:out value="${d.status}"/></span>
                </div>
                <p class="hint">
                    Type: <c:out value="${d.deviceType}"/> ·
                    <c:choose>
                        <c:when test="${d.status == 'MAINTENANCE'}">In maintenance mode: receipts cannot be printed.</c:when>
                        <c:when test="${not empty d.detail}"><c:out value="${d.detail}"/></c:when>
                        <c:otherwise>In service.</c:otherwise>
                    </c:choose>
                    <c:if test="${!canWrite}">
                        (Only technician or administrator can change maintenance status)
                    </c:if>
                </p>
            </div>
            <div class="setting-control">
                <c:choose>
                    <c:when test="${canWrite}">
                        <form method="post" action="${ctx}/settings" class="device-toggle-form">
                            <input type="hidden" name="id" value="${d.deviceId}">
                            <input type="hidden" name="action" value="${d.status == 'MAINTENANCE' ? 'online' : 'maintenance'}">
                            <label class="toggle-switch" title="Maintenance mode">
                                <input type="checkbox" ${d.status == 'MAINTENANCE' ? 'checked' : ''} onchange="this.form.submit()">
                                <span class="toggle-slider"></span>
                            </label>
                            <span class="hint">Maintenance mode</span>
                        </form>
                    </c:when>
                    <c:otherwise>
                        <label class="toggle-switch disabled" title="Only technician or admin can change device status">
                            <input type="checkbox" ${d.status == 'MAINTENANCE' ? 'checked' : ''} disabled>
                            <span class="toggle-slider"></span>
                        </label>
                        <span class="hint">Maintenance mode</span>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </c:forEach>
    <c:if test="${empty devices}">
        <p class="hint">No hardware devices registered in the system.</p>
    </c:if>
    <c:if test="${canWrite}">
        <p class="hint" style="margin-top: 8px;"><a href="${ctx}/admin/devices">Manage all hardware devices &rarr;</a></p>
    </c:if>
    <div class="setting-item">
        <div class="setting-info">
            <strong>Receipt printer on this PC</strong>
            <p class="hint">The status above is read from Windows, so the server must run on the PC the printer is plugged into. Receipts are sent straight to this printer by the server, whatever browser is used.</p>
        </div>
        <div class="setting-control">
            <c:choose>
                <c:when test="${canWrite}">
                    <form method="post" action="${ctx}/settings" class="filters">
                        <select name="printerName" aria-label="Windows printer">
                            <c:if test="${empty printerName}"><option value="">Choose a printer</option></c:if>
                            <c:forEach var="pr" items="${installedPrinters}">
                                <option value="<c:out value='${pr}'/>" ${pr == printerName ? 'selected' : ''}><c:out value="${pr}"/></option>
                            </c:forEach>
                        </select>
                        <button type="submit" name="action" value="printer" class="btn">Save</button>
                        <button type="submit" name="action" value="printtest" class="btn">Print test slip</button>
                    </form>
                </c:when>
                <c:otherwise><strong><c:out value="${empty printerName ? 'Not selected' : printerName}"/></strong></c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<div class="card">
    <h2>Recent Voice Announcements</h2>
    <ul id="speakerLog" class="speaker-log">
        <li class="hint">No payments confirmed yet.</li>
    </ul>
</div>

<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
<script>
function handleTaxToggle(checkbox) {
    if (!checkbox.checked) {
        document.getElementById('taxDisableModal').hidden = false;
    }
}
function cancelTaxDisable() {
    var cb = document.getElementById('featureTaxInput');
    if (cb) cb.checked = true;
    document.getElementById('taxDisableModal').hidden = true;
}
function confirmTaxDisable() {
    document.getElementById('taxDisableModal').hidden = true;
}
</script>
<script src="${ctx}/assets/js/settings.js?v=6" charset="UTF-8"></script>
</body>
</html>
