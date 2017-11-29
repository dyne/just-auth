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
(ns just-auth.test.core
  (:require [midje.sweet :refer :all]
            [just-auth
             [core  :as auth-lib]
             [schema :as schema]
             [messaging :as m]
             [util :as u]]
            [just-auth.db.account :as account]
            [clj-storage.core :as storage] 
            [schema.core :as s]
            [taoensso.timbre :as log]
            [buddy.hashers :as hashers]
            [failjure.core :as f]))

(fact "Create a new email based authentication record and validate schemas"
      (let [stores-m (storage/create-in-memory-stores ["account-store" "password-recovery-store"])
            hash-fns {:hash-fn hashers/derive
                      :hash-check-fn hashers/check}
            email-authentication (auth-lib/new-email-based-authentication stores-m
                                                 (m/new-stub-account-activator stores-m)
                                                 (m/new-stub-password-recoverer stores-m)
                                                 hash-fns)]

        (f/ok? email-authentication) => truthy
        
        (s/validate schema/AuthStores stores-m) => truthy

        (s/validate schema/HashFns hash-fns) => truthy

        (fact "Sign up a user and check that email has been sent"
              (let [email "some@mail.com"
                    uri "http://test.com"
                    password "12345678"]
                (auth-lib/sign-up email-authentication
                                  "Some name"
                                  email
                                  password
                                  {:activation-uri uri}
                                  ["nickname"])
                (-> email-authentication :account-activator :emails deref count) => 1
                (let [account-created (account/fetch (:account-store stores-m) email)]
                  account-created => truthy
                  (:activated (account/fetch (:account-store stores-m) email)) => false
                  (fact "Before activation one can't sign in"
                        (f/failed? (auth-lib/sign-in email-authentication email password)) => true
                        (:message (auth-lib/sign-in email-authentication email password)) => "The account needs to be activated first")
                  (fact "Activate account"
                        (let [activation-link (:activation-link account-created)]
                          (f/ok? (auth-lib/activate-account email-authentication email
                                                            {:activation-link activation-link})) => truthy
                          (:activated (account/fetch (:account-store stores-m) email)) => true))
                  (fact "We can now log in"
                        (f/ok? (auth-lib/sign-in email-authentication email password)) => true)

                  (fact "Reset password and sign in with new password"
                        ;; TODO expiration
                        ))))))
