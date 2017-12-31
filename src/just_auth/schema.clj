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

(ns just-auth.schema
  (:require [clj-storage.core :refer [Store]]
            [schema.core :as s]))

(def StoreSchema clj_storage.core.Store)

(s/defschema HashFns
  {:hash-fn clojure.lang.Fn
   :hash-check-fn clojure.lang.Fn})

(def AuthStores {:account-store StoreSchema
                 :password-recovery-store StoreSchema
                 :failed-login StoreSchema})

(def EmailSignUp
  {:name s/Str
   :other-names [s/Str]
   :email s/Str ;;TODO email reg exp?
   :password s/Str
   :activation-uri s/Str ;; TODO URI
   })

(def EmailConfig
  {:email-server s/Str 
   :email-user s/Str
   :email-pass s/Str
   :email-address s/Str})

