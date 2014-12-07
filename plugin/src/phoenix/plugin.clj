(ns phoenix.plugin
  (:require [phoenix.deps :refer [with-runtime-dep]]))

(defn middleware [project]
  (with-runtime-dep project))
