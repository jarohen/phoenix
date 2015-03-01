(ns phoenix.parser
  (:require [phoenix.jar :as jar]
            [phoenix.secret :as ps]
            [camel-snake-kebab.core :as csk]
            [clojure.tools.reader.edn :as edn]
            [medley.core :as m]))

(defmulti parse-config-pair
  (fn [config acc k v]
    (or (#{:phoenix/component} k)
        (#{:phoenix/dep} v)

        (and (vector? v)
             (let [[fst & more] v]
               (when (and (keyword? fst)
                          (= (namespace fst) "phoenix"))
                 fst))))))

(defmethod parse-config-pair :phoenix/dep [_ acc k v]
  (let [referred-component (or (when (= v :phoenix/dep)
                                 k)
                               (when (vector? v)
                                 (second v)))]

    (assoc-in acc [:component-deps k] referred-component)))

(defmethod parse-config-pair :phoenix/component [_ acc _ v]
  (assoc acc :component-fn v))

(defn read-env-var [var-name]
  (System/getenv (csk/->SNAKE_CASE_STRING var-name)))

(defmethod parse-config-pair :phoenix/env-var [_ acc k [_ var-name default]]
  (assoc-in acc [:component-config k] (or (read-env-var var-name) default)))

(defmethod parse-config-pair :phoenix/edn-env-var [_ acc k [_ var-name default]]
  (let [env-value (read-env-var var-name)
        edn-env-value (try
                        (when env-value
                          (edn/read-string env-value))
                        (catch Exception e
                          (throw (ex-info "Phoenix: failed reading env-var"
                                          {:env-var var-name
                                           :value env-value}))))]

    (assoc-in acc [:component-config k] (or edn-env-value default))))

(defmethod parse-config-pair :phoenix/secret [{:keys [:phoenix/secret-keys]} acc k [_ secret-key-name cypher-text]]
  (let [secret-key (get secret-keys secret-key-name)]
    (assert secret-key (format "Phoenix: can't find secret key '%s'" secret-key-name))

    (assoc-in acc [:component-config k] (ps/decrypt cypher-text (get secret-keys secret-key-name)))))

(defmethod parse-config-pair :default [_ acc k v]
  (assoc-in acc [:component-config k] v))

(defn parse-config [config]
  (->> config
       (m/map-vals (fn [component-config]
                     (if-not (map? component-config)
                       {:component-config component-config}

                       (reduce (fn [acc [k v]]
                                 (parse-config-pair config acc k v))

                               {:component-config {:phoenix/built? jar/built?}}
                               component-config))))))
