(ns phoenix-sample.service.db
  (:require [com.stuartsierra.component :as c]))

(defprotocol Database
  (get-obj [_ obj-id])
  (put-obj! [_ obj-id obj]))

(defrecord SimpleDatabase [config !db]
  c/Lifecycle
  (start [this]
    (println "Starting database with config:" (pr-str config))
    this)

  (stop [this]
    (println "Stopping database")
    this)

  Database
  (get-obj [this obj-id]
    (get @!db obj-id))

  (put-obj! [this obj-id obj]
    (swap! !db assoc obj-id obj)))

(defn make-database [config]
  (->SimpleDatabase config (atom {})))

