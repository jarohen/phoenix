(ns phoenix.build
  (:require [phoenix.config :as config]
            [phoenix.location :as l]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [com.stuartsierra.dependency :as dep]))

(defprotocol BuiltComponent
  (build [_ project]
    "Builds any necessary artifacts, and returns an updated project
    map"))

(defn- build-system [{phoenix-config :phoenix/config, :as project}]
  (config/assert-config phoenix-config)

  project)

