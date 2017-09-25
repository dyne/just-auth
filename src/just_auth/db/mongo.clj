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

(ns just-auth.db.mongo
  (:require [monger.collection :as mc]
            [monger.db :as mdb]
            [monger.core :as mongo]
            [just-auth.db.storage :as storage :refer [AuthStore]]))

(defn get-mongo-db-and-conn [mongo-uri]
  (let [db-and-conn (mongo/connect-via-uri mongo-uri)]
    db-and-conn))

(defn get-mongo-db [mongo-uri]
  (:db (get-mongo-db-and-conn mongo-uri)))

(defn disconnect [db]
  (if (not (empty? db)) (mongo/disconnect db)))

(defrecord MongoStore [mongo-db coll]
  AuthStore
  (store! [this k item]
    (-> (mc/insert-and-return mongo-db coll (assoc item :_id (k item)))
        (dissoc :_id)))

  (update! [this k update-fn]
    (when-let [item (mc/find-map-by-id mongo-db coll k)]
      (let [updated-item (update-fn item)]
        (-> (mc/save-and-return mongo-db coll updated-item)
            (dissoc :_id)))))

  (fetch [this k]
    (when k
      (-> (mc/find-map-by-id mongo-db coll k)
          (dissoc :_id))))

  (query [this query]
    (->> (mc/find-maps mongo-db coll query)
         (map #(dissoc % :_id))))

  (delete! [this k]
    (when k
      (mc/remove-by-id mongo-db coll k)))

  (delete-all! [this]
    (mc/remove mongo-db coll)))

(defn create-mongo-store [mongo-db coll & params]
  (let [store (MongoStore. mongo-db coll)]
    (when-let  [ttl-seconds (:expireAfterSeconds [params])]
      (mc/ensure-index mongo-db coll {:created-at 1}
                       {:expireAfterSeconds ttl-seconds}))
    store))

(defn create-mongo-stores
  [db store-names & params]
  (zipmap
   (map #(keyword %) store-names)
   (map #(create-mongo-store db % params) store-names)))

(defn empty-db-stores! [stores-m]
  (map #(storage/delete-all! (% stores-m)) stores-m))
