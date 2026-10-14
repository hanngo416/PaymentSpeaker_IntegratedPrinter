(function () {
    var again = document.getElementById('print-again');
    if (again) {
        again.addEventListener('click', function () { window.print(); });
    }
    // Opens the print dialog as soon as the receipt is shown
    window.print();
})();
