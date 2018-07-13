(ns controller-tk3.instance
  (:require [k8s.core :as k8s]
            [controller-tk3.naming :as naming]
            [controller-tk3.model :as model]
            [cheshire.core :as json]
            [controller-tk3.utils :as ut]
            [controller-tk3.fsm :as fsm]
            [unifn.core :as u]
            [controller-tk3.aidbox :as aidbox]))

(defn pvc? [res]
  (and (map? res) (= (:kind res) "PersistentVolumeClaim")))

(defn pvc-patch [spec]
  (let [res (k8s/find spec)]
    (if (pvc? res)
      res
      (k8s/patch spec))))

(defmethod u/*fn ::init-instance-volumes [{inst :resource}]
  (let [data-v (pvc-patch (model/instance-data-volume inst))]
    (if (pvc? data-v)
      {::u/status :success}
      {::u/status :error
       ::u/message (str "Instance volumes request error: " data-v)})))

(defmethod u/*fn ::create-deployment [{inst :resource}]
  (let [res (k8s/patch (model/jupyter-deployment inst))]
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

(defmethod u/*fn ::create-ingress [{inst :resource}]
  (let [res (k8s/patch (model/jupyter-ingress inst))]
    (if (= (:kind res) "Status")
      {::u/status :error
       ::u/message (str res)}
      {::u/status :success})))

(defmethod u/*fn ::volumes-ready? [{inst :resource}]
  (let [vols [(naming/data-volume-name inst)]
        ready? (reduce
                (fn [acc v]
                  (let [pvc (k8s/find
                             {:kind "PersistentVolumeClaim"
                              :apiVersion "v1"
                              :id v
                              :ns naming/namespace})]
                    (and acc (= "Bound" (get-in pvc [:status :phase])))))
                true vols)]
    (when-not ready?
      {::u/status :stop})))

(def fsm-main
  {:initializing {:action-stack [::init-instance-volumes]
                  :success :waiting-volumes
                  :error :failed}
   :waiting-volumes {:action-stack [::volumes-ready?
                                    {::u/fn ::ut/success}]
                     :success :volumes-are-ready
                     :error :failed}
   :volumes-are-ready {:action-stack [::create-deployment
                                      ::create-service
                                      ::create-ingress
                                      {::u/fn ::ut/success}]
                       :success :ready}
   :ready {}
   :failed {}})

(defn- make-resource
  [{:keys [boxCredentials databaseCredentials jupyterCredentials id status]}]
  {:id id
   :status status
   :metadata {:labels {:system "tk3"}
              :namespace naming/namespace
              :name (str "jupyterinstance-" id)}
   :spec {:size "10Mi"
          :env [{:name "BOX_HOST"
                 :value (:host boxCredentials)}
                {:name "BOX_TOKEN"
                 :value (:token boxCredentials)}]}
   :config {:token (:token jupyterCredentials)
            :host (:host jupyterCredentials)}})

(defn watch []
  (doseq [inst (aidbox/get-updated-instances)]
    (fsm/process-state fsm-main (make-resource inst))))

(comment
  (watch)
  )
