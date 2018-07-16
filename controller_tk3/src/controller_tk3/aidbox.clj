(ns controller-tk3.aidbox
  (:require
   [cheshire.core :as json]
   [clojure.string :as str]
   [org.httpkit.client :as http-client]))

(def aidbox-url (or "http://127.0.0.1.xip.io:8080"))

(def default-headers
  (if-let [token (System/getenv "AIDBOX_TOKEN")]
    {"Authorization" (str "Bearer " token)}
    {}))

(defn- index-by [key-fn coll]
  (into {} (map (juxt key-fn identity) coll)))

(defn- fetch [path params]
  (->
   @(http-client/get
     (str aidbox-url path)
     {:insecure? true
      :headers (merge default-headers {"Content-Type" "application/json"})
      :query-params params})
   :body
   (json/parse-string true)))

(defn- fetch-instances-statuses [instances-ids]
  (when-not (empty? instances-ids)
   (->> (fetch "/JupyterInstanceStatus" {:_id (str/join "," instances-ids)})
        :entry
        (map :resource))))

(defn- fetch-updated-instances [& [since]]
  (->> (fetch "/JupyterInstance/_history" (if since {:_since since} {}))
       :entry
       (map :resource)
       reverse
       (index-by :id)
       vals))

(defn get-updated-instances [& [since]]
  (let [instances (fetch-updated-instances since)
        statuses (index-by :id (fetch-instances-statuses (map :id instances)))]
    (mapv #(assoc % :status (get-in statuses [(:id %) :status])
                    :state (keyword (get-in % [:meta :tag 0 :code]))) instances)))

(defn- patch [path body]
  @(http-client/patch
    (str aidbox-url path)
    {:insecure? true
     :headers (merge default-headers {"Content-Type" "application/json"})
     :body (json/generate-string body)}))

(defn patch-instance-status [id status]
  (patch (str "/JupyterInstanceStatus/" id) {:status status}))

(comment
 (let [instances-ids (map :id (get-updated-instances))]
   (doall (map #(patch-instance-status % "initializing") instances-ids)))
 )
