(ns tk3.scratch
  (:require [k8s.core :as k8s]))

(def jupyter-instance
  {:kind "JupyterInstance"
   :ns "tk3"
   :apiVersion "tk3.io/v1"
   :metadata {:name "jupyter-instance-box-1"
              :namespace "tk3"
              :labels {:service "pegasus"
                       :system "tk3"}}
   :config {:jupyter_token "123qwe"
            :aidbox_url "https://box-1.aidbox.app"}})

(comment

  (k8s/patch jupyter-instance)

  (k8s/patch (assoc-in (k8s/find {:kind "PgBackup"
                                  :ns "tk3"
                                  :apiVersion "tk3.io/v1"
                                  :id "tk3-perseus"}) [:spec :pod-spec :spec :containers 0 :args] ["sql"]))

  
  )
