(ns phoenix.build
  (:require [phoenix]
            [phoenix.config :as config]
            [phoenix.location :as l]
            [phoenix.system :as s]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as c]
            [medley.core :as m]))

(defprotocol BuiltComponent
  (build [_ project]
    "Builds any necessary artifacts, and returns a pair of the updated
    component and the updated project map"))

(defn- populate-deps [system {:keys [component-id component-deps]}]
  (let [built-deps (m/map-vals #(get system %) (or component-deps {}))]
    (update-in system [component-id] merge built-deps)))

(defn- build-component [{:keys [system project] :as acc} component-id]
  (let [component (get system component-id)]
    (if (satisfies? BuiltComponent component)
      (let [build-results (build component project)
            _ (assert (and (vector? build-results)
                           (= 2 (count build-results)))
                      "phoenix.build/BuiltComponent.build should return a pair - the updated component and the updated project map.")
            [new-component new-project] build-results]
        {:system (assoc system component-id new-component)
         :project new-project})
      
      acc)))

(defn build-system [{phoenix-config :phoenix/config, :as project} out-file]
  (config/assert-config phoenix-config)

  (let [parsed-config (config/read-config (io/resource phoenix-config)
                                          {:location (l/get-location)})
        {:keys [sorted-deps]} (meta parsed-config)

        initial-system (c/map->SystemMap (->> parsed-config
                                              (m/map-vals s/make-component)))]

    (alter-var-root #'phoenix/system (constantly initial-system))

    (->> (reduce (fn [{:keys [system project] :as acc} component-id]
                   (-> acc
                       (update-in [:system] populate-deps (get parsed-config component-id))
                       (build-component component-id)))

                 {:system initial-system
                  :project project}

                 sorted-deps)

         :project
         pr-str
         (spit out-file))))

(defn build-system-main [project out-file]
  (try
    (build-system project out-file)
    (System/exit 0)

    (catch Exception e
      (.printStackTrace e)
      (System/exit 1))))
