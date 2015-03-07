(ns ^{:clojure.tools.namespace.repl/load false
      :clojure.tools.namespace.repl/unload false}
  phoenix.core
  (:require [phoenix.loader :as pl]
            [phoenix.location :as l]
            [phoenix.parser :as pp]
            [phoenix.system :as ps]
            [com.stuartsierra.component :as c]
            phoenix.readers))

(defn read-config [{:keys [config-resource location]}]
  (-> config-resource
      (pl/load-config (merge (l/get-location)
                             location))
      (pp/parse-config)))

(defn make-system [& [{:keys [config targets]}]]
  (ps/make-system (or config (read-config))
                  {:targets targets}))

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
