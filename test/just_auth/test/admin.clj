;; Just auth - a simple two factor authenticatiokn library

;; part of PIE News (http://pieproject.eu/)
;; R&D funded by the European Commission (Horizon 2020/grant agreement No 687922)

;; Copyright (C) 2017-2018 Dyne.org foundation

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
(ns just-auth.test.admin
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
            [failjure.core :as f]
            [clj-storage.test.db.test-db :as test-db]))

(def emails (atom []))
(def user-email "user@mail.com")
(def admin-email "admin@mail.com")

(fact "Check using a stub implementation the effect of the admin absence"
      (let [stores-m (auth-db/create-in-memory-stores)
             hash-fns u/sample-hash-fns
             email-authenticator (auth-lib/new-stub-email-based-authentication
                                  stores-m
                                  emails
                                  {}
                                  {:criteria #{:email} 
                                   :type :block
                                   :time-window-secs 10
                                   :threshold 5})]

         (f/ok? email-authenticator) => truthy 

         (s/validate schema/HashFns hash-fns) => truthy

         (fact "When there is no admin included the activation email is sent to the user."
               (let [uri "http://test.com"
                     password "12345678"
                     created-account (auth-lib/sign-up email-authenticator
                                                       "Some name"
                                                       user-email
                                                       password
                                                       {:activation-uri uri}
                                                       ["nickname"])] 
                 (-> @emails last :email) => user-email))))

(fact "Check using a stub implementation the effect of the admin absence"
      (let [stores-m (auth-db/create-in-memory-stores)
            hash-fns u/sample-hash-fns
            email-authenticator (auth-lib/new-stub-email-based-authentication
                                 stores-m
                                 emails
                                 {:email-admin admin-email}
                                 {:criteria #{:email} 
                                  :type :block
                                  :time-window-secs 10
                                  :threshold 5})]

        (f/ok? email-authenticator) => truthy 

        (s/validate schema/HashFns hash-fns) => truthy

        (fact "When there is no admin included the activation email is sent to the user."
              (let [uri "http://test.com"
                    password "12345678"
                    created-account (auth-lib/sign-up email-authenticator
                                                      "Some name"
                                                      user-email
                                                      password
                                                      {:activation-uri uri}
                                                      ["nickname"])] 
                (-> @emails last :email) => admin-email))))
