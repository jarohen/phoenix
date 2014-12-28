(ns phoenix.plugin
  (:require [clojure.java.io :as io]))

(defn project-deps [{:keys [dependencies] :as project}]
  (set (map first dependencies)))

(defn with-runtime-dep [project]
  (cond-> project
    (not (contains? (project-deps project) 'jarohen/phoenix.runtime))
    (update-in [:dependencies] conj ['jarohen/phoenix.runtime
                                     (slurp (io/resource "phoenix-version"))])))

(defn select-project-keys [project]
  (select-keys project [:phoenix/config :target-path :repl-options]))

(defn middleware [project]
  (-> project
      with-runtime-dep
      (update-in [:injections]
                 concat
                 `[(require '~'phoenix)
                   (#'phoenix/init-phoenix! (quote ~(select-project-keys project)))])))

