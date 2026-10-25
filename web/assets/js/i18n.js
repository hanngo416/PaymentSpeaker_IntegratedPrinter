(function (root) {
    var COOKIE = 'tinky_lang';

    function readLang() {
        try {
            var m = document.cookie.match(/(?:^|; )tinky_lang=([^;]*)/);
            return m ? decodeURIComponent(m[1]) : 'en';
        } catch (e) {
            return 'en';
        }
    }

    function setLang(lang) {
        var maxAge = 60 * 60 * 24 * 365;
        document.cookie = COOKIE + '=' + (lang === 'vi' ? 'vi' : 'en') + '; path=/; max-age=' + maxAge + '; SameSite=Lax';
    }

    function squash(s) {
        return s.replace(/\s+/g, ' ');
    }

    function makeTranslator(dict) {
        var exact = {};
        var patterns = [];
        Object.keys(dict).forEach(function (key) {
            if (key.indexOf('{}') < 0) {
                exact[key] = dict[key];
                return;
            }
            var parts = key.split('{}');
            var src = parts.map(function (p) {
                return p.replace(/[.*+?^$()|[\]\\\/]/g, '\\$&').replace(/\{/g, '\\{').replace(/\}/g, '\\}');
            }).join('(.*?)');
            patterns.push({re: new RegExp('^' + src + '$'), to: dict[key], weight: key.length});
        });
        patterns.sort(function (a, b) {
            return b.weight - a.weight;
        });

        function tr(text) {
            if (!text) {
                return text;
            }
            var lead = text.match(/^\s*/)[0];
            var tail = text.match(/\s*$/)[0];
            var core = squash(text.trim());
            if (!core) {
                return text;
            }
            var hit = Object.prototype.hasOwnProperty.call(exact, core) ? exact[core] : null;
            if (hit === null) {
                for (var i = 0; i < patterns.length; i++) {
                    var m = core.match(patterns[i].re);
                    if (m) {
                        var n = 0;
                        hit = patterns[i].to.replace(/\{\}/g, function () {
                            n++;
                            var part = m[n];
                            return part === undefined ? '' : tr(part);
                        });
                        break;
                    }
                }
            }
            return hit === null ? text : lead + hit + tail;
        }
        return tr;
    }

    root.TinkyI18n = {makeTranslator: makeTranslator, setLang: setLang, readLang: readLang};

    if (typeof document === 'undefined') {
        return;
    }

    var lang = readLang();
    if (lang !== 'vi' || !root.TINKY_VI) {
        return;
    }
    var tr = makeTranslator(root.TINKY_VI);
    var ATTRS = ['placeholder', 'title', 'alt', 'aria-label', 'data-tip', 'data-title'];
    var SKIP = {SCRIPT: 1, STYLE: 1, TEXTAREA: 1, CODE: 1, PRE: 1};

    function skipNode(el) {
        while (el && el.nodeType === 1) {
            if (SKIP[el.nodeName] || el.hasAttribute('data-no-i18n')) {
                return true;
            }
            el = el.parentNode;
        }
        return false;
    }

    function fixText(node) {
        var v = node.nodeValue;
        if (!v || !v.trim()) {
            return;
        }
        var out = tr(v);
        if (out !== v) {
            node.nodeValue = out;
        }
    }

    function fixElement(el) {
        for (var i = 0; i < ATTRS.length; i++) {
            var v = el.getAttribute(ATTRS[i]);
            if (v) {
                var out = tr(v);
                if (out !== v) {
                    el.setAttribute(ATTRS[i], out);
                }
            }
        }
        if (el.nodeName === 'INPUT' && (el.type === 'submit' || el.type === 'button') && el.value) {
            var ov = tr(el.value);
            if (ov !== el.value) {
                el.value = ov;
            }
        }
    }

    function walk(node) {
        if (node.nodeType === 3) {
            if (!skipNode(node.parentNode)) {
                fixText(node);
            }
            return;
        }
        if (node.nodeType !== 1 || skipNode(node)) {
            return;
        }
        fixElement(node);
        for (var c = node.firstChild; c; c = c.nextSibling) {
            walk(c);
        }
    }

    var nativeConfirm = root.confirm;
    root.confirm = function (message) {
        return nativeConfirm.call(root, tr(String(message)));
    };
    var nativeAlert = root.alert;
    root.alert = function (message) {
        return nativeAlert.call(root, tr(String(message)));
    };

    document.documentElement.lang = 'vi';
    document.title = tr(document.title);
    walk(document.body);

    if (root.MutationObserver) {
        new MutationObserver(function (records) {
            for (var i = 0; i < records.length; i++) {
                var r = records[i];
                if (r.type === 'characterData') {
                    if (!skipNode(r.target.parentNode)) {
                        fixText(r.target);
                    }
                } else if (r.type === 'attributes') {
                    if (!skipNode(r.target)) {
                        fixElement(r.target);
                    }
                } else {
                    for (var j = 0; j < r.addedNodes.length; j++) {
                        walk(r.addedNodes[j]);
                    }
                }
            }
        }).observe(document.body, {childList: true, subtree: true, characterData: true, attributes: true, attributeFilter: ATTRS});
    }
})(typeof window !== 'undefined' ? window : globalThis);
