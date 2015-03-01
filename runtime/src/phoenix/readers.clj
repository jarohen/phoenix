(ns phoenix.readers
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.tools.logging :as log]))

(def file-reader
  (comp io/file #(s/replace % #"^~" (System/getProperty "user.home"))))

(def resource-reader
  (some-fn io/resource

           (fn [path]
             (log/warnf "Can't read config-file: '%s', ignoring..." path)
             ::invalid-include)))
