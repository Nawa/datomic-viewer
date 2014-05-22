(ns service.datomic
  (:require
   [datomic.api :as d]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [service.utils :as utils]
   [clojure.edn :as edn])
  (:use clojure.pprint))

(defn ^:private connect-to-datomic [uri]
  (d/create-database uri)
  (d/connect uri))

(defn ^:private db [uri]
  (fn [] (d/db (connect-to-datomic uri))))

(defn check-uri [uri]
  ((db uri)))

(defn get-schema [uri]
  (->> (d/q '[:find ?e
              :where [?e :db/valueType]]
            ((db uri)))
         (map #(d/touch (d/entity ((db uri)) (first %))))
         (utils/prettify)))

;; (defn create-schema-attribute [uri attribute]
;;   (let [prepared-attribute (map #(assoc % :db/id
;;                                    (let [id (:db/id %)]
;;                                      (if (list? id)
;;                                        (do
;;                                          (binding [*ns* 'service.datomic]
;;                                            (eval id)))
;;                                        id))) (edn/read-string attribute))]
;;     @(d/transact (connect-to-datomic uri)  prepared-attribute)))

(defn create-schema-attribute [uri attribute]
  (let [prepared-attribute (map #(assoc % :db/id (d/tempid :db.part/db)) (edn/read-string attribute))]
    @(d/transact (connect-to-datomic uri)  prepared-attribute)))


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



