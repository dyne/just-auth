;; Just auth - a simple two factor authenticatiokn library

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2018- Dyne.org foundation

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
  (:require [midje.sweet :refer [against-background fact facts before after => truthy falsey]]
            [just-auth.db
             [just-auth :as ja]
             [failed-login :as fl]
             [just-auth :as auth-db]]
            [just-auth
             [throttling :as thr]
             [schema :as auth-schema]]
            [clj-storage.core :as storage]
            [clj-storage.test.db.sqlite.test-db :as test-db]
            [schema.core :as s]
            [taoensso.timbre :as log]))

(def failed-attempts [{:email "email-1"
                       :ipaddress "ip-1"}
                      {:email "email-1"
                       :ipaddress "ip-1"}
                      {:email "email-2"
                       :ipaddress "ip-1"}
                      {:email "email-3"
                       :ipaddress "ip-3"}
                      {:email "email-4"
                       :ipaddress "ip-4"}
                      {}
                      {:email "email-4"}])

(against-background [(before :contents (test-db/setup-db))
                     (after :contents (ja/drop-auth-tables (test-db/get-datasource)))]
                    
                    (facts "Create some failed attempts" 
                           (let [stores (auth-db/create-auth-stores
                                         (test-db/get-datasource))]
                             (doseq [attempt failed-attempts]
                               (fl/new-attempt! (get stores "failedlogin")
                                                (:email attempt)
                                                (:ipaddress attempt)))
                             
                             #_(fact "Check inserted entries and dates"
                                     (storage/query (get stores "failedlogin") {} {}) => [])
                             
                             (fact "Check that block returns true when the number of attempts according to criteria surpass the thr"
                                   (thr/block? (get stores "failedlogin")
                                               10
                                               2
                                               {:ipaddress "ip-1"}) => truthy
                                   (thr/block? (get stores "failedlogin")
                                               10
                                               2
                                               {:ipaddress "ip-4"}) => falsey
                                   (thr/block? (get stores "failedlogin")
                                               10
                                               2
                                               {}) => truthy
                                   (thr/block? (get stores  "failedlogin")
                                               10
                                               10
                                               {}) => falsey
                                   (thr/block? (get stores "failedlogin")
                                               10
                                               1
                                               {:ipaddress "ip-1"
                                                :email "email-1"}) => truthy
                                   (thr/block? (get stores "failedlogin")
                                               10
                                               2
                                               {:ipaddress "ip-1"
                                                :email "email-1"}) => falsey
                                   ;; Check that time gets "renewed"
                                   (Thread/sleep 1000)
                                   (thr/block? (get stores "failedlogin")
                                               1
                                               2
                                               {:ipaddress "ip-1"}) => falsey)
                             (fact "Check that delay-in-secs returns the right amout of seconds when the number of attempts according to criteria surpass the thr"
                                   (thr/delay-in-secs? (get stores "failedlogin")
                                                       10
                                                       2
                                                       {:ipaddress "ip-1"}) => 2
                                   (thr/delay-in-secs? (get stores "failedlogin")
                                                       10
                                                       2
                                                       {:ipaddress "ip-4"}) => falsey
                                   (thr/delay-in-secs? (get stores "failedlogin")
                                                       10
                                                       2
                                                       {}) => 32
                                   (thr/delay-in-secs? (get stores "failedlogin")
                                                       10
                                                       10
                                                       {}) => falsey
                                   (thr/delay-in-secs? (get stores  "failedlogin")
                                                       10
                                                       1
                                                       {:ipaddress "ip-1"
                                                        :email "email-1"}) => 2
                                   (thr/delay-in-secs? (get stores "failedlogin")
                                                       10
                                                       2
                                                       {:ipaddress "ip-1"
                                                        :email "email-1"}) => falsey)

                             (fact "Check that throttle returns errors when needed"
                                   (let [config {:criteria #{:email} 
                                                 :type :block
                                                 :time-window-secs 10
                                                 :threshold 1}
                                         config-2 {:criteria #{:email} 
                                                   :type :delay
                                                   :time-window-secs 10
                                                   :threshold 1}
                                         config-3 {:criteria #{:ipaddress} 
                                                   :type :delay
                                                   :time-window-secs 10
                                                   :threshold 1}]
                                     (s/validate auth-schema/ThrottlingConfig config) => truthy

                                     ;; No throttling yet
                                     (thr/throttle? (get stores "failedlogin")
                                                    config
                                                    {:ipaddress "ip-1"})
                                     => nil
                                     
                                     ;; Store it
                                     (fl/new-attempt! (get stores "failedlogin")
                                                      nil
                                                      (:ipaddress "ip-1"))

                                     (let [attempt (thr/throttle?
                                                    (get stores "failedlogin")
                                                    config
                                                    {:ipaddress "ip-1"})]
                                       (class attempt) => failjure.core.Failure
                                       (:message attempt) => "Blocked access for {:email nil}. Please contact the website admin." )

                                     ;; 2 with same email was added on test above
                                     (:message (thr/throttle? (get stores "failedlogin")
                                                              config
                                                              {:ipaddress "ip-1"
                                                               :email "email-1"}))
                                     => "Blocked access for {:email \"email-1\"}. Please contact the website admin."

                                     ;; Try the same but now with delay instead of block
                                     (:message (thr/throttle? (get stores "failedlogin")
                                                              config-2
                                                              {:ipaddress "ip-1"
                                                               :email "email-1"}))
                                     => "Suspicious behaviour for {:email \"email-1\"}. Retry again in 2 seconds"
                                     ;; And now with the ip as criteria
                                     (:message (thr/throttle? (get stores "failedlogin")
                                                              config-3
                                                              {:ipaddress "ip-1"
                                                               :email "email-1"}))
                                     => "Suspicious behaviour for {:ip-address \"ip-1\"}. Retry again in 4 seconds")))))
