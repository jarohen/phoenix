(ns phoenix.config
  (:require [phoenix.location :as l]
            [phoenix.jar :as jar]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [com.stuartsierra.dependency :as deps]
            [medley.core :as m]))

(defn assert-config [config-resource]
  (assert (and config-resource
               (io/resource config-resource))
          "Please make sure you have a valid config resource specified at ':phoenix/config' in your 'project.clj'"))

(defn read-string-in-ns [s]
  (binding [*ns* (find-ns 'phoenix.config)]
    (read-string s)))

(defn load-config [config-resource]
  ;; TODO multiple config files
  (-> (slurp config-resource)
      read-string-in-ns
      (dissoc :phoenix/nrepl-port)))

(defn normalise-deps [config]
  (m/map-vals (fn [component-config]
                (reduce (fn [acc [k v]]
                          (cond
                            (= ::component k) (assoc acc
                                                :component v)
              
                            (= v ::dep) (assoc-in acc [:component-deps k] k)
              
                            (and (vector? v)
                                 (= (first v) ::dep))
                            (assoc-in acc [:component-deps k] (second v))

                            :otherwise (assoc-in acc [:component-config k] v)))

                        {:phoenix/built? jar/built?}
                        component-config))
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
                                
                                {:component component
                                 :component-deps component-deps
                                 :static-config component-config}
                                
                                component-deps))))
          {}
          sorted-deps))

(defn read-config [config-resource {:keys [location]}]
  (let [normalised-config (-> (load-config config-resource)
                              (l/combine-config location)
                              (normalise-deps))
        sorted-deps (calculate-deps normalised-config)]
    (-> (with-static-config normalised-config sorted-deps)
        (with-meta {:sorted-deps sorted-deps}))))

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
