(defproject controller_tk3 "0.0.1-SNAPSHOT"
  :description "tk controller in k8s"
  :url "http://github.com/beda-software/tk3/controller_tk3"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha16"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/java.jdbc "0.6.1"]
                 [org.postgresql/postgresql "9.4.1211.jre7"]
                 [org.clojure/core.async "0.3.443"]
                 [cheshire "5.7.1"]
                 [http-kit "2.2.0"]
                 [clj-time "0.13.0"]
                 [clojure-humanize "0.2.2"]
                 [environ "1.1.0"]
                 [matcho "0.1.0-RC5"]

                 ;; Dependencies for common-tk3
                 [ch.qos.logback/logback-classic "1.2.2"]
                 [clj-json-patch "0.1.4"]
                 [cheshire "5.7.1"]
                 [stylefruits/gniazdo "1.0.1"]
                 [ring/ring-defaults "0.3.0"]
                 [http.async.client "1.2.0"]
                 [inflections "0.13.0"]
                 [http-kit "2.2.0"]]
  :uberjar-name "controller_tk3.jar"
  :main controller-tk3.core
  :profiles {:dev {:source-paths  ["test" "src"  "../common_tk3/src"]
                   :plugins [[lein-dotenv "RELEASE"]]}
             :uberjar {:source-paths  ["test" "src"  "../common_tk3/src"]
                       :aot :all
                       :omit-source true}})
