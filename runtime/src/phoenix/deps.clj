(ns phoenix.deps
  (:require [com.stuartsierra.dependency :as deps]))

(defn calculate-deps-graph [config]
  (reduce (fn [graph [id {:keys [component-deps]}]]
            (reduce (fn [graph [dep-key dep-value]]
                      (deps/depend graph id dep-value))
                    (-> graph
                        (deps/depend ::system id))

                    component-deps))

          (deps/graph)
          config))

(defn ordered-deps [config]
  (->> (calculate-deps-graph config)
       deps/topo-sort
       (remove #{::system})))
