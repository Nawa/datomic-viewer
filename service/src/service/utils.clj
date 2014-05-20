(ns service.utils
  (:use clojure.pprint))

(defn prettify [structure]
  (with-out-str (clojure.pprint/pprint structure)))
