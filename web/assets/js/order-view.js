(function () {
    var box = document.getElementById('box');
    if (!box) {
        return;
    }

    var code = box.dataset.code;
    var paidKey = 'paid-amount-' + code;

    function speak(text) {
        if (!window.speechSynthesis) { return; }
        var u = new SpeechSynthesisUtterance(text);
        u.lang = 'vi-VN';
        var voices = window.speechSynthesis.getVoices();
        for (var i = 0; i < voices.length; i++) {
            if (/^vi/i.test(voices[i].lang.replace('_', '-'))) { u.voice = voices[i]; break; }
        }
        window.speechSynthesis.speak(u);
    }

    if (box.dataset.status === 'PAID') {
        try {
            var amount = sessionStorage.getItem(paidKey);
            if (amount) {
                sessionStorage.removeItem(paidKey);
                speak('\u0110\u00e3 nh\u1eadn ' + amount + ' \u0111\u1ed3ng');
            }
        } catch (e) { }
        return;
    }
    if (box.dataset.status !== 'PENDING') {
        return;
    }

    var statusUrl = box.dataset.statusUrl;
    var endAt = Date.now() + parseInt(box.dataset.remaining, 10) * 1000;
    var countdown = document.getElementById('countdown');

    function pad(n) { return n < 10 ? '0' + n : '' + n; }

    function tick() {
        var left = Math.max(0, Math.round((endAt - Date.now()) / 1000));
        countdown.textContent = pad(Math.floor(left / 60)) + ':' + pad(left % 60);
        if (left === 0) {
            location.reload();
        }
    }

    function poll() {
        fetch(statusUrl + '?code=' + encodeURIComponent(code), { credentials: 'same-origin' })
            .then(function (res) {
                if (res.status === 401) { location.reload(); return null; }
                return res.ok ? res.json() : null;
            })
            .then(function (data) {
                if (data && data.status !== 'PENDING') {
                    if (data.status === 'PAID') {
                        try { sessionStorage.setItem(paidKey, data.amount); } catch (e) { }
                    }
                    location.reload();
                }
            })
            .catch(function () { });
    }

    tick();
    setInterval(tick, 1000);
    setInterval(poll, 1000);
})();
