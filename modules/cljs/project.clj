(defproject jarohen/phoenix.modules.cljs "0.0.1"
  :description "A module to compile and serve CLJS files"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.stuartsierra/component "0.2.2"]

                 [jarohen.forks/shadow-build "1.0.0-jh-20150112.144936-3"]
                 ;; Required for dep diamond. :/
                 [org.clojure/data.priority-map "0.0.5"]
                 
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/tools.logging "0.3.1"]
                 [bidi "1.15.0"]]

  :profiles {:dev {:dependencies [[org.clojure/clojurescript "0.0-2665"]]}})
