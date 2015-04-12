(def version (slurp "../common/phoenix-version"))

(defproject jarohen/phoenix.runtime version
  :description "The runtime for the Phoenix plugin"

  :url "https://github.com/james-henderson/phoenix"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.namespace "0.2.10"]
                 [org.clojure/tools.nrepl "0.2.10"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/tools.reader "0.9.1"]

                 [jarohen/phoenix.build "0.0.1"]

                 [camel-snake-kebab "0.3.1" :exclusions [org.clojure/clojure]]

                 [com.stuartsierra/component "0.2.3"]
                 [com.stuartsierra/dependency "0.1.1"]
                 [medley "0.5.5"]

                 [buddy/buddy-core "0.5.0"]

                 [prismatic/schema "0.4.0"]]

  :resource-paths ["resources" "../common"]

  :aot [phoenix.main])
