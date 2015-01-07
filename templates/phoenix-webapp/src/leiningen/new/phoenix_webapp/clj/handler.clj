(ns {{name}}.service.handler
  (:require [{{name}}.service.css :as css]
            [bidi.bidi :as bidi]
            [bidi.ring :refer [make-handler]]
            [com.stuartsierra.component :refer [Lifecycle]]
            [hiccup.page :refer [html5 include-css include-js]]
            [modular.ring :refer [WebRequestHandler]]
            [phoenix.modules.cljs :as cljs]
            [ring.util.response :refer [response content-type]]
            [simple-brepl.service :refer [brepl-js]]))

;; This is all in one NS for now, but you'll likely want to split it
;; out when your webapp grows!

(def site-routes
  ["" {"/" {:get :page-handler}
       "/css" {"/site.css" {:get :site-css}}}])

(defn page-handler [cljs-compiler]
  (fn [req]
    (-> (response
         (html5
          [:head
           [:title "{{name}} - CLJS Single Page Web Application"]
     
           (include-js "//cdnjs.cloudflare.com/ajax/libs/jquery/2.0.3/jquery.min.js")
           (include-js "//netdna.bootstrapcdn.com/bootstrap/3.0.0/js/bootstrap.min.js")
           (include-css "//netdna.bootstrapcdn.com/bootstrap/3.0.0/css/bootstrap.min.css")

           [:script (brepl-js)]

           (include-js (get-in (cljs/compiler-settings cljs-compiler) [:modules :main]))
           (include-css (bidi/path-for site-routes :site-css :request-method :get))]
    
          [:body]))

        (content-type "text/html"))))

(defn site-handlers [cljs-compiler]
  {:page-handler (page-handler cljs-compiler)
   :site-css (fn [req]
                    (-> (response (css/site-css))
                        (content-type "text/css")))})

(def api-routes
  ["/api" {}])

(defn api-handlers []
  {})

(defrecord AppHandler []
  Lifecycle
  (start [this] this)
  (stop [this] this)

  WebRequestHandler
  (request-handler [{:keys [db cljs-compiler]}]
    (make-handler ["" [site-routes
                       api-routes
                       
                       (let [{:keys [web-context-path resource-prefix]} cljs-compiler]
                         [web-context-path (bidi.ring/resources {:prefix resource-prefix})])]]
                  
                  (some-fn (site-handlers cljs-compiler)
                           (api-handlers)
                           #(when (fn? %) %)))))
