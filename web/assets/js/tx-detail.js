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
        field('mOrder', d.order);
        field('mContent', d.content);
        var statusEl = document.getElementById('mStatus') || document.getElementById('mLabel');
        var statusVal = d.status || d.label;
        if (statusEl) {
            statusEl.textContent = statusVal;
            statusEl.className = 'pill ' + statusVal;
        }
        var printWrap = document.getElementById('mPrintWrapper');
        var printId = document.getElementById('modalPrintTransId');
        if (printWrap && printId) {
            if (d.canPrint === '1' && d.id) {
                printId.value = d.id;
                printWrap.style.display = 'flex';
            } else {
                printWrap.style.display = 'none';
            }
        }
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
        row.addEventListener('click', function (e) {
            if (e.target.closest('button, form, a, input, select')) {
                return;
            }
            open(row);
        });
        row.addEventListener('keydown', function (e) {
            if (e.key === 'Enter' || e.key === ' ') {
                if (e.target.closest('button, form, a, input, select')) {
                    return;
                }
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
