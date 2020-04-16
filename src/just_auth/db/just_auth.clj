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
            [clj-storage.db.sqlite :as sqlite]
            [clj-storage.db.sqlite.queries :as q]
            
            [next.jdbc :as jdbc]
            [taoensso.timbre :as log]))

(def column-params-m {"account" ["ID INTEGER PRIMARY KEY AUTOINCREMENT"
                                 "name VARCHAR(32) UNIQUE"
                                 "email VARCHAR(32) UNIQUE"
                                 "password VARCHAR(32) NOT NULL"
                                 "othernames VARCHAR(255)"
                                 "flags VARCHAR(255) NOT NULL"
                                 "activated BOOLEAN DEFAULT 0"
                                 "createdate TEXT NOT NULL"]
                      "passwordrecovery" ["ID INTEGER PRIMARY KEY AUTOINCREMENT"
                                          "email VARCHAR(32) UNIQUE"
                                          "createdate TEXT NOT NULL"
                                          "recoverylink TEXT"]
                      "failedlogin" ["ID INTEGER PRIMARY KEY AUTOINCREMENT"
                                     "email VARCHAR(32)"
                                     "createdate TEXT NOT NULL"
                                     "ipaddress TEXT"]})

(def failed-login-columns)

(defn stores-params-m [args]
  (when-not (:ttl-password-recovery (first args))
    (log/warn "No password expiration time was set, defaulting to 1800 seconds"))
  {"account" {}
   "passwordrecovery" {:expireAfterSeconds (if-let [arg-map (first args)]
                                                    (:ttl-password-recovery arg-map)
                                                    1800)}
   "failedlogin" {}})



(defn create-auth-stores [db & args]
  (log/debug "Creating the authentication sqlite stores")
  (sqlite/create-sqlite-tables db (stores-params-m args) column-params-m))

(defn drop-auth-tables [db]
  (log/debug "Dropping the authentication sqlite tables")
  (jdbc/execute-one! db [(q/drop-table "account")])
  (jdbc/execute-one! db [(q/drop-table "passwordrecovery")])
  (jdbc/execute-one! db [(q/drop-table "failedlogin")]))

(defn create-in-memory-stores []
  (log/debug "Creating in memory stores for testing the authentication lib")
  (log/spy (clojure.set/rename-keys (storage/create-in-memory-stores (keys (stores-params-m [])))
                                    {:account "account"
                                     :passwordrecovery "passwordrecovery"
                                     :failedlogin "failedlogin"})))

