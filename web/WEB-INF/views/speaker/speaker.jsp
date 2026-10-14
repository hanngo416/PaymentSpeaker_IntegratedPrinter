<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Payment sound"/>
<c:set var="nav" value="speaker"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>

<div class="card narrow center" id="sound-settings">
    <h1>Payment notification</h1>
    <p class="hint">When money arrives, a popup shows on whatever page you are on and this computer reads the amount aloud. The popup stays until you press OK.</p>
    <p><span id="sound-state" class="badge PAID">Sound is on</span></p>
    <p id="speaker-voice" class="hint">Checking voices...</p>
    <div class="speaker-actions">
        <button id="sound-toggle" type="button" class="primary go">Turn sound off</button>
        <button id="speaker-test" type="button">Test voice</button>
    </div>
</div>

<div class="card narrow">
    <h2>Confirmed on this computer</h2>
    <ul id="speaker-log" class="speaker-log">
        <li class="hint">No payments confirmed yet.</li>
    </ul>
</div>

<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
<script src="${ctx}/assets/js/speaker.js" charset="UTF-8"></script>
</body>
</html>
