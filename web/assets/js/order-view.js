(function () {
    var box = document.getElementById('box');
    if (!box) {
        return;
    }

    var code = box.dataset.code;

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
                    location.reload();
                }
            })
            .catch(function () { });
    }

    tick();
    setInterval(tick, 1000);
    setInterval(poll, 1000);
})();
