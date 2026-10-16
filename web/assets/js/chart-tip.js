(function () {
    var tip = document.getElementById('tip');
    if (!tip) { return; }
    if (tip.parentElement !== document.body) {
        document.body.appendChild(tip);
    }
    document.querySelectorAll('.bar').forEach(function (g) {
        g.addEventListener('mousemove', function (e) {
            var text = g.getAttribute('data-tip');
            if (!text) { return; }
            tip.textContent = text;
            tip.style.display = 'block';
            var tipW = tip.offsetWidth || 220;
            var tipH = tip.offsetHeight || 50;
            var x = e.clientX + 14;
            var y = e.clientY + 14;
            if (x + tipW > window.innerWidth - 12) {
                x = Math.max(8, e.clientX - tipW - 14);
            }
            if (y + tipH > window.innerHeight - 12) {
                y = Math.max(8, e.clientY - tipH - 14);
            }
            tip.style.left = x + 'px';
            tip.style.top = y + 'px';
        });
        g.addEventListener('mouseleave', function () {
            tip.style.display = 'none';
        });
    });
})();
