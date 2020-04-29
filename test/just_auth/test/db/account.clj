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
                             (let [first-name "a-user"
                                   last-name "user-surname"
                                   email "user@mail.com"
                                   pswrd "a-password"]

                               (fact "Can create an account"
                                     (account/new-account! account-store
                                                           {:name (str first-name " " last-name)
                                                            :email email
                                                            :password pswrd}
                                                           hashers/derive) => truthy)
                               (fact "An empty flag vector is created"
                                     (dissoc (account/fetch account-store email) :account/password :account/createdate :account/id) => {:account/ID 1
                                                                                                                                        :account/activated 0
                                                                                                                                        :account/email "user@mail.com"
                                                                                                                                        :account/flags []
                                                                                                                                        :account/name "a-user user-surname"
                                                                                                                                        :account/othernames nil
                                                                                                                                        :account/activationlink nil
                                                                                                                                         :account/activationuri nil}
                                     
                                     (:account/flags (account/fetch account-store email)) => [])

                               (fact "Can add a flag"
                                     (account/add-flag! account-store email "admin")
                                     (:account/flags (account/fetch account-store email))  => [:admin])

                               (fact "Can add another flag"
                                     (account/add-flag! account-store email "group1")
                                     (:account/flags (account/fetch account-store email)) => [:admin :group1])

                               (fact "Can remove a flag"
                                     (account/remove-flag! account-store email "admin")
                                     (:account/flags (account/fetch account-store email)) => [:group1])))))
