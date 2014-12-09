(def version (slurp "../common/phoenix-version"))

(defproject jarohen/phoenix.runtime version
  :description "The runtime for the Phoenix plugin"

  :url "https://github.com/james-henderson/phoenix"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.namespace "0.2.7"]
                 [org.clojure/tools.nrepl "0.2.6"]                 
                 [com.stuartsierra/component "0.2.2"]
                 [com.stuartsierra/dependency "0.1.1"]
                 [medley "0.5.3"]]
  
  :resource-paths ["resources" "../common"])
