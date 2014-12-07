(ns phoenix.deps
  (:require [clojure.java.io :as io]))

(defn project-deps [{:keys [dependencies] :as project}]
  (set (map first dependencies)))

(defn with-runtime-dep [project]
  (cond-> project
    (not (contains? (project-deps project) 'jarohen/phoenix.runtime))
    (update-in [:dependencies] conj ['jarohen/phoenix.runtime (slurp (io/resource "phoenix-version"))])))
