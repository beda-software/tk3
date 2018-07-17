(ns controller-tk3.instance
  (:require [k8s.core :as k8s]
            [controller-tk3.naming :as naming]
            [controller-tk3.model :as model]
            [controller-tk3.utils :as ut]
            [unifn.core :as u]
            [controller-tk3.aidbox :as aidbox]))

(defonce last-updated (atom nil))

(defn pvc? [res]
  (and (map? res) (= (:kind res) "PersistentVolumeClaim")))

(defn pvc-patch [spec]
  (let [res (k8s/find spec)]
    (if (pvc? res)
      res
      (k8s/patch spec))))

(defmethod u/*fn ::create-instance-volume [{inst :resource}]
  (let [res (pvc-patch (model/instance-data-volume inst))]
    (if (pvc? res)
      {::u/status :success}
      {::u/status :error
       ::u/message (str "Instance volume request error: " res)})))

(defmethod u/*fn ::create-deployment [{inst :resource}]
  (let [res (k8s/patch (model/jupyter-deployment (assoc inst :status "waiting-app")))]
    (if (= (:kind res) "Status")
      {::u/status :error
       ::u/message (str res)}
      {::u/status :success})))

(defmethod u/*fn ::create-service [{inst :resource}]
  (let [res (k8s/patch (model/jupyter-service inst))]
    (if (= (:kind res) "Status")
      {::u/status :error
       ::u/message (str res)}
      {::u/status :success})))

(defn- make-resource
  [{:keys [id boxCredentials databaseCredentials jupyterCredentials box status meta]}]
  {:id id
   :status status
   :metadata {:annotations {:jupyter-instance-id id}
              :labels {:system "controller"}
              :namespace naming/namespace
              :name (str "jupyterinstance-" (:id box))}
   :spec {:size "10Mi"
          :env [{:name "BOX_HOST"
                 :value (:host boxCredentials)}
                {:name "BOX_TOKEN"
                 :value (:token boxCredentials)}]}
   :config {:jupyterToken (:token jupyterCredentials)}})

(defn watch []
  (doseq [inst (aidbox/get-updated-instances @last-updated)]
    ;; TODO: save last-updated
    (condp = (:state inst)
      :deleted nil

      (u/*apply
       [::create-instance-volume
        ::create-deployment
        ::create-service]
       (make-resource inst))))

  (let [deployments (->> (k8s/query {:apiVersion "apps/v1beta1"
                                     :kind "Deployment"
                                     :ns naming/namespace}
                                    {:labelSelector "system=controller"})
                         :items)]
    (doseq [deployment deployments]
      ;; TODO: Watch deployments states (failed/ready)
      nil)))

(comment
 (watch))

;; TODO: Fetch JupyterInstance resources from aidbox on start
;; TODO: use one unnamed token
;; TODO: create custom nginx config