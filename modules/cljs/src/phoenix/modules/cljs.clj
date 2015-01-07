(ns ^{:clojure.tools.namespace.repl/load false
      :clojure.tools.namespace.repl/unload false}

  phoenix.modules.cljs
  
  (:require [com.stuartsierra.component :as c]
            [shadow.cljs.build :as cljs]
            [clojure.java.io :as io]
            [clojure.core.async :as a :refer [go-loop]]
            [clojure.tools.logging :as log]))

(defonce !initial-state
  (future
    (log/with-logs ['shadow.cljs.build :debug :warn]
      (-> (cljs/init-state)
          (cljs/step-find-resources-in-jars)
          (cljs/step-finalize-config)
          (cljs/step-compile-core)
          #_(cljs/step-find-resources "lib/js-closure" {:reloadable false})))))

(defprotocol CLJSComponent
  (compiler-settings [_]))

(defn add-modules [state modules]
  (reduce (fn [state {:keys [name mains dependencies]}]
            (cljs/step-configure-module state name mains dependencies))
          state
          modules))

(defn init-compiler-state [{:keys [source-maps? pretty-print? optimizations modules
                                   web-context-path externs output-dir source-path]
                            :or {pretty-print? true
                                 optimizations :none}
                            :as opts}]
  (log/with-logs ['shadow.cljs.build :debug :warn]
    (-> @!initial-state
        (cond-> source-maps? (cljs/enable-source-maps))
        (assoc :optimizations optimizations
               :pretty-print pretty-print?
               :public-path web-context-path
               :public-dir output-dir
               :externs externs)
        
        (cljs/step-find-resources source-path)
        
        (add-modules modules))))

(defrecord CLJSCompiler []
  c/Lifecycle
  (start [{:keys [optimizations] :as this}]
    (let [stop-ch (a/chan)
          {:keys [modules] :as initial-state} (init-compiler-state this)]
      
      (go-loop [cljs-state initial-state]
        (log/info "Compiling CLJS")
        
        (let [new-state (let [compiled-state (log/with-logs ['shadow.cljs.build :info :warn]
                                               (-> cljs-state
                                                   (cljs/step-reload-modified)
                                                   (cljs/step-compile-modules)))]
                          
                          (log/with-logs ['shadow.cljs.build :debug :warn]
                            (if (and optimizations
                                     (not= optimizations :none))
                              (-> compiled-state
                                  (cljs/closure-optimize)
                                  (cljs/flush-to-disk)
                                  (cljs/flush-modules-to-disk))
                              
                              (-> compiled-state
                                  (cljs/flush-unoptimized)))))]
          
          (log/info "Compiled CLJS.")
          
          (a/alt!
            (a/thread (log/with-logs ['shadow.cljs.build :info :warn]
                        (cljs/wait-and-reload! new-state)))
            ([new-state]
             (recur new-state))

            stop-ch nil)))

      (assoc this
        ::stop-ch stop-ch
        :modules modules)))
  
  (stop [{stop-ch ::stop-ch}]
    (a/close! stop-ch))

  CLJSComponent
  (compiler-settings [{:keys [web-context-path modules resource-prefix]}]
    {:web-context-path web-context-path
     :resource-prefix resource-prefix
     :modules (->> (for [[module {:keys [js-name]}] modules]
                     [module (format "%s/%s" web-context-path js-name)])
                   (into {}))}))

(defn make-cljs-compiler [opts]
  (map->CLJSCompiler opts))
