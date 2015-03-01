(ns phoenix.system
  (:require [phoenix.deps :as d]
            [clojure.set :as set]
            [com.stuartsierra.component :as c]
            [com.stuartsierra.dependency :as deps]
            [medley.core :as m]))

(defn calculate-component-subset [system-config targets]
  (if (empty? targets)
    (keys system-config)

    (set/union (let [deps-graph (d/calculate-deps-graph system-config)]
                 (->> (for [target targets]
                        (deps/transitive-dependencies deps-graph target))
                      (apply set/intersection)))
               (set targets))))

(defn make-component [{:keys [component-id component-config component-fn]}]
  (when (symbol? component-fn)
    (require (symbol (namespace component-fn))))

  (try
    (if component-fn
      (eval `(~component-fn '~component-config))
      component-config)

    (catch Exception e
      (throw (ex-info "Failed initialising component"
                      {:component-id component-id
                       :component-fn component-fn
                       :component-config component-config}
                      e)))))

(defn make-system [system-config & [{:keys [targets]}]]
  (let [system-subset (-> system-config
                          (select-keys (calculate-component-subset system-config targets)))]

    (-> (c/map->SystemMap (->> system-subset
                               (m/map-vals make-component)))

        (c/system-using (->> system-subset
                             (m/map-vals :component-deps)
                             (m/remove-vals empty?))))))
