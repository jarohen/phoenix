(ns leiningen.new.phoenix-webapp
  (:require [clojure.java.io :as io]
            [leiningen.new.templates :refer [renderer name-to-path ->files]]))

(def render (renderer "phoenix-webapp"))

(defn phoenix-webapp
  "Create a new Phoenix Single Page Application"
  [name]
  (println "Creating a new Phoenix Single Page Application...")

  (let [data {:name name
              :sanitized (name-to-path name)}]
    
    (->files data
             ["project.clj" (render "project.clj" data)]
             [".gitignore" (render "gitignore" data)]
             ["resources/{{name}}-config.edn" (render "resources/config.edn" data)]
             ["resources/log4j2.json" (render "resources/log4j2.json" data)]
             
             ["src/{{sanitized}}/service/handler.clj" (render "clj/handler.clj" data)]
             ["src/{{sanitized}}/service/css.clj" (render "clj/css.clj" data)]
             ["ui-src/{{sanitized}}/ui/app.cljs" (render "cljs/app.cljs" data)]
             ["externs/jquery.js" (render "externs/jquery.js")]))
  
  (println "Created!")
  (println "To start the application, run `lein dev`, and then go to http://localhost:3000"))
