(function () {
    var box = document.getElementById('taxBot');
    if (!box) { return; }

    var feedUrl = box.dataset.feedUrl || '';
    var ctx = box.dataset.ctx;
    var toggle = document.getElementById('botToggle');
    var panel = document.getElementById('botPanel');
    var dot = document.getElementById('botDot');
    var statusLine = document.getElementById('botStatus');
    var list = document.getElementById('botMessages');
    var SEEN_PREFIX = 'tinky-bot-seen-';
    var UNREAD_KEY = 'tinky-bot-unread';
    var LOG_KEY = 'tinky-bot-log';

    function load(key) { try { return localStorage.getItem(key); } catch (e) { return null; } }
    function save(key, value) { try { localStorage.setItem(key, value); } catch (e) { } }

    function readLog() {
        try { return JSON.parse(load(LOG_KEY) || '[]'); } catch (e) { return []; }
    }

    function renderMessage(m) {
        var item = document.createElement('div');
        item.className = 'bot-msg';
        var text = document.createElement('p');
        text.textContent = m.text;
        item.appendChild(text);
        if (m.link) {
            var a = document.createElement('a');
            a.className = 'btn';
            a.href = ctx + m.link;
            a.textContent = m.label || 'Open';
            item.appendChild(a);
        }
        list.appendChild(item);
    }

    function renderLog() {
        list.innerHTML = '';
        var log = readLog();
        for (var i = 0; i < log.length; i++) { renderMessage(log[i]); }
    }

    function setLevel(level) {
        box.className = 'tax-bot level' + level;
    }

    function showDot(on) {
        dot.hidden = !on;
        save(UNREAD_KEY, on ? '1' : '0');
    }

    function handle(data) {
        setLevel(data.level);
        statusLine.textContent = 'Tax: ' + data.percent + '% of the threshold, '
            + Number(data.remaining).toLocaleString('en-US') + ' VND left.';
        var log = readLog();
        var added = false;
        for (var i = 0; i < data.messages.length; i++) {
            var m = data.messages[i];
            if (load(SEEN_PREFIX + m.key) === '1') { continue; }
            save(SEEN_PREFIX + m.key, '1');
            log.push(m);
            added = true;
        }
        if (added) {
            save(LOG_KEY, JSON.stringify(log.slice(-20)));
            showDot(true);
            if (!panel.hidden) { renderLog(); }
        }
    }

    function poll() {
        var xhr = new XMLHttpRequest();
        xhr.open('GET', feedUrl, true);
        xhr.onload = function () {
            if (xhr.status !== 200) { return; }
            try { handle(JSON.parse(xhr.responseText)); } catch (e) { }
        };
        xhr.send();
    }

    toggle.addEventListener('click', function () {
        panel.hidden = !panel.hidden;
        if (!panel.hidden) {
            renderLog();
            showDot(false);
        }
    });

    var chatUrl = box.dataset.chatUrl;
    var chat = document.getElementById('botChat');
    var form = document.getElementById('botForm');
    var input = document.getElementById('botInput');
    var sendBtn = document.getElementById('botSend');
    var CHAT_KEY = 'tinky-bot-chat';

    function plain(s) {
        return String(s).replace(/\*\*|__|`/g, '').replace(/^\s*\*\s+/gm, '- ');
    }

    function readChat() {
        try { return JSON.parse(sessionStorage.getItem(CHAT_KEY) || '[]'); } catch (e) { return []; }
    }

    function saveChat(items) {
        try { sessionStorage.setItem(CHAT_KEY, JSON.stringify(items.slice(-30))); } catch (e) { }
    }

    function renderChat() {
        chat.innerHTML = '';
        var items = readChat();
        for (var i = 0; i < items.length; i++) {
            var row = document.createElement('div');
            row.className = 'bot-line ' + items[i].who;
            var p = document.createElement('p');
            p.textContent = plain(items[i].text);
            row.appendChild(p);
            if (items[i].steps && items[i].steps.length) {
                var s = document.createElement('small');
                s.textContent = 'Used: ' + items[i].steps.join(', ');
                row.appendChild(s);
            }
            chat.appendChild(row);
        }
        panel.scrollTop = panel.scrollHeight;
    }

    function addChat(who, text, steps) {
        var items = readChat();
        items.push({ who: who, text: text, steps: steps || [] });
        saveChat(items);
        renderChat();
    }

    form.addEventListener('submit', function (ev) {
        ev.preventDefault();
        var text = input.value.trim();
        if (!text) { return; }
        addChat('me', text);
        input.value = '';
        sendBtn.disabled = true;
        var wait = document.createElement('div');
        wait.className = 'bot-line bot';
        wait.innerHTML = '<p>Thinking...</p>';
        chat.appendChild(wait);
        panel.scrollTop = panel.scrollHeight;
        var xhr = new XMLHttpRequest();
        xhr.open('POST', chatUrl, true);
        xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
        xhr.timeout = 60000;
        xhr.onload = function () {
            sendBtn.disabled = false;
            try {
                var data = JSON.parse(xhr.responseText);
                addChat('bot', data.answer, data.steps);
            } catch (e) {
                addChat('bot', 'The assistant could not answer. Please try again.');
            }
        };
        xhr.onerror = xhr.ontimeout = function () {
            sendBtn.disabled = false;
            addChat('bot', 'The assistant could not be reached. Please try again.');
        };
        var past = readChat().slice(0, -1).filter(function (m) { return m.text; }).map(function (m) { return { who: m.who, text: m.text }; });
        xhr.send('message=' + encodeURIComponent(text) + '&history=' + encodeURIComponent(JSON.stringify(past.slice(-8))));
    });

    document.getElementById('botClear').addEventListener('click', function () {
        saveChat([]);
        renderChat();
    });

    toggle.addEventListener('click', function () {
        if (!panel.hidden) { renderChat(); input.focus(); }
    });

    dot.hidden = load(UNREAD_KEY) !== '1';
    renderChat();
    if (feedUrl) {
        poll();
        setInterval(poll, 30000);
    } else {
        statusLine.hidden = true;
    }
})();
