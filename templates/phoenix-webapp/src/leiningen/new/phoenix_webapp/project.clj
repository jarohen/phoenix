(defproject {{name}} ""

  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  
  :dependencies [[org.clojure/clojure "1.7.0-alpha4"]

                 [ring/ring-core "1.2.0"]
                 [bidi "1.12.0"]
                 [hiccup "1.0.5"]
                 [garden "1.2.1"]
                 [ring-middleware-format "0.4.0"]

                 [jarohen/flow "0.3.0-alpha1"]

                 [org.clojure/clojurescript "0.0-2371"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]]

  :plugins [[jarohen/simple-brepl "0.1.2"]]

  :frodo/config-resource "{{name}}-config.edn"

  :source-paths ["src" "target/generated/clj"]

  :resource-paths ["resources" "target/resources"])
