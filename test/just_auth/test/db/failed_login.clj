;; Just auth - a simple two factor authenticatiokn library

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2017- Dyne.org foundation

;; Sourcecode designed, written and maintained by
;; Aspasia Beneti <aspra@dyne.org>

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

(ns just-auth.test.db.failed-login
  (:require [midje.sweet :refer [fact => facts truthy against-background before after]]
            [just-auth.db 
             [failed-login :as fl]
             [just-auth :as ja]]
            [clj-storage.test.db.sqlite.test-db :as test-db]
            [taoensso.timbre :as log]))

(def failed-attempts [{:email "email-1"
                       :ipaddress "ip-1"}
                      {:email "email-1"
                       :ipaddress "ip-1"}
                      {:email "email-2"
                       :ipaddress "ip-1"}
                      {:email "email-3"
                       :ipaddress "ip-3"}
                      {:email "email-4"
                       :ipaddress "ip-4"}
                      {}
                      {:email "email-4"}])

(against-background [(before :contents (test-db/setup-db))
                     (after :contents (ja/drop-auth-tables (test-db/get-datasource)))]
                    
                    (facts "Create some failed attempts" 
                           (let [stores (ja/create-auth-stores
                                         (test-db/get-datasource))
                                 failed-login-store (get stores "failedlogin")]
                             (doseq [attempt failed-attempts]
                               (fl/new-attempt! failed-login-store
                                                (:email attempt)
                                                (:ipaddress attempt)))
                             (fact "Count the number of all failed attempts the last 10 seconds"
                                   (fl/number-attempts failed-login-store
                                                       10
                                                       {}) => (count failed-attempts)

                                   (fl/number-attempts failed-login-store
                                                       10
                                                       {:email "email-1"}) => (count (filter (comp (partial = "email-1") :email) failed-attempts))

                                   (fl/number-attempts failed-login-store
                                                       10
                                                       {:ipaddress "ip-1"})  => (count (filter (comp (partial = "ip-1") :ipaddress) failed-attempts))
                                   (fl/number-attempts failed-login-store
                                                       0
                                                       {}) => 0))))
