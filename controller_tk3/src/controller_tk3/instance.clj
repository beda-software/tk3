(ns controller-tk3.instance
  (:require [k8s.core :as k8s]
            [controller-tk3.naming :as naming]
            [controller-tk3.model :as model]
            [cheshire.core :as json]
            [controller-tk3.utils :as ut]
            [controller-tk3.fsm :as fsm]
            [unifn.core :as u]))

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
      {::u/status :success
       :status-data {:volumes [data-v]}}
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
  (let [vols (get-in inst [:status :volumes])
        ready? (reduce
                (fn [acc v]
                  (let [pvc (k8s/find
                             (assoc v
                                    :kind "PersistentVolumeClaim"
                                    :apiVersion "v1"))]
                    (and acc (= "Bound" (get-in pvc [:status :phase])))))
                true vols)]
    (when-not ready?
      {::u/status :stop})))

(def fsm-main
  {:init {:action-stack [::init-instance-volumes]
          :success :waiting-volumes
          :error :error-state}
   :waiting-volumes {:action-stack [::volumes-ready?
                                    {::u/fn ::ut/success}]
                     :success :volumes-are-ready
                     :error :error-state}
   :volumes-are-ready {:action-stack [::create-deployment
                                      ::create-service
                                      ;;::create-ingress
                                      {::u/fn ::ut/success}]
                   :success :initialized}
   :initialized {}
   :error-state {}})

(defn watch []
  (doseq [inst (:items (k8s/query {:kind naming/instance-resource-kind :apiVersion naming/api}))]
    (fsm/process-state fsm-main inst)))

(comment
  (watch)
  )
