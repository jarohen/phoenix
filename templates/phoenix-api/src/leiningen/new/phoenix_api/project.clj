(defproject {{name}} ""

  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.7.0-alpha4"]

                 [ring/ring-core "1.3.0"]
                 [bidi "1.15.0"]
                 [ring-middleware-format "0.4.0"]

                 [jarohen/phoenix.modules.aleph "0.0.1"]

                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-api "1.7.9"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.1"]
                 [org.apache.logging.log4j/log4j-core "2.1"]]

  :exclusions [org.clojure/clojure]

  :plugins [[jarohen/phoenix "0.0.9"]]

  :phoenix/config "{{name}}-config.edn"

  :aliases {"dev" "phoenix"
            "build" ["phoenix" "uberjar"]
            "start" ["trampoline" "phoenix"]})
