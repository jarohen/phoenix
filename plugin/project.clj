(def version (slurp "../common/phoenix-version"))

(defproject jarohen/phoenix version
  :description "A plugin for configuring, co-ordinating and reloading Components"

  :url "https://github.com/james-henderson/phoenix"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :resource-paths ["resources" "../common"]
  
  :eval-in-leiningen true)
