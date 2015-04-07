(ns phoenix.references
  (:require [phoenix.secret :as ps]
            [camel-snake-kebab.core :as csk]
            [clojure.tools.reader.edn :as edn]
            [clojure.walk :as w]))

(defmulti resolve-reference
  (fn [[ref-type & args] config]
    ref-type))

(defn try-read-edn [s handle-error]
  (try
    (when s
      (edn/read-string s))
    (catch Exception e
      (handle-error))))

(defmethod resolve-reference :phoenix/env-var [[_ var-name default] config]
  (or (System/getenv (csk/->SCREAMING_SNAKE_CASE_STRING var-name)) default))

(defmethod resolve-reference :phoenix/edn-env-var [[_ var-name default] config]
  (let [env-value (resolve-reference [:phoenix/env-var var-name default] config)]
    (try-read-edn env-value #(throw (ex-info "Phoenix: failed reading edn-env-var"
                                             {:env-var var-name
                                              :value env-value})))))

(defmethod resolve-reference :phoenix/jvm-prop [[_ prop-name default] config]
  (System/getProperty (name prop-name) default))

(defmethod resolve-reference :phoenix/edn-jvm-prop [[_ prop-name default] config]
  (let [prop-value (resolve-reference [:phoenix/jvm-prop prop-name default] config)]
    (try-read-edn prop-value #(throw (ex-info "Phoenix: failed reading edn-jvm-prop"
                                             {:jvm-prop prop-name
                                              :value prop-value})))))

(defmethod resolve-reference :phoenix/secret [[_ secret-key-name cypher-text] {:keys [:phoenix/secret-keys]}]
  (let [secret-key (get secret-keys secret-key-name)]
    (assert secret-key (format "Phoenix: can't find secret key '%s'" secret-key-name))

    (ps/decrypt cypher-text (get secret-keys secret-key-name))))

(defmethod resolve-reference :default [ref _]
  ref)

(defn resolve-references [config]
  (w/postwalk (fn [obj]
                (or (and (vector? obj)
                         (let [[vec-key :as reference] obj]
                           (when (and (keyword? vec-key)
                                      (= "phoenix" (namespace vec-key)))
                             (resolve-reference reference config))))
                    obj))
              config))
