(ns tk3.model
  (:require [clojure.string :as str]
            [tk3.naming :as naming]
            [k8s.core :as k8s]
            [unifn.core :as u]))

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

(defn instance-spec [cluster color role]
  {:kind naming/instance-resource-kind
   :apiVersion naming/api
   :metadata {:name (naming/instance-name cluster color)
              :namespace (inherited-namespace cluster)
              :labels (merge (naming/cluster-labels cluster)
                             (naming/instance-labels role color))}
   :spec (merge (:spec cluster)
                {:pg-cluster (naming/resource-name cluster)
                 :role role})
   :config (:config cluster)})

(def default-volume-annotiations {"volume.beta.kubernetes.io/storage-class" "standard"})

(defn volume-spec
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

(defn instance-data-volume-spec [inst-spec]
  (volume-spec
   {:name (naming/data-volume-name inst-spec)
    :labels (merge (inherited-labels inst-spec) {:type "data"})
    :namespace (inherited-namespace inst-spec)
    :annotations {"volume.beta.kubernetes.io/storage-class" (get-in inst-spec [:spec :storageClass] "standard")}
    :storage (get-in inst-spec [:spec :size])}))

(defn config-map [cluster]
  {:kind "ConfigMap"
   :apiVersion "v1"
   :metadata {:name (naming/config-map-name (get-in cluster [:metadata :name]))
              :labels (inherited-labels cluster)
              :namespace (inherited-namespace cluster)}
   :data {}})

(defn rand-str [len]
  (apply str (take len (repeatedly #(char (+ (rand 26) 65))))))

(defn secret [cluster & [pass]]
  {:kind "Secret"
   :apiVersion "v1"
   :type "Opaque"
   :metadata {:name (naming/secret-name (get-in cluster [:metadata :name]))
              :labels (inherited-labels cluster)
              :namespace (inherited-namespace cluster)}
   :data {:username (k8s/base64-encode "postgres")
          :password (k8s/base64-encode (or pass (rand-str 10)))}})

(defn volumes [inst-spec]
  [(let [nm (naming/data-volume-name inst-spec)]
     {:name nm :persistentVolumeClaim {:claimName nm}})])

(defn volume-mounts [inst-spec]
  [{:name (naming/data-volume-name inst-spec)
    :mountPath naming/data-path
    :subPath "pgdata"}])

(defn run-jupyter-command []
  ["jupyter"
   "notebook"
   "-x"
   (str/join " && "
             [(format "chown postgres -R %s" naming/data-path)
              (format "chown postgres -R %s" naming/wals-path)
              (format "su -m postgres -c 'bash %s/initscript'" naming/config-path)])])

(defn jupyter-pod [inst-spec opts]
  {:kind "Pod"
   :apiVersion "v1"
   :metadata {:name (:name opts)
              :namespace (inherited-namespace inst-spec)
              :labels (inherited-labels inst-spec)}
   :spec {:restartPolicy (or (:restartPolicy opts) "Always")
          :volumes (volumes inst-spec)
          :containers
          [{:name "pg"
            :image (get-in inst-spec [:spec :image])
            :ports [{:containerPort 5432}]
            :imagePullPolicy :Always
            :env
            [{:name "PGUSER" :value "postgres"}
             {:name "PGPASSWORD" :valueFrom {:secretKeyRef
                                             {:name (naming/secret-name (get-in inst-spec [:spec :pg-cluster]))
                                              :key "password"}}}]
            :command (:command opts)
            :volumeMounts (volume-mounts inst-spec)}]}})

(defn init-master-pod [inst-spec]
  (db-pod
   (assoc-in inst-spec [:metadata :labels :type] "init")
   {:name (str (get-in inst-spec [:metadata :name]) "-init-master")
    :restartPolicy "Never"
    :command (initdb-command)}))

(defn postgres-pod [inst-spec opts]
  (db-pod
   (assoc-in inst-spec [:metadata :labels :type] "instance")
   (merge opts {:command ["su" "-m" "postgres" "-c"
                          (format "postgres --config-file=%1$s/postgresql.conf --hba-file=%1$s/pg_hba.conf"
                                  naming/config-path)]})))

(defn postgres-deployment [inst-spec]
  (let [pod (-> (postgres-pod inst-spec
                              {:name (str "tk3-" (get-in inst-spec [:spec :pg-cluster])
                                          "-" (get-in inst-spec [:metadata :labels :color]))})
                (update-in [:spec :containers]
                           conj (merge
                                 {:image "healthsamurai/wal-export:latest"}
                                 (get-in inst-spec [:spec :wal-export])
                                 {:name "pg-wal-export"
                                  :imagePullPolicy :Always
                                  :env [{:name "WAL_DIR" :value naming/wals-path}]
                                  :volumeMounts (volume-mounts inst-spec)})))]
    {:apiVersion "apps/v1beta1"
     :kind "Deployment"
     :metadata (:metadata pod)
     :spec {:replicas 1
            :template (update pod :metadata dissoc :name)}}))

(defn master-service [inst-spec]
  (let [cluster-name (get-in inst-spec [:spec :pg-cluster])]
    {:apiVersion "v1"
     :kind "Service"
     :metadata {:name (naming/service-name cluster-name)
                :namespace (inherited-namespace inst-spec)
                :labels (inherited-labels inst-spec)}
     :spec {:selector (naming/master-service-selector cluster-name)
            :type "ClusterIP"
            :ports [{:protocol "TCP"
                     :port 5432
                     :targetPort 5432}]}}))
