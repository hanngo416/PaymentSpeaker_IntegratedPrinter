(function () {
    if (!window.Announcer) { return; }

    var soundToggle = document.getElementById('soundToggle');
    var soundStatus = document.getElementById('soundStatus');
    var testBtn = document.getElementById('speakerTest');
    var voiceEl = document.getElementById('speakerVoice');
    var autoprintToggle = document.getElementById('autoprintToggle');
    var autoprintStatus = document.getElementById('autoprintStatus');
    var logEl = document.getElementById('speakerLog');
    var paperSize = document.getElementById('paperSize');

    function money(n) { return n.toLocaleString('vi-VN'); }

    function renderSound() {
        if (!soundToggle) { return; }
        var on = window.Announcer.soundOn();
        soundToggle.checked = on;
        if (soundStatus) {
            soundStatus.textContent = on ? 'Sound is enabled' : 'Sound is muted';
            soundStatus.className = on ? 'pill ACTIVE' : 'pill';
        }
        if (voiceEl) {
            voiceEl.textContent = window.Announcer.voiceText();
        }
    }

    function renderAutoprint() {
        if (!autoprintToggle) { return; }
        var on = localStorage.getItem('spk-autoprint') !== 'off';
        autoprintToggle.checked = on;
        if (autoprintStatus) {
            autoprintStatus.textContent = on ? 'Print on OK is enabled' : 'Print on OK is off';
            autoprintStatus.className = on ? 'pill ACTIVE' : 'pill';
        }
    }

    function renderLog() {
        if (!logEl) { return; }
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

    if (soundToggle) {
        soundToggle.addEventListener('change', function () {
            window.Announcer.setSound(soundToggle.checked);
            renderSound();
        });
    }

    if (testBtn) {
        testBtn.addEventListener('click', function () {
            window.Announcer.test();
        });
    }

    if (autoprintToggle) {
        autoprintToggle.addEventListener('change', function () {
            localStorage.setItem('spk-autoprint', autoprintToggle.checked ? 'on' : 'off');
            renderAutoprint();
        });
    }

    if (paperSize) {
        paperSize.value = localStorage.getItem('spk-paper') || '80';
        if (!paperSize.value) { paperSize.value = '80'; }
        paperSize.addEventListener('change', function () {
            localStorage.setItem('spk-paper', paperSize.value);
        });
    }

    var langSelect = document.getElementById('langSelect');
    if (langSelect) {
        langSelect.value = /(?:^|; )tinky_lang=vi/.test(document.cookie) ? 'vi' : 'en';
        langSelect.addEventListener('change', function () {
            TinkyI18n.setLang(langSelect.value);
            location.reload();
        });
    }

    document.addEventListener('announcer-history', renderLog);

    renderSound();
    renderAutoprint();
    renderLog();
    setTimeout(renderSound, 1000);
})();
