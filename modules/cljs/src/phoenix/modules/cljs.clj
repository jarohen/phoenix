(ns ^{:clojure.tools.namespace.repl/load false
      :clojure.tools.namespace.repl/unload false}

  phoenix.modules.cljs
  
  (:require [phoenix.build :as pb]
            [com.stuartsierra.component :as c]
            [shadow.cljs.build :as cljs]
            [clojure.java.io :as io]
            [clojure.core.async :as a :refer [go-loop]]
            [clojure.tools.logging :as log]
            [bidi.ring :as br]
            [bidi.server :as bs]))

(defprotocol CLJSComponent
  (bidi-routes [_])
  (cljs-handler [_])
  (path-for-module [_ module]))

(defn add-modules [state modules]
  (reduce (fn [state {:keys [name mains dependencies]}]
            (cljs/step-configure-module state name mains dependencies))
          state
          modules))

(defn initial-cached-compiler []
  (-> (cljs/init-state)
      (assoc :logger (reify cljs/BuildLog
                       (log-warning [_ msg]
                         (log/log 'shadow.cljs.build :warn nil msg))

                       (log-progress [_ msg]
                         (log/log 'shadow.cljs.build :trace nil msg))

                       (log-time-start [_ msg]
                         (log/log 'shadow.cljs.build :debug nil (format "-> %s" msg)))

                       (log-time-end [_ msg ms]
                         (log/log 'shadow.cljs.build :debug nil (format "<- %s (%dms)" msg ms)))))
      
      (cljs/step-find-resources-in-jars)))

(defonce !initial-state
  (delay
    (initial-cached-compiler)))

(defn init-compiler-state [state
                           {:keys [source-maps? pretty-print? modules optimizations
                                   web-context-path externs output-dir public-dir source-path]
                            :as opts}]
  (-> state   
      (assoc :optimizations optimizations
             :pretty-print pretty-print?
             :public-path web-context-path
             :public-dir (or public-dir
                             (io/file output-dir "public"))
             :cache-dir (io/file output-dir "cache")
             :work-dir (io/file output-dir "work")
             :externs externs)
      
      (cond-> source-maps? (cljs/enable-source-maps))
      
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
  (start [this]
    (let [stop-ch (a/chan)
          {:keys [modules] :as initial-state} (-> (init-compiler-state @!initial-state
                                                                       (assoc this
                                                                         :optimizations (get-in this [:dev :optimizations] :none)
                                                                         :pretty-print? (get-in this [:dev :pretty-print?] true)))
                                                  do-compile-run)]
      
      (go-loop [cljs-state initial-state]
        (a/alt!
          (a/thread (cljs/wait-for-modified-files! cljs-state))
          ([modified-files] (recur (-> (cljs/reload-modified-files! cljs-state modified-files)
                                       do-compile-run)))

          stop-ch nil))

      (assoc this
        ::stop-ch stop-ch
        :configured-modules modules)))
  
  (stop [{stop-ch ::stop-ch}]
    (a/close! stop-ch))

  pb/BuiltComponent
  (build [{:keys [output-dir], :or {output-dir (io/file "target/cljs")}, :as this} project]
    (log/info "Building CLJS...")
    
    (let [build-output-dir (io/file output-dir "build")
          jar-dir (io/file output-dir "jar")]
      (-> @!initial-state
          (init-compiler-state (assoc this
                                 :optimizations (get-in this [:build :optimizations] :advanced)
                                 :pretty-print? (get-in this [:build :pretty-print?] false)
                                 :output-dir build-output-dir
                                 :public-dir (io/file jar-dir (get-in this [:build :classpath-prefix]))))
          (doto (#(intern 'user 'state %)))
          do-compile-run)

      (log/info "Built CLJS.")
      
      [this (update project :filespecs conj {:type :path
                                             :path (.getPath jar-dir)})]))
  
  CLJSComponent
  (bidi-routes [{:keys [web-context-path output-dir]}]
    [web-context-path (bs/files {:dir (.getPath (io/file output-dir "public"))})])

  (cljs-handler [this]
    (br/make-handler (bidi-routes this)))

  (path-for-module [{:keys [web-context-path configured-modules]} module]
    (format "%s/%s" web-context-path (get-in configured-modules [module :js-name]))))

(defrecord PreBuiltCLJSComponent []
  c/Lifecycle
  (start [this]
    this)
  (stop [this]
    this)

  CLJSComponent
  (bidi-routes [{:keys [web-context-path] :as this}]
    [web-context-path (bs/resources {:prefix (get-in this [:build :classpath-prefix])})])

  (cljs-handler [this]
    (br/make-handler (bidi-routes this)))

  (path-for-module [{:keys [web-context-path modules]} module]
    (format "%s/%s.js" web-context-path (name module))))

(defn make-cljs-compiler [{built? :phoenix/built?, :as opts}]
  (if built?
    (map->PreBuiltCLJSComponent opts)
    (map->CLJSCompiler opts)))
