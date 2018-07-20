(ns controller-tk3.instance
  (:require [k8s.core :as k8s]
            [controller-tk3.naming :as naming]
            [controller-tk3.model :as model]
            [controller-tk3.utils :as ut]
            [unifn.core :as u]
            [clojure.tools.logging :as log]
            [controller-tk3.aidbox :as aidbox]))

(defonce last-updated (atom nil))

(defonce instance-status-cache (atom {}))

(defn pvc? [res]
  (and (map? res) (= (:kind res) "PersistentVolumeClaim")))

(defn pvc-patch [spec]
  (let [res (k8s/find spec)]
    (if (pvc? res)
      res
      (k8s/patch spec))))

(defn- process-k8s-result [res]
  (if (and (= (:kind res) "Status") (not= (:status res) "Success"))
    {::u/status :error
     ::u/message (str res)}
    {::u/status :success}))

(defmethod u/*fn ::patch-instance-volume [{inst :resource}]
  (process-k8s-result (pvc-patch (model/instance-data-volume inst))))

(defmethod u/*fn ::patch-deployment [{inst :resource}]
  (process-k8s-result (k8s/patch (model/jupyter-deployment inst))))

(defmethod u/*fn ::patch-service [{inst :resource}]
  (process-k8s-result (k8s/patch (model/jupyter-service inst))))

(defmethod u/*fn ::delete-service [{inst :resource}]
  (process-k8s-result (k8s/delete (model/jupyter-service inst))))

(defmethod u/*fn ::delete-deployment [{inst :resource}]
  (process-k8s-result (k8s/delete (model/jupyter-deployment inst))))

(defmethod u/*fn ::delete-instance-volume [{inst :resource}]
  (process-k8s-result (k8s/delete (model/instance-data-volume inst))))

(defn- make-resource
  [{:keys [id boxCredentials databaseCredentials jupyterCredentials box status]}]
  {:id id
   :status status
   :metadata {:annotations {:jupyter-instance-id id}
              :labels {:image "jupyter"}
              :namespace naming/namespace
              :name (str "jupyterinstance-" (:id box))}
   :spec {:size "10Mi"
          :env [{:name "BOX_URL"
                 :value (:url boxCredentials)}
                {:name "BOX_AUTHORIZATION"
                 :value (:authorization boxCredentials)}]}
   :config {:jupyterToken (:token jupyterCredentials)}})

(defn watch []
  (doseq [inst (aidbox/get-updated-instances @last-updated)]
    (let [inst-last-updated (get-in inst [:meta :lastUpdated])]
      (when (or (not @last-updated) (< (compare @last-updated inst-last-updated) 0))
        (reset! last-updated inst-last-updated))
      (swap! instance-status-cache assoc (:id inst) (:status inst)))
    (condp = (:state inst)
      :deleted
      (let [{pipeline-status ::u/status
             status-message ::u/message} (u/*apply
                                          [::delete-service
                                           ::delete-deployment
                                           ::delete-instance-volume]
                                          {::u/safe? true
                                           :resource (make-resource inst)})]
        (when (= pipeline-status :error)
          (log/error "error while deleting: " status-message)))

      (let [{pipeline-status ::u/status
             status-message ::u/message} (u/*apply
                                          [::patch-instance-volume
                                           ::patch-deployment
                                           ::patch-service]
                                          {::u/safe? true
                                           :resource (make-resource inst)})]
        (when (= pipeline-status :error)
          (log/error "error while updating: " status-message)))))

  (let [deployments (->> (k8s/query {:apiVersion "apps/v1beta1"
                                     :kind "Deployment"
                                     :ns naming/namespace}
                                    {:labelSelector "image=jupyter"})
                         :items)]
    (doseq [deployment deployments]
      (let [instance-id (get-in deployment [:metadata :annotations :jupyter-instance-id])
            prev-status (get @instance-status-cache instance-id)
            replicas-count (or (get-in deployment [:status :readyReplicas]) 0)
            curr-status (cond
                          (and (= prev-status "initializing") (>= replicas-count 1))
                          "ready"

                          (and (= prev-status "ready") (= replicas-count 0))
                          "failed"

                          :else
                          prev-status)]
        (when-not (= prev-status curr-status)
          (swap! instance-status-cache assoc instance-id curr-status)
          (aidbox/patch-instance-status instance-id curr-status)))
      nil)))

(comment
  (watch)

  (reset! last-updated nil)
  )
