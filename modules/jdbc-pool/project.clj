(defproject jarohen/phoenix.modules.jdbc-pool "0.0.4"
  :description "A module to set up a JDBC connection pool component in Phoenix"
  :url "https://github.com/james-henderson/phoenix"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [com.stuartsierra/component "0.2.2"]

                 [org.clojure/java.jdbc "0.3.5"]
                 [org.apache.commons/commons-dbcp2 "2.0.1"]])
