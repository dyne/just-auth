;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2015 Dyne.org foundation

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

(ns just-auth.db.just-auth
  (:require [clj-storage.core :as storage]
            [clj-storage.db.mongo :as mongo]
            [taoensso.timbre :as log]))

(defn stores-params-m [args]
  (when-not (:ttl-password-recovery args)
    (log/warn "No password expiration time was set, defaulting to 1800 seconds"))
  {"account-store" {}
   "password-recovery-store" {:expireAfterSeconds (if-let [arg-map (first args)]
                                                    (:ttl-password-recovery arg-map)
                                                    1800)}})

(defn create-auth-stores [db & args]
  (log/debug "Creating the authentication mongo stores")
  (mongo/create-mongo-stores
   db
   (stores-params-m args)))

(defn create-in-memory-stores []
  (log/debug "Creating in memory stores for testing the authentication lib")
  (storage/create-in-memory-stores (keys (stores-params-m []))))

