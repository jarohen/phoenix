(ns ^{:clojure.tools.namespace.repl/load false
      :clojure.tools.namespace.repl/unload false}
  phoenix
  (:require [phoenix.config :as config]
            [phoenix.loader :as pl]
            [phoenix.parser :as pp]
            [phoenix.location :as l]
            [phoenix.nrepl :refer [start-nrepl!]]
            [phoenix.system :as ps]
            [clojure.java.io :as io]
            [clojure.tools.namespace.repl :as tn]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as c]
            [medley.core :as m]
            phoenix.readers))

(defonce !default-config-resource
  (atom nil))

(defonce !system
  (atom nil))

(defn read-config [{:keys [config-resource location]}]
  (-> (or config-resource @!default-config-resource)
      (pl/load-config (merge (l/get-location)
                             location))
      (pp/parse-config)))

(defn make-system [& [{:keys [config targets]}]]
  (ps/make-system (or config (read-config))
                  {:targets targets}))

(defonce ^:private !location
  (atom (let [location (l/get-location)]
          {:original location
           :current location})))

(defn set-location! [{:keys [environment host user] :as new-location}]
  (assert (nil? @!system) "Can't change location when system is running...")

  (let [merged-location (swap! !location
                               (fn [{:keys [original] :as old-location}]
                                 (assoc old-location
                                   :current (merge original new-location))))]
    (log/info "Setting Phoenix location:" (:current merged-location))
    merged-location))

(defn- do-start! []
  (reset! !system (-> {:config (read-config {:location @!location})}
                      make-system
                      c/start-system)))

(defn start! []
  (assert (nil? @!system) "System already started!")

  (binding [clojure.test/*load-tests* false]
    (tn/refresh :after 'phoenix/do-start!)))

(defn stop! []
  (boolean (when-let [old-system (m/deref-reset! !system nil)]
             (c/stop-system old-system))))

(defn reload! [& [{:keys [environment host user] :as new-location}]]
  (stop!)

  (when new-location
    (set-location! new-location))

  (start!))

(defn init-phoenix! [config-resource]
  (assert config-resource "Please specify a valid config resource")

  (reset! !default-config-resource config-resource))

(defn init-nrepl! [{:keys [repl-options target-port root] :as project}]
  (when-let [nrepl-port (get-in (read-config) [:phoenix/nrepl-port :component-config])]
    (start-nrepl! nrepl-port project)))
