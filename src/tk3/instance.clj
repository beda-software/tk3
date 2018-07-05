(ns tk3.instance
  (:require [k8s.core :as k8s]
            [tk3.naming :as naming]
            [tk3.model :as model]
            [cheshire.core :as json]
            [tk3.utils :as ut]
            [tk3.fsm :as fsm]
            [unifn.core :as u]))

(defn pvc? [res]
  (and (map? res) (= (:kind res) "PersistentVolumeClaim")))

(defn pvc-patch [spec]
  (let [res (k8s/find spec)]
    (if (pvc? res)
      res
      (k8s/patch spec))))

(defn find-resource [kind ns res-name]
  (k8s/find {:kind kind
             :apiVersion "v1"
             :metadata {:name res-name
                        :namespace ns}}))

(def fsm-main
  {:init {:action-stack [{::u/fn ::ut/success}]
          :success :start-init}
   :start-init {:action-stack []}
   :error-state {}})

(defn watch []
  (doseq [inst (:items (k8s/query {:kind naming/instance-resource-kind :apiVersion naming/api}))]
    (fsm/process-state fsm-main inst)))

(comment
  (watch)

  )
