(ns webapp-tk3.core
  (:require
   [k8s.core :as k8s]
   [org.httpkit.server :as http-kit]
   [route-map.core :as rm]
   [cheshire.core :as json]))

(defn- jupyter-instance-name [aidbox-id]
  (str "jupyter-instance-" aidbox-id))

(defn- make-aidbox-jupyter-instance-spec [{:keys [aidbox-url aidbox-id aidbox-token jupyter-token host]}]
  {:kind "JupyterInstance"
   :ns "tk3"
   :apiVersion "tk3.io/v1"
   :metadata {:name (jupyter-instance-name aidbox-id)
              :namespace "tk3"
              :labels {:system "tk3"}}
   :spec {:size "10Mi"
          :env [{:name "AIDBOX_URL"
                 :value aidbox-url}
                {:name "AIDBOX_TOKEN"
                 :value aidbox-token}]}
   :config {:token jupyter-token
            :base_url "/jupyter/"
            :host host}})

(defn- make-response [status body]
  {:body body
   :status status
   :headers {"Content-Type" "text/html"}})

(defn- aidbox-jupyter-instance-exists? [aidbox-id]
  (let [instance (k8s/find {:kind "JupyterInstance"
                            :ns "tk3"
                            :apiVersion "tk3.io/v1"
                            :id (jupyter-instance-name aidbox-id)})]
    (= (:kind instance) "JupyterInstance")))

(defn- init-aidbox-jupyter-instance [{data :data :as req}]
  (cond
    (not (every? #(contains? data %) [:aidbox-url :aidbox-token :aidbox-id :jupyter-token :host]))
    (make-response 400 "Invalid request (ensure aidbox-url, aidbox-token, aidbox-id, jupyter-token, host are set)")

    (aidbox-jupyter-instance-exists? (:aidbox-id data))
    (make-response 400 (str "JupyterInstance with id " (:aidbox-id data) " already exists"))

    :else
    (do
      (k8s/patch (make-aidbox-jupyter-instance-spec data))
      (make-response 200 "Jupyter instance was created"))))

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

(defn- check-access-token [h]
  (fn [{headers :headers :as req}]
    (let [access-token (System/getenv "WEBAPP_ACCESS_TOKEN")]
     (if (= (get headers "authorization") (str "Token " access-token))
       (h req)
       (make-response 403 "Invalid access token passed in Authorization header")))))

(defn wrap-not-found [h]
  (fn [{rm :route-match :as req}]
    (if rm (h req)
        (make-response 404 (str "Ups, no route for " (:uri req))))))

(defn handler [{rm :route-match :as req}]
  (let [handler-fn (:match rm)]
    (handler-fn req)))

(def routes
  {"init-aidbox-jupyter-instance" {:POST init-aidbox-jupyter-instance}})

(def app
  (-> handler
      (parse-json-body)
      (check-content-type)
      (wrap-not-found)
      (rm/wrap-route-map routes)
      (check-access-token)))

(def server (atom nil))

(defn start []
  (reset! server (http-kit/run-server #'app {:port 3003})))

(defn stop [] (.stop @server))

(comment
 (start)
 (stop))
