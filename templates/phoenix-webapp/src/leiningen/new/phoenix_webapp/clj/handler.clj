(ns {{name}}.service.handler
  (:require [{{name}}.service.css :as css]
            [ring.util.response :refer [response content-type]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [clojure.java.io :as io]
            [bidi.bidi :refer [make-handler]]
            [bidi.ring :refer [->WrapMiddleware]]
            [hiccup.page :refer [html5 include-css include-js]]
            [simple-brepl.service :refer [brepl-js]]
            [com.stuartsierra.component :refer [Lifecycle]]
            [modular.ring :refer [WebRequestHandler]]))

(defn page-frame []
  (html5
   [:head
    [:title "{{name}} - Phoenix Web Application"]

    [:script (brepl-js)]

    (include-css "/css/site.css")
    
    (include-js "//cdnjs.cloudflare.com/ajax/libs/jquery/2.0.3/jquery.min.js")
    (include-js "//netdna.bootstrapcdn.com/bootstrap/3.0.0/js/bootstrap.min.js")
    (include-css "//netdna.bootstrapcdn.com/bootstrap/3.0.0/css/bootstrap.min.css")
    
    (if (io/resource "js/goog/base.js")
      (list (include-js "/js/goog/base.js")
            (include-js "/js/{{name}}.js")
            [:script "goog.require('{{sanitized}}.ui.app');"])
      
      (include-js "/js/{{name}}.js"))]
   
   [:body]))

(defn site-routes []
  (routes
    (GET "/" [] (response (page-frame)))

    (resources "/js" {:root "js"})
    (resources "/img" {:root "img"})
    
    (GET "/css/site.css" []
      (-> (response (css/site-css))
          (content-type "text/css")))))

(defn api-routes []
  {"/api" (->WrapMiddleware {}
                            #(wrap-restful-format % :formats [:edn :json-kw]))})

(defrecord AppHandler []
  Lifecycle
  (start [this] this)
  (stop [this] this)

  WebRequestHandler
  (request-handler [this]
    (make-handler )))

(defn app-handler []
  (map->AppHandler {}))
