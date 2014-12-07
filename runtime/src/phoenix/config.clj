(ns phoenix.config
  (:require [clojure.walk :refer [postwalk]]
            [medley.core :as m]
            [com.stuartsierra.dependency :as deps]
            [com.stuartsierra.component :as c]))

(defn read-string-in-ns [s]
  (binding [*ns* (find-ns 'phoenix.config)]
    (read-string s)))

(defn load-config [config-resource]
  ;; TODO multiple config files
  (-> (slurp config-resource)
      read-string-in-ns
      (dissoc :phoenix/nrepl-port)
      (->> (m/map-keys (fn [k]
                         {:named k})))))

(defn resolve-location [config]
  ;; TODO combine environments etc
  config)

(defn calculate-deps [config]
  (deps/topo-sort (reduce (fn [graph [id {:keys [component component-config]}]]
                            (reduce (fn [graph [dep-key dep-value]]
                                      (if (and (vector? dep-value)
                                               (= (first dep-value) ::dep))
                                        (deps/depend graph id (second dep-value))
                                        graph))
                                    graph
                                    component-config))
          
                          (deps/graph)
                          config)))

(defn normalise-deps [config]
  (->> (for [[k v] config]
         [k {:component (::component v)
             :component-config (if-not (map? v)
                                 v
                                 (->> (for [[config-key config-value] (dissoc v ::component)]
                                        [config-key (if (= config-value ::dep)
                                                      [::dep {:named config-key}]
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
                                      (let [{:keys [component static-config]} (get acc (second config-value))]
                                        (if (nil? component)
                                          (assoc-in component-acc [:static-config config-key] static-config)
                                          (update-in component-acc [:component-deps] assoc config-key (second config-value))))
                                      
                                      (assoc-in component-acc [:static-config config-key] config-value)))
                                  
                                  {:component component}
                                  
                                  component-config)))))
          {}
          sorted-deps))

(defn read-config [config-resource]
  (let [config (-> (load-config config-resource)
                   (resolve-location)
                   (normalise-deps))]
    
    (->> (with-static-config config (calculate-deps config))
         (m/filter-vals :component))))

(comment
  (let [config (-> {:c {::component 'my.ns/function
                        :map-a ::dep
                        :c1 ::dep
                        :host "some-host"}

                    :c1 {::component 'my.ns/function-1
                         :a [::dep {:named :map-a}]}
              
                    :map-a {:a 1
                            :b 2}}

                   (->> (m/map-keys (fn [k]
                                      {:named k})))
                   normalise-deps)
        sorted-deps (calculate-deps config)]
    (with-static-config config sorted-deps)))
