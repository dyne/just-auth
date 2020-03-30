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
  (:require [midje.sweet :refer [fact => facts truthy against-background before after]]
            [just-auth.db
             [just-auth :as ja]
             [account :as account]]
            [clj-storage.test.db.sqlite.test-db :as test-db]
            [taoensso.timbre :as log]
            [buddy.hashers :as hashers]))

(against-background [(before :contents (test-db/setup-db))
                     (after :contents (ja/drop-auth-tables (test-db/get-datasource)))]

                    (let [db (test-db/get-datasource)
                          stores-m (ja/create-auth-stores db)
                          account-store (get stores-m "account")]
                      (facts "Create an account"
                             (let [flag :admin
                                   first-name "a-user"
                                   last-name "user-surname"
                                   email "user@mail.com"
                                   pswrd "a-password"
                                   user-account (log/spy (account/new-account! account-store
                                                                               {:name (str first-name " " last-name)
                                                                                :email email
                                                                                :password pswrd}
                                                                               hashers/derive))]
                               
                               (fact "An empty flag vector is created"
                                     (dissoc (account/fetch account-store email) :account/password :account/createdate :account/id) => {:account/ID 1
                                                                                                                                        :account/activated 0
                                                                                                                                        :account/email "user@mail.com"
                                                                                                                                        :account/flags '()
                                                                                                                                        :account/name "a-user user-surname"
                                                                                                                                        :account/othernames nil}
                                     
                                     (:account/flags (account/fetch account-store email)) => '())

                               (fact "Can add a flag"
                                     (account/add-flag! account-store email :admin)
                                     (:flags (account/fetch account-store email))  => [:admin]

                                     ;; This could be a bug - have posted a question https://stackoverflow.com/questions/45677891/keyword-item-in-moger-vector-is-converted-to-string
                                     ;; UPDATE: manually converted to a keyword
                                     (fact "Caution!! Mongo converts the keywords to a string"
                                           (-> (account/fetch account-store email)
                                               :flags
                                               (first))  => flag))

                               #_(fact "Can remove a flag"
                                     (account/remove-flag! account-store email "admin")
                                     (:flags (account/fetch account-store email)) => [])))))
