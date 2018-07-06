(defproject webapp_tk3 "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://github.com/beda-software/tk3/webapp_tk3"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :main ^:skip-aot webapp-tk3.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
