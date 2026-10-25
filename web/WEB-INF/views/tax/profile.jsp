<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Tax profile"/>
<c:set var="nav" value="tax"/>
<c:set var="taxTab" value="profile"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<%@ include file="/WEB-INF/views/common/tax-tabs.jspf" %>
<div class="card">
    <h1>Business profile</h1>
    <p class="hint">Answer what you know. "I do not know" is fine.</p>
    <p>Profile completeness: <b>${completion}%</b></p>
</div>

<c:if test="${canManage}">
<div class="card narrow">
    <h2>Business details</h2>
    <form method="post" action="${ctx}/tax/profile">
        <input type="hidden" name="action" value="save">
        <div class="form-field">
            <label class="field-label" for="nameInput">Business name</label>
            <input type="text" id="nameInput" name="name" required minlength="2" maxlength="100" value="<c:out value='${profile.name}'/>" placeholder="e.g. Tinky Coffee & Bakery">
            <c:if test="${not empty errors.name}"><div class="field-error"><c:out value="${errors.name}"/></div></c:if>
        </div>
        <div class="form-field">
            <label class="field-label" for="typeSelect">Business type <span class="hint-inline">The rules differ for households, individuals and companies.</span></label>
            <select id="typeSelect" name="type">
                <option value="">Choose...</option>
                <option value="HOUSEHOLD" ${profile.type == 'HOUSEHOLD' ? 'selected' : ''}>Household business</option>
                <option value="INDIVIDUAL" ${profile.type == 'INDIVIDUAL' ? 'selected' : ''}>Individual business</option>
                <option value="COMPANY" ${profile.type == 'COMPANY' ? 'selected' : ''}>Company</option>
                <option value="UNREGISTERED" ${profile.type == 'UNREGISTERED' ? 'selected' : ''}>Not registered yet</option>
                <option value="UNKNOWN" ${profile.type == 'UNKNOWN' ? 'selected' : ''}>I do not know</option>
            </select>
        </div>
        <div class="form-field">
            <label class="field-label" for="sectorSelect">Business sector <span class="hint-inline">Picks the tax rates used in estimates.</span></label>
            <select id="sectorSelect" name="sector">
                <option value="GOODS" ${profile.sector == 'GOODS' ? 'selected' : ''}>Goods</option>
                <option value="SERVICE" ${profile.sector == 'SERVICE' ? 'selected' : ''}>Services</option>
                <option value="PRODUCTION" ${profile.sector == 'PRODUCTION' ? 'selected' : ''}>Production</option>
                <option value="OTHER" ${profile.sector == 'OTHER' ? 'selected' : ''}>Other</option>
            </select>
        </div>
        <div class="form-field">
            <label class="field-label" for="addressInput">Address <span class="hint-inline">Decides which tax office manages you.</span></label>
            <input type="text" id="addressInput" name="address" maxlength="100" value="<c:out value='${profile.address == "UNKNOWN" ? "" : profile.address}'/>" placeholder="e.g. 123 Nguyen Hue, Ben Nghe, District 1, HCMC">
            <c:if test="${not empty errors.address}"><div class="field-error"><c:out value="${errors.address}"/></div></c:if>
            <label class="check-opt">
                <input type="checkbox" id="address_unknown" name="address_unknown" ${profile.address == 'UNKNOWN' ? 'checked' : ''} onchange="toggleUnknown(this, 'addressInput')">
                <span>I do not know</span>
            </label>
        </div>
        <div class="form-field">
            <label class="field-label" for="branchesInput">Number of branches</label>
            <input type="text" id="branchesInput" name="branches" inputmode="numeric" maxlength="2" value="<c:out value='${profile.branches == "UNKNOWN" ? "" : profile.branches}'/>" placeholder="1">
            <c:if test="${not empty errors.branches}"><div class="field-error"><c:out value="${errors.branches}"/></div></c:if>
            <label class="check-opt">
                <input type="checkbox" id="branches_unknown" name="branches_unknown" ${profile.branches == 'UNKNOWN' ? 'checked' : ''} onchange="toggleUnknown(this, 'branchesInput')">
                <span>I do not know</span>
            </label>
        </div>
        <div class="form-field">
            <label class="field-label" for="channelsSelect">Where you sell</label>
            <select id="channelsSelect" name="channels">
                <option value="">Choose...</option>
                <option value="STORE" ${profile.channels == 'STORE' ? 'selected' : ''}>Physical store</option>
                <option value="ONLINE" ${profile.channels == 'ONLINE' ? 'selected' : ''}>Online</option>
                <option value="BOTH" ${profile.channels == 'BOTH' ? 'selected' : ''}>Both</option>
                <option value="UNKNOWN" ${profile.channels == 'UNKNOWN' ? 'selected' : ''}>I do not know</option>
            </select>
        </div>
        <div class="form-field">
            <label class="field-label" for="regSelect">Business registration</label>
            <select id="regSelect" name="registration">
                <option value="">Choose...</option>
                <option value="REGISTERED" ${profile.registration == 'REGISTERED' ? 'selected' : ''}>Registered</option>
                <option value="IN_PROGRESS" ${profile.registration == 'IN_PROGRESS' ? 'selected' : ''}>In progress</option>
                <option value="NONE" ${profile.registration == 'NONE' ? 'selected' : ''}>Not registered</option>
                <option value="UNKNOWN" ${profile.registration == 'UNKNOWN' ? 'selected' : ''}>I do not know</option>
            </select>
        </div>
        <div class="form-field">
            <label class="field-label" for="tinInput">Tax identification number <span class="hint-inline">Needed for declarations and invoices.</span></label>
            <input type="text" id="tinInput" name="tin" maxlength="14" value="<c:out value='${profile.tin == "UNKNOWN" ? "" : profile.tin}'/>" placeholder="e.g. 0123456789">
            <c:if test="${not empty errors.tin}"><div class="field-error"><c:out value="${errors.tin}"/></div></c:if>
            <label class="check-opt">
                <input type="checkbox" id="tin_unknown" name="tin_unknown" ${profile.tin == 'UNKNOWN' ? 'checked' : ''} onchange="toggleUnknown(this, 'tinInput')">
                <span>I do not know</span>
            </label>
        </div>
        <div class="form-field">
            <label class="field-label" for="methodSelect">How you declare today</label>
            <select id="methodSelect" name="method">
                <option value="">Choose...</option>
                <option value="QUARTERLY" ${profile.method == 'QUARTERLY' ? 'selected' : ''}>Quarterly</option>
                <option value="MONTHLY" ${profile.method == 'MONTHLY' ? 'selected' : ''}>Monthly</option>
                <option value="OTHER" ${profile.method == 'OTHER' ? 'selected' : ''}>Other</option>
                <option value="UNKNOWN" ${profile.method == 'UNKNOWN' ? 'selected' : ''}>I do not know</option>
            </select>
        </div>
        <div class="form-field">
            <label class="field-label" for="einvoiceSelect">E-invoice registration</label>
            <select id="einvoiceSelect" name="einvoice">
                <option value="">Choose...</option>
                <option value="YES" ${profile.einvoice == 'YES' ? 'selected' : ''}>Registered</option>
                <option value="NO" ${profile.einvoice == 'NO' ? 'selected' : ''}>Not registered</option>
                <option value="UNKNOWN" ${profile.einvoice == 'UNKNOWN' ? 'selected' : ''}>I do not know</option>
            </select>
        </div>
        <button type="submit" class="primary" style="margin-top:8px; width:100%;">Save profile</button>
    </form>
</div>
<script>
function toggleUnknown(cb, inputId) {
    var inp = document.getElementById(inputId);
    if (!inp) return;
    if (cb.checked) {
        inp.dataset.saved = inp.value;
        inp.value = '';
        inp.readOnly = true;
        inp.style.background = 'var(--surface-soft)';
        inp.style.color = 'var(--muted)';
    } else {
        inp.readOnly = false;
        inp.style.background = '#ffffff';
        inp.style.color = 'var(--navy)';
        if (inp.dataset.saved) inp.value = inp.dataset.saved;
        inp.focus();
    }
}
document.addEventListener('DOMContentLoaded', function() {
    ['address', 'branches', 'tin'].forEach(function(f) {
        var cb = document.getElementById(f + '_unknown');
        var inp = document.getElementById(f + 'Input');
        if (cb && inp && cb.checked) {
            inp.readOnly = true;
            inp.style.background = 'var(--surface-soft)';
            inp.style.color = 'var(--muted)';
        }
    });
});
</script>
</c:if>

<div class="card">
    <p>Kết quả kiểm tra nghĩa vụ và lịch sử đánh giá nằm ở trang <a href="${ctx}/tax/checker">Kiểm tra nghĩa vụ</a>.</p>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
