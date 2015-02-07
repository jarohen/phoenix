(ns phoenix.config
  (:require [phoenix.location :as l]
            [phoenix.merge :as pm]
            [phoenix.jar :as jar]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as s]
            [clojure.tools.logging :as log]
            [com.stuartsierra.dependency :as deps]
            [camel-snake-kebab.core :as csk]
            [clojure.tools.reader.edn :as edn]
            [medley.core :as m]))

(defn assert-config [config-resource]
  (assert (and config-resource
               (io/resource config-resource))
          "Please make sure you have a valid config resource specified at ':phoenix/config' in your 'project.clj'"))

(def phoenix-readers
  {'phoenix/file (comp io/file #(s/replace % #"^~" (System/getProperty "user.home")))
   'phoenix/resource (some-fn io/resource
                              
                              (fn [path]
                                (log/warn "Can't read config-file:" path)
                                ::invalid-include))})

(defn parse-config [s]
  (when (string? s)
    (edn/read-string {:readers phoenix-readers}
                     s)))

(defn try-slurp [slurpable]
  (try
    (slurp slurpable)
    
    (catch Exception e
      (log/warn "Can't read config-file:" slurpable)
      ::invalid-include)))

(defn ->seq [el-or-coll]
  (if (coll? el-or-coll)
    el-or-coll
    [el-or-coll]))

(defn load-config [config-resource location]
  (loop [[config-resource & more-resources :as config-resources] [config-resource]
         loaded-resources #{}
         config {}]

    (if (empty? config-resources)
      config

      (if-not config-resource
        (recur more-resources loaded-resources config)
        
        (let [new-config (-> (try-slurp config-resource)
                             parse-config
                             (l/combine-config location))]

          (recur (concat more-resources (->> (:phoenix/includes new-config)
                                             ->seq
                                             (remove #(contains? loaded-resources %))
                                             (remove #{::invalid-include})))
                 
                 (conj loaded-resources config-resource)
                 
                 (pm/deep-merge config (dissoc new-config
                                         :phoenix/includes))))))))

(defmulti process-config-pair
  (fn [config acc k v]
    (or (#{:phoenix/component} k)
        (#{:phoenix/dep} v)

        (and (vector? v)
             (let [[fst & more] v]
               (when (and (keyword? fst)
                          (= (namespace fst) "phoenix"))
                 fst))))))

(defmethod process-config-pair :phoenix/component [_ acc _ v]
  (assoc acc :component v))

(defmethod process-config-pair :phoenix/dep [_ acc k v]
  (let [referred-component (or (when (= v :phoenix/dep)
                                       k)
                                     (when (vector? v)
                                       (second v)))]
    
    (assoc-in acc [:component-deps k] referred-component)))

(defn read-env-var [var-name]
  (System/getenv (csk/->SNAKE_CASE_STRING var-name)))

(defmethod process-config-pair :phoenix/env-var [_ acc k [_ var-name default]]
  (assoc-in acc [:component-config k] (or (read-env-var var-name) default)))

(defmethod process-config-pair :phoenix/edn-env-var [_ acc k [_ var-name default]]
  (letfn [(try-read-string [s]
            (try
              (when s
                (edn/read-string s))
              (catch Exception e
                (throw (ex-info "Phoenix: failed reading env-var"
                                {:env-var var-name
                                 :value s})))))]
    (assoc-in acc [:component-config k] (or (try-read-string (read-env-var var-name)) default))))

(defmethod process-config-pair :phoenix/secret [{:keys [:phoenix/secret-keys]} acc k [_ secret-key-name [cypher-text iv]]]
  (let [secret-key (get secret-keys secret-key-name)]
    (assert secret-key (format "Phoenix: can't find secret key '%s'" secret-key-name))
    
    (assoc-in acc [:component-config k] (ps/decrypt [cypher-text iv] (get secret-keys secret-key-name)))))

(defmethod process-config-pair :default [_ acc k v]
  (assoc-in acc [:component-config k] v))

(defn process-config [config]
  (m/map-vals (fn [component-config]
                (if-not (map? component-config)
                  {:component-config component-config}
                  
                  (reduce (fn [acc [k v]]
                            (process-config-pair config acc k v))

                          {:component-config {:phoenix/built? jar/built?}}
                          component-config)))
              config))

(defn calculate-deps [config]
  (->> (reduce (fn [graph [id {:keys [component component-deps]}]]
                 (reduce (fn [graph [dep-key dep-value]]
                           (deps/depend graph id dep-value))
                         (-> graph
                             (deps/depend ::system id))
                         
                         component-deps))
               
               (deps/graph)
               config)
       
       deps/topo-sort
       (remove #{::system})))

(defn with-static-config [config sorted-deps]
  (reduce (fn [acc dep-key]
            (let [{:keys [component component-deps component-config]} (get config dep-key)]
              (assoc acc
                dep-key (reduce (fn [component-acc [config-key dependent-key]]
                                  (let [{:keys [component static-config]} (get acc dependent-key)]
                                    (if (nil? component)
                                      (assoc-in component-acc [:static-config config-key] static-config)
                                      (update-in component-acc [:component-deps] assoc config-key dependent-key))))
                                
                                {:component-id dep-key
                                 :component component
                                 :component-deps component-deps
                                 :static-config component-config}
                                
                                component-deps))))
          {}
          sorted-deps))

(defn read-config [config-resource {:keys [location]}]
  (let [processed-config (-> (load-config config-resource location)
                             (process-config))
        sorted-deps (calculate-deps processed-config)]
    (-> (with-static-config processed-config sorted-deps)
        (with-meta {:sorted-deps sorted-deps}))))

(comment
  (with-redefs [slurp {(io/file "/home/james/config.edn") "{:phoenix/includes [#phoenix/file \"~/config-include.edn\"]
                                                            :config {:a 1, :c 3}}"
                       (io/file "/home/james/config-include.edn") "{:phoenix/includes [#phoenix/file \"~/config-include2.edn\"]
                                                                    :config {:b 2, :c 4}}"
                       (io/file "/home/james/config-include2.edn") "{:config {:c 5, :d 10}
                                                                     :a 2}"}]
    (load-config (io/file "/home/james/config.edn") (l/get-location))))

(comment
  (let [config (-> {:c {:phoenix/component 'my.ns/function
                        :map-a :phoenix/dep
                        :c1 :phoenix/dep
                        :host "some-host"}

                    :c1 {:phoenix/component 'my.ns/function-1
                         :a [:phoenix/dep :map-a]}
              
                    :map-a {:a 1
                            :b 2
                            :c [:phoenix/env-var :lein-home]}}

                   process-config)
        sorted-deps (calculate-deps config)]
    (with-static-config config sorted-deps)))
