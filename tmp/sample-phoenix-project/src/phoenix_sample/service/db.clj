(ns phoenix-sample.service.db
  (:require [com.stuartsierra.component :as c]
            [clojure.tools.logging :as log]))

(defprotocol Database
  (get-obj [_ obj-id])
  (put-obj! [_ obj-id obj]))

(defrecord SimpleDatabase []
  c/Lifecycle
  (start [this]
    (log/info "Starting database with config:" this)
    (assoc this :!db (atom {})))

  (stop [this]
    (log/info "Stopping database")
    this)

  Database
  (get-obj [{:keys [!db] :as this} obj-id]
    (get @!db obj-id))

  (put-obj! [{:keys [!db] :as this} obj-id obj]
    (swap! !db assoc obj-id obj)))

