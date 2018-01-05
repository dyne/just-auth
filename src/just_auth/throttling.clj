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
            [just-auth.db.failed-login :as fl]))

(defn- pow [x n]
  (reduce * (repeat n x)))


;; Inspired by https://blog.codinghorror.com/dictionary-attacks-101/
(defn delay [db failed-login-store time-window-secs threshold {:keys [ip-address email]}]
  "When a account or ip have more than a thrshold of attempts return the delay in seconds until next allowed login"
  (let [max-number-attempts (max (fl/number-attempts db failed-login-store time-window-secs {:ip-address ip-address})
                                 (fl/number-attempts db failed-login-store time-window-secs {:ip-address ip-address}))]
    (when (> max-number-attempts threshold)
      (pow 2 max-number-attempts))))

(defn block [db failed-login-store time-window-secs threshold {:keys [ip-address email]}]
  (when (and ip-address
         (>
          (fl/number-attempts db failed-login-store time-window-secs {:ip-address ip-address})
          threshold))
    ip-address)
  (when (and email
             (>
              (fl/number-attempts db failed-login-store time-window-secs {:email email})
              threshold))
    email))
