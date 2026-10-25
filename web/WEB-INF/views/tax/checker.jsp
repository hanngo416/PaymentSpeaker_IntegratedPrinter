<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Kiểm tra nghĩa vụ"/>
<c:set var="nav" value="tax"/>
<c:set var="taxTab" value="checker"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<%@ include file="/WEB-INF/views/common/tax-tabs.jspf" %>
<div class="card">
    <h1>Kiểm tra nghĩa vụ thuế</h1>
    <p class="hint">Kết quả chỉ mang tính tham khảo, không phải quyết định pháp lý: hãy đối chiếu với cơ quan thuế hoặc kế toán. Hệ thống không bao giờ kết luận chỉ từ doanh thu, mà cần cả loại hình và thông tin đăng ký của bạn, và sẽ nói rõ còn thiếu gì.</p>
    <p>Hồ sơ đã hoàn thành: <b>${completion}%</b> - <a href="${ctx}/tax/profile">cập nhật hồ sơ kinh doanh</a></p>
</div>

<div class="card">
    <h2>Kết quả kiểm tra</h2>
    <p class="hint">Ngày đánh giá: <b>hôm nay</b>, kỳ áp dụng: <b>năm dương lịch hiện tại</b>.</p>
    <c:forEach var="c" items="${checks}">
        <div class="card">
            <h3><c:out value="${c.label}"/> <span class="pill ${c.status == 'APPLICABLE' ? 'ACTIVE' : (c.status == 'NOT_APPLICABLE' ? 'EXPIRED' : 'PENDING')}"><c:choose><c:when test="${c.status == 'APPLICABLE'}">Áp dụng</c:when><c:when test="${c.status == 'NOT_APPLICABLE'}">Không áp dụng</c:when><c:otherwise>Cần xác minh</c:otherwise></c:choose></span></h3>
            <p><c:out value="${c.reason}"/></p>
            <c:forEach var="cond" items="${c.conds}">
                <div class="hint"><c:choose><c:when test="${cond[1] == true}">&#10003;</c:when><c:when test="${cond[1] == false}">&#10007;</c:when><c:otherwise>?</c:otherwise></c:choose> <c:out value="${cond[0]}"/></div>
            </c:forEach>
            <c:if test="${not empty c.missing}"><div class="alert-line">Còn thiếu: <c:forEach var="m" items="${c.missing}" varStatus="st"><c:out value="${m}"/><c:if test="${!st.last}">, </c:if></c:forEach></div></c:if>
            <p class="hint">Căn cứ: <c:out value="${c.basis}"/> - quy tắc ${c.verified ? 'đã xác minh' : 'chưa xác minh'} - lấy từ <a href="${ctx}/admin/taxes">Quy tắc thuế</a>.</p>
        </div>
    </c:forEach>
    <c:if test="${canManage}">
        <form method="post" action="${ctx}/tax/checker">
            <input type="hidden" name="action" value="confirm">
            <button type="submit" class="primary" onclick="return confirm('Lưu kết quả đánh giá này với ngày hôm nay?');">Xác nhận và lưu đánh giá</button>
        </form>
    </c:if>
</div>

<div class="card">
    <h2>Lịch sử đánh giá</h2>
    <table>
        <thead><tr><th>Ngày</th><th>Kỳ</th><th>Kết quả</th><th>Xác minh</th><th>Người lưu</th></tr></thead>
        <tbody>
        <c:forEach var="a" items="${assessments}">
            <tr><td>${a.assessed_on}</td><td><c:out value="${a.period_label}"/></td><td><c:out value="${a.summary}"/></td><td>${a.verification}</td><td><c:out value="${a.created_name}"/></td></tr>
        </c:forEach>
        <c:if test="${empty assessments}"><tr><td colspan="5">Chưa có đánh giá nào được lưu.</td></tr></c:if>
        </tbody>
    </table>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
