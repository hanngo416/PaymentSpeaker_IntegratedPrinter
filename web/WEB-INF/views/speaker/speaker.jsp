<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Speaker"/>
<c:set var="nav" value="speaker"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<div class="card narrow center" id="speaker"
     data-poll-url="${ctx}/speaker/poll" data-ack-url="${ctx}/speaker/ack">
    <h1>Payment speaker</h1>
    <p class="hint">Open this page on the phone at the counter. The speaker is on by default. If the browser blocks the sound, click anywhere on this page once.</p>
    <p id="speaker-voice" class="hint">Checking voices...</p>
    <p id="speaker-state" class="badge">Speaker is off</p>
    <p>
        <button id="speaker-on" type="button" class="primary go">Turn speaker on</button>
    </p>
    <p><button id="speaker-test" type="button">Test voice</button></p>
</div>
<div class="card narrow">
    <h2>Read out loud</h2>
    <ul id="speaker-log" class="speaker-log"><li class="hint">Nothing yet.</li></ul>
</div>
<%@ include file="/WEB-INF/views/common/layout-bottom.jspf" %>
<script src="${ctx}/assets/js/speaker.js"></script>
</body>
</html>
