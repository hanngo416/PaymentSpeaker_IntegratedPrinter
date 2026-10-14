// Sound settings page: switch the voice on or off, test it, and list the payments confirmed on this computer.
(function () {
    var box = document.getElementById('sound-settings');
    if (!box || !window.Announcer) { return; }

    var stateEl = document.getElementById('sound-state');
    var toggleBtn = document.getElementById('sound-toggle');
    var testBtn = document.getElementById('speaker-test');
    var voiceEl = document.getElementById('speaker-voice');
    var logEl = document.getElementById('speaker-log');

    function money(n) { return n.toLocaleString('vi-VN'); }

    function renderState() {
        var on = window.Announcer.soundOn();
        stateEl.textContent = on ? 'Sound is on' : 'Sound is off';
        stateEl.className = on ? 'badge PAID' : 'badge';
        toggleBtn.textContent = on ? 'Turn sound off' : 'Turn sound on';
        voiceEl.textContent = window.Announcer.voiceText();
    }

    function renderLog() {
        var list = window.Announcer.history();
        logEl.innerHTML = '';
        if (list.length === 0) {
            var empty = document.createElement('li');
            empty.className = 'hint';
            empty.textContent = 'No payments confirmed yet.';
            logEl.appendChild(empty);
            return;
        }
        list.forEach(function (h) {
            var li = document.createElement('li');
            var time = document.createElement('span');
            time.className = 'log-time';
            time.textContent = new Date(h.at).toLocaleTimeString('vi-VN');
            var text = document.createElement('span');
            text.className = 'log-text';
            text.textContent = 'Received ' + money(h.amount) + ' VND' + (h.order ? ' (' + h.order + ')' : '');
            li.appendChild(time);
            li.appendChild(text);
            logEl.appendChild(li);
        });
    }

    toggleBtn.addEventListener('click', function () {
        window.Announcer.setSound(!window.Announcer.soundOn());
        renderState();
    });
    testBtn.addEventListener('click', function () { window.Announcer.test(); });
    document.addEventListener('announcer-history', renderLog);

    renderState();
    renderLog();
    // Voices load a moment after the page does
    setTimeout(renderState, 1000);
})();
