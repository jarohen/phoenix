(ns phoenix.system
  (:require [phoenix.config :as config :refer [read-config]]
            [medley.core :as m]
            [com.stuartsierra.component :as c]))

(defn make-component [{:keys [static-config component]}]
  ;; TODO
  )

(defn system-deps [system-config]
  ;; TODO
  )

(defn phoenix-system [config-resource]
  (let [system-config (read-config config-resource)]
    
    (-> (apply c/system-map (->> system-config
                                 (m/filter-vals :component)
                                 (m/map-vals make-component)
                                 (apply concat)))
        
        (c/system-using (system-deps system-config)))))

(comment
  '{:db {:phoenix/generator my-app.db/db-component
         :host ""
         :port ""}

    :handler {:phoenix/generator my-app.service.handler/handler}
    
    :web-server {:phoenix/generator phoenix.http-kit/web-server-component
                 :handler ::component
                 }})

