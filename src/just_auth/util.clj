(ns just-auth.util
  (:require [taoensso.timbre :as log]))

(defn construct-link
  [{:keys [uri token email]}]
  (try
    (.toString (java.net.URL. (str uri "/" email "/token")))
    (catch Exception e 
      (.toString (java.net.URL. (str "http://" uri "/" email "/token"))))))
