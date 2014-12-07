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
             ;; TODO
             )

    (main/info "All done!")
    (main/info (str "Change into the project directory, "
                    "and run 'lein phoenix' to start developing!"))))
