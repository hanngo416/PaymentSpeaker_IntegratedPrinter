// Payment popup + computer speaker. Loaded on every page for cashier, manager and admin (layout-bottom.jspf).
// Polls the server; the oldest unconfirmed payment shows in a popup and is read aloud; OK confirms it.
(function () {
    var box = document.getElementById('announcer');
    var popup = document.getElementById('payPopup');
    if (!box || !popup) { return; }

    var pollUrl = box.dataset.pollUrl;
    var ackUrl = box.dataset.ackUrl;
    var printUrl = box.dataset.printUrl;
    var okBtn = document.getElementById('payOk');
    var SOUND_KEY = 'spk-sound';
    var HISTORY_KEY = 'spk-history';
    var ACKED_KEY = 'spk-acked';
    var SAID_PREFIX = 'spk-said-';
    var PRINTED_PREFIX = 'spk-printed-';

    var queue = [];        // payments not confirmed yet; queue[0] is the one in the popup
    var confirmed = {};    // ids confirmed in this page, so a slow poll cannot bring them back
    var viVoice = null;
    var voiceText = '';
    var pendingSpeech = null;

    function load(key) { try { return localStorage.getItem(key); } catch (e) { return null; } }
    function save(key, value) { try { localStorage.setItem(key, value); } catch (e) { } }
    function drop(key) { try { localStorage.removeItem(key); } catch (e) { } }

    function soundOn() { return load(SOUND_KEY) !== 'off'; }

    function pickVoice() {
        if (!window.speechSynthesis) { voiceText = 'This browser cannot read aloud.'; return; }
        var voices = window.speechSynthesis.getVoices();
        viVoice = null;
        for (var i = 0; i < voices.length; i++) {
            if (/^vi/i.test(voices[i].lang.replace('_', '-'))) { viVoice = voices[i]; break; }
        }
        voiceText = viVoice
            ? 'Voice: ' + viVoice.name + ' (' + viVoice.lang + ')'
            : 'No Vietnamese voice installed. Windows: Settings > Time & language > Speech > Add voices > Vietnamese, then restart the browser.';
    }
    pickVoice();
    if (window.speechSynthesis) { window.speechSynthesis.onvoiceschanged = pickVoice; }

    function chime() {
        var Ctx = window.AudioContext || window.webkitAudioContext;
        if (!Ctx) { return; }
        try {
            var ctx = new Ctx();
            [880, 1320].forEach(function (freq, i) {
                var osc = ctx.createOscillator();
                var gain = ctx.createGain();
                var start = ctx.currentTime + i * 0.25;
                osc.frequency.value = freq;
                gain.gain.setValueAtTime(0.3, start);
                gain.gain.exponentialRampToValueAtTime(0.001, start + 0.5);
                osc.connect(gain);
                gain.connect(ctx.destination);
                osc.start(start);
                osc.stop(start + 0.5);
            });
            setTimeout(function () { ctx.close(); }, 1200);
        } catch (e) { }
    }

    function say(text) {
        if (!viVoice) { chime(); return; }
        if (!window.speechSynthesis) { return; }
        var u = new SpeechSynthesisUtterance(text);
        u.lang = 'vi-VN';
        if (viVoice) { u.voice = viVoice; }
        u.onerror = function (e) {
            // The browser only lets a page speak after the user clicked on it once; keep the text for the next click
            if (e.error === 'not-allowed') { pendingSpeech = text; }
        };
        window.speechSynthesis.cancel();
        window.speechSynthesis.speak(u);
    }

    function announceText(amount) { return '\u0110\u00e3 nh\u1eadn ' + amount + ' \u0111\u1ed3ng'; }

    function speakFor(item) {
        if (!soundOn()) { return; }
        // Several tabs show the same popup but only the first one reads it
        var key = SAID_PREFIX + item.id;
        if (load(key)) { return; }
        save(key, String(Date.now()));
        say(announceText(item.amount));
    }

    // Auto-print: the receipt is posted into a hidden frame like the Print button does, so it is marked printed and
    // audited, and the receipt page opens the print dialog by itself. Only the first tab does it.
    function autoPrint(item) {
        if (load('spk-autoprint') !== 'on' || !printUrl || !item.order) { return; }
        var key = PRINTED_PREFIX + item.id;
        if (load(key)) { return; }
        save(key, String(Date.now()));
        var frame = document.getElementById('printFrame');
        if (!frame) {
            frame = document.createElement('iframe');
            frame.id = 'printFrame';
            frame.name = 'printFrame';
            frame.style.display = 'none';
            document.body.appendChild(frame);
        }
        var form = document.createElement('form');
        form.method = 'post';
        form.action = printUrl;
        form.target = 'printFrame';
        var field = document.createElement('input');
        field.type = 'hidden';
        field.name = 'id';
        field.value = item.id;
        form.appendChild(field);
        document.body.appendChild(form);
        form.submit();
        document.body.removeChild(form);
    }

    function money(n) { return n.toLocaleString('vi-VN'); }

    function show() {
        if (!popup.hidden || queue.length === 0) { return; }
        var item = queue[0];
        document.getElementById('payAmount').textContent = money(item.amount) + ' VND';
        document.getElementById('payOrder').textContent = item.order || '-';
        document.getElementById('payTime').textContent = item.time;
        document.getElementById('payMore').textContent = queue.length > 1 ? (queue.length - 1) + ' more payment(s) waiting' : '';
        popup.hidden = false;
        okBtn.focus();
        if (!item.spoken) {
            item.spoken = true;
            speakFor(item);
            autoPrint(item);
        }
    }

    function remove(id) {
        for (var i = 0; i < queue.length; i++) {
            if (queue[i].id === id) {
                queue.splice(i, 1);
                if (i === 0) { popup.hidden = true; }
                return;
            }
        }
    }

    function addHistory(item) {
        var list = [];
        try { list = JSON.parse(load(HISTORY_KEY) || '[]'); } catch (e) { list = []; }
        list.unshift({ at: Date.now(), amount: item.amount, order: item.order });
        save(HISTORY_KEY, JSON.stringify(list.slice(0, 30)));
        document.dispatchEvent(new Event('announcer-history'));
    }

    function confirm() {
        if (queue.length === 0) { return; }
        var item = queue[0];
        confirmed[item.id] = true;
        remove(item.id);
        var body = new URLSearchParams();
        body.append('id', item.id);
        fetch(ackUrl, { method: 'POST', body: body, credentials: 'same-origin', keepalive: true }).catch(function () { });
        drop(SAID_PREFIX + item.id);
        save(ACKED_KEY, item.id + '@' + Date.now());
        addHistory(item);
        show();
    }

    function poll() {
        fetch(pollUrl, { credentials: 'same-origin' })
            .then(function (res) {
                if (res.status === 401) { location.reload(); return null; }
                return res.ok ? res.json() : null;
            })
            .then(function (items) {
                if (!items) { return; }
                var seen = {};
                items.forEach(function (item) {
                    seen[item.id] = true;
                    if (confirmed[item.id]) { return; }
                    for (var i = 0; i < queue.length; i++) {
                        if (queue[i].id === item.id) { return; }
                    }
                    queue.push(item);
                });
                // Confirmed somewhere else (another tab or another cashier): take it off this screen too
                queue.slice().forEach(function (item) {
                    if (!seen[item.id]) { remove(item.id); }
                });
                show();
            })
            .catch(function () { });
    }

    okBtn.addEventListener('click', confirm);

    function onGesture() {
        if (pendingSpeech) {
            var text = pendingSpeech;
            pendingSpeech = null;
            say(text);
        }
    }
    document.addEventListener('click', onGesture);
    document.addEventListener('keydown', onGesture);

    window.addEventListener('storage', function (e) {
        if (e.key === ACKED_KEY && e.newValue) {
            remove(parseInt(e.newValue, 10));
            show();
        }
    });

    window.Announcer = {
        soundOn: soundOn,
        setSound: function (on) { save(SOUND_KEY, on ? 'on' : 'off'); },
        test: function () { say('\u0110\u00e3 nh\u1eadn n\u0103m m\u01b0\u01a1i ngh\u00ecn \u0111\u1ed3ng'); },
        voiceText: function () { pickVoice(); return voiceText; },
        history: function () {
            try { return JSON.parse(load(HISTORY_KEY) || '[]'); } catch (e) { return []; }
        }
    };

    poll();
    setInterval(poll, 3000);
})();
