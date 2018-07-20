(ns controller-tk3.core
  (:require [k8s.core :as k8s]
            [controller-tk3.model :as m]
            [controller-tk3.instance :as instance]
            [clojure.tools.logging :as log])
  (:gen-class))

(defn watch []
  (instance/watch))

(defonce server (atom nil))

(defn stop []
  (when-let [thr @server]
    (.interrupt thr)
    (reset! server nil)))

(defn start []
  (stop)
  (let [thr (Thread.
             (fn []
               (log/debug "Start")
               (try
                 (while (not (Thread/interrupted))
                   (try
                     (watch)
                     (catch Exception e
                       (log/error "An error has occurred\n" e)))
                   (Thread/sleep 10000))
                 (catch java.lang.InterruptedException e
                   (log/debug "Bye, bye")))))]
    (reset! server thr)
    (.start thr)))

(defn -main []
  (start))
