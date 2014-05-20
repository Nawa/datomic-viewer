(ns service.datomic
  (:require
   [datomic.api :as d]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [service.utils :as utils])
  (:use clojure.pprint))


(defn connect-to-datomic [uri]
  (d/create-database uri)
  (let [conn (d/connect uri)]
    (fn [] (d/db conn))))

(defn get-schema [uri]
  (let [db (connect-to-datomic uri)]
    (->> (d/q '[:find ?e
           :where [?e :db/valueType]]
              (db))
         (map #(d/touch (d/entity (db) (first %))))
         (utils/prettify))))


;;   (utils/prettify '({:key1 1 :key2 2}
;;                            {:key1 1 :key2 2}
;;                            {:key1 1 :key2 2}
;;                            {:key1 1 :key2 2}
;;                            {:key1 1 :key2 2}
;;                            {:key1 1 :key2 2}
;;                            {:key1 1 :key2 2}))


;(get-schema "datomic:free://localhost:4335/datomic-test")

;; (d/q schema-tx (d/db conn))
;; (map (comp id2entity first))
;; (map entity2schema)



