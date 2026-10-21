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
                <strong>Auto-print receipts</strong>
                <span id="autoprintStatus" class="pill">Auto-print is off</span>
            </div>
            <p class="hint">Automatically open and print receipt as soon as payment is confirmed at the counter.</p>
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
            <p class="hint">Printed on the receipt as "VAT included". 8% applies until 31/12/2026, then 10% unless the law is extended. Use 0 to hide the VAT line. Each order keeps the rate it was created with.</p>
        </div>
        <div class="setting-control">
            <c:choose>
                <c:when test="${isAdmin}">
                    <form method="post" action="${ctx}/settings" class="filters">
                        <input type="hidden" name="action" value="vat">
                        <input type="text" name="vatRate" value="${vatRate}" inputmode="decimal" maxlength="5" aria-label="VAT rate in percent" style="width:80px">
                        <span>%</span>
                        <button type="submit" class="btn">Save</button>
                    </form>
                </c:when>
                <c:otherwise><strong><c:out value="${vatRate}"/>%</strong></c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

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
                        <c:when test="${d.status == 'ONLINE'}">Device is ready to print receipts.</c:when>
                        <c:when test="${d.status == 'MAINTENANCE'}">Device is under maintenance by technician.</c:when>
                        <c:otherwise>Device has been idle or offline.</c:otherwise>
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
                            <label class="toggle-switch" title="Switch between Online and Maintenance mode">
                                <input type="checkbox" ${d.status != 'MAINTENANCE' ? 'checked' : ''} onchange="this.form.submit()">
                                <span class="toggle-slider"></span>
                            </label>
                        </form>
                    </c:when>
                    <c:otherwise>
                        <label class="toggle-switch disabled" title="Only technician or admin can change device status">
                            <input type="checkbox" ${d.status != 'MAINTENANCE' ? 'checked' : ''} disabled>
                            <span class="toggle-slider"></span>
                        </label>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </c:forEach>
    <c:if test="${empty devices}">
        <p class="hint">No hardware devices registered in the system.</p>
    </c:if>
</div>

<div class="card">
    <h2>Recent Voice Announcements</h2>
    <ul id="speakerLog" class="speaker-log">
        <li class="hint">No payments confirmed yet.</li>
    </ul>
</div>

<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
<script src="${ctx}/assets/js/settings.js?v=3" charset="UTF-8"></script>
</body>
</html>
