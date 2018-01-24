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

(ns just-auth.throttling
  (:require [taoensso.timbre :as log]
            [just-auth.db.failed-login :as fl]
            [failjure.core :as f]))

(defn- pow [x n]
  (reduce * (repeat n x)))

;; Inspired by https://blog.codinghorror.com/dictionary-attacks-101/
(defn delay-in-secs? [failed-login-store time-window-secs threshold {:keys [ip-address email] :as criteria}]
  "When a account or ip have more than a thrshold of attempts return the delay in seconds until next allowed login attempt. Returns nil when no delay is required."
  (let [number-attempts (fl/number-attempts failed-login-store time-window-secs criteria)
        excess (- (log/spy number-attempts) (log/spy threshold))]
    (when (> excess 0)
      (pow 2 excess))))

(defn block? [failed-login-store time-window-secs threshold {:keys [ip-address email] :as criteria}]
  "This function checks where there are more failed attempts than a threshold given an ip, an email, none or both. It will return true when the failed attempts with the given criteria surpass the thr."
  (when (>
         (fl/number-attempts failed-login-store time-window-secs criteria)
         threshold)
    true))

(defn throttle?
  [failed-login-store throttling-config {:keys [email ip-address]}]
  "Wrapper fn that returns errors when necessary or nil when not throttling behaviour is necessary"
  (let [thr-fn (if (= :block (:type throttling-config))
                 block?
                 delay-in-secs?)
        criteria (select-keys {:email email :ip-address ip-address}
                              (:criteria throttling-config))
        result (thr-fn failed-login-store
                       (:time-window-secs throttling-config)
                       (:threshold throttling-config)
                       criteria)]
    (cond  (= (log/spy result) true)
           (f/fail (str "Blocked access for " criteria ". Please contact the website admin."))
           (number? result)
           (f/fail (str "Suspicious behaviour for " criteria ". Retry again in " result " seconds")))))
