(ns phoenix.system
  (:require [medley.core :as m]
            [com.stuartsierra.component :as c]))

(defn make-component [[id {:keys [static-config component]}]]
  (when component
    (require (symbol (namespace component))))

  (try
    [id (if component
          (eval `(~component ~static-config))
          static-config)]
    (catch Exception e
      (throw (ex-info "Failed initialising component"
                      {:component id
                       :generator-fn component
                       :config static-config}
                      e)))))

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


