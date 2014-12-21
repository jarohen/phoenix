(ns phoenix-sample.service.handler
  (:require [phoenix-sample.service.db :as db]
            [bidi.ring :refer [make-handler]]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :refer [Lifecycle]]
            [medley.core :as m]
            [modular.ring :refer [WebRequestHandler]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [ring.util.response :refer [response status]]))

(def routes
  [["/object/" :oid] {:get :object-lookup
                      :put :object-set}])

(defn handlers [db]
  (->> {:object-lookup (fn [req]
                         (response (db/get-obj db (get-in req [:route-params :oid]))))
        
        :object-set (fn [req]
                      (future
                        (db/put-obj! db (get-in req [:route-params :oid]) (:body-params req)))
                      (-> (response "OK!")
                          (status 202)))}
       
       (m/map-vals #(wrap-restful-format % :formats [:edn :json-kw]))))

(defrecord AppHandler []
  Lifecycle
  (start [{:keys [db] :as this}]
    (log/info "starting handler with db" (pr-str db))

    (db/put-obj! db "foo" :bar)
    (println "Foo is:" (db/get-obj db "foo"))
    
    this)
  (stop [this] this)

  WebRequestHandler
  (request-handler [{:keys [db]}]
    (make-handler routes (handlers db))))
