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

(ns just-auth.db.storage
  (:require [just-auth.db.mongo :as m]))

;; TODO duplicate from freecoin-lib
(defprotocol AuthStore
  (store! [e k item]
    "Store item against the key k")
  (update! [e k update-fn]
    "Update the item found using key k by running the update-fn on it and storing it")
  (fetch [e k]
    "Retrieve item based on primary id")
  (query [e query]
    "Items are returned using a query map")
  (delete! [e k]
    "Delete item based on primary id")
  (delete-all! [e]
    "Delete all items from a coll"))

(defn create-mongo-stores
  [db store-names & params]
  (zipmap
   (map #(keyword %) store-names)
   (map #(m/create-mongo-store db % params) store-names))) 

(defn create-in-memory-stores [store-names]
  (zipmap
   (map #(keyword %) store-names)
   (m/create-memory-store)))

(defn empty-db-stores! [stores-m]
  (map #(m/delete-all! (% stores-m)) stores-m))
