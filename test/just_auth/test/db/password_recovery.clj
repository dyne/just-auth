;; Just auth - a simple two factor authenticatiokn library

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2017 Dyne.org foundation

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

(ns just-auth.test.db.password-recovery
  (:require [midje.sweet :refer [fact => facts truthy against-background before after]]
            [just-auth.db 
             [password-recovery :as password-recovery]
             [account :as account]
             [just-auth :as ja]]
            [clj-storage.test.db.sqlite.test-db :as test-db]
            [taoensso.timbre :as log]))

(against-background [(before :contents (test-db/setup-db))
                     (after :contents (ja/drop-auth-tables (test-db/get-datasource)))]

                    
                    (facts "Create a password recovery request which expires after a while" :slow
                           (let [stores (ja/create-auth-stores
                                         (test-db/get-datasource) {:ttl-password-recovery 30})
                                 email "user@mail.com"
                                 password-recovery-store (get stores "passwordrecovery")]
                             (fact "Create a reset password entry with link"
                                   (-> (password-recovery/new-entry! password-recovery-store
                                                                     email
                                                                     "some-link")
                                       first
                                       last) => 1)

                             (fact "Can retrieve the password recovery entry"
                                   (:passwordrecovery/email (password-recovery/fetch-by-password-recovery-link
                                            password-recovery-store
                                            "some-link")) => email)

                             (fact "After a while we cannot retrieve it any longer" :slow
                                   (Thread/sleep (* 90 1000))
                                   
                                   (password-recovery/fetch-by-password-recovery-link
                                    password-recovery-store
                                    "some-link") => nil))))
