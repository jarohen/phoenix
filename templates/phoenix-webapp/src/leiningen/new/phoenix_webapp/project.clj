(defproject {{name}} ""

  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  
  :dependencies [[org.clojure/clojure "1.7.0-alpha4"]

                 [ring/ring-core "1.3.0"]
                 [bidi "1.12.0"]
                 [hiccup "1.0.5"]
                 [garden "1.2.1"]
                 [ring-middleware-format "0.4.0"]

                 [jarohen/phoenix.modules.http-kit "0.1.0-SNAPSHOT"]
                 [jarohen/phoenix.modules.cljs "0.1.0-SNAPSHOT"]

                 [org.clojure/clojurescript "0.0-2511"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [jarohen/flow "0.3.0-alpha1"]

                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-api "1.7.9"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.1"]
                 [org.apache.logging.log4j/log4j-core "2.1"]]

  :exclusions [org.clojure/clojure]

  :plugins [[jarohen/phoenix "0.1.0-SNAPSHOT"]
            [jarohen/simple-brepl "0.1.2"]
            [lein-shell "0.4.0"]]

  :phoenix/config "{{name}}-config.edn"

  :resource-paths ["resources" "target/resources"]

  :aliases {"dev" ["do"
                   ["shell" "mkdir" "-p"
                    "target/resources"]
                   
                   "phoenix"]
            
            "start" ["trampoline" "phoenix"]})
