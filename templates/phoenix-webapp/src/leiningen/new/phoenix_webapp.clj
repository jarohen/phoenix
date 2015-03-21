(ns leiningen.new.phoenix-webapp
  (:require [clojure.java.io :as io]
            [clojure.tools.reader.edn :as edn]
            [leiningen.new.templates :refer [renderer name-to-path ->files]]))

(def render (renderer "phoenix-webapp"))

(defn phoenix-webapp
  "Create a new Phoenix Single Page Application"
  [app-name & [frontend-library]]
  (when (nil? frontend-library)
    (println (format "Please choose a front-end library (e.g. `lein new phoenix-webapp %s -- :flow`):" app-name))
    (println "You can choose from :flow, :reagent, :om or :om-sablono")
    (System/exit 1))

  (let [frontend-library (edn/read-string frontend-library)]
    (assert (contains? #{:om-sablono :om :reagent :flow} frontend-library)
            "Please pick from #{flow reagent om-sablono om}")

    (println "Creating a new Phoenix Single Page Application...")

    (let [data {:name app-name
                :sanitized (name-to-path app-name)}]

      (->files data
               ["project.clj" (render "project.clj" (assoc data
                                                      :flow? (= :flow frontend-library)
                                                      :om? (contains? #{:om-sablono :om} frontend-library)
                                                      :sablono? (= :om-sablono frontend-library)
                                                      :reagent? (= :reagent frontend-library)))]
               [".gitignore" (render "gitignore" data)]
               ["resources/{{name}}-config.edn" (render "resources/config.edn" data)]
               ["resources/log4j2.json" (render "resources/log4j2.json" data)]

               ["src/{{sanitized}}/service/handler.clj" (render "clj/handler.clj" data)]
               ["src/{{sanitized}}/service/css.clj" (render "clj/css.clj" data)]
               ["ui-src/{{sanitized}}/ui/app.cljs" (render (format "cljs/%s.cljs" (name-to-path (name frontend-library))) data)]
               ["externs/jquery.js" (render "externs/jquery.js")]))

    (println "Created!")
    (println "To start the application, run `lein dev`, and then go to http://localhost:3000")))
