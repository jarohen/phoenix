(ns phoenix.location
  (:require [phoenix.merge :refer [deep-merge]]
            [medley.core :as m]
            [clojure.string :as s]
            [schema.core :as sc])
  (:import java.net.InetAddress))

(def Location
  {(sc/optional-key :environment) (sc/maybe sc/Str)
   (sc/optional-key :host) (sc/maybe sc/Str)
   (sc/optional-key :user) (sc/maybe sc/Str)})

(sc/defn get-location :- Location []
  {:environment (or (get (System/getenv) "PHOENIX_ENV")
                    (System/getProperty "phoenix.env"))

   :host (s/trim (.getHostName (InetAddress/getLocalHost)))

   :user (System/getProperty "user.name")})

(sc/defn select-location [config {:keys [environment host user] :as location} :- Location]
  {:general (dissoc config :phoenix/environments :phoenix/hosts)
   :host (-> (get-in config [:phoenix/hosts (or host :default)])
             (dissoc :phoenix/users))
   :user (get-in config [:phoenix/hosts (or host :default)
                         :phoenix/users (or user :default)])
   :environment (get-in config [:phoenix/environments (or environment :default)])})
