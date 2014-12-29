(ns phoenix-sample.service.handler
  (:require [phoenix-sample.service.css :as css]
            [phoenix.modules.cljs :as cljs]
            [bidi.bidi :as bidi]
            [bidi.ring :refer [make-handler]]
            [com.stuartsierra.component :refer [Lifecycle]]
            [hiccup.page :refer [html5 include-css include-js]]
            [modular.ring :refer [WebRequestHandler]]
            [ring.util.response :refer [response content-type]]))

(def site-routes
  ["" {"/" {:get ::page-handler}
       "/css" {"/site.css" {:get ::site-css}}}])

(defn page-handler [cljs-compiler]
  (fn [req]
    (response
     (html5
      [:head
       [:title "Sample Application"]
     
       (include-js "//cdnjs.cloudflare.com/ajax/libs/jquery/2.0.3/jquery.min.js")
       (include-js "//netdna.bootstrapcdn.com/bootstrap/3.0.0/js/bootstrap.min.js")
       (include-css "//netdna.bootstrapcdn.com/bootstrap/3.0.0/css/bootstrap.min.css")

       (include-js (get-in (cljs/compiler-settings cljs-compiler) [:modules :main]))
       (include-css (bidi/path-for site-routes ::site-css :request-method :get))

       [:script "phoenix_sample.ui.app.test();"]]
    
      [:body]))))

(defn site-handlers [cljs-compiler]
  {::page-handler (page-handler cljs-compiler)
   ::site-css (fn [req]
                (-> (response (css/site-css))
                    (content-type "text/css")))})

(defrecord AppHandler []
  Lifecycle
  (start [this] this)
  (stop [this] this)

  WebRequestHandler
  (request-handler [{:keys [db cljs-compiler]}]
    (make-handler ["" [site-routes
                       
                       (let [{:keys [web-context-path resource-prefix]} cljs-compiler]
                         [web-context-path (bidi.ring/resources {:prefix resource-prefix})])]]
                  
                  (some-fn (site-handlers cljs-compiler)
                           #(when (fn? %) %)))))
