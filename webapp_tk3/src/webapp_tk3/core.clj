(ns webapp-tk3.core
  (:require
   [k8s.core :as k8s]
   [org.httpkit.server :as http-kit]
   [route-map.core :as rm]
   [cheshire.core :as json]))

(defn- create-jupyter-instance-spec [{:keys [aidbox-url aidbox-id jupyter-token host]}]
  {:kind "JupyterInstance"
   :ns "tk3"
   :apiVersion "tk3.io/v1"
   :metadata {:name (str "jupyter-instance-" aidbox-id)
              :namespace "tk3"
              :labels {:system "tk3"}}
   :spec {:size "10Mi"
          :env [{:name "AIDBOX_URL"
                 :value aidbox-url}]}
   :config {:token jupyter-token
            :base_url "/jupyter/"
            :host host}})

(defn- make-response [status body]
  {:body body
   :status status
   :headers {"Content-Type" "text/html"}})

(defn- init-jupyter-instance [{data :data :as req}]
  (if (every? #(contains? data %) [:aidbox-url :aidbox-id :jupyter-token :host])
    (do
      (k8s/patch (create-jupyter-instance-spec data))
      (make-response 200 "Jupyter instance was created"))
    (make-response 400 "Invalid request (ensure aidbox-url, jupyter-token, host are set)")))

(def routes
  {"init-jupyter-instance" {:POST init-jupyter-instance}})

(defn- parse-json-body [h]
  (fn [{body :body :as req}]
    (try
      (h (assoc req :data (json/parse-string (slurp body) true)))
      (catch Exception e
        (make-response 500 (str "Unable to parse body in json format: " e))))))

(defn- check-content-type [h]
  (fn [{headers :headers :as req}]
    (if (= (get headers "content-type") "application/json")
      (h req)
      (make-response 400 "Unsupported content-type, use application/json"))))

(defn wrap-not-found [h]
  (fn [{rm :route-match :as req}]
    (if rm (h req)
        (make-response 404 (str "Ups, no route for " (:uri req))))))

(defn handler [{rm :route-match :as req}]
  (let [handler-fn (:match rm)]
    (handler-fn req)))

(def app
  (-> handler
      (parse-json-body)
      (check-content-type)
      (wrap-not-found)
      (rm/wrap-route-map routes)))

(def server (atom nil))

(defn start []
  (reset! server (http-kit/run-server #'app {:port 3003})))

(defn stop [] (.stop @server))

(comment
 (start)
 (stop))
