(function () {
    var box = document.getElementById('speaker');
    if (!box) { return; }

    var pollUrl = box.dataset.pollUrl;
    var ackUrl = box.dataset.ackUrl;
    var stateEl = document.getElementById('speaker-state');
    var onBtn = document.getElementById('speaker-on');
    var testBtn = document.getElementById('speaker-test');
    var logEl = document.getElementById('speaker-log');

    var enabled = false;
    var busy = {};      // ids that are queued or being read, so the next poll does not take them again
    var queue = [];
    var speaking = false;

    var voiceEl = document.getElementById('speaker-voice');
    var viVoice = null;

    // Voices load asynchronously, so listen for the voiceschanged event
    function pickVoice() {
        var voices = window.speechSynthesis.getVoices();
        viVoice = null;
        for (var i = 0; i < voices.length; i++) {
            if (/^vi/i.test(voices[i].lang.replace('_', '-'))) { viVoice = voices[i]; break; }
        }
        if (viVoice) {
            voiceEl.textContent = 'Voice: ' + viVoice.name + ' (' + viVoice.lang + ')';
            voiceEl.className = 'hint';
        } else {
            voiceEl.textContent = 'No Vietnamese voice found (' + voices.length + ' voices on this browser). Install one in the system text-to-speech settings.';
            voiceEl.className = 'error';
        }
    }
    pickVoice();
    window.speechSynthesis.onvoiceschanged = pickVoice;

    function say(text, done, blocked) {
        var u = new SpeechSynthesisUtterance(text);
        u.lang = 'vi-VN';
        if (viVoice) { u.voice = viVoice; }
        u.onend = done;
        u.onerror = function (e) {
            if (e.error === 'not-allowed' && blocked) {
                voiceEl.textContent = 'The browser blocked the sound. Click anywhere on this page once.';
                voiceEl.className = 'error';
                blocked();
                return;
            }
            if (e.error !== 'canceled' && e.error !== 'interrupted') {
                voiceEl.textContent = 'Speech error: ' + e.error;
                voiceEl.className = 'error';
            }
            done();
        };
        window.speechSynthesis.cancel();
        window.speechSynthesis.speak(u);
    }

    function money(n) { return n.toLocaleString('vi-VN'); }

    function addLog(text) {
        if (logEl.firstChild && logEl.firstChild.className === 'hint') { logEl.innerHTML = ''; }
        var li = document.createElement('li');
        li.textContent = new Date().toLocaleTimeString('vi-VN') + '  ' + text;
        logEl.insertBefore(li, logEl.firstChild);
    }

    function ack(id) {
        var body = new URLSearchParams();
        body.append('id', id);
        return fetch(ackUrl, { method: 'POST', body: body, credentials: 'same-origin' });
    }

    function next() {
        if (speaking || queue.length === 0) { return; }
        var item = queue.shift();
        speaking = true;
        say('\u0110\u00e3 nh\u1eadn ' + item.amount + ' \u0111\u1ed3ng', function () {
            ack(item.id).catch(function () { }).then(function () {
                delete busy[item.id];
                addLog('\u0110\u00e3 nh\u1eadn ' + money(item.amount) + ' \u0111\u1ed3ng');
                speaking = false;
                pickVoice();
                next();
            });
        }, function () {
            queue.unshift(item);
            speaking = false;
        });
    }

    function setEnabled(on) {
        enabled = on;
        stateEl.textContent = enabled ? 'Speaker is on' : 'Speaker is off';
        stateEl.className = enabled ? 'badge PAID' : 'badge';
        onBtn.textContent = enabled ? 'Turn speaker off' : 'Turn speaker on';
    }

    function poll() {
        if (!enabled) { return; }
        fetch(pollUrl, { credentials: 'same-origin' })
            .then(function (res) {
                if (res.status === 401) { location.reload(); return []; }
                return res.ok ? res.json() : [];
            })
            .then(function (items) {
                items.forEach(function (item) {
                    if (!busy[item.id]) {
                        busy[item.id] = true;
                        queue.push(item);
                    }
                });
                next();
            })
            .catch(function () { });
    }

    onBtn.addEventListener('click', function () {
        setEnabled(!enabled);
        if (enabled) { say('Loa \u0111\u00e3 b\u1eadt', function () { }); }
    });

    document.addEventListener('click', function () { next(); });

    setEnabled(true);

    testBtn.addEventListener('click', function () {
        say('\u0110\u00e3 nh\u1eadn n\u0103m m\u01b0\u01a1i ngh\u00ecn \u0111\u1ed3ng', function () { });
    });

    setInterval(poll, 3000);
})();
