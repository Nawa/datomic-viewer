DatomicViewer = {
    createCodeMirror: function(selector, params) {
        var defaultParams = {
            lineNumbers: true,
            autoCloseBrackets: true,
            matchBrackets: true,
            showCursorWhenSelecting: true,
            theme: "monokai",
            mode: "text/x-clojure"
        };
        var el = $(selector);
        var codemirror = CodeMirror.fromTextArea(el[0], $.extend(defaultParams, params));
        el.data("codemirror", codemirror);
        return codemirror;
    }
}