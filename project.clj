(defproject burgerboard "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.5"]
                 [hiccup "1.0.5"]
                 [ring/ring-jetty-adapter "1.2.2"]
                 [org.clojure/data.json "0.2.3"]
                 [korma "0.3.0-RC5"]
                 [org.xerial/sqlite-jdbc "3.7.15-M1"]
                 [postgresql/postgresql "8.4-702.jdbc4"]
                 [javax.mail/mail "1.4.7"]
                 [crypto-password "0.1.0"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/clojurescript "0.0-2411"]
                 [om "0.7.3"]
                 [cljs-ajax "0.3.3"]]
  :plugins [[lein-ring "0.8.5"]
            [lein-cljsbuild "1.0.3"]
            [com.cemerick/clojurescript.test "0.3.1"]
            [com.cemerick/austin "0.1.5"]]
  :ring {:handler burgerboard.server/prod-app
         :init burgerboard.server/init}
  :profiles
  {:dev {:dependencies [[ring-mock "0.1.5"]]}}
  :cljsbuild {
              :builds [{:id "dev"
                        :source-paths ["web-client/src"]
                        :compiler {
                                   :output-to "web-client/dev-burgerboard.js"
                                   :output-dir "web-client/dev-out"
                                   :optimizations :none
                                   :source-map true
                                   :externs ["react/externs/react.js"
                                             "externs/burgerboard.js"]
                                   }
                        }
                       {:id "prod"
                        :source-paths ["web-client/src"]
                        :compiler {
                                   :output-to "web-client/burgerboard.js"
                                   :output-dir "web-client/out"
                                   :optimizations :advanced
                                   ;; :pretty-print false
                                   :preamble ["react/react.min.js"]
                                   :source-map "web-client/burgerboard.js.map"
                                   :externs ["web-client/externs/burgerboard.js"]
                                   }
                        }]
              })
