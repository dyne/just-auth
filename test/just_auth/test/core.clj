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
            [just-auth.db
             [account :as account]
             [just-auth :as auth-db]]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [buddy.hashers :as hashers]
            [failjure.core :as f]
            [auxiliary.translation :as t]
            [environ.core :as env]
            [clj-storage.test.db.test-db :as test-db]))

(def auth-configuration {:email-config {:email-server "server"
                                        :email-user "user"
                                        :email-address "address"
                                        :email-pass "pass"}})

(fact "Can sign up using the real authenticator implementation and a real db"
      (let [_ (test-db/setup-db)
            db (test-db/get-test-db)
            stores-m (auth-db/create-auth-stores db)
            email-authenticator (auth-lib/email-based-authentication stores-m
                                                                     auth-configuration                                 
                                                                     {:criteria #{:email}
                                                                      :type :block
                                                                      :time-window-secs 10
                                                                      :threshold 10})
            attempt (auth-lib/sign-in (log/spy email-authenticator) 
                                      "test@mail.com"
                                      "password"
                                      {})]
        (f/failed? attempt) => true
        (:message attempt) => "No account found for email test@mail.com"))

(facts "Some basic core behaviour tested using the stub implementation."
       (let [stores-m (auth-db/create-in-memory-stores)
             hash-fns u/sample-hash-fns
             email-authenticator (auth-lib/new-stub-email-based-authentication
                                  stores-m
                                  (atom [])
                                  {}
                                  {:criteria #{:email} 
                                   :type :block
                                   :time-window-secs 10
                                   :threshold 5})]

         (f/ok? email-authenticator) => truthy 

         (s/validate schema/HashFns hash-fns) => truthy

         (fact "Sign up a user and check that email has been sent"
               (let [email "some@mail.com"
                     uri "http://test.com"
                     password "12345678"
                     created-account (auth-lib/sign-up email-authenticator
                                                       "Some name"
                                                       email
                                                       password
                                                       {:activation-uri uri}
                                                       ["nickname"])] 
                 (-> email-authenticator :account-activator :emails deref count) => 1
                 created-account => truthy
                 (:activated (account/fetch (:account-store stores-m) email)) => false
                 (fact "Before activation one can't sign in"
                       (f/failed? (auth-lib/sign-in email-authenticator email password {})) => true
                       (:message (auth-lib/sign-in email-authenticator email password {})) => "The account needs to be activated first")
                 (fact "Activate account"
                       (let [activation-link (:activation-link created-account)]
                         (f/ok? (auth-lib/activate-account email-authenticator email
                                                           {:activation-link activation-link})) => truthy
                         (:activated (account/fetch (:account-store stores-m) email)) => true))
                 (fact "We can now log in"
                       (f/ok? (auth-lib/sign-in email-authenticator email password {})) => true)

                 (fact "Reset password and sign in with new password"
                       ;; TODO expiration
                       )))))
