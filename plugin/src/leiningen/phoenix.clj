(ns leiningen.phoenix
  (:require [leinjacker.eval :refer [eval-in-project]]
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

(defn phoenix
  "Plugin to configure and co-ordinate a Component-based system

  Usage: lein phoenix [server, uberjar]

  If no arguments are provided, 'server' is assumed.

  For more details of how to set up and use Phoenix, please refer to
  the documentation at https://github.com/james-henderson/phoenix"
  
  [project & [command & args]]

  (case command
    "server" (server project)
    nil (server project)))
