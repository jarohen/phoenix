(ns phoenix.all-in-test
  (:require [phoenix.core :as pc]
            [phoenix]
            [clojure.java.io :as io]
            [clojure.test :refer :all]
            [com.stuartsierra.component :as c]))

(defrecord CComponent [map-a host]
  c/Lifecycle

  (start [this]
    (println "Starting c" (pr-str this))
    this)

  (stop [this]
    (println "Stopping c" (pr-str this))
    this))

(defn test-component-c [{:keys [map-a host] :as opts}]
  (map->CComponent opts))

(defrecord C1Component [a]
  c/Lifecycle

  (start [this]
    (println "Starting c1" (pr-str this))
    (assoc this :started? true))

  (stop [this]
    (println "Stopping c1" (pr-str this))
    (dissoc this :started?)))

(defn test-component-c1 [{:keys [a] :as opts}]
  (map->C1Component opts))

(defn run-system []
  (let [config-resource (io/resource "phoenix/test-config.edn")
        started-system (-> (pc/load-config {:config-source config-resource})
                           pc/analyze-config
                           pc/make-system
                           c/start-system)]
    (println "System started!")
    (c/stop-system started-system)
    (println "System stopped!")))

(defn init-phoenix []
  (phoenix/init-phoenix! {:phoenix/config "phoenix/test-config.edn"}))
