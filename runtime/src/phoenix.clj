(ns ^{:clojure.tools.namespace.repl/load false
      :clojure.tools.namespace.repl/unload false}
  phoenix
  (:require [nomad :refer [read-config]]
            [phoenix.core :as pc]
            [phoenix.nrepl :refer [start-nrepl!]]
            [clojure.tools.logging :as log]
            [clojure.tools.namespace.repl :as tn]
            [com.stuartsierra.component :as c]
            [medley.core :as m]))

;; This namespace is the 'magical' API :)

(defonce ^:private !default-config-source
  (atom nil))

(def system (atom nil))

(defn- do-start! []
  (reset! system (-> (read-config @!default-config-source)
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

(defn reload! []
  (stop!)
  (start!))

(defn init-phoenix! [config-source]
  (assert config-source "Please specify a valid config source")

  (reset! !default-config-source config-source))

(defn init-nrepl! [{:keys [repl-options target-port root] :as project}]
  (when-let [nrepl-port (-> (read-config @!default-config-source)
                            :phoenix/nrepl-port)]
    (start-nrepl! nrepl-port project)))
