(ns ^{:clojure.tools.namespace.repl/load false
      :clojure.tools.namespace.repl/unload false}
  phoenix
  (:require [phoenix.config :as config]
            [phoenix.location :as l]
            [phoenix.nrepl :refer [start-nrepl!]]
            [phoenix.system :refer [phoenix-system]]
            [clojure.java.io :as io]
            [clojure.tools.namespace.repl :as tn]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as c]))

(defonce ^:private config-resource
  nil)

(defonce system
  nil)

(defonce ^:private !started?
  (atom false))

(defonce ^:private !location
  (let [location (l/get-location)]
    (atom {:original location
           :current location})))

(defn- make-system [location]
  (-> (config/read-config config-resource
                          {:location location})
      phoenix-system))

(defn set-location! [{:keys [environment host user] :as new-location}]
  (assert (false? @!started?) "Can't change location when system is running...")

  (let [new-location (swap! !location
                            (fn [{:keys [original] :as old-location}]
                              (assoc old-location
                                :current (merge original new-location))))]

    (log/info "Setting Phoenix location:" (pr-str (:current new-location)))

    (alter-var-root #'system (constantly (make-system new-location)))
    new-location))

(defn- do-start! []
  (let [started-system (alter-var-root #'system
                                       (comp c/start-system
                                             (constantly (make-system (:current @!location)))))]
    (reset! !started? true)
    started-system))

(defn start! []
  (assert (false? @!started?) "System already started!")

  (binding [clojure.test/*load-tests* false]
    (tn/refresh :after 'phoenix/do-start!)))

(defn stop! []
  (alter-var-root #'system c/stop-system)
  (reset! !started? false)
  #'system)

(defn reload! [& [{:keys [environment host user] :as new-location}]]
  (when @!started?
    (stop!))

  (when new-location
    (set-location! new-location))

  (start!))

(defn init-phoenix! [config-resource]
  (assert config-resource "Please specify a valid config resource")

  (alter-var-root #'phoenix/config-resource (constantly config-resource)))

(defn init-nrepl! [{:keys [repl-options target-port root] :as project}]
  (when-let [nrepl-port (get-in (config/read-config config-resource {:location (l/get-location)})
                                [:phoenix/nrepl-port :static-config])]
    (start-nrepl! nrepl-port project)))
