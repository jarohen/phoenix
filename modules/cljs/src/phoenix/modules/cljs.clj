(ns phoenix.modules.cljs
  (:require [phoenix.build.protocols :as pbp]
            [phoenix.modules.cljs.file-watcher :as watch]
            [bidi.ring :as br]
            [clojure.core.async :as a :refer [go-loop]]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [cljs.closure :as cljs]
            [cljs.env :as cljs-env]
            [com.stuartsierra.component :as c]))

(defprotocol ICLJSCompiler
  (bidi-routes [_])
  (cljs-handler [_])
  (path-for-js [_])
  (path-for-module [_ module]))

(defn normalise-output-locations [{:keys [web-context-path target-path classpath-prefix modules], :or {target-path "target/cljs"} :as opts} build-mode]
  (let [output-dir (doto (io/file target-path (name build-mode))
                     .mkdirs)
        mains-dir (doto (io/file output-dir "mains" (or classpath-prefix ""))
                       .mkdirs)
        module-dir (when (not-empty modules)
                     (doto (io/file mains-dir "modules")
                       .mkdirs))]
    (-> opts
        (cond-> (empty? modules) (assoc :output-to (.getPath (io/file mains-dir "main.js"))))

        (assoc :output-dir (.getPath output-dir)
               :target-path target-path
               :asset-path web-context-path)

        (update :modules (fn [modules]
                           (->> (for [[module-key module-opts] modules]
                                  [module-key (assoc module-opts
                                                :output-to (.getPath (io/file module-dir
                                                                              (str (name module-key) ".js"))))])
                                (into {})))))))

(defn build-cljs! [{:keys [source-path target-path] :as cljs-opts} cljs-compiler-env]
  (let [start-time (System/nanoTime)]
    (log/infof "Compiling CLJS, from '%s' to '%s'..." source-path target-path)
    (cljs/build source-path (into {} cljs-opts) cljs-compiler-env)
    (log/infof "Compiled CLJS, from '%s' to '%s', in %.2fs."
               source-path
               target-path
               (/ (- (System/nanoTime) start-time) 1e9))))

(defrecord CLJSCompiler []
  c/Lifecycle
  (start [{:keys [source-path] :as cljs-opts}]
    (let [component-latch-ch (a/chan)
          {file-change-ch :out-ch, file-watch-latch-ch :latch-ch} (watch/watch-files! (io/file source-path))

          {:keys [target-path], :as cljs-opts} (-> cljs-opts
                                                   (merge (:dev cljs-opts))
                                                   (normalise-output-locations :dev))
          cljs-compiler-env (cljs-env/default-compiler-env cljs-opts)]

      (build-cljs! cljs-opts cljs-compiler-env)

      (log/infof "Watching CLJS directory '%s'..." source-path)

      (go-loop []
        (a/alt!
          file-change-ch (do
                           (build-cljs! cljs-opts cljs-compiler-env)
                           (recur))

          component-latch-ch (a/close! file-watch-latch-ch)))

      (assoc cljs-opts
        ::component-latch-ch component-latch-ch)))

  (stop [{:keys [source-path ::component-latch-ch]}]
    (a/close! component-latch-ch)
    (log/infof "Stopped watching CLJS directory '%s'." source-path))

  pbp/BuiltComponent
  (build [{:keys [source-path] :as cljs-opts} project]
    (let [{:keys [output-dir] :as cljs-opts} (-> cljs-opts
                                                 (merge (:build cljs-opts))
                                                 (normalise-output-locations :build))
          cljs-compiler-env (cljs-env/default-compiler-env cljs-opts)]

      (build-cljs! cljs-opts cljs-compiler-env)

      [cljs-opts (update project :filespecs conj {:type :path
                                                  :path (.getPath (io/file output-dir "mains"))})]))

  ICLJSCompiler
  (bidi-routes [{:keys [web-context-path target-path] :as cljs-opts}]
    [web-context-path (br/files {:dir (.getPath (io/file target-path (name :dev)))})])

  (cljs-handler [this]
    (br/make-handler (bidi-routes this)))

  (path-for-js [{:keys [web-context-path]}]
    (format "%s/mains/main.js" web-context-path))

  (path-for-module [{:keys [web-context-path]} module]
    (format "%s/mains/modules/%s.js" web-context-path (name module))))

(comment
  (pbp/build (map->CLJSCompiler (:cljs-compiler @phoenix/!system)) {}))

(defrecord PreBuiltCLJSComponent []
  c/Lifecycle
  (start [this]
    this)
  (stop [this]
    this)

  ICLJSCompiler
  (bidi-routes [{:keys [web-context-path] :as this}]
    [web-context-path (br/resources {:prefix (get-in this [:build :classpath-prefix])})])

  (cljs-handler [this]
    (br/make-handler (bidi-routes this)))

  (path-for-js [{:keys [web-context-path]}]
    (format "%s/main.js" web-context-path))

  (path-for-module [{:keys [web-context-path]} module]
    (format "%s/modules/%s.js" web-context-path (name module))))

(defn make-cljs-compiler [{built? :phoenix/built?, :as opts}]
  (if built?
    (map->PreBuiltCLJSComponent opts)
    (map->CLJSCompiler opts)))
