(ns controller-tk3.core
  (:require [k8s.core :as k8s]
            [controller-tk3.model :as m]
            [controller-tk3.instance :as instance])
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
               (println "Start")
               (try
                 (while (not (Thread/interrupted))
                   (watch)
                   (Thread/sleep 10000))
                 (catch java.lang.InterruptedException e
                   (println "Bye, bye")))))]
    (reset! server thr)
    (.start thr)))

(defn -main []
  (start))
