(defproject burgerboard "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.5"]
                 [org.clojure/data.json "0.2.3"]
                 [korma "0.3.0-RC5"]
                 [org.xerial/sqlite-jdbc "3.7.15-M1"]
                 [javax.mail/mail "1.4.7"]
                 [crypto-password "0.1.0"]]
  :plugins [[lein-ring "0.8.5"]]
  :ring {:handler burgerboard.handler/app}
  :profiles
  {:dev {:dependencies [[ring-mock "0.1.5"]]}})
