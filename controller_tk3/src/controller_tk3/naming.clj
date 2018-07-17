(ns controller-tk3.naming
  (:require [clojure.string :as str]
            #_[controller-tk3.utils :as ut]))

;; The system is very sensitive to naming changes
;; be carefull and backward compatible here

(def api-group "tk3.io")
(def api-version "v1")
(def api (str api-group "/" api-version))

;; /home/jovyan is set in jupyter/base-notebook images
(def data-path "/home/jovyan/work")

(def namespace "tk3")

(defn resource-name [x]
  (get-in x [:metadata :name]))

(defn service-name [inst]
  (resource-name inst))

(defn deployment-name [inst]
  (resource-name inst))

(defn data-volume-name [inst]
  (str (resource-name inst) "-data"))
