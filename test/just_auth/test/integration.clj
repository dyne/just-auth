;; Just auth - a simple two factor authenticatiokn library

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2017-2018 Dyne.org foundation

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

(ns just-auth.integration
  (:require [midje.sweet :refer :all]
            [just-auth
             [core :as auth-lib]]
            [clj-storage.test.db.test-db :as test-db]
            [just-auth.db
             [account :as account]
             [just-auth :as auth-db]]
            [failjure.core :as f]
            [taoensso.timbre :as log]))

(against-background [(before :contents (test-db/setup-db))
                     (after :contents (test-db/teardown-db))]
                    
                    (facts "Check that throttling works as expected during login for both email and ip-address" 
                           (let [stores (auth-db/create-auth-stores
                                         (test-db/get-test-db))
                                 authenticator (auth-lib/new-stub-email-based-authentication stores
                                                                                             (atom [])
                                                                                             {:criteria #{:email :ip-address} 
                                                                                              :type :block
                                                                                              :time-window-secs 10
                                                                                              :threshold 1})]
                             
                             (fact "First sign up and activate a couple of accounts"
                                   (:error (auth-lib/sign-up authenticator
                                                             "First person"
                                                             "first@mail.com"
                                                             "password"
                                                             {:activation-uri "http://localhost:8000"}
                                                             ["nickname"])) => :SUCCESS
                                   (let [account (account/fetch (:account-store stores) "first@mail.com")]                                (f/ok? (auth-lib/activate-account authenticator
                                                                                                                                                                            "first@mail.com"                                                                                                     
                                                                                                                                                                            (:activation-link account))) => true)
                                   (:error (auth-lib/sign-up authenticator
                                                             "Second person"
                                                             "second@mail.com"
                                                             "password"
                                                             {:activation-uri "http://localhost:8000"}
                                                             ["nickname"])) => :SUCCESS
                                   (let [account (account/fetch (:account-store stores) "second@mail.com")]                                (f/ok? (auth-lib/activate-account authenticator
                                                                                                                                                                            "second@mail.com"                                                                                                     
                                                                                                                                                                            (:activation-link account))) => true)
                                   (:error (auth-lib/sign-up authenticator
                                                             "Third person"
                                                             "third@mail.com"
                                                             "password"
                                                             {:activation-uri "http://localhost:8000"}
                                                             ["nickname"])) => :SUCCESS
                                   (let [account (account/fetch (:account-store stores) "third@mail.com")]                                (f/ok? (auth-lib/activate-account authenticator
                                                                                                                                                                            "third@mail.com"                                                                                                     
                                                                                                                                                                            (:activation-link account))) => true))
                             (fact "No attempt to login from the same ip faulty for more the allowed thrs."
                                   (let [ip-address "66.249.76.00"]
                                     (:message (auth-lib/sign-in authenticator "first@mail.com" "wrong" {:ip-address ip-address})) => "Wrong username and/or password"
                                     (:message (auth-lib/sign-in authenticator "second@mail.com" "wrong" {:ip-address ip-address})) => "Wrong username and/or password"
                                     (:message (auth-lib/sign-in authenticator "third@mail.com" "wrong" {:ip-address ip-address})) => "sfdo")))))
