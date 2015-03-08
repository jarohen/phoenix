(ns {{name}}.handler
  (:require [bidi.bidi :as bidi]
            [bidi.ring :refer [make-handler]]
            [modular.ring :refer [WebRequestHandler]]
            [ring.util.response :refer [response]]))

;; This is all in one NS for now, but you'll likely want to split it
;; out when your application grows!

(def api-routes
  {"/" {:get ::default-route}
   "/api" {}})

(defn api-handlers []
  {::default-route (fn [req]
                     (response "Hello world from Phoenix! Try `(phoenix/reload!)` to completely reload the application"))})

(defrecord AppHandler []
  WebRequestHandler
  (request-handler [_]
    (make-handler ["" api-routes]

                  (some-fn (api-handlers)

                           #(when (fn? %) %)

                           (constantly {:status 404
                                        :body "Not found."})))))
