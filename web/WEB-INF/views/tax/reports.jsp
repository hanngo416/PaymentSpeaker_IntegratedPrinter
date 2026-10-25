<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Doanh thu và báo cáo"/>
<c:set var="nav" value="tax"/>
<c:set var="taxTab" value="reports"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<%@ include file="/WEB-INF/views/common/tax-tabs.jspf" %>
<div class="card">
    <h1>Doanh thu và báo cáo thuế</h1>
    <p class="hint">Tự động tổng hợp từ đơn hàng tại quầy và đơn sàn đã nhập. Doanh thu này chưa chắc bằng doanh thu tính thuế theo luật: xem cách tính ở cuối trang và đối chiếu với kế toán.</p>
</div>

<div class="card">
    <form method="get" action="${ctx}/tax/reports" class="filters">
        <label>Kỳ báo cáo
            <select name="range" id="rangeSelect">
                <option value="ytd" ${range == 'ytd' ? 'selected' : ''}>Từ đầu năm</option>
                <option value="m0" ${range == 'm0' ? 'selected' : ''}>Tháng này</option>
                <option value="m1" ${range == 'm1' ? 'selected' : ''}>Tháng trước</option>
                <option value="q" ${range == 'q' ? 'selected' : ''}>Quý này</option>
                <option value="custom" ${range == 'custom' ? 'selected' : ''}>Tùy chọn</option>
            </select>
        </label>
        <label>Từ ngày <input type="date" name="from" value="${from}" id="fromInput" ${range == 'custom' ? '' : 'disabled'}></label>
        <label>Đến ngày <input type="date" name="to" value="${to}" id="toInput" ${range == 'custom' ? '' : 'disabled'}></label>
        <button type="submit" class="primary">Xem</button>
        <c:if test="${empty error}"><a class="btn" href="${ctx}/tax/reports?range=${range}&amp;from=${from}&amp;to=${to}&amp;export=csv">Xuất CSV</a></c:if>
    </form>
    <c:if test="${not empty error}"><div class="alert-line"><c:out value="${error}"/></div></c:if>
    <p class="hint">Kỳ báo cáo: ${from} đến ${to}</p>
</div>

<div class="kpis">
    <div class="card"><span class="kpi-label">Doanh thu gộp</span><div class="kpi-value"><fmt:formatNumber value="${report.gross}" pattern="#,##0"/></div><p class="kpi-sub">VND, trước giảm giá</p></div>
    <div class="card"><span class="kpi-label">Giảm giá</span><div class="kpi-value">-<fmt:formatNumber value="${report.discounts}" pattern="#,##0"/></div><p class="kpi-sub">Mã giảm giá và số tiền nhập tay</p></div>
    <div class="card"><span class="kpi-label">Đơn hủy và hoàn trả</span><div class="kpi-value"><fmt:formatNumber value="${report.removed}" pattern="#,##0"/></div><p class="kpi-sub">Đã loại khỏi doanh thu</p></div>
    <div class="card"><span class="kpi-label">Doanh thu dùng để tính thuế</span><div class="kpi-value"><fmt:formatNumber value="${report.taxable}" pattern="#,##0"/></div><p class="kpi-sub">Doanh thu ròng, cần bạn xem lại</p></div>
</div>

<div class="card">
    <h2>Theo kênh bán</h2>
    <table>
        <thead><tr><th>Kênh</th><th class="r">Doanh thu ròng</th><th>Tỷ trọng</th></tr></thead>
        <tbody>
        <tr><td>Cửa hàng (POS)</td><td class="r"><fmt:formatNumber value="${report.store}" pattern="#,##0"/></td><td><progress class="bar" value="${report.store}" max="${report.taxable > 0 ? report.taxable : 1}"></progress></td></tr>
        <tr><td>Shopee</td><td class="r"><fmt:formatNumber value="${report.shopee}" pattern="#,##0"/></td><td><progress class="bar" value="${report.shopee}" max="${report.taxable > 0 ? report.taxable : 1}"></progress></td></tr>
        <tr><td>TikTok</td><td class="r"><fmt:formatNumber value="${report.tiktok}" pattern="#,##0"/></td><td><progress class="bar" value="${report.tiktok}" max="${report.taxable > 0 ? report.taxable : 1}"></progress></td></tr>
        </tbody>
    </table>
</div>

<div class="card">
    <h2>Chi tiết theo tháng</h2>
    <table>
        <thead><tr><th>Tháng</th><th class="r">Cửa hàng</th><th class="r">Shopee</th><th class="r">TikTok</th><th class="r">Giảm giá</th><th class="r">Hủy / hoàn trả</th><th class="r">Doanh thu tính thuế</th><th>So sánh</th></tr></thead>
        <tbody>
        <c:forEach var="m" items="${report.months}">
            <tr>
                <td>${m.ym}</td>
                <td class="r"><fmt:formatNumber value="${m.store}" pattern="#,##0"/></td>
                <td class="r"><fmt:formatNumber value="${m.shopee}" pattern="#,##0"/></td>
                <td class="r"><fmt:formatNumber value="${m.tiktok}" pattern="#,##0"/></td>
                <td class="r">-<fmt:formatNumber value="${m.discounts}" pattern="#,##0"/></td>
                <td class="r"><fmt:formatNumber value="${m.removed}" pattern="#,##0"/></td>
                <td class="r"><b><fmt:formatNumber value="${m.taxable}" pattern="#,##0"/></b></td>
                <td><progress class="bar" value="${m.taxable}" max="${maxMonth}"></progress></td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
    <p class="hint">Số liệu tính bằng VND. Đơn hủy và hoàn trả chỉ để tham khảo, đã không nằm trong doanh thu tính thuế.</p>
</div>

<div class="card">
    <h2>Cách tính</h2>
    <ul class="plain">
        <li>Nguồn dữ liệu: đơn hoàn tất tại quầy (không tính VAT cộng thêm) và đơn Shopee, TikTok đã nhập ở trạng thái hoàn tất.</li>
        <li>Doanh thu dùng để tính thuế = doanh thu cửa hàng ròng + doanh thu các sàn.</li>
        <li>Doanh thu gộp = doanh thu dùng để tính thuế + giảm giá.</li>
        <li>Loại trừ: đơn đã hủy, đơn sàn bị hủy hoặc hoàn trả. Hệ thống chưa có hoàn tiền cho đơn tại quầy.</li>
        <li>Bản tổng hợp này để bạn tham khảo, không phải tờ khai và hệ thống không tự nộp thay bạn.</li>
    </ul>
    <c:forEach var="w" items="${warnings}"><div class="alert-line"><c:out value="${w}"/></div></c:forEach>
</div>
<script>
    (function () {
        var sel = document.getElementById('rangeSelect');
        sel.addEventListener('change', function () {
            var custom = sel.value === 'custom';
            document.getElementById('fromInput').disabled = !custom;
            document.getElementById('toInput').disabled = !custom;
        });
    })();
</script>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
