<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Hồ sơ và chứng từ"/>
<c:set var="nav" value="tax"/>
<c:set var="taxTab" value="documents"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<%@ include file="/WEB-INF/views/common/tax-tabs.jspf" %>
<c:url var="base" value="/tax/documents"><c:param name="q" value="${q}"/><c:param name="category" value="${category}"/><c:param name="status" value="${status}"/></c:url>
<div class="card">
    <h1>Hồ sơ và chứng từ</h1>
    <p class="hint">Lưu giấy tờ thuế ở một nơi và biết mình còn thiếu gì. Tệp tối đa 10 MB, nhận PDF, ảnh PNG/JPG, CSV, Excel và Word. Tệp được lưu trong cơ sở dữ liệu và chỉ tải xuống dưới dạng tệp đính kèm.</p>
</div>

<div class="kpis">
    <div class="card"><span class="kpi-label">Đã tải lên</span><div class="kpi-value">${counts.uploaded}</div></div>
    <div class="card"><span class="kpi-label">Cần bổ sung</span><div class="kpi-value ${counts.missing > 0 ? 'bad' : ''}">${counts.missing}</div><p class="kpi-sub">Chưa có tệp</p></div>
    <div class="card"><span class="kpi-label">Cần kiểm tra</span><div class="kpi-value">${counts.review}</div></div>
    <div class="card"><span class="kpi-label">Tổng</span><div class="kpi-value">${counts.total}</div></div>
</div>

<c:if test="${canManage}">
<div class="card narrow">
    <h2>Tải tài liệu lên</h2>
    <form method="post" action="${ctx}/tax/documents" enctype="multipart/form-data">
        <input type="hidden" name="action" value="upload">
        <label>Tệp <input type="file" name="file" required accept=".pdf,.png,.jpg,.jpeg,.csv,.xls,.xlsx,.doc,.docx"></label>
        <label>Tên tài liệu <input type="text" name="name" maxlength="150" placeholder="Để trống để dùng tên tệp"></label>
        <label>Nhóm
            <select name="category">
                <option value="REGISTRATION">Đăng ký kinh doanh</option>
                <option value="TAX_CODE">Mã số thuế</option>
                <option value="INVOICE">Hóa đơn</option>
                <option value="BANK">Sao kê ngân hàng</option>
                <option value="EXPENSE">Chi phí</option>
                <option value="FILING">Tờ khai và nộp thuế</option>
                <option value="OTHER">Khác</option>
            </select>
        </label>
        <label>Kỳ báo cáo <input type="text" name="period" maxlength="40" placeholder="Ví dụ: Quý III/2026"></label>
        <button type="submit" class="primary">Tải lên</button>
    </form>
</div>
</c:if>

<div class="card">
    <form method="get" action="${ctx}/tax/documents" class="filters">
        <label>Tìm kiếm <input type="text" name="q" maxlength="100" value="<c:out value='${q}'/>" placeholder="Tên, tệp hoặc kỳ"></label>
        <label>Nhóm
            <select name="category">
                <option value="">Tất cả</option>
                <option value="REGISTRATION" ${category == 'REGISTRATION' ? 'selected' : ''}>Đăng ký kinh doanh</option>
                <option value="TAX_CODE" ${category == 'TAX_CODE' ? 'selected' : ''}>Mã số thuế</option>
                <option value="INVOICE" ${category == 'INVOICE' ? 'selected' : ''}>Hóa đơn</option>
                <option value="BANK" ${category == 'BANK' ? 'selected' : ''}>Sao kê ngân hàng</option>
                <option value="EXPENSE" ${category == 'EXPENSE' ? 'selected' : ''}>Chi phí</option>
                <option value="FILING" ${category == 'FILING' ? 'selected' : ''}>Tờ khai và nộp thuế</option>
                <option value="OTHER" ${category == 'OTHER' ? 'selected' : ''}>Khác</option>
            </select>
        </label>
        <label>Trạng thái
            <select name="status">
                <option value="">Tất cả</option>
                <option value="UPLOADED" ${status == 'UPLOADED' ? 'selected' : ''}>Đã tải lên</option>
                <option value="MISSING" ${status == 'MISSING' ? 'selected' : ''}>Cần bổ sung</option>
                <option value="REVIEW" ${status == 'REVIEW' ? 'selected' : ''}>Cần kiểm tra</option>
            </select>
        </label>
        <button type="submit" class="primary">Lọc</button>
    </form>
    <table>
        <thead><tr><th>Tên</th><th>Nhóm</th><th>Kỳ</th><th>Tệp</th><th class="r">Dung lượng</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
        <tbody>
        <c:forEach var="d" items="${docs}">
            <tr>
                <td><b><c:out value="${d.doc_name}"/></b></td>
                <td><c:choose>
                    <c:when test="${d.category == 'REGISTRATION'}">Đăng ký kinh doanh</c:when>
                    <c:when test="${d.category == 'TAX_CODE'}">Mã số thuế</c:when>
                    <c:when test="${d.category == 'INVOICE'}">Hóa đơn</c:when>
                    <c:when test="${d.category == 'BANK'}">Sao kê ngân hàng</c:when>
                    <c:when test="${d.category == 'EXPENSE'}">Chi phí</c:when>
                    <c:when test="${d.category == 'FILING'}">Tờ khai và nộp thuế</c:when>
                    <c:otherwise>Khác</c:otherwise></c:choose></td>
                <td><c:out value="${d.period_label}" default="-"/></td>
                <td><c:out value="${d.file_name}" default="-"/></td>
                <td class="r"><c:choose><c:when test="${d.size_bytes > 0}"><fmt:formatNumber value="${d.size_bytes / 1024}" maxFractionDigits="0"/> KB</c:when><c:otherwise>-</c:otherwise></c:choose></td>
                <td><span class="pill ${d.status == 'UPLOADED' ? 'ACTIVE' : (d.status == 'MISSING' ? 'LOCKED' : 'PENDING')}"><c:choose><c:when test="${d.status == 'UPLOADED'}">Đã tải lên</c:when><c:when test="${d.status == 'MISSING'}">Cần bổ sung</c:when><c:otherwise>Cần kiểm tra</c:otherwise></c:choose></span></td>
                <td>
                    <c:if test="${d.status != 'MISSING'}"><a class="btn" href="${ctx}/tax/documents?download=${d.doc_id}">Tải xuống</a></c:if>
                    <c:if test="${canManage}">
                        <c:if test="${d.status == 'MISSING'}">
                            <form method="post" action="${ctx}/tax/documents" enctype="multipart/form-data" class="inline">
                                <input type="hidden" name="action" value="attach">
                                <input type="hidden" name="id" value="${d.doc_id}">
                                <input type="file" name="file" required accept=".pdf,.png,.jpg,.jpeg,.csv,.xls,.xlsx,.doc,.docx">
                                <button type="submit" class="primary">Tải lên</button>
                            </form>
                        </c:if>
                        <c:if test="${d.status == 'UPLOADED'}">
                            <form method="post" action="${ctx}/tax/documents" class="inline"><input type="hidden" name="action" value="review"><input type="hidden" name="id" value="${d.doc_id}"><button type="submit" class="btn">Đánh dấu cần kiểm tra</button></form>
                        </c:if>
                        <c:if test="${d.status == 'REVIEW'}">
                            <form method="post" action="${ctx}/tax/documents" class="inline"><input type="hidden" name="action" value="unreview"><input type="hidden" name="id" value="${d.doc_id}"><button type="submit" class="btn">Đã kiểm tra xong</button></form>
                        </c:if>
                        <c:if test="${d.status != 'MISSING'}">
                            <form method="post" action="${ctx}/tax/documents" class="inline"><input type="hidden" name="action" value="delete"><input type="hidden" name="id" value="${d.doc_id}"><button type="submit" class="btn danger" onclick="return confirm('Xóa tài liệu này? Không thể khôi phục.');">Xóa</button></form>
                        </c:if>
                    </c:if>
                </td>
            </tr>
        </c:forEach>
        <c:if test="${empty docs}"><tr><td colspan="7">Không có tài liệu nào phù hợp.</td></tr></c:if>
        </tbody>
    </table>
    <p>Trang ${page} / ${pages}</p>
    <div class="pages">
        <c:if test="${page > 1}"><a href="${base}&amp;page=${page - 1}" title="Trang trước">&laquo;</a></c:if>
        <c:forEach begin="1" end="${pages}" var="pg">
            <c:choose>
                <c:when test="${pg == page}"><span>${pg}</span></c:when>
                <c:otherwise><a href="${base}&amp;page=${pg}">${pg}</a></c:otherwise>
            </c:choose>
        </c:forEach>
        <c:if test="${page < pages}"><a href="${base}&amp;page=${page + 1}" title="Trang sau">&raquo;</a></c:if>
    </div>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
