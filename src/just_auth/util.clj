;; Just auth - a simple two factor authenticatiokn library

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2017 Dyne.org foundation

;; Sourcecode designed, written and maintained by
;; Aspasia Beneti  <aspra@dyne.org>

;; This program is free software: you can redistribute it and/or modify
;; it under the terms of the GNU Affero General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or
;; (at your option) any later version.

;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU Affero General Public License for more details.

;; You should have received a copy of the GNU Affero General Public License
;; along with this program.  If not, see <http://www.gnu.org/licenses/>.
(ns just-auth.util
  (:require [taoensso.timbre :as log]))

(defn link->token [link]
  (-> link (clojure.string/split #"/") last))

(defn parts->path [uri token email action]
  (if (clojure.string/ends-with? uri "/")
    (str uri action "/" email "/" token)
    (str uri "/" action "/" email "/" token)))

(defn construct-link
  ;; TODO: add schema
  [{:keys [uri token email action]}]
  (try
    (.toString (java.net.URL. (parts->path uri token email action)))
    (catch Exception e 
      (.toString (java.net.URL. (str "http://" (parts->path uri token email action)))))))
