(ns phoenix.modules.garden
  (:require [phoenix.build.protocols :as pbp]
            [bidi.ring :as br]
            [clojure.core.async :as a :refer [go-loop]]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.tools.logging :as log]
            [garden.core :as css]
            [com.stuartsierra.component :as c]))

(defprotocol GardenSnippet
  (css-snippet [_]))

(defprotocol ICSSCompiler
  (bidi-routes [_])
  (css-handler [_])
  (path-for-module [_ module]))

(defn configure-compiler [{:keys [target-path modules classpath-prefix] :as css-opts} build-mode]
  (let [output-dir (io/file target-path (name build-mode))]
    (assoc css-opts
      :pretty-print? (case build-mode
                       :dev true
                       :build false)
      :output-dir output-dir
      :modules (->> (for [[module-key snippets] modules]
                      [module-key {:snippets snippets
                                   :output-file (io/file output-dir classpath-prefix (str (name module-key) ".css"))}])
                    (into {})))))

(defn compile-snippets [{:keys [pretty-print? modules] :as css-opts}]
  (->> (for [snippet-key (->> modules
                              vals
                              (mapcat :snippets)
                              set)]
         (do
           (log/debugf "Compiling '%s' CSS snippet..." snippet-key)
           [snippet-key (css/css {:pretty-print? pretty-print?}
                                 (css-snippet (get css-opts snippet-key)))]))
       (into {})))

(defn write-snippets! [compiled-snippets {:keys [modules] :as css-opts}]
  (doseq [[module-key {:keys [output-file snippets]}] modules]
    (log/debugf "Writing '%s' CSS module to '%s'..." module-key (.getPath output-file))
    (spit (doto output-file io/make-parents)
          (->> snippets
               (map #(get compiled-snippets %))
               (s/join "\n")))))

(defn compile-css! [{:keys [target-path] :as css-opts}]
  (let [start-time (System/nanoTime)]
    (log/infof "Compiling CSS to '%s'..." target-path)

    (-> (compile-snippets css-opts)
        (write-snippets! css-opts))

    (log/infof "Compiled CSS to '%s', in %.2fs."
               target-path
               (/ (- (System/nanoTime) start-time) 1e9))))

(defrecord CSSCompiler []
  c/Lifecycle
  (start [css-opts]
    (let [configured-css-opts (configure-compiler css-opts :dev)]

      (compile-css! configured-css-opts)

      (assoc css-opts
        :configured-css-opts configured-css-opts)))

  (stop [css-opts]
    (dissoc css-opts :configured-css-opts))

  pbp/BuiltComponent
  (build [css-opts project]
    (let [{:keys [output-dir] :as configured-css-opts} (configure-compiler css-opts :build)]

      (compile-css! configured-css-opts)

      [configured-css-opts (update project :filespecs conj {:type :path
                                                            :path (.getPath output-dir)})]))

  ICSSCompiler
  (bidi-routes [{{:keys [web-context-path output-dir classpath-prefix] :as configured-css-opts} :configured-css-opts}]
    [web-context-path (br/files {:dir (.getPath (io/file output-dir classpath-prefix))})])

  (css-handler [this]
    (br/make-handler (bidi-routes this)))

  (path-for-module [{:keys [web-context-path]} module]
    (format "%s/%s.css" web-context-path (name module))))

(defrecord PreBuiltCSSComponent []
  ICSSCompiler
  (bidi-routes [{:keys [web-context-path classpath-prefix] :as this}]
    [web-context-path (br/resources {:prefix classpath-prefix})])

  (css-handler [this]
    (br/make-handler (bidi-routes this)))

  (path-for-module [{:keys [web-context-path]} module]
    (format "%s/%s.css" web-context-path (name module))))

(defn make-css-compiler [{built? :phoenix/built?, :as opts}]
  (if built?
    (map->PreBuiltCSSComponent opts)
    (map->CSSCompiler opts)))
