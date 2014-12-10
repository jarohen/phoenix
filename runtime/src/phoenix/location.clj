(ns phoenix.location
  (:require [phoenix.merge :refer [deep-merge]]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as s]))

(defn get-location []
  {:environment (get (System/getenv) "PHOENIX_ENV")

   ;; not sure how I plan to make this work on Windoze... Will see if
   ;; someone complains first, I suspect.
   :host (s/trim (:out (sh "hostname")))})

(defn extract-location-config [{environments :phoenix/environments, hosts :phoenix/hosts, :as config}]
  {:general (dissoc config :phoenix/environments :phoenix/hosts)
   :hosts hosts
   :environments environments})

(defn merge-configs [{:keys [general hosts environments]} {:keys [environment host]}]
  (deep-merge general
              (get hosts host)
              (get environments environment)))

(defn combine-config [config location]
  (-> config
      extract-location-config
      (merge-configs location)))
