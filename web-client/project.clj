(defproject web-client "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/clojurescript "0.0-2411"]
                 [om "0.7.3"]
                 [cljs-ajax "0.3.3"]
                 [huh "0.9.0"]]

  :plugins [[lein-cljsbuild "1.0.3"]
            [com.cemerick/clojurescript.test "0.3.1"]
            [com.cemerick/austin "0.1.5"]]

  :source-paths ["src"]

  :cljsbuild {
              :builds [{:id "dev"
                        :source-paths ["src"]
                        :compiler {
                                   :output-to "burgerboard.js"
                                   :output-dir "out"
                                   :optimizations :none
                                   :source-map true
                                   }
                        }
                       {:id "test"
                        :source-paths ["src" "test"]
                        :notify-command ["phantomjs" :cljs.test/runner
                                         "contrib/es5-shim.js"
                                         "contrib/react-with-addons-0.9.0.js"
                                         "contrib/sinon-server-1.12.1.js"
                                         "test-burgerboard.js"]
                        :compiler {:output-to "test-burgerboard.js"
                                   :optimizations :whitespace
                                   :pretty-print true
                                   :source-map "test-burgerboard.js.map"
                                   }
                        }
                       {:id "repl"
                        :source-paths ["utilities"]
                        :compiler {:output-to "repl.js"
                                   :optimizations :whitespace
                                   :pretty-print true
                                   }
                        }
                       ]
              :test-commands {"unit" ["phantomjs" :runner
                                      "contrib/es5-shim.js"
                                      "contrib/react-0.9.0.js"
                                      "test-burgerboard.js"]
                              }
              :repl-listen-port 9000
              :repl-launch-commands
              {
               "test-repl" ["phantomjs" "utilities/cljs-repl.js"
                            "contrib/es5-shim.js" "contrib/react-0.9.0.js"
                            "test-burgerboard.js"
                            "repl.js"]
               }
              }
  )
