(ns tk3.scratch
  (:require [k8s.core :as k8s]
            [tk3.model :as model]))

(def jupyter-instance
  {:kind "JupyterInstance"
   :ns "tk3"
   :apiVersion "tk3.io/v1"
   :metadata {:name "jupyter-instance-box-2"
              :namespace "tk3"
              :labels {:system "tk3"}}
   :spec {:size "1Gi"
          :env [{:name "AIDBOX_URL"
                 :value "https://box-2.aidbox.app"}]}
   :config {:token "qwe"
            :base_url "/jupyter/"
            :host "box-2.testapp"}})

(comment
  (k8s/patch jupyter-instance)

  (k8s/patch (assoc-in (k8s/find {:kind "PgBackup"
                                  :ns "tk3"
                                  :apiVersion "tk3.io/v1"
                                  :id "tk3-perseus"}) [:spec :pod-spec :spec :containers 0 :args] ["sql"]))

  
  )
