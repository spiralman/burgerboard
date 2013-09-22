(defproject burgerboard "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.5"]
                 [org.clojure/data.json "0.2.3"]
                 [org.xerial/sqlite-jdbc "3.7.2"]]
  :plugins [[lein-ring "0.8.5"]]
  :ring {:handler burgerboard.handler/app}
  :profiles
  {:dev {:dependencies [[ring-mock "0.1.5"]]}})
