(defproject jarohen/phoenix.modules.cljs "0.0.3"
  :description "A module to compile and serve CLJS files"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.7.0-alpha5"]
                 [com.stuartsierra/component "0.2.3"]

                 [jarohen/phoenix.build "0.0.1"]

                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/tools.logging "0.3.1"]
                 [bidi "1.15.0"]]

  :profiles {:provided {:dependencies [[org.clojure/clojurescript "0.0-3126"]]}})
