(defproject jarohen/phoenix.modules.cljs "0.1.0-SNAPSHOT"
  :description "A module to compile and serve CLJS files"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.stuartsierra/component "0.2.2"]
                 [thheller/shadow-build "0.5.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/tools.logging "0.3.1"]]

  :profiles {:dev {:dependencies [[org.clojure/clojurescript "0.0-2511"]]}})
