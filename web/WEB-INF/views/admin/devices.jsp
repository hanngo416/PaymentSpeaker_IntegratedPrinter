<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<c:set var="pageTitle" value="Devices"/>
<c:set var="nav" value="devices"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<div class="card">
    <h1>Devices</h1>
    <c:if test="${not empty message}"><div class="msg"><c:out value="${message}"/></div></c:if>
    <p class="hint">A device shows ONLINE while it is in service and MAINTENANCE when a technician has taken it out of service. The app cannot see the physical printer, so ONLINE does not prove the hardware is connected.</p>
</div>

<div class="card">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; flex-wrap: wrap; gap: 0.5rem;">
        <div>
            <h2 style="margin: 0; font-size: 1.25rem;">Payment Gateway (SePay)</h2>
            <p class="hint" style="margin: 0.25rem 0 0 0;">Automatic background synchronization for VietQR bank transfers.</p>
        </div>
        <div>
            <c:choose>
                <c:when test="${sepayStatus.lastStatus == 'SUCCESS' && sepayStatus.consecutiveFailures == '0'}">
                    <span class="pill ACTIVE">ONLINE</span>
                </c:when>
                <c:when test="${sepayStatus.consecutiveFailures > 0 && sepayStatus.consecutiveFailures < 5}">
                    <span class="pill PENDING">DEGRADED (<c:out value="${sepayStatus.consecutiveFailures}"/> errors)</span>
                </c:when>
                <c:when test="${sepayStatus.consecutiveFailures >= 5}">
                    <span class="pill LOCKED">DISCONNECTED</span>
                </c:when>
                <c:otherwise>
                    <span class="pill PENDING"><c:out value="${sepayStatus.lastStatus}"/></span>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 1rem; margin-top: 1rem;">
        <div style="padding: 0.85rem; border-radius: 12px; background: rgba(255, 255, 255, 0.45); border: 1px solid rgba(255, 255, 255, 0.6);">
            <div style="font-size: 0.8rem; color: #6b7280; text-transform: uppercase; font-weight: 600;">Last Sync Time</div>
            <div style="font-size: 1.05rem; font-weight: 600; margin-top: 0.25rem; color: #1f2937;">
                <c:out value="${sepayStatus.lastPollTime}"/>
            </div>
        </div>
        <div style="padding: 0.85rem; border-radius: 12px; background: rgba(255, 255, 255, 0.45); border: 1px solid rgba(255, 255, 255, 0.6);">
            <div style="font-size: 0.8rem; color: #6b7280; text-transform: uppercase; font-weight: 600;">Sync Status</div>
            <div style="font-size: 1.05rem; font-weight: 600; margin-top: 0.25rem; color: #1f2937;">
                <c:out value="${sepayStatus.lastStatus}"/>
            </div>
        </div>
        <div style="padding: 0.85rem; border-radius: 12px; background: rgba(255, 255, 255, 0.45); border: 1px solid rgba(255, 255, 255, 0.6);">
            <div style="font-size: 0.8rem; color: #6b7280; text-transform: uppercase; font-weight: 600;">Last Ingested</div>
            <div style="font-size: 1.05rem; font-weight: 600; margin-top: 0.25rem; color: #1f2937;">
                <c:out value="${sepayStatus.lastCount}"/> orders
            </div>
        </div>
        <div style="padding: 0.85rem; border-radius: 12px; background: rgba(255, 255, 255, 0.45); border: 1px solid rgba(255, 255, 255, 0.6);">
            <div style="font-size: 0.8rem; color: #6b7280; text-transform: uppercase; font-weight: 600;">Failures / Backoff</div>
            <div style="font-size: 1.05rem; font-weight: 600; margin-top: 0.25rem; color: #1f2937;">
                <c:choose>
                    <c:when test="${sepayStatus.consecutiveFailures == '0'}">Normal (5s)</c:when>
                    <c:when test="${sepayStatus.consecutiveFailures == '1'}">Backoff (15s)</c:when>
                    <c:when test="${sepayStatus.consecutiveFailures == '2'}">Backoff (30s)</c:when>
                    <c:otherwise>Backoff (60s max)</c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
    <c:if test="${not empty sepayStatus.lastError}">
        <div style="margin-top: 0.85rem; padding: 0.65rem 0.85rem; border-radius: 8px; background: rgba(239, 68, 68, 0.1); border: 1px solid rgba(239, 68, 68, 0.2); font-size: 0.85rem; color: #b91c1c;">
            <strong>Recent Error:</strong> <c:out value="${sepayStatus.lastError}"/>
        </div>
    </c:if>
</div>

<div class="card">
    <table>
        <thead>
        <tr><th>Code</th><th>Type</th><th>Model</th><th>Status</th><th>Last seen</th><c:if test="${canWrite}"><th>Action</th></c:if></tr>
        </thead>
        <tbody>
        <c:forEach var="d" items="${devices}">
            <tr>
                <td><c:out value="${d.deviceCode}"/></td>
                <td><c:out value="${d.deviceType}"/></td>
                <td><c:out value="${d.modelName}"/></td>
                <td><span class="pill ${d.status == 'ONLINE' ? 'ACTIVE' : d.status == 'MAINTENANCE' ? 'PENDING' : 'LOCKED'}"><c:out value="${d.status}"/></span></td>
                <td><fmt:formatDate value="${d.lastPing}" pattern="yyyy-MM-dd HH:mm:ss"/></td>
                <c:if test="${canWrite}">
                    <td>
                        <form method="post" action="${ctx}/admin/devices" class="inline">
                            <input type="hidden" name="id" value="${d.deviceId}">
                            <c:choose>
                                <c:when test="${d.status == 'MAINTENANCE'}">
                                    <button type="submit" name="action" value="online" class="primary">Back to work</button>
                                </c:when>
                                <c:otherwise>
                                    <button type="submit" name="action" value="maintenance">Put in maintenance</button>
                                </c:otherwise>
                            </c:choose>
                        </form>
                    </td>
                </c:if>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
</body>
</html>
