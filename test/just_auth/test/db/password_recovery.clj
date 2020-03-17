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
  (:require [midje.sweet :refer :all]
            [just-auth.db 
             [password-recovery :as password-recovery]
             [account :as account]
             [just-auth :as auth-db]]
            [clj-storage.db.mongo :as mongo]
            [clj-storage.test.db.sqlite.test-db :as test-db]
            [taoensso.timbre :as log]
            [buddy.hashers :as hashers]))

(against-background [(before :contents (test-db/setup-db))
                     (after :contents (test-db/teardown-db))]

                    
                    (facts "Create a password recovery request which expires after a while" :slow
                           (let [flag :admin
                                 stores (auth-db/create-auth-stores
                                                              (test-db/get-test-db)
                                                              ;; TTL is set to 30 seconds but mongo checks only every ~60 secs
                                                              {:ttl-password-recovery 30})
                                 first-name "a-user"
                                 last-name "user-surname"
                                 email "user@mail.com"
                                 pswrd "a-password"
                                 user-account (account/new-account! (:account-store stores)
                                                                    {:first-name first-name
                                                                     :last-name last-name
                                                                     :email email
                                                                     :password pswrd}
                                                                    hashers/derive)]
                             (fact "Create a reset password entry with link"
                                   (password-recovery/new-entry! (:password-recovery-store stores)
                                                                 email
                                                                 "some-link"))

                             (fact "Can retrieve the password recovery entry"
                                   (:email (password-recovery/fetch-by-password-recovery-link
                                            (:password-recovery-store stores)
                                            "some-link")) => email)

                             (fact "After a while we cannot retrieve it any longer" :slow
                                   (Thread/sleep (* 90 1000))

                                   (password-recovery/fetch-by-password-recovery-link
                                    (:password-recovery-store stores)
                                     "some-link") => nil))))
