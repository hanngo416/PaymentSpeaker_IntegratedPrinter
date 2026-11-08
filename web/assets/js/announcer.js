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

    var queue = [];
    var confirmed = {};
    var viVoice = null;
    var voiceText = '';
    var pendingSpeech = null;

    function load(key) { try { return localStorage.getItem(key); } catch (e) { return null; } }
    function save(key, value) { try { localStorage.setItem(key, value); } catch (e) { } }
    function drop(key) { try { localStorage.removeItem(key); } catch (e) { } }

    function isVi() { return /(?:^|; )tinky_lang=vi/.test(document.cookie); }
    function speechLang() { return isVi() ? 'vi-VN' : 'en-US'; }

    function soundOn() { return load(SOUND_KEY) !== 'off'; }

    function pickVoice() {
        if (!window.speechSynthesis) { voiceText = 'This browser cannot read aloud.'; return; }
        var voices = window.speechSynthesis.getVoices();
        viVoice = null;
        for (var i = 0; i < voices.length; i++) {
            if (new RegExp('^' + (isVi() ? 'vi' : 'en'), 'i').test(voices[i].lang.replace('_', '-'))) { viVoice = voices[i]; break; }
        }
        voiceText = viVoice
            ? 'Voice: ' + viVoice.name + ' (' + viVoice.lang + ')'
            : (isVi()
                ? 'No Vietnamese voice installed. Windows: Settings > Time & language > Speech > Add voices > Vietnamese, then restart the browser.'
                : 'No English voice installed. Windows: Settings > Time & language > Speech > Add voices > English, then restart the browser.');
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
        u.lang = speechLang();
        if (viVoice) { u.voice = viVoice; }
        u.onerror = function (e) {
            if (e.error === 'not-allowed') {
                pendingSpeech = text;
                document.getElementById('payMore').textContent = 'Click anywhere on this page to hear the announcement';
            }
        };
        window.speechSynthesis.cancel();
        window.speechSynthesis.speak(u);
    }

    function announceText(amount) {
        return isVi() ? '\u0110\u00e3 nh\u1eadn ' + amount + ' \u0111\u1ed3ng' : 'Received ' + amount + ' dong';
    }

    function speakFor(item) {
        if (!soundOn()) { return; }
        var key = SAID_PREFIX + item.id;
        if (load(key)) { return; }
        save(key, String(Date.now()));
        say(announceText(item.amount));
    }

    function printReceipt(item) {
        if (load('spk-autoprint') === 'off' || !printUrl || !item.order) { return; }
        var body = new URLSearchParams();
        body.append('id', item.id);
        body.append('silent', '1');
        fetch(printUrl, { method: 'POST', body: body, credentials: 'same-origin', keepalive: true }).catch(function () { });
    }

    function money(n) { return n.toLocaleString(isVi() ? 'vi-VN' : 'en-US'); }

    function showMore() {
        document.getElementById('payMore').textContent = queue.length > 1 ? (queue.length - 1) + ' more payment(s) waiting' : '';
    }

    function show() {
        if (!popup.hidden || queue.length === 0) { return; }
        var item = queue[0];
        document.getElementById('payAmount').textContent = money(item.amount) + ' VND';
        document.getElementById('payOrder').textContent = item.order || '-';
        document.getElementById('payTime').textContent = item.time;
        showMore();
        popup.hidden = false;
        okBtn.focus();
        if (!item.spoken) {
            item.spoken = true;
            speakFor(item);
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
        fetch(ackUrl, { method: 'POST', body: body, credentials: 'same-origin', keepalive: true })
            .then(function (res) { if (res.status === 204) { printReceipt(item); } })
            .catch(function () { });
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
            showMore();
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
        test: function () { say(announceText(50000)); },
        voiceText: function () { pickVoice(); return voiceText; },
        history: function () {
            try { return JSON.parse(load(HISTORY_KEY) || '[]'); } catch (e) { return []; }
        }
    };

    poll();
    setInterval(poll, 3000);
})();
