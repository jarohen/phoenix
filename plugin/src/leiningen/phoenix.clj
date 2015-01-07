(ns leiningen.phoenix
  (:require [leiningen.uberjar :as u]
            [leinjacker.eval :refer [eval-in-project]]
            [phoenix.plugin :refer [select-project-keys]]))

(defn server
  "Starts the Phoenix application, as per the configuration file specified in project.clj.

   Usage: lein phoenix [server]"
  
  [project]

  (let [project-subset (select-project-keys project)]
    (eval-in-project project
                     `(do
                        (#'phoenix/init-nrepl! (quote ~project-subset))
                        (phoenix/start!))
                     `(require '~'phoenix))))

(defn uberjar-project-map [project]
  (-> project
      (assoc :main 'phoenix.main)
      (update-in [:aot] conj 'phoenix.main)
      (update-in [:filespecs] conj {:type :bytes
                                    :path "META-INF/phoenix-config-resource"
                                    :bytes (:phoenix/config project)})
      (update-in [:filespecs] conj {:type :bytes
                                    :path "META-INF/phoenix-repl-options.edn"
                                    :bytes (pr-str (:repl-options project))})))

(defn uberjar
  "Creates an uberjar of the Phoenix application

   Usage: lein phoenix uberjar"
  [project]
  
  (let [project (-> project
                    uberjar-project-map
                    (vary-meta #(update-in % [:without-profiles] uberjar-project-map)))]
    (u/uberjar project 'phoenix.main)))

(defn phoenix
  "Plugin to configure and co-ordinate a Component-based system

  Usage: lein phoenix [server, uberjar]

  If no arguments are provided, 'server' is assumed.

  For more details of how to set up and use Phoenix, please refer to
  the documentation at https://github.com/james-henderson/phoenix"
  
  [project & [command & args]]

  (case command
    "server" (server project)
    "uberjar" (uberjar project)
    nil (server project)))
