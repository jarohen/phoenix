(ns phoenix.build
  (:require [phoenix.build.protocols :as pbp]
            [phoenix.core :as pc]
            [phoenix.deps :as pd]
            [phoenix.location :as l]
            [phoenix.system :as s]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as c]
            [com.stuartsierra.dependency :as deps]
            [medley.core :as m]))

(defn merge-deps [m1 m2]
  (if (and (map? m1)
           (map? m2))
    (merge m1 m2)
    m2))

(defn- populate-deps [system {:keys [component-id component-deps]}]
  (let [built-deps (m/map-vals #(get system %) (or component-deps {}))]
    (update-in system [component-id] merge-deps built-deps)))

(defn- build-component [{:keys [system project] :as acc} component-id]
  (let [component (get system component-id)]
    (if (satisfies? BuiltComponent component)
      (let [build-results (pbp/build component project)
            _ (assert (and (vector? build-results)
                           (= 2 (count build-results)))
                      "phoenix.build/BuiltComponent.build should return a pair - the updated component and the updated project map.")
            [new-component new-project] build-results]
        {:system (assoc system component-id new-component)
         :project new-project})

      acc)))

(defn build-system [{phoenix-config :phoenix/config, :as project}]
  (let [analyzed-config (-> (pc/load-config {:config-source (io/resource phoenix-config)})
                            pc/analyze-config)
        initial-system (pc/make-system {:config analyzed-config})
        sorted-deps (->> analyzed-config
                         pd/calculate-deps-graph
                         deps/topo-sort)]

    (->> (reduce (fn [{:keys [system project] :as acc} component-id]
                   (-> acc
                       (update-in [:system] populate-deps (get analyzed-config component-id))
                       (build-component component-id)))

                 {:system initial-system
                  :project project}

                 sorted-deps)

         :project)))

(defn build-system-main [project out-file]
  (try
    (->> (build-system project)
         pr-str
         (spit out-file))
    (System/exit 0)

    (catch Exception e
      (.printStackTrace e)
      (System/exit 1))))
