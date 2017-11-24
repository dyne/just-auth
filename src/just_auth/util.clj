(ns just-auth.util)

(defn construct-link
  [{:keys [uri token]}]
  (str uri "/" token))
