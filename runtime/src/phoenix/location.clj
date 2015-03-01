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

(defn select-location [config {:keys [environment host user] :as location}]
  {:general (dissoc config :phoenix/environments :phoenix/hosts)
   :host (-> (get-in config [:phoenix/hosts (or host :default)])
             (dissoc :phoenix/users))
   :user (get-in config [:phoenix/hosts (or host :default)
                         :phoenix/users (or user :default)])
   :environment (get-in config [:phoenix/environments (or environment :default)])})
