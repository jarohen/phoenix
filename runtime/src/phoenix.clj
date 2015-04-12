(ns ^{:clojure.tools.namespace.repl/load false
      :clojure.tools.namespace.repl/unload false}
  phoenix
  (:require [phoenix.core :as pc]
            [phoenix.location :as l]
            [phoenix.nrepl :refer [start-nrepl!]]
            [clojure.tools.logging :as log]
            [clojure.tools.namespace.repl :as tn]
            [com.stuartsierra.component :as c]
            [medley.core :as m]))

;; This namespace is the 'magical' API :)

(defonce ^:private !default-config-source
  (atom nil))

(def system (atom nil))

(defonce ^:private !location
  (atom (let [location (l/get-location)]
          {:original location
           :current location})))

(defn set-location! [{:keys [environment host user] :as new-location}]
  (assert (nil? @system) "Can't change location when system is running...")

  (let [merged-location (swap! !location
                               (fn [{:keys [original] :as old-location}]
                                 (assoc old-location
                                   :current (merge original new-location))))]
    (log/info "Setting Phoenix location:" (:current merged-location))
    merged-location))

(defn- do-start! []
  (reset! system (-> (pc/load-config {:config-source @!default-config-source
                                       :location (:current @!location)})
                      pc/analyze-config
                      pc/make-system
                      c/start-system)))

(defn start! []
  (assert (nil? @system) "System already started!")

  (binding [clojure.test/*load-tests* false]
    (log/with-logs ['clojure.tools.namespace.repl :info :warn]
      (tn/refresh :after 'phoenix/do-start!))))

(defn stop! []
  (boolean (when-let [old-system (m/deref-reset! system nil)]
             (c/stop-system old-system))))

(defn reload! [& [{:keys [environment host user] :as new-location}]]
  (stop!)

  (when new-location
    (set-location! new-location))

  (start!))

(defn init-phoenix! [config-source]
  (assert config-source "Please specify a valid config source")

  (reset! !default-config-source config-source))

(defn init-nrepl! [{:keys [repl-options target-port root] :as project}]
  (when-let [nrepl-port (-> (pc/load-config {:config-source @!default-config-source})
                            :phoenix/nrepl-port)]
    (start-nrepl! nrepl-port project)))
