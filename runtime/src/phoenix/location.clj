(ns phoenix.location
  (:require [phoenix.merge :refer [deep-merge]]
            [medley.core :as m]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as s]))

(defn get-location []
  {:environment (or (get (System/getenv) "PHOENIX_ENV")
                    (System/getProperty "phoenix.env"))

   ;; not sure how I plan to make this work on Windoze... Will see if
   ;; someone complains first, I suspect. If you do see this, I'm
   ;; generally quite quick at merging PRs ;)
   :host (s/trim (:out (sh "hostname")))
   
   :user (System/getProperty "user.name")})

(defn extract-location-config [{environments :phoenix/environments, hosts :phoenix/hosts, :as config}]
  {:general (dissoc config :phoenix/environments :phoenix/hosts)
   :hosts (some->> hosts
                   (m/map-vals #(dissoc % :phoenix/users)))
   :hosts-users (->> (for [[hostname {users :phoenix/users}] hosts
                           [user host-user-config] users]
                       [[hostname user] host-user-config])
                     (into {}))
   :environments environments})

(defn merge-configs [{:keys [general hosts hosts-users environments]} {:keys [environment host user]}]
  (deep-merge general
              (get hosts host)
              (get hosts-users [host user])
              (get environments environment)))

(defn combine-config [config location]
  (-> config
      extract-location-config
      (merge-configs location)))
