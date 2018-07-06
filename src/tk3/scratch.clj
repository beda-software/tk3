(ns tk3.scratch
  (:require [k8s.core :as k8s]
            [tk3.model :as model]))

(defn update-status [inst status]
  (k8s/patch
   (assoc inst
          :kind (:kind inst)
          :apiVersion (:apiVersion inst)
          :status (merge (or (:status inst) {})
                         {:lastUpdate (java.util.Date.)}
                         status))))

(def jupyter-instance
  {:kind "JupyterInstance"
   :ns "tk3"
   :apiVersion "tk3.io/v1"
   :metadata {:name "jupyter-instance-box-1"
              :namespace "tk3"
              :labels {:system "tk3"}}
   :spec {:size "10Mi"
          :env [{:name "AIDBOX_URL"
                 :value "https://box-1.aidbox.app"}]}
   :config {:token "qwe"
            :base_url "/jupyter/"
            :host "box-1.testapp"}})

(comment
  (k8s/patch jupyter-instance)

  (k8s/patch (assoc-in (k8s/find {:kind "PgBackup"
                                  :ns "tk3"
                                  :apiVersion "tk3.io/v1"
                                  :id "tk3-perseus"}) [:spec :pod-spec :spec :containers 0 :args] ["sql"]))

  (update-status (k8s/find jupyter-instance) {:phase "init"})


  )
