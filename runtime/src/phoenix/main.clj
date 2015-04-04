(ns ^{:clojure.tools.namespace.repl/load false
      :clojure.tools.namespace.repl/unload false}
  phoenix.main
  (:gen-class)
  (:require [clojure.java.io :as io]))

(defn phoenix-config-location []
  (slurp (io/resource "META-INF/phoenix-config-resource")))

(defn repl-options []
  (read-string (slurp (io/resource "META-INF/phoenix-repl-options.edn"))))

(defn -main []
  ;; This is split out because we don't want Phoenix and its
  ;; transitive dependencies AOT-compiled with this namespace. See #8

  (require '[phoenix]
           '[phoenix.jar])

  (eval
   `(let [project# {:phoenix/config (phoenix-config-location)
                    :repl-options (repl-options)}]

      (alter-var-root #'phoenix.jar/built? (constantly true))

      (phoenix/init-phoenix! (io/resource (phoenix-config-location)))

      (phoenix/init-nrepl! project#)

      (#'phoenix/do-start!))))
