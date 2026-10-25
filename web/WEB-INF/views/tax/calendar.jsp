<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Lịch thuế"/>
<c:set var="nav" value="tax"/>
<c:set var="taxTab" value="calendar"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<%@ include file="/WEB-INF/views/common/tax-tabs.jspf" %>
<div class="card">
    <h1>Lịch thuế</h1>
    <p class="hint">Các mốc cần nhớ lấy từ danh sách nghĩa vụ thuế (hạn nộp và trạng thái). Thời hạn cần đối chiếu với quy định hiện hành. Muốn đổi hạn hay trạng thái, mở <a href="${ctx}/tax/obligations">Nghĩa vụ thuế</a>.</p>
    <div>
        <a class="btn ${view == 'grid' ? 'primary' : ''}" href="${ctx}/tax/calendar?view=grid&amp;m=${month}">Lịch</a>
        <a class="btn ${view == 'list' ? 'primary' : ''}" href="${ctx}/tax/calendar?view=list&amp;m=${month}">Danh sách</a>
    </div>
    <c:if test="${savedMsg}"><div class="msg">Đã lưu cài đặt nhắc việc.</div></c:if>
</div>

<div class="card">
    <c:choose>
        <c:when test="${view == 'grid'}">
            <div class="cal-head">
                <a class="btn" href="${ctx}/tax/calendar?view=grid&amp;m=${prevMonth}">&lsaquo; Trước</a>
                <h2>Tháng ${monthLabel}</h2>
                <a class="btn" href="${ctx}/tax/calendar?view=grid&amp;m=${nextMonth}">Sau &rsaquo;</a>
            </div>
            <div class="cal-grid">
                <div class="cal-dow">T2</div><div class="cal-dow">T3</div><div class="cal-dow">T4</div><div class="cal-dow">T5</div><div class="cal-dow">T6</div><div class="cal-dow">T7</div><div class="cal-dow">CN</div>
                <c:forEach var="cell" items="${cells}">
                    <c:choose>
                        <c:when test="${cell == null}"><div class="cal-day empty"></div></c:when>
                        <c:otherwise>
                            <div class="cal-day ${cell.today ? 'today' : ''}">
                                <span class="cal-num">${cell.day}<c:if test="${cell.today}"> - Hôm nay</c:if></span>
                                <c:forEach var="t" items="${cell.items}">
                                    <a class="pill ${t.status == 'PAID' || t.status == 'NOT_APPLICABLE' ? 'ACTIVE' : (t.late == 1 ? 'LOCKED' : 'PENDING')}" href="${ctx}/tax/obligations" title="<c:out value='${t.kind} - ${t.period_label}'/>"><c:out value="${t.kind}"/> <c:out value="${t.period_label}"/></a>
                                </c:forEach>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </c:forEach>
            </div>
        </c:when>
        <c:otherwise>
            <table>
                <thead><tr><th>Hạn</th><th>Nghĩa vụ</th><th>Kỳ</th><th>Trạng thái</th><th>Người phụ trách</th></tr></thead>
                <tbody>
                <c:forEach var="t" items="${tasks}">
                    <tr>
                        <td>${t.due_date}</td>
                        <td><c:out value="${t.kind}"/></td>
                        <td><c:out value="${t.period_label}"/></td>
                        <td><span class="pill ${t.status == 'PAID' || t.status == 'NOT_APPLICABLE' ? 'ACTIVE' : (t.late == 1 ? 'LOCKED' : 'PENDING')}"><c:out value="${t.status}"/></span><c:if test="${t.late == 1}"> quá hạn</c:if></td>
                        <td><c:out value="${t.responsible}"/></td>
                    </tr>
                </c:forEach>
                <c:if test="${empty tasks}"><tr><td colspan="5">Chưa có nghĩa vụ thuế nào.</td></tr></c:if>
                </tbody>
            </table>
        </c:otherwise>
    </c:choose>
</div>

<div class="card">
    <h2>Việc sắp tới</h2>
    <table>
        <thead><tr><th>Hạn</th><th>Nghĩa vụ</th><th>Kỳ</th><th>Trạng thái</th></tr></thead>
        <tbody>
        <c:forEach var="t" items="${upcoming}">
            <tr><td>${t.due_date}</td><td><c:out value="${t.kind}"/></td><td><c:out value="${t.period_label}"/></td><td><span class="pill ${t.late == 1 ? 'LOCKED' : 'PENDING'}"><c:out value="${t.status}"/></span></td></tr>
        </c:forEach>
        <c:if test="${empty upcoming}"><tr><td colspan="4">Không còn việc nào chưa xong.</td></tr></c:if>
        </tbody>
    </table>
</div>

<c:if test="${canManage}">
<div class="card narrow">
    <h2>Cài đặt nhắc việc</h2>
    <p class="hint">Các lựa chọn được lưu lại. Hệ thống chưa tự gửi email nhắc: thông báo hạn nộp hiện hiện trong mục Thông báo.</p>
    <form method="post" action="${ctx}/tax/calendar">
        <input type="hidden" name="view" value="${view}">
        <label><input type="checkbox" name="d7" ${reminders.d7 ? 'checked' : ''}> Nhắc trước 7 ngày</label>
        <label><input type="checkbox" name="d3" ${reminders.d3 ? 'checked' : ''}> Nhắc trước 3 ngày</label>
        <label><input type="checkbox" name="d1" ${reminders.d1 ? 'checked' : ''}> Nhắc trước 1 ngày</label>
        <label><input type="checkbox" name="mail" ${reminders.mail ? 'checked' : ''}> Gửi email</label>
        <label><input type="checkbox" name="app" ${reminders.app ? 'checked' : ''}> Thông báo trong ứng dụng</label>
        <button type="submit" class="primary">Lưu cài đặt</button>
    </form>
</div>
</c:if>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
