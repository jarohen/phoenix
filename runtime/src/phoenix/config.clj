(ns phoenix.config
  (:require [phoenix.location :as l]
            [clojure.walk :refer [postwalk]]
            [medley.core :as m]
            [clojure.set :as set]
            [com.stuartsierra.dependency :as deps]
            [com.stuartsierra.component :as c]))

(defn read-string-in-ns [s]
  (binding [*ns* (find-ns 'phoenix.config)]
    (read-string s)))

(defn load-config [config-resource]
  ;; TODO multiple config files
  (-> (slurp config-resource)
      read-string-in-ns
      (dissoc :phoenix/nrepl-port)))

(defn calculate-deps [config]
  (let [components (set (keys config))
        sorted-deps (deps/topo-sort (reduce (fn [graph [id {:keys [component component-config]}]]
                                              (reduce (fn [graph [dep-key dep-value]]
                                                        (if (and (vector? dep-value)
                                                                 (= (first dep-value) ::dep))
                                                          (deps/depend graph id (second dep-value))
                                                          graph))
                                                      graph
                                                      component-config))
          
                                            (deps/graph)
                                            config))
        
        ;; these components don't depend on anything or have anything
        ;; depend on them, but they still need to be in the list
        island-components (set/difference components (set sorted-deps))]
    
    (concat sorted-deps island-components)))

(defn normalise-deps [config]
  (->> (for [[k v] config]
         [k {:component (::component v)
             :component-config (if-not (map? v)
                                 v
                                 (->> (for [[config-key config-value] (dissoc v ::component)]
                                        [config-key (if (= config-value ::dep)
                                                      [::dep config-key]
                                                      config-value)])
                                      (into {})))}])
       
       (into {})))

(defn with-static-config [config sorted-deps]
  (reduce (fn [acc dep-key]
            (let [{:keys [component component-config]} (get config dep-key)]
              (assoc acc
                dep-key (if-not (map? component-config)
                          component-config
                          
                          (reduce (fn [component-acc [config-key config-value]]
                                    (if (and (vector? config-value)
                                             (= (first config-value) ::dep))
                                      (let [dependent-key (second config-value)
                                            {:keys [component static-config]} (get acc dependent-key)]
                                        (if (nil? component)
                                          (assoc-in component-acc [:static-config config-key] static-config)
                                          (update-in component-acc [:component-deps] assoc config-key dependent-key)))
                                      
                                      (assoc-in component-acc [:static-config config-key] config-value)))
                                  
                                  {:component component}
                                  
                                  component-config)))))
          {}
          sorted-deps))

(defn read-config [config-resource {:keys [location]}]
  (let [normalised-config (-> (load-config config-resource)
                              (l/combine-config location)
                              (normalise-deps))]
    (->> (with-static-config normalised-config (calculate-deps normalised-config))
         (m/filter-vals :component))))

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
