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

(def system nil)

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

    (println "Setting Phoenix location:" (pr-str (:current new-location)))
    
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
  (tn/refresh :after 'phoenix/do-start!))

(defn stop! []
  (alter-var-root #'system c/stop-system)
  (reset! !started? false))

(defn reload! [& [{:keys [environment host user] :as new-location}]]
  (when @!started?
    (stop!))
  (when new-location
    (set-location! new-location))
  (start!))

(defn- assert-config [phoenix-config]
  (assert (and phoenix-config
               (io/resource phoenix-config))
          "Please make sure you have a valid config resource specified at ':phoenix/config' in your 'project.clj'"))

(defn- init-phoenix! [{phoenix-config :phoenix/config, :as project}]
  (assert-config phoenix-config)
  
  (alter-var-root #'config-resource (constantly (io/resource phoenix-config)))
  (alter-var-root #'system (constantly (make-system @!location))))

(defn- init-nrepl! [{phoenix-config :phoenix/config, :as project}]
  (assert-config phoenix-config)
  
  (when-let [nrepl-port (:phoenix/nrepl-port (read-string (slurp (io/resource phoenix-config))))]
    (start-nrepl! nrepl-port project)))
