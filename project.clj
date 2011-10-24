(defproject drumcirclr "1.0.0-SNAPSHOT"
  :description "Drumcirclr"
  :main drumcirclr.core
  :source-path "src"
  :dependencies [[aleph "0.2.0-beta3-SNAPSHOT"]
                 [hiccup "0.3.5"]
                 [org.clojure/clojure "1.2.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [ring/ring-devel "1.0.0-beta2"]
                 [clj-json "0.3.2"]
                 [compojure "0.6.5"]
                 [lein-ring "0.4.6"]]
  :dev-dependencies [[ring-mock "0.1.1"]
                     [org.clojars.gjahad/debug-repl "0.3.1"]]
  :ring {:handler drumcirclr.core/app-routes})