(ns phoenix-sample.service.handler
  (:require [phoenix-sample.service.server :as s]
            [phoenix-sample.service.db :as db]
            [ring.util.response :refer [response status content-type]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [bidi.ring :refer [make-handler]]
            [com.stuartsierra.component :refer [Lifecycle]]))

(def routes
  [["/object/" :oid] {:get :object-lookup
                      :put :object-set}])

(defn handlers [db]
  {:object-lookup (fn [req]
                    (response (db/get-obj db (get-in req [:route-params :oid]))))

   :object-set (fn [req]
                 (future
                   (db/put-obj! db (get-in req [:route-params :oid]) (:body-params req)))
                 (-> (response "OK!")
                     (status 202)))})

(defrecord AppHandler [opts]
  Lifecycle
  (start [{:keys [db] :as this}]
    (println "starting handler with db:" (pr-str db))

    (db/put-obj! db :foo :bar)
    (println "Foo is:" (db/get-obj db :foo))
    
    this)
  (stop [this] this)

  s/WebHandler
  (make-handler [{:keys [db] :as opts}]
    (-> (make-handler routes (handlers db))
        (wrap-restful-format :formats [:edn]))))
