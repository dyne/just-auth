(ns just-auth.config
  (:require [environ.core :as env]))

(def env-vars #{:email-config :ttl-password-recovery})

(defn create-config []
  (select-keys env/env env-vars))

(defn get-env
  "Like a normal 'get' except it also ensures the key is in the env-vars set"
  ([config-m key]
   (get config-m (env-vars key)))
  ([config-m key default]
   (get config-m (env-vars key) default)))

(defn email-config [config-m]
  (get-env config-m :email-config))

(defn ttl-password-recovery [config-m]
  (clojure.edn/read-string (get-env config-m :ttl-password-recovery)))
