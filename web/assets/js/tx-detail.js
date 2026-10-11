(function () {
    var backdrop = document.getElementById('txModal');
    if (!backdrop) { return; }
    var closeBtn = document.getElementById('txModalClose');
    var rows = document.querySelectorAll('.tx-row');
    var lastRow = null;

    function field(id, value) {
        document.getElementById(id).textContent = value;
    }

    function open(row) {
        var d = row.dataset;
        field('mAmount', d.amount + ' VND');
        field('mBankId', d.bankId);
        field('mTime', d.time);
        field('mDevice', d.device);
        field('mOrder', d.order);
        field('mContent', d.content);
        var label = document.getElementById('mLabel');
        label.textContent = d.label;
        label.className = 'pill ' + d.label;
        lastRow = row;
        backdrop.hidden = false;
        document.body.style.overflow = 'hidden';
        closeBtn.focus();
    }

    function close() {
        backdrop.hidden = true;
        document.body.style.overflow = '';
        if (lastRow) { lastRow.focus(); }
    }

    rows.forEach(function (row) {
        row.addEventListener('click', function () { open(row); });
        row.addEventListener('keydown', function (e) {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                open(row);
            }
        });
    });
    closeBtn.addEventListener('click', close);
    backdrop.addEventListener('click', function (e) {
        if (e.target === backdrop) { close(); }
    });
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && !backdrop.hidden) { close(); }
    });
})();
