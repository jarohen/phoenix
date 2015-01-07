(ns ^{:clojure.tools.namespace.repl/load false
      :clojure.tools.namespace.repl/unload false}
  phoenix.main
  (:gen-class)
  (:require [clojure.java.io :as io]
            [phoenix]))

(defn phoenix-config-location []
  (slurp (io/resource "META-INF/phoenix-config-resource")))

(defn repl-options []
  (read-string (slurp (io/resource "META-INF/phoenix-repl-options.edn"))))

(defn -main []
  (let [project {:phoenix/config (phoenix-config-location)
                 :repl-options (repl-options)}]
    
    (#'phoenix/init-phoenix! project)

    (#'phoenix/init-nrepl! project)

    (#'phoenix/do-start!)))
