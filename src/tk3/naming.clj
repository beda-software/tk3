(ns tk3.naming
  (:require [clojure.string :as str]
            #_[tk3.utils :as ut]))

;; The system is very sensitive to naming changes
;; be carefull and backward compatible here

(def api-group "tk3.io")
(def api-version "v1")
(def api (str api-group "/" api-version))

(def instance-resource-name (str "jupyterinstances." api-group))
(def instance-resource-kind "JupyterInstance")
(def instance-resource-plural "jupyterinstances")

(def data-path "/data")

(defn resource-name [x]
  (get-in x [:metadata :name]))

(defn secret-name [inst]
  (resource-name inst))

(defn service-name [inst]
  (resource-name inst))

(defn ingress-name [inst]
  (resource-name inst))

(defn deployment-name [inst]
  (resource-name inst))

(defn data-volume-name [inst]
  (str (resource-name inst) "-data"))
