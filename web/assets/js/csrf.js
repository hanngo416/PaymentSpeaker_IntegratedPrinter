(function () {
    var meta = document.querySelector('meta[name="csrf-token"]');
    var token = meta ? meta.getAttribute('content') : '';
    if (!token) {
        return;
    }

    function sameOrigin(url) {
        try {
            return new URL(url, window.location.href).origin === window.location.origin;
        } catch (e) {
            return false;
        }
    }

    function isUnsafe(method) {
        var m = (method || 'GET').toUpperCase();
        return m !== 'GET' && m !== 'HEAD' && m !== 'OPTIONS';
    }

    if (window.fetch) {
        var nativeFetch = window.fetch;
        window.fetch = function (input, init) {
            var url = typeof input === 'string' ? input : (input && input.url);
            var method = (init && init.method) || (input && input.method) || 'GET';
            if (isUnsafe(method) && sameOrigin(url)) {
                init = init || {};
                var headers = new Headers(init.headers || (input && input.headers) || {});
                headers.set('X-CSRF-Token', token);
                init.headers = headers;
            }
            return nativeFetch.call(this, input, init);
        };
    }

    var nativeOpen = XMLHttpRequest.prototype.open;
    var nativeSend = XMLHttpRequest.prototype.send;
    XMLHttpRequest.prototype.open = function (method, url) {
        this._csrfNeeded = isUnsafe(method) && sameOrigin(url);
        return nativeOpen.apply(this, arguments);
    };
    XMLHttpRequest.prototype.send = function () {
        if (this._csrfNeeded) {
            this.setRequestHeader('X-CSRF-Token', token);
        }
        return nativeSend.apply(this, arguments);
    };

    function protect(form) {
        if (!form || (form.method || '').toLowerCase() !== 'post') {
            return;
        }
        if (!sameOrigin(form.getAttribute('action') || window.location.href)) {
            return;
        }
        if ((form.enctype || '').toLowerCase() === 'multipart/form-data') {
            var action = form.getAttribute('action') || window.location.pathname;
            if (action.indexOf('_csrf=') < 0) {
                form.setAttribute('action', action + (action.indexOf('?') < 0 ? '?' : '&') + '_csrf=' + encodeURIComponent(token));
            }
            return;
        }
        if (!form.querySelector('input[name="_csrf"]')) {
            var input = document.createElement('input');
            input.type = 'hidden';
            input.name = '_csrf';
            input.value = token;
            form.appendChild(input);
        }
    }

    function protectAll() {
        var forms = document.forms;
        for (var i = 0; i < forms.length; i++) {
            protect(forms[i]);
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', protectAll);
    } else {
        protectAll();
    }
    document.addEventListener('submit', function (e) {
        protect(e.target);
    }, true);
})();
