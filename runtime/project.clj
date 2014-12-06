(def version (slurp "../common/phoenix-version"))

(defproject jarohen/phoenix.runtime version
  :description "The runtime for the Phoenix plugin"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  
  :dependencies [[org.clojure/clojure "1.6.0"]]

  :resource-paths ["resources" "../common"])
