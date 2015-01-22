(defproject jarohen/phoenix.modules.http-kit "0.0.1"
  :description "A module to set up an http-kit component in Phoenix"
  :url "https://github.com/james-henderson/phoenix"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [com.stuartsierra/component "0.2.2"]
                 [http-kit "2.1.18"]
                 
                 [juxt.modular/ring "0.5.2"]])
