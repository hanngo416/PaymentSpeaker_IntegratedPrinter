(function () {
    var PAPERS = {
        '58': { page: '58mm auto', slip: '52mm', pad: '2mm 1.5mm 3mm', font: '10px', label: 'POS 58mm' },
        '76': { page: '76mm 130mm', slip: '72mm', pad: '3mm 2mm 3mm', font: '10.5px', label: 'POS 76×130mm' },
        '80': { page: '76mm 130mm', slip: '72mm', pad: '3mm 2mm 3mm', font: '10.5px', label: 'POS 76×130mm' },
        'a4': { page: 'A4', slip: '110mm', pad: '8mm 8mm 10mm', font: '13px', label: 'A4 sheet' }
    };

    var key = '80';
    try {
        var saved = localStorage.getItem('spk-paper');
        if (PAPERS[saved]) { key = saved; }
    } catch (e) { }
    var paper = PAPERS[key];

    var css = '@page{size:' + paper.page + ';margin:' + (key === 'a4' ? '10mm' : '0') + '}'
        + '@media print{.receipt-slip{width:' + paper.slip + ' !important;padding:' + paper.pad + ' !important;'
        + (paper.font ? 'font-size:' + paper.font + ' !important;' : '')
        + (key === '76' || key === '80' ? 'height:126mm !important;max-height:126mm !important;display:flex !important;flex-direction:column !important;justify-content:space-between !important;' : '')
        + 'border:none !important;box-shadow:none !important;border-radius:0 !important;}}';
    var style = document.createElement('style');
    style.appendChild(document.createTextNode(css));
    document.head.appendChild(style);

    var badge = document.getElementById('paper-label');
    if (badge) { badge.textContent = paper.label; }

    var again = document.getElementById('print-again');
    if (again) {
        again.addEventListener('click', function () { window.print(); });
    }
    // Opens the print dialog as soon as the receipt is shown
    window.print();
})();
