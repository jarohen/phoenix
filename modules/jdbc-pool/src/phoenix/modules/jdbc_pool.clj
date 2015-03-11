(ns phoenix.modules.jdbc-pool
  (:require [com.stuartsierra.component :as c]
            [clojure.tools.logging :as log])
  (:import [org.apache.commons.dbcp2 BasicDataSource]))

(defprotocol DatabasePool
  (db-conn [db]))

(def known-drivers
  {"postgresql" "org.postgresql.Driver"
   "mysql" "com.mysql.jdbc.Driver"
   "mssql" "com.microsoft.sqlserver.jdbc.SQLServerDriver"
   "odbc" "sun.jdbc.odbc.JdbcOdbcDriver"
   "sqlite" "org.sqlite.JDBC"
   "h2" "org.h2.Driver"})

(defrecord DatabaseComponent []
  c/Lifecycle
  (start [{:keys [driver subprotocol host port username password db max-total max-idle] :as this}]
    (log/info "Starting JDBC pool...")

    (assoc this
      ::pool (doto (BasicDataSource.)
               (.setDriverClassName (or driver (get known-drivers subprotocol)))
               (.setAccessToUnderlyingConnectionAllowed true)
               (.setUrl (format "jdbc:%s://%s:%s/%s" subprotocol host port db))
               (.setUsername username)
               (.setPassword password)
               (cond-> max-total (.setMaxTotal max-total))
               (cond-> max-idle (.setMaxIdle max-idle)))))

  (stop [{:keys [::pool] :as this}]
    (when pool
      (.close pool))

    (dissoc this ::pool))

  DatabasePool
  (db-conn [{:keys [::pool]}]
    {:datasource pool}))

(defn make-jdbc-pool [opts]
  (map->DatabaseComponent opts))
