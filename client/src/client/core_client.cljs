(ns client.core
  (:require [enfocus.core :as ef]
            [enfocus.events :as events :refer (listen)]
            [ajax.core :refer [GET POST]]
            [client.utils :as utils]
            [client.ladda :as ladda])
  (:require-macros [enfocus.macros :as em]))

;;;;;;;;;;;;;;;;;;;
;; snippets
;;;;;;;;;;;;;;;;;;;

(em/defsnippet login-navbar "/html/login.html" ".navbar" [])
(em/defsnippet login-form "/html/login.html" ".form-horizontal" [])
(em/defsnippet alert "/html/login.html" "#main-alert" [])
(em/defsnippet logged-navbar "/html/schema.html" ".navbar" [])
(em/defsnippet schema-attribute-create-form "/html/schema.html" "#schema-attribute-create" []
               "#schema-attribute-create" (ef/set-style :display "none"))
(em/defsnippet schema-view "/html/schema.html" "#schema-view" []
               "#add-new-attr-btn" (listen :click show-schema-attribute-create-form))

;;;;;;;;;;;;;;;;;;;
;; errors
;;;;;;;;;;;;;;;;;;;
(defn error-handler [{:keys [status status-text]}]
  (.log js/console (str "Something bad happened: " status " " status-text)))

(defn alert-error-handler [{:keys [response]}]
  (if (not (utils/exists? "#main-alert"))
    (ef/at ".navbar" (ef/after (alert))))
  (ef/at "#alert-content" (ef/content response)))

;;;;;;;;;;;;;;;;;;;
;; index
;;;;;;;;;;;;;;;;;;;
(defn show-index-depends-uri [uri]
  (if (clojure.string/blank? uri)
    (show-login-page)
    (show-logged-page uri)))

;;;;;;;;;;;;;;;;;;;
;; login
;;;;;;;;;;;;;;;;;;;
(defn show-login-page []
  (ef/at "#main-container" (ef/content ""))
  (ef/at "#main-container" (ef/append (login-navbar)))
  (ef/at "#main-container" (ef/append (login-form))))

(defn try-login-success-handler [response]
  (ladda/stop "#login-button")
  (show-index-depends-uri response))

(defn try-login-error-handler [response]
  (ladda/stop "#login-button")
  (alert-error-handler response))

(defn ^:export try-login [button]
  (ladda/start button)
  (POST "/service/uri"
       {:params {:value (ef/from "#uri" (ef/read-form-input))}
        :handler try-login-success-handler
        :error-handler try-login-error-handler}))

;;;;;;;;;;;;;;;;;;;
;; logged
;;;;;;;;;;;;;;;;;;;

(def new-attribute-params (atom {}))

(defn show-logged-page [uri]
  (ef/at "#main-container" (ef/do-> (ef/content "")
                                    (ef/append (logged-navbar))
                                    (ef/append (schema-attribute-create-form))))
  (define-new-attribute-params)
  (init-listeners-schema-attribute-create-form)
  (.iCheck (js/$ ".checkbox input") (js* "{checkboxClass: 'icheckbox_flat', increaseArea: '20%'}"))
  (.iCheck (js/$ ".radio input") (js* "{radioClass: 'iradio_flat', increaseArea: '20%'}"))
  (js/DatomicViewer.createCodeMirror "#schema-attribute-create-textarea")
  (ef/at "#main-container" (ef/append (schema-view)))
  (js/DatomicViewer.createCodeMirror "#schema-view-textarea" (js* "{readOnly: true}"))
  (try-load-schema-overall))

(defn show-schema-attribute-create-form [event]
  (.slideToggle (js/$ "#schema-attribute-create") 300)
  (.refresh (.data (js/$ "#schema-attribute-create-textarea") "codemirror")))

(defn init-listeners-schema-attribute-create-form []
  (ef/at "#ident" (events/listen :keyup schema-attribute-create-textarea-update))
  (ef/at "#valueType" (events/listen :change schema-attribute-create-textarea-update))
  (.on (js/$ "input[name='cardinality']") "ifChanged" #(schema-attribute-create-textarea-update))
  (.on (js/$ "input[name='isComponent']") "ifChanged" #(schema-attribute-create-textarea-update)))

(defn fill-schema-attribute-create-form []
  (ef/at "#valueType" (ef/content ""))
  (map #(ef/at "#valueType" (ef/append (str "<option value='" % "'>" % "</option>"))) (:db/valueType @new-attribute-params)))

(defn schema-attribute-create-textarea-update []
  (let [values (ef/from "#schema-attribute-create form" (ef/read-form))
        res (str
             "[{:db/ident        " (:ident values) "\n"
             "  :db/valueType    " (:valueType values) "\n"
             "  :db/cardinality  " (:cardinality values) "\n"
             (if (nil? (:isComponent values))
               ""
               "  :db/isComponent  true\n")
             "  :db/id           (d/tempid :db.part/db)\n"
             "  :db.install/_attribute :db.part/db}]")]
    (.setValue (.data (js/$ "#schema-attribute-create-textarea") "codemirror") res)))

(defn try-send-schema-new-attribute-create-success-handler [response]
  (ladda/stop "#send-schema-new-attribute-create")
  (try-load-schema-overall))

(defn try-send-schema-new-attribute-create-error-handler [response]
  (ladda/stop "#send-schema-new-attribute-create")
  (alert-error-handler response))

(defn ^:export try-send-schema-new-attribute-create [button]
  (ladda/start button)
  (POST "/service/schema/new-attribute/create"
       {:params {:value (.getValue (.data (js/$ "#schema-attribute-create-textarea") "codemirror"))}
        :handler try-send-schema-new-attribute-create-success-handler
        :error-handler try-send-schema-new-attribute-create-error-handler}))

(defn define-new-attribute-params []
  (GET "/service/schema/new-attribute/params"
       {:handler (fn [params](
                              (swap! new-attribute-params (fn [prev] params))
                              (fill-schema-attribute-create-form)))
        :error-handler error-handler}))

(defn try-load-schema-overall []
  (GET "/service/schema"
       {:handler try-load-schema-overall-handler
        :error-handler error-handler}))

(defn try-load-schema-overall-handler [response]
  (.setValue (.data (js/$ "#schema-view-textarea") "codemirror") response))

(defn ^:export logout []
  (GET "/logout"
       {:handler (fn []
                   (show-index-depends-uri ""))
        :error-handler error-handler}))

(defn start []
  (GET "/service/uri"
       {:handler show-index-depends-uri
        :error-handler error-handler}))

(set! (.-onload js/window) #(em/wait-for-load (start)))
