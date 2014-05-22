(ns service.core
  (:require [compojure.route :as route]
            [clojure.java.io :as io]
            [service.datomic :as datomic])
  (:use compojure.core
        compojure.handler
        ring.middleware.edn
        carica.core
        clojure.pprint))

(defn response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defn response-with-changed-session [data session & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)
   :session session})

(defn set-uri [uri session]
  (try
    (datomic/check-uri uri)
    (response-with-changed-session uri (assoc session :uri uri))
    (catch Exception e
      (response (.getMessage e) 500))))

(defn schema-new-attribute-params []
  {:db/valueType '(":db.type/keyword"  ":db.type/string"  ":db.type/boolean"
                   ":db.type/long"     ":db.type/bigint"  ":db.type/float"
                   ":db.type/double"   ":db.type/bigdec"  ":db.type/ref"
                   ":db.type/instant"  ":db.type/uuid"    ":db.type/uri"
                   ":db.type/bytes")
   :db.install/_attribute ":db.part/db"
   :db/id "(d/tempid :db.part/db)"})

(defn schema-new-attribute-create [uri attribute]
  (try
    (datomic/create-schema-attribute uri attribute)
    (response "Attribute successfully created")
    (catch Exception e
      (response (.getMessage e) 500))))

(defn logout [session]
  (response-with-changed-session "" (dissoc session :uri)))

(defroutes service-routes
  (GET "/uri" request (response (:uri (:session request))))
  (POST "/uri" {params :params session :session} (set-uri (:value params) session))
  (GET "/schema/new-attribute/params" [] (response (schema-new-attribute-params)))
  (POST "/schema/new-attribute/create"
        {params :params session :session}
        (schema-new-attribute-create (:uri session) (:value params)))
  (GET "/schema" request (response (datomic/get-schema (:uri (:session request))))))

(defroutes compojure-handler
  (GET "/" [] (slurp (io/resource "public/html/index.html")))
  (GET "/req" request (str request))
  (context "/service" [] service-routes)
  (GET "/logout" request (logout(:session request)))
  (route/resources "/")
  (route/files "/" {:root (config :external-resources)})
  (route/not-found "Not found!"))

(def app
  (-> compojure-handler
      site
      wrap-edn-params))
