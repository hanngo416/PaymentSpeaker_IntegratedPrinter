(function () {
    var tip = document.getElementById('tip');
    if (!tip) { return; }
    document.querySelectorAll('.bar').forEach(function (g) {
        g.addEventListener('mousemove', function (e) {
            tip.textContent = g.getAttribute('data-tip');
            tip.style.display = 'block';
            tip.style.left = (e.clientX + 14) + 'px';
            tip.style.top = (e.clientY + 14) + 'px';
        });
        g.addEventListener('mouseleave', function () { tip.style.display = 'none'; });
    });
})();
