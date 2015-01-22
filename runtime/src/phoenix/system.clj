(ns phoenix.system
  (:require [medley.core :as m]
            [com.stuartsierra.component :as c]))

(defn make-component [{:keys [component-id static-config component]}]
  (when component
    (require (symbol (namespace component))))

  (try
    (if component
      (eval `(~component ~static-config))
      static-config)
    
    (catch Exception e
      (throw (ex-info "Failed initialising component"
                      {:component component-id
                       :generator-fn component
                       :config static-config}
                      e)))))

(defn system-deps [system-config]
  (->> system-config
       (m/map-vals :component-deps)
       (m/remove-vals empty?)))

(defn phoenix-system [system-config]
  (-> (apply c/system-map (->> system-config
                               (m/map-vals make-component)
                               seq
                               (apply concat)))
      
      (c/system-using (system-deps system-config))))


