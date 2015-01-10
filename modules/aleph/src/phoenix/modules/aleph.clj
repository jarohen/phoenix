;; This is based on JUXT's http-kit module in modular -
;; https://github.com/juxt/modular/blob/master/modules/http-kit/src/modular/http_kit.clj

(ns phoenix.modules.aleph
  (:require [com.stuartsierra.component :as c]
            [aleph.http :as http]
            [modular.ring :refer [request-handler]]
            [clojure.tools.logging :as log]))

(defrecord WebServer []
  c/Lifecycle
  (start [{:keys [handler port aleph-opts] :as this}]
    (log/info "Starting web server on port" port)
    (assoc this
      :server (http/start-server (request-handler handler)
                                 
                                 (merge {:port port}
                                        aleph-opts))))
  
  (stop [{:keys [server] :as this}]
    (when server
      (log/info "Stopping web server")
      (.close server))
    
    (dissoc this :server)))

(defn make-web-server [opts]
  (map->WebServer opts))

