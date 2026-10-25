<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Trợ lý AI thuế"/>
<c:set var="nav" value="tax"/>
<c:set var="taxTab" value="assistant"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<%@ include file="/WEB-INF/views/common/tax-tabs.jspf" %>
<div class="card">
    <h1>Trợ lý AI thuế</h1>
    <p class="hint">Dùng chính trợ lý Tinky ở góc phải bên dưới (Gemini và cơ sở tri thức thuế có sẵn), không phải một chatbot riêng. Bấm một câu hỏi gợi ý để mở khung chat và gửi ngay.</p>
    <button type="button" class="primary" id="openAssistant">Mở khung chat</button>
</div>

<div class="card">
    <h2>Câu hỏi gợi ý</h2>
    <div class="chips">
        <button type="button" class="btn ask">Tôi mới mở cửa hàng thì cần làm gì về thuế?</button>
        <button type="button" class="btn ask">Doanh thu của tôi dưới 1 tỷ thì có phải đóng thuế không?</button>
        <button type="button" class="btn ask">Tôi có cần xuất hóa đơn điện tử không?</button>
        <button type="button" class="btn ask">Tôi cần chuẩn bị giấy tờ gì?</button>
        <button type="button" class="btn ask">Doanh thu và ước tính thuế năm nay của tôi là bao nhiêu?</button>
    </div>
</div>

<div class="card">
    <h2>Trợ lý có thể dùng</h2>
    <ul class="plain">
        <li>Doanh thu, đơn hàng và sản phẩm bán chạy (chỉ đọc, theo quyền của bạn)</li>
        <li>Ước tính thuế và các quy tắc thuế đang bật</li>
        <li>Tài liệu hướng dẫn mở cửa hàng, đăng ký, hóa đơn, sổ sách và kê khai</li>
    </ul>
    <h2>Quy tắc an toàn</h2>
    <ul class="plain">
        <li>Chỉ đọc dữ liệu, không sửa dữ liệu và không tự viết SQL</li>
        <li>Thiếu thông tin thì hỏi lại, không đoán</li>
        <li>Không tự nộp tờ khai, tự trả tiền hay tự phát hành hóa đơn</li>
        <li>Luôn cần xác minh với cơ quan thuế hoặc kế toán trước khi quyết định</li>
    </ul>
</div>
<script>
    (function () {
        function openBot() {
            var panel = document.getElementById('botPanel');
            if (panel && panel.hidden) {
                document.getElementById('botToggle').click();
            }
        }
        document.getElementById('openAssistant').addEventListener('click', openBot);
        var buttons = document.querySelectorAll('.ask');
        for (var i = 0; i < buttons.length; i++) {
            buttons[i].addEventListener('click', function () {
                openBot();
                document.getElementById('botInput').value = this.textContent;
                document.getElementById('botForm').dispatchEvent(new Event('submit', {cancelable: true, bubbles: true}));
            });
        }
    })();
</script>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
