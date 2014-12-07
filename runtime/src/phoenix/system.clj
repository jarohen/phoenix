(ns phoenix.system
  (:require [medley.core :as m]
            [com.stuartsierra.component :as c]))

(defn make-component [[id {:keys [static-config component]}]]
  (do
    (require (symbol (namespace component)))

    (try
      [id (eval `(~component ~static-config))]
      (catch Exception e
        (throw (ex-info "Failed initialising component"
                        {:component id
                         :generator-fn component
                         :config static-config}
                        e))))))

(defn system-deps [system-config]
  (->> system-config
       (m/map-vals :component-deps)
       (m/remove-vals empty?)))

(defn phoenix-system [system-config]
  (-> (apply c/system-map (->> system-config
                               (map make-component)
                               (into {})
                               (apply concat)))
      
      (c/system-using (system-deps system-config))))

(comment
  '{:db {:phoenix/generator my-app.db/db-component
         :host ""
         :port ""}

    :handler {:phoenix/generator my-app.service.handler/handler}
    
    :web-server {:phoenix/generator phoenix.http-kit/web-server-component
                 :handler ::component
                 }})

