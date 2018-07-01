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

(ns just-auth.test.integration
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
                                 authenticator (auth-lib/new-stub-email-based-authentication
                                                stores
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
                             (fact "Attempt to login with a wrong password with the same ip and email for more times than the threshold. It should block after the second try"
                                   (let [ip-address "66.249.76.00"
                                         email "first@mail.com"]
                                     (:message (auth-lib/sign-in authenticator email "wrong" {:ip-address ip-address})) => "Wrong username and/or password"
                                     (:message (auth-lib/sign-in authenticator email "wrong" {:ip-address ip-address})) => "Wrong username and/or password" 
                                     (:message (auth-lib/sign-in authenticator email "wrong" {:ip-address ip-address})) => "Blocked access for {:email \"first@mail.com\", :ip-address \"66.249.76.00\"}. Please contact the website admin."))

                             (fact "Now try to login with the same ip but different email. It will block after two tries of the same email."
                                   (let [ip-address "66.249.76.00"]
                                     (:message (auth-lib/sign-in authenticator "second@mail.com" "wrong" {:ip-address ip-address})) => "Wrong username and/or password"
                                     (:message (auth-lib/sign-in authenticator "third@mail.com" "wrong" {:ip-address ip-address})) => "Wrong username and/or password"
                                     (:message (auth-lib/sign-in authenticator "second@mail.com" "wrong" {:ip-address ip-address})) => "Wrong username and/or password"
                                     (:message (auth-lib/sign-in authenticator "third@mail.com" "wrong" {:ip-address ip-address})) => "Wrong username and/or password"
                                     (:message (auth-lib/sign-in authenticator "second@mail.com" "wrong" {:ip-address ip-address})) => "Blocked access for {:email \"second@mail.com\", :ip-address \"66.249.76.00\"}. Please contact the website admin."
                                     (:message (auth-lib/sign-in authenticator "third@mail.com" "wrong" {:ip-address ip-address})) => "Blocked access for {:email \"third@mail.com\", :ip-address \"66.249.76.00\"}. Please contact the website admin."))
                             
                             (fact "Now try with the first email and a different ip. Since it takes the combination as a filter it should not block."
                                   (:message (auth-lib/sign-in authenticator "first@mail.com" "wrong" {:ip-address "66.249.76.01"})) => "Wrong username and/or password")))

                    (facts "Check that throttling works as expected during login for none of email and ip-address (so any login)" 
                           (let [stores (auth-db/create-auth-stores
                                         (test-db/get-test-db))
                                 authenticator (auth-lib/new-stub-email-based-authentication
                                                stores
                                                (atom [])
                                                {:criteria #{} 
                                                 :type :block
                                                 :time-window-secs 2
                                                 :threshold 1})]
                             
                             (fact "First sign up and activate a couple more accounts"
                                   (:error (auth-lib/sign-up authenticator
                                                             "fourth person"
                                                             "fourth@mail.com"
                                                             "password"
                                                             {:activation-uri "http://localhost:8000"}
                                                             ["nickname"])) => :SUCCESS
                                   (let [account (account/fetch (:account-store stores) "fourth@mail.com")]                                (f/ok? (auth-lib/activate-account authenticator "fourth@mail.com" (:activation-link account))) => true)
                                   (:error (auth-lib/sign-up authenticator
                                                             "Fifth person"
                                                             "fifth@mail.com"
                                                             "password"
                                                             {:activation-uri "http://localhost:8000"}
                                                             ["nickname"])) => :SUCCESS
                                   (let [account (account/fetch (:account-store stores) "fifth@mail.com")]                                (f/ok? (auth-lib/activate-account authenticator "fifth@mail.com" (:activation-link account))) => true))
                             
                             (fact "Attempt to login with a wrong password any ip or email. It should block after in any case cause more than 1 wrong attempts have taken place."
                                   (:message (auth-lib/sign-in authenticator "fourth@mail.com" "wrong" {:ip-address "66.123.12.00"})) => "Blocked access for {}. Please contact the website admin."
                                   (:message (auth-lib/sign-in authenticator "fifth@mail.com" "wrong" {:ip-address "66.123.12.01"})) => "Blocked access for {}. Please contact the website admin."
                                   (:message (auth-lib/sign-in authenticator "fourth@mail.com" "wrong" {:ip-address "66.123.12.02"})) => "Blocked access for {}. Please contact the website admin.")

                             (fact "Try again with correct password after the block time has passed, it should just work."
                                   (Thread/sleep 2000)
                                   (auth-lib/sign-in authenticator "fourth@mail.com" "password" {:ip-address "66.123.12.00"}) => {:email "fourth@mail.com" :name "fourth person" :other-names ["nickname"]}))))
