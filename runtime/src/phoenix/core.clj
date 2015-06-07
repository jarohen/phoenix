(ns ^{:clojure.tools.namespace.repl/load false
      :clojure.tools.namespace.repl/unload false}
  phoenix.core
  (:require [phoenix.analyzer :as pa]
            [phoenix.system :as ps]
            [com.stuartsierra.component :as c]
            [clojure.java.io :as io]
            [schema.core :as s]))

(defn analyze-config [config]
  (pa/analyze-config config))

(defn make-system [analyzed-config & [{:keys [targets]}]]
  (ps/make-system analyzed-config {:targets targets}))

(defn with-running-system* [system f]
  (let [started-system (c/start-system system)]
    (try
      (f started-system)
      (finally
        (c/stop-system started-system)))))

(defmacro with-running-system [binding & body]
  (let [[bound-sym system] binding]
    `(with-running-system* ~system
       (fn [~bound-sym]
         ~@body))))
