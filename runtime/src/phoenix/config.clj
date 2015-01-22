(ns phoenix.config
  (:require [phoenix.location :as l]
            [phoenix.merge :as pm]
            [phoenix.jar :as jar]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as s]
            [clojure.tools.reader.edn :as edn]
            [clojure.tools.logging :as log]
            [com.stuartsierra.dependency :as deps]
            [medley.core :as m]))

(defn assert-config [config-resource]
  (assert (and config-resource
               (io/resource config-resource))
          "Please make sure you have a valid config resource specified at ':phoenix/config' in your 'project.clj'"))

(def phoenix-readers
  {'phoenix/file (comp io/file #(s/replace % #"^~" (System/getProperty "user.home")))
   'phoenix/resource (some-fn io/resource
                              #(throw (ex-info "Phoenix: can't find resource"
                                               {:resource %})))})

(defn parse-config [s]
  (binding [*ns* (find-ns 'phoenix.config)]
    (edn/read-string {:readers phoenix-readers} s)))

(defn try-slurp [slurpable]
  (try
    (slurp slurpable)
    (catch Exception e
      (log/warn "Can't find config:" slurpable))))

(defn ->seq [el-or-coll]
  (if (coll? el-or-coll)
    el-or-coll
    [el-or-coll]))

(defn load-config [config-resource location]
  (loop [[config-resource & more-resources :as config-resources] [config-resource]
         loaded-resources #{}
         config {}]

    (prn config-resources)
    
    (if (empty? config-resources)
      config

      (if-not config-resource
        (recur more-resources loaded-resources config)
        
        (let [new-config (-> (try-slurp config-resource)
                             parse-config
                             (l/combine-config location))]

          (recur (concat more-resources (->> (:phoenix/includes new-config)
                                             ->seq
                                             (remove #(contains? loaded-resources %))))
                 
                 (conj loaded-resources config-resource)
                 
                 (pm/deep-merge config (dissoc new-config
                                         :phoenix/includes))))))))

(defn normalise-deps [config]
  (m/map-vals (fn [component-config]
                (if-not (map? component-config)
                  {:component-config component-config}
                  
                  (reduce (fn [acc [k v]]
                            (cond
                              (= ::component k) (assoc acc
                                                  :component v)
              
                              (= v ::dep) (assoc-in acc [:component-deps k] k)
              
                              (and (vector? v)
                                   (= (first v) ::dep))
                              (assoc-in acc [:component-deps k] (second v))

                              :otherwise (assoc-in acc [:component-config k] v)))

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
  (let [normalised-config (-> (load-config config-resource location)
                              (normalise-deps))
        sorted-deps (calculate-deps normalised-config)]
    (-> (with-static-config normalised-config sorted-deps)
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
  (let [config (-> {:c {::component 'my.ns/function
                        :map-a ::dep
                        :c1 ::dep
                        :host "some-host"}

                    :c1 {::component 'my.ns/function-1
                         :a [::dep :map-a]}
              
                    :map-a {:a 1
                            :b 2}}

                   normalise-deps)
        sorted-deps (calculate-deps config)]
    (with-static-config config sorted-deps)))
