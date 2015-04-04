(defproject {{name}} ""

  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.7.0-alpha4"]

                 [ring/ring-core "1.3.0"]
                 [bidi "1.15.0"]
                 [hiccup "1.0.5"]
                 [garden "1.2.1"]
                 [ring-middleware-format "0.4.0"]

                 [jarohen/phoenix.modules.aleph "0.0.1"]

                 [org.clojure/clojurescript "0.0-3126"]
                 [jarohen/phoenix.modules.cljs "0.0.4"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]

                 {{#flow?}}[jarohen/flow "0.3.0-alpha3"]{{/flow?}}{{#om?}}[org.omcljs/om "0.8.8"]{{#sablono?}}
                 [sablono "0.3.4"]{{/sablono?}}{{/om?}}{{#reagent?}}[reagent "0.5.0"]{{/reagent?}}

                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-api "1.7.9"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.1"]
                 [org.apache.logging.log4j/log4j-core "2.1"]]

  :exclusions [org.clojure/clojure]

  :plugins [[jarohen/phoenix "0.1.1"]
            [jarohen/simple-brepl "0.2.1"]]

  :phoenix/config "{{name}}-config.edn"

  :aliases {"dev" "phoenix"
            "build" ["phoenix" "uberjar"]
            "start" ["trampoline" "phoenix"]})
