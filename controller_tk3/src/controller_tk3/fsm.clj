(ns controller-tk3.fsm
  (:require [unifn.core :as u]
            [k8s.core :as k8s]
            [controller-tk3.aidbox :as aidbox]
            [controller-tk3.utils :as ut]
            [controller-tk3.naming :as naming]))

(defn update-status [resource status]
  (aidbox/patch-instance-status (:id resource) (name status)))

(defn timeout? [state resource]
  (when-let [timeout (:timeout state)]
    (let [t (ut/parse-period timeout)
          d (ut/duration resource)]
      (> d t))))

(defn process-state [fsm resource]
  (let [state-key (keyword (:status resource))]
    (if-let [state (fsm state-key)]
      (let [action-stack (or (:action-stack state) [])
            result (u/*apply action-stack {:resource resource
                                           ::u/safe? true})]
        (if-let [next-state (get state (::u/status result))]
          (update-status resource next-state)
          (when (timeout? state resource)
            (println (name state-key) "Timeout" resource)
            (update-status resource :state-timeout))))
      (when-not (#{:unprocessable-state :state-timeout} state-key)
        (println (name state-key) "Unprocessable state" resource)
        (update-status resource :unprocessable-state)))))
