(function () {
    var root = document.getElementById('guideArticle');
    if (!root) { return; }
    var base = root.getAttribute('data-ctx');
    var article = root.getAttribute('data-article');

    function post(kind, state, item, done) {
        var body = 'article=' + encodeURIComponent(article) + '&kind=' + kind + '&state=' + state;
        if (item !== null) { body += '&item=' + item; }
        var xhr = new XMLHttpRequest();
        xhr.open('POST', base + '/guide/progress');
        xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
        xhr.onload = function () {
            var data = null;
            try { data = JSON.parse(xhr.responseText); } catch (e) { data = null; }
            done(xhr.status === 200 && data && data.ok ? data : null);
        };
        xhr.onerror = function () { done(null); };
        xhr.send(body);
    }

    var count = document.getElementById('checkCount');
    var boxes = root.querySelectorAll('input[data-item]');
    Array.prototype.forEach.call(boxes, function (box) {
        box.addEventListener('change', function () {
            var wanted = box.checked;
            post('CHECKLIST', wanted ? 'CHECKED' : 'UNCHECKED', box.getAttribute('data-item'), function (data) {
                if (!data) {
                    box.checked = !wanted;
                    return;
                }
                if (count) { count.textContent = data.checked + '/' + data.total; }
            });
        });
    });

    function toggle(id, kind, onState, onLabel, offLabel) {
        var btn = document.getElementById(id);
        if (!btn) { return; }
        btn.addEventListener('click', function () {
            var on = btn.getAttribute('data-on') === '1';
            post(kind, on ? 'UNCHECKED' : onState, null, function (data) {
                if (!data) { return; }
                btn.setAttribute('data-on', on ? '0' : '1');
                btn.textContent = on ? offLabel : onLabel;
                btn.classList.toggle('primary', !on);
            });
        });
    }

    toggle('btnBookmark', 'BOOKMARK', 'SAVED', 'Saved', 'Save for later');
    toggle('btnRead', 'READ', 'CHECKED', 'Marked as read', 'Mark as read');
})();
