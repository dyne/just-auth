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

(ns just-auth.test.db.account
  (:require [midje.sweet :refer :all]
            [just-auth.db 
             [account :as account]]
            [clj-storage.db.mongo :as mongo]
            [clj-storage.test.db.mongo.test-db :as test-db]
            [taoensso.timbre :as log]
            [buddy.hashers :as hashers]))

(against-background [(before :contents (test-db/setup-db))
                     (after :contents (test-db/teardown-db))]

                    
                    (facts "Create an account"
                           (let [flag :admin
                                 account-store (mongo/create-mongo-store (test-db/get-test-db) "accounts")
                                 first-name "a-user"
                                 last-name "user-surname"
                                 email "user@mail.com"
                                 pswrd "a-password"
                                 user-account (account/new-account! account-store
                                                                    {:first-name first-name
                                                                     :last-name last-name
                                                                     :email email
                                                                     :password pswrd}
                                                                    hashers/derive)]
                             
                             (fact "An empty flag vector is created"
                                   (dissoc (account/fetch account-store email) :password :created-at) =>
                                   {:first-name first-name
                                    :last-name last-name
                                    :email email
                                    :flags []
                                    :activated false}
                                   
                                   (:flags user-account) => [])

                             (fact "Can add a flag"
                                   (account/add-flag! account-store email :admin)
                                   (:flags (account/fetch account-store email))  => [:admin]

                                   ;; This could be a bug - have posted a question https://stackoverflow.com/questions/45677891/keyword-item-in-moger-vector-is-converted-to-string
                                   ;; UPDATE: manually converted to a keyword
                                   (fact "Caution!! Mongo converts the keywords to a string"
                                         (-> (account/fetch account-store email)
                                             :flags
                                             (first))  => flag))

                             (fact "Can remove a flag"
                                   (account/remove-flag! account-store email "admin")
                                   (:flags (account/fetch account-store email)) => []))))
