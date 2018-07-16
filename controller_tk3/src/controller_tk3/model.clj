(ns controller-tk3.model
  (:require [controller-tk3.naming :as naming]))

(def instance-definition
  {:apiVersion "apiextensions.k8s.io/v1beta1"
   :kind "CustomResourceDefinition"
   :metadata {:name naming/instance-resource-name}
   :spec {:group naming/api-group
          :version naming/api-version
          :names {:kind naming/instance-resource-kind
                  :plural naming/instance-resource-plural}
          :scope "Namespaced"}})

(defn inherited-namespace [x]
  (or (get-in x [:metadata :namespace]) "default"))

(defn inherited-labels [x]
  (or (get-in x [:metadata :labels]) {}))

(def default-volume-annotiations {"volume.beta.kubernetes.io/storage-class" "standard"})

(defn volume
  [{nm :name
    labels :labels
    ns :namespace
    size :storage
    anns :annotations}]
  {:kind "PersistentVolumeClaim"
   :apiVersion "v1"
   :metadata {:name nm
              :lables labels
              :namespace ns
              :annotations  (merge default-volume-annotiations anns)}
   :spec {:accessModes ["ReadWriteOnce"]
          :resources {:requests {:storage size}}}})

(defn instance-data-volume [inst]
  (volume
   {:name (naming/data-volume-name inst)
    :labels (merge (inherited-labels inst) {:type "data"})
    :namespace (inherited-namespace inst)
    :annotations {"volume.beta.kubernetes.io/storage-class" (get-in inst [:spec :storageClass] "standard")}
    :storage (get-in inst [:spec :size])}))

(defn volumes [inst]
  [(let [nm (naming/data-volume-name inst)]
     {:name nm :persistentVolumeClaim {:claimName nm}})])

(defn container-volume-mounts [inst]
  [{:name (naming/data-volume-name inst)
    :mountPath naming/data-path
    :subPath "data"}])

(defn jupyter-pod [inst]
  {:kind "Pod"
   :apiVersion "v1"
   :metadata {:namespace (inherited-namespace inst)
              :labels (merge
                       (inherited-labels inst)
                       {:service (naming/resource-name inst)})}
   :spec {:restartPolicy "Always"
          :volumes (volumes inst)
          :containers
          [(merge
            ;; TODO: use whitelist for images
            ;; TODO: use "isolated namespace" to avoid network scanning
            {:image "jupyter/base-notebook:latest"}
            (:spec inst)
            {:name "jupyter"
             :ports [{:containerPort 8888}]
             :imagePullPolicy :Always
             :args ["jupyter"
                    "notebook"
                    (str "--NotebookApp.token='" (get-in inst [:config :token]) "'")]
             :volumeMounts (container-volume-mounts inst)
             })]}})

(defn jupyter-deployment [inst]
  (let [pod (jupyter-pod inst)]
    {:apiVersion "apps/v1beta1"
     :kind "Deployment"
     :metadata {:annotations (get-in inst [:metadata :annotations])
                :name (naming/deployment-name inst)
                :namespace (inherited-namespace inst)
                :labels (inherited-labels inst)}
     :spec {:replicas 1
            :template pod}}))

(defn jupyter-service [inst]
  {:apiVersion "v1"
   :kind "Service"
   :metadata {:name (naming/service-name inst)
              :namespace (inherited-namespace inst)
              :labels (inherited-labels inst)}
   :spec {:selector {:service (naming/resource-name inst)}
          :type "ClusterIP"
          :ports [{:protocol "TCP"
                   :port 8888
                   :targetPort 8888}]}})

(defn jupyter-ingress [inst]
  {:apiVersion "extensions/v1beta1"
   :kind "Ingress"
   :metadata {:name (naming/ingress-name inst)
              :namespace (inherited-namespace inst)
              :labels (inherited-labels inst)}
   :spec {:rules [{:host (get-in inst [:config :host])
                   :http {:paths [{:path "/"
                                   :backend {:serviceName (naming/service-name inst)
                                             :servicePort 8888}}]}}]}})
