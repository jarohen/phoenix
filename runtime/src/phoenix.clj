(ns ^{:clojure.tools.namespace.repl/load false
      :clojure.tools.namespace.repl/unload false}
  phoenix
  (:require [phoenix.config :refer [read-config]]
            [phoenix.nrepl :refer [start-nrepl!]]
            [phoenix.system :refer [phoenix-system]]
            [clojure.java.io :as io]
            [clojure.tools.namespace.repl :as tn]
            [com.stuartsierra.component :as c]))

(def ^:private config-resource)

(def system)

(defn start! []
  (let [started-system (c/start-system (phoenix-system (read-config config-resource)))]
    (alter-var-root #'system (constantly started-system))
    started-system))

(defn stop! []
  (c/stop-system system)
  (alter-var-root #'system (constantly nil)))

(defn reload! []
  (stop!)
  (tn/refresh :after 'phoenix/start!))

(defn init-phoenix! [{phoenix-config :phoenix/config, :as project}]
  (assert (and phoenix-config
               (io/resource phoenix-config))
          "Please make sure you have a valid config resource specified at ':phoenix/config' in your 'project.clj'")
  
  (alter-var-root #'config-resource (constantly (io/resource phoenix-config)))

  (when-let [nrepl-port (:phoenix/nrepl-port (read-string (slurp (io/resource phoenix-config))))]
    (start-nrepl! nrepl-port project))
  
  (start!))
