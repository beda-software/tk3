(ns webapp-tk3.core
  (:require
   [k8s.core :as k8s]
   [org.httpkit.server :as http-kit]
   [route-map.core :as rm]))

(defn init-jupyter-instance [{params :params rm :route-match :as req}]
  {:body (str "Init!")
   :status 200
   :headers {"Content-Type" "text/html"}}
  )

(def routes
  {:interceptors ['ensure-logged-in]
   "init-jupyter-instance" {:GET init-jupyter-instance}})

(defn wrap-not-found [h]
  (fn [{rm :route-match :as req}]
    (if rm (h req)
           {:body (str "Ups, no route for " (:uri req))
            :status 404
            :headers {"Content-Type" "text/html"}})))

(defn handler [{rm :route-match :as req}]
  (let [handler-fn (:match rm)]
    (handler-fn req)))

(def app
  (-> handler
      (wrap-not-found)
      (rm/wrap-route-map routes)))

(def server (atom nil))

(defn start []
  (reset! server (http-kit/run-server #'app {:port 3003})))

(defn stop [] (.stop @server))

(comment
 (start)
 (stop))