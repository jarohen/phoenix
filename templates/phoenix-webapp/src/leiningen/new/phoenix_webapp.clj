(ns leiningen.new.phoenix-webapp
  (:require [leiningen.new.templates :refer [renderer name-to-path ->files]]
            [leiningen.core.main :as main]))

(def render (renderer "phoenix-webapp"))

(defn phoenix-webapp
  "A template to create a new webapp, based on
  Phoenix (https://github.com/james-henderson/phoenix)"
  [name]
  (let [data {:name name
              :sanitized (name-to-path name)}]
    (main/info "Generating a new phoenix-webapp project.")

    (->files data
             (->files data
                      ["project.clj" (render "project.clj" data)]
                      [".gitignore" (render "gitignore" data)]
                      ["resources/{{name}}-config.edn" (render "resources/config.edn" data)]
             
                      ["src/{{sanitized}}/service/handler.clj" (render "clj/handler.clj" data)]
                      ["src/{{sanitized}}/service/css.clj" (render "clj/css.clj" data)]
                      ["ui-src/{{sanitized}}/ui/app.cljs" (render "cljs/app.cljs" data)]
                      ["externs/jquery.js" (render "externs/jquery.js")]
                      
                      "common-src"))

    (main/info "All done!")
    (main/info (str "Change into the project directory, "
                    "and run 'lein phoenix' to start developing!"))))
