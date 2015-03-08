(ns leiningen.new.phoenix-api
  (:require [clojure.java.io :as io]
            [leiningen.new.templates :refer [renderer name-to-path ->files]]))

(def render (renderer "phoenix-api"))

(defn phoenix-api
  "Create a new Phoenix API application"
  [name]
  (println "Creating a new Phoenix API Application...")

  (let [data {:name name
              :sanitized (name-to-path name)}]

    (->files data
             ["project.clj" (render "project.clj" data)]
             [".gitignore" (render "gitignore" data)]
             ["resources/{{name}}-config.edn" (render "resources/config.edn" data)]
             ["resources/log4j2.json" (render "resources/log4j2.json" data)]

             ["src/{{sanitized}}/handler.clj" (render "handler.clj" data)]))

  (println "Created!")
  (println "To start the application, run `lein dev`, and then `curl http://localhost:3000`"))
