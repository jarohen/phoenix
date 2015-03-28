(ns phoenix.analyzer
  (:require [phoenix.jar :as jar]
            [phoenix.secret :as ps]
            [camel-snake-kebab.core :as csk]
            [clojure.tools.reader.edn :as edn]
            [medley.core :as m]))

(defmulti analyze-config-pair
  (fn [config acc k v]
    (or (#{:phoenix/component} k)
        (#{:phoenix/dep} v))))

(defmethod analyze-config-pair :phoenix/dep [_ acc k v]
  (let [referred-component (or (when (= v :phoenix/dep)
                                 k)
                               (when (vector? v)
                                 (second v)))]

    (assoc-in acc [:component-deps k] referred-component)))

(defmethod analyze-config-pair :phoenix/component [_ acc _ v]
  (assoc acc :component-fn v))

(defmethod analyze-config-pair :default [_ acc k v]
  (assoc-in acc [:component-config k] v))

(defn analyze-config [config]
  (->> config
       (m/map-vals (fn [component-config]
                     (if-not (map? component-config)
                       {:component-config component-config}

                       (reduce (fn [acc [k v]]
                                 (analyze-config-pair config acc k v))

                               {:component-config {:phoenix/built? jar/built?}}
                               component-config))))))
