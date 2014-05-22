(ns client.ladda
  "Helper for ladda buttons loading http://msurguy.github.io/ladda-bootstrap/"
  (:require [enfocus.core :as ef])
  (:require-macros [enfocus.macros :as em]))

(defn start [selector-or-element]
  (let [button (to-element selector-or-element)
        ladda (js/Ladda.create button)]
    (.start ladda)
    (set! (.-ladda button) ladda)
    ladda))

(defn stop [selector-or-element]
  (let [button (to-element selector-or-element)
        ladda (.-ladda button)]
    (.stop ladda)
    (set! (.-ladda button) nil))
  nil)

(defn ^:private to-element[selector-or-element]
  (if (string? selector-or-element)
    (.get (js/$ selector-or-element) 0)
    selector-or-element))
