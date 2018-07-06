(defproject common_tk3 "0.1.0-SNAPSHOT"
  :description "Common libraries for tk3"
  :url "http://github.com/beda-software/tk3/common_tk3"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha16"]
                 [ch.qos.logback/logback-classic "1.2.2"]
                 [clj-json-patch "0.1.4"]
                 [cheshire "5.7.1"]
                 [stylefruits/gniazdo "1.0.1"]
                 [ring/ring-defaults "0.3.0"]
                 [http.async.client "1.2.0"]
                 [inflections "0.13.0"]
                 [http-kit "2.2.0"]]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
