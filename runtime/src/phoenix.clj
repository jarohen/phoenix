(ns ^{:clojure.tools.namespace.repl/load false
      :clojure.tools.namespace.repl/unload false}
  phoenix
  (:require [phoenix.config :as config]
            [phoenix.location :as l]
            [phoenix.nrepl :refer [start-nrepl!]]
            [phoenix.system :refer [phoenix-system]]
            [clojure.java.io :as io]
            [clojure.tools.namespace.repl :as tn]
            [com.stuartsierra.component :as c]))

(def ^:private config-resource)

(defonce !system
  (atom nil))

(defonce ^:private !location
  (let [location (l/get-location)]
    (atom {:original location
           :current location})))

(defn set-location! [{:keys [environment host] :as new-location}]
  (assert (nil? @!system) "Can't change location when system is running...")

  (let [new-location (swap! !location
                            (fn [{:keys [original] :as old-location}]
                              (assoc old-location
                                :current (merge original new-location))))]
    (println "Setting Phoenix location:" (pr-str (:current new-location)))
    new-location))

(defn- do-start! []
  (reset! !system (-> (config/read-config config-resource
                                          {:location (:current @!location)})
                      phoenix-system
                      c/start-system)))

(defn start! []
  (assert (nil? @!system) "System already started!")
  (tn/refresh :after 'phoenix/do-start!))

(defn stop! []
  (c/stop-system @!system)
  (reset! !system nil))

(defn reload! [& [{:keys [environment host] :as new-location}]]
  (stop!)
  (when new-location
    (set-location! new-location))
  (start!))

(defn init-phoenix! [{phoenix-config :phoenix/config, :as project}]
  (assert (and phoenix-config
               (io/resource phoenix-config))
          "Please make sure you have a valid config resource specified at ':phoenix/config' in your 'project.clj'")
  
  (alter-var-root #'config-resource (constantly (io/resource phoenix-config)))

  (when-let [nrepl-port (:phoenix/nrepl-port (read-string (slurp (io/resource phoenix-config))))]
    (start-nrepl! nrepl-port project))
  
  (start!))
