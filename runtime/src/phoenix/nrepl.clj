(ns phoenix.nrepl
  (:require [clojure.tools.nrepl.server :as nrepl]
            [clojure.java.io :as io]))

(defn resolve-middleware [sym]
  (let [name-space (symbol (namespace sym))]
    (require name-space)
    (ns-resolve (create-ns name-space)
                (symbol (name sym)))))

(defn repl-handler [{:keys [nrepl-middleware]}]
  (apply nrepl/default-handler
         (map resolve-middleware nrepl-middleware)))

(defn start-nrepl! [nrepl-port & [{:keys [repl-options target-path]} :as project]]
  (when target-path
    (doto (io/file target-path "repl-port")
      (spit nrepl-port)
      (.deleteOnExit)))

  (let [server (nrepl/start-server :port nrepl-port
                                   :handler (repl-handler repl-options))]

    (println "Started nREPL server, port" nrepl-port)
    server))
