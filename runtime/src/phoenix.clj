(ns ^{:clojure.tools.namespace.repl/load false
      :clojure.tools.namespace.repl/unload false}
  phoenix
  (:require [phoenix.core :refer [read-config make-system]]
            [phoenix.location :as l]
            [phoenix.nrepl :refer [start-nrepl!]]
            [clojure.tools.logging :as log]
            [clojure.tools.namespace.repl :as tn]
            [com.stuartsierra.component :as c]
            [medley.core :as m]))

;; This namespace is the 'magical' API :)

(defonce !default-config-resource
  (atom nil))

(defonce !system
  (atom nil))

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
  (reset! !system (-> {:config (read-config {:location @!location
                                             :config-resource @!default-config-resource})}
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
  (when-let [nrepl-port (-> (read-config {:config-resource @!default-config-resource})
                            (get-in [:phoenix/nrepl-port :component-config]))]
    (start-nrepl! nrepl-port project)))
