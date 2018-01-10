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

(ns just-auth.test.throttling
  (:require [midje.sweet :refer :all]
            [just-auth.db 
             [failed-login :as fl]
             [just-auth :as auth-db]]
            [just-auth
             [throttling :as thr]
             [schema :as auth-schema]]
            [clj-storage.test.db.test-db :as test-db]
            [schema.core :as s]
            [taoensso.timbre :as log]))

(def failed-attempts [{:email "email-1"
                       :ip-address "ip-1"}
                      {:email "email-1"
                       :ip-address "ip-1"}
                      {:email "email-2"
                       :ip-address "ip-1"}
                      {:email "email-3"
                       :ip-address "ip-3"}
                      {:email "email-4"
                       :ip-address "ip-4"}
                      {}
                      {:email "email-4"}])

(against-background [(before :contents (test-db/setup-db))
                     (after :contents (test-db/teardown-db))]
                    
                    (facts "Create some failed attempts" 
                           (let [stores (auth-db/create-auth-stores
                                         (test-db/get-test-db))]
                             (doseq [attempt failed-attempts]
                               (fl/new-attempt! (:failed-login-store stores)
                                                (:email attempt)
                                                (:ip-address attempt)))
                             (fact "Check that block returns true when the number of attempts according to criteria surpass the thr"
                                   (thr/block? (test-db/get-test-db)
                                               (:coll (:failed-login-store stores))
                                               10
                                               2
                                               {:ip-address "ip-1"}) => truthy
                                   (thr/block? (test-db/get-test-db)
                                               (:coll (:failed-login-store stores))
                                               10
                                               2
                                               {:ip-address "ip-4"}) => falsey
                                   (thr/block? (test-db/get-test-db)
                                               (:coll (:failed-login-store stores))
                                               10
                                               2
                                               {}) => truthy
                                   (thr/block? (test-db/get-test-db)
                                               (:coll (:failed-login-store stores))
                                               10
                                               10
                                               {}) => falsey
                                   (thr/block? (test-db/get-test-db)
                                               (:coll (:failed-login-store stores))
                                               10
                                               1
                                               {:ip-address "ip-1"
                                                :email "email-1"}) => truthy
                                   (thr/block? (test-db/get-test-db)
                                               (:coll (:failed-login-store stores))
                                               10
                                               2
                                               {:ip-address "ip-1"
                                                :email "email-1"}) => falsey
                                   ;; Check that time gets "renewed"
                                   (Thread/sleep 1000)
                                   (thr/block? (test-db/get-test-db)
                                               (:coll (:failed-login-store stores))
                                               1
                                               2
                                               {:ip-address "ip-1"}) => falsey)
                             (fact "Check that delay-in-secs returns the right amout of seconds when the number of attempts according to criteria surpass the thr"
                                   (thr/delay-in-secs? (test-db/get-test-db)
                                                       (:coll (:failed-login-store stores))
                                                       10
                                                       2
                                                       {:ip-address "ip-1"}) => 2
                                   (thr/delay-in-secs? (test-db/get-test-db)
                                                       (:coll (:failed-login-store stores))
                                                       10
                                                       2
                                                       {:ip-address "ip-4"}) => falsey
                                   (thr/delay-in-secs? (test-db/get-test-db)
                                                       (:coll (:failed-login-store stores))
                                                       10
                                                       2
                                                       {}) => 32
                                   (thr/delay-in-secs? (test-db/get-test-db)
                                                       (:coll (:failed-login-store stores))
                                                       10
                                                       10
                                                       {}) => falsey
                                   (thr/delay-in-secs? (test-db/get-test-db)
                                                       (:coll (:failed-login-store stores))
                                                       10
                                                       1
                                                       {:ip-address "ip-1"
                                                        :email "email-1"}) => 2
                                   (thr/delay-in-secs? (test-db/get-test-db)
                                                       (:coll (:failed-login-store stores))
                                                       10
                                                       2
                                                       {:ip-address "ip-1"
                                                        :email "email-1"}) => falsey)

                             (fact "Check that throttle returns errors when needed"
                                   (let [config {:criteria #{:email} 
                                                 :type :delay
                                                 :time-window-secs 10
                                                 :threshold 1}
                                         config-2 {:criteria #{:email} 
                                                   :type :block
                                                   :time-window-secs 10
                                                   :threshold 1}]
                                     (s/validate auth-schema/ThrottlingConfig config) => truthy
                                     (let [check-1 (thr/throttle? (test-db/get-test-db)
                                                                    (:coll (:failed-login-store stores))
                                                                    config
                                                                    {:ip-address "ip-1"})
                                           check-2 (thr/throttle? (test-db/get-test-db)
                                                                    (:coll (:failed-login-store stores))
                                                                    config
                                                                    {:email "email-5"})
                                           check-3 (thr/throttle? (test-db/get-test-db)
                                                                  (:coll (:failed-login-store stores))
                                                                  config-2
                                                                  {:email "email-1"})]

                                       (class check-1) => failjure.core.Failure
                                       (:message check-1) => "Suspicious behaviour for {:email nil}. Retry again in 64 seconds"
                                       check-2 => nil
                                       (:message check-3) => "Blocked access for {:email \"email-1\"}. Please contact the website admin."))))))
