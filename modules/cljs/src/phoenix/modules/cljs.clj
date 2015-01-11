(ns ^{:clojure.tools.namespace.repl/load false
      :clojure.tools.namespace.repl/unload false}

  phoenix.modules.cljs
  
  (:require [com.stuartsierra.component :as c]
            [shadow.cljs.build :as cljs]
            [clojure.java.io :as io]
            [clojure.core.async :as a :refer [go-loop]]
            [clojure.tools.logging :as log]
            [bidi.ring :as br]
            [bidi.server :as bs]))

(defonce !initial-state
  (future
    (-> (cljs/init-state)
        (assoc :logger (reify cljs/BuildLog
                         (log-warning [_ msg]
                           (log/log 'shadow.cljs.build :warn nil msg))

                         (log-progress [_ msg]
                           (log/log 'shadow.cljs.build :debug nil msg))

                         (log-time-start [_ msg]
                           (log/log 'shadow.cljs.build :info nil (format "-> %s" msg)))

                         (log-time-end [_ msg ms]
                           (log/log 'shadow.cljs.build :info nil (format "<- %s (%dms)" msg ms)))))
        (cljs/step-find-resources-in-jars))))

(defprotocol CLJSComponent
  (bidi-routes [_])
  (cljs-handler [_])
  (path-for-module [_ module]))

(defn add-modules [state modules]
  (reduce (fn [state {:keys [name mains dependencies]}]
            (cljs/step-configure-module state name mains dependencies))
          state
          modules))

(defn init-compiler-state [{:keys [source-maps? pretty-print? modules optimizations
                                   web-context-path externs output-dir source-path]
                            :or {pretty-print? true
                                 optimizations :none}
                            :as opts}]
  (-> @!initial-state
      (cond-> source-maps? (cljs/enable-source-maps))
      (assoc :optimizations optimizations
             :pretty-print pretty-print?
             :public-path web-context-path
             :public-dir output-dir
             :externs externs)
      
      (cljs/step-find-resources source-path)
      
      (add-modules modules)
      (cljs/step-finalize-config)
      (cljs/step-compile-core)))

(defn do-compile-run [{:keys [optimizations] :as state}]
  (log/info "Compiling CLJS...")
  
  (let [state-with-compiled-modules (-> state
                                        (cljs/step-reload-modified)
                                        (cljs/step-compile-modules))

        _ (log/info "Compiled CLJS.")
        
        optimized-state (if (and optimizations
                                 (not= optimizations :none))
                          (let [_ (log/info "Optimizing CLJS...")

                                optimized-state (-> state-with-compiled-modules
                                                    (cljs/closure-optimize)
                                                    (cljs/flush-to-disk)
                                                    (cljs/flush-modules-to-disk))
                                
                                _ (log/info "Optimized CLJS.")]
                            
                            optimized-state)
                          
                          (-> state-with-compiled-modules
                              (cljs/flush-unoptimized)))]
    
    
    
    optimized-state))

(defrecord CLJSCompiler []
  c/Lifecycle
  (start [{:keys [optimizations] :as this}]
    (let [stop-ch (a/chan)
          {:keys [modules] :as initial-state} (init-compiler-state this)]
      
      (go-loop [cljs-state initial-state]
        (let [new-state (do-compile-run cljs-state)]
          (a/alt!
            (a/thread (cljs/wait-for-modified-files! new-state))
            ([modified-files] (recur (cljs/reload-modified-files! new-state modified-files)))

            stop-ch nil)))

      (assoc this
        ::stop-ch stop-ch
        :modules modules)))
  
  (stop [{stop-ch ::stop-ch}]
    (a/close! stop-ch))

  CLJSComponent
  (bidi-routes [{:keys [web-context-path output-dir]}]
    [web-context-path (bs/files {:dir output-dir})])

  (cljs-handler [this]
    (br/make-handler (bidi-routes this)))

  (path-for-module [{:keys [web-context-path modules]} module]
    (format "%s/%s" web-context-path (get-in modules [module :js-name]))))

(defrecord PreBuiltCLJSComponent []
  c/Lifecycle
  (start [this]
    this)
  (stop [this]
    this)

  CLJSComponent
  (bidi-routes [{:keys [web-context-path classpath-prefix] :as this}]
    [web-context-path (bs/resources {:prefix classpath-prefix})])

  (cljs-handler [this]
    (br/make-handler (bidi-routes this)))

  (path-for-module [{:keys [web-context-path modules]} module]
    (format "%s/%s" web-context-path (get-in modules [module :js-name]))))

(defn combine-opts [opts mode]
  (-> opts
      (dissoc :dev :build)
      (merge (get opts mode))))

(defn make-cljs-compiler [{built? :phoenix/built?, :as opts}]
  (if built?
    (map->PreBuiltCLJSComponent (combine-opts opts :build))
    (map->CLJSCompiler (combine-opts opts :dev))))

(comment
  (def foo-state
    (-> (cljs/init-state)
        (assoc :optimizations :none
               :pretty-print true
               :work-dir (io/file "target/cljs-work")
               :cache-dir (io/file "target/cljs-cache")
               :public-dir (io/file "target/resources/js")
               :public-path "/js"
               :logger (reify cljs/BuildLog
                         (log-warning [_ msg]
                           (log/log 'shadow.cljs.build :warn nil msg))

                         (log-progress [_ msg]
                           (log/log 'shadow.cljs.build :debug nil msg))

                         (log-time-start [_ msg]
                           (log/log 'shadow.cljs.build :info nil (format "-> %s" msg)))

                         (log-time-end [_ msg ms]
                           (log/log 'shadow.cljs.build :info nil (format "<- %s (%dms)" msg ms)))))
      
        (cljs/step-find-resources-in-jars)
        (cljs/step-find-resources "ui-src")

        (cljs/step-finalize-config)
        (cljs/step-compile-core)
        (cljs/step-configure-module :main ['test-project.ui.app] #{})
      
        (cljs/step-compile-modules)
        (cljs/flush-unoptimized))))

