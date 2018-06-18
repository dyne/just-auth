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

(ns just-auth.db.failed-login
  (:require [clj-storage.core :as storage]
            monger.json 
            [clj-time.core :as dt]
            [taoensso.timbre :as log]
            monger.joda-time))

(defn new-attempt!
  [failed-login-store email ip-address]
  (let [created-at (java.util.Date.)]
    (storage/store-and-create-id! failed-login-store {:email email
                                                         :created-at created-at
                                                         :ip-address ip-address})))

(defn number-attempts [failed-login-store time-window-secs {:keys [email ip-address] :as formula}]
  (let [from-date-time (dt/minus- (dt/now) (dt/seconds time-window-secs))]
    (storage/count-since failed-login-store from-date-time formula)))
