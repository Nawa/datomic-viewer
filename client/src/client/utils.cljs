(ns client.utils
  (:require [enfocus.core :as ef])
  (:require-macros [enfocus.macros :as em]))

(defn exists? [jq-selector]
  (> (.size (js/$ jq-selector)) 0))
