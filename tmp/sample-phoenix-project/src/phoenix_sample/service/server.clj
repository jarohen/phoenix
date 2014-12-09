(ns phoenix-sample.service.server
  (:require [com.stuartsierra.component :as c]
            [org.httpkit.server :refer [run-server]]))

(defprotocol WebHandler
  (make-handler [_]))

(defrecord SimpleServer []
  c/Lifecycle
  (start [{:keys [handler port] :as this}]
    (println "Starting web server on port" port)
    (assoc this
      :server (run-server (make-handler handler) {:port port})))
  
  (stop [{:keys [server] :as this}]
    (when server
      (println "Stopping web server")
      (server))
    (dissoc this :server)))

(defn make-server [{:keys [port]}]
  (assoc (->SimpleServer) :port port))
