<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Tax AI assistant"/>
<c:set var="nav" value="tax"/>
<c:set var="taxTab" value="assistant"/>
<%@ include file="/WEB-INF/views/common/layout-top.jspf" %>
<%@ include file="/WEB-INF/views/common/tax-tabs.jspf" %>
<div class="card">
    <h1>Tax AI assistant</h1>
    <p class="hint">This uses the Tinky assistant in the bottom-right corner (Gemini and the existing tax knowledge base), not a separate chatbot. Click a suggested question to open the chat and send it.</p>
    <button type="button" class="primary" id="openAssistant">Open chat</button>
</div>

<div class="card">
    <h2>Suggested questions</h2>
    <div class="chips">
        <button type="button" class="btn ask">I just opened a store. What do I need to do about tax?</button>
        <button type="button" class="btn ask">My revenue is under 1 billion. Do I have to pay tax?</button>
        <button type="button" class="btn ask">Do I need to issue e-invoices?</button>
        <button type="button" class="btn ask">Which documents do I need to prepare?</button>
        <button type="button" class="btn ask">What are my revenue and estimated tax this year?</button>
    </div>
</div>

<div class="card">
    <h2>What the assistant can use</h2>
    <ul class="plain">
        <li>Revenue, orders and best-selling products (read only, within your permissions)</li>
        <li>The tax estimate and the tax rules that are switched on</li>
        <li>Guides on opening a store, registration, invoices, bookkeeping and filing</li>
    </ul>
    <h2>Safety rules</h2>
    <ul class="plain">
        <li>It only reads data, never changes data and never writes SQL itself</li>
        <li>If information is missing it asks again instead of guessing</li>
        <li>It never files a return, pays money or issues an invoice by itself</li>
        <li>Always verify with the tax office or an accountant before deciding</li>
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
