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

(ns just-auth.core
  (:require [just-auth.db 
             [account :as account]
             [password-recovery :as pr]]
            [just-auth
             [schema :refer [HashFns
                             AuthStores
                             EmailMessagingSchema
                             EmailSignUp
                             StoreSchema]]
             [messaging :as m]
             [util :as u]]
            [taoensso.timbre :as log]
            [schema.core :as s]
            [fxc.core :as fxc]
            [failjure.core :as f]
            [auxiliary.translation :as t]))

(defprotocol Authentication
  ;; About names http://www.kalzumeus.com/2010/06/17/falsehoods-programmers-believe-about-name
  (sign-up [this name email password second-step-conf other-names])

  (sign-in [this email password])

  ;; TODO: sign-out?
  
  ;; TODO: maybe add password?
  (activate-account [this email second-step-conf])

  (send-activation-message [this email second-step-conf])

  (send-password-reset-message [this email second-step-conf])

  (de-activate-account [this email password])

  (reset-password [this email old-password new-password second-step-conf]))


(s/defn ^:always-validate sign-up-with-email
  [authenticator :- just_auth.core.Authentication
   account-store :- StoreSchema
   {:keys [name
           other-names
           email
           password 
           activation-uri]} :- EmailSignUp
   hash-fns :- HashFns]
  (if (account/fetch account-store email)
    ;; TODO: warn with an email for this attempt?
    (f/fail (t/locale [:error :core :account-exists]))
    (do (account/new-account! account-store
                              (cond-> {:name name
                                       :email email
                                       :password password}
                                other-names (assoc :other-names other-names))
                              (:hash-fn hash-fns))
        (send-activation-message authenticator email {:activation-uri activation-uri}))))

(s/defrecord EmailBasedAuthentication
    [account-store :-  StoreSchema
     password-recovery-store :- StoreSchema
     account-activator :- EmailMessagingSchema
     password-recoverer :- EmailMessagingSchema
     hash-fns :- HashFns]

  Authentication
  (send-activation-message [_ email {:keys [activation-uri]}]
    (let [account (account/fetch account-store email)]
      (if account
        (if (:activated account)
          (f/fail (t/locale [:error :core :already-active]))
          (let [activation-id (fxc.core/generate 32)
                activation-link (u/construct-link {:uri activation-uri
                                                   :action "activate"
                                                   :token activation-id
                                                   :email email})]
            (if-not (m/email-and-update! account-activator email activation-link)
              (f/fail (t/locale [:error :core :not-sent]))
              account)))
        ;; TODO: send an email to that email
        (f/fail (str (t/locale [:error :core :account-not-found]) email)))))

  (send-password-reset-message [_ email {:keys [reset-uri]}]
    (let [account (account/fetch account-store email)]
      (if account
        (if (pr/fetch password-recovery-store email)
          (f/fail (t/locale [:error :core :password-already-sent]))
          (let [password-reset-id (fxc.core/generate 32)
                password-reset-link (u/construct-link {:uri reset-uri
                                                       :token password-reset-id
                                                       :email email
                                                       :action "reset-password"})]
            (if-not (m/email-and-update! password-recoverer email password-reset-link)
              (f/fail (t/locale [:error :core :not-sent]))
              account)))
        ;; TODO: send an email to that email?
        (f/fail (str (t/locale [:error :core :account-not-found]) email)))))
  
  (sign-up [this name email password {:keys [activation-uri]} other-names]
    (sign-up-with-email this account-store {:name name
                                            :other-names other-names
                                            :email email
                                            :password password
                                            :activation-uri activation-uri} hash-fns))
  
  (sign-in [_ email password]
    (if-let [account (account/fetch account-store email)]
      (if (:activated account)
        (if (account/correct-password? account-store email password (:hash-check-fn hash-fns))
          {:email email
           :name (:name account)
           :other-names (:other-names account)}
          ;; TODO: send email?
          ;; TODO: waht to do after x amount of times? Maybe should be handled on server level?
          (f/fail (t/locale [:error :core :wrong-pass])))
        (f/fail (t/locale [:error :core :not-active])))
      (f/fail (str (t/locale [:error :core :account-not-found]) email))))

  (activate-account [_ email {:keys [activation-link]}]
    (let [account (account/fetch-by-activation-link account-store activation-link)]
      (if account
        (if (= (:email account) email)
          (account/activate! account-store email)
          (f/fail (t/locale :error :core :not-matching-code)))
        (f/fail (str (t/locale [:error :core :activation-not-found]) activation-link)))))

  (de-activate-account [_ email de-activation-link]
    ;; TODO
    )

  (reset-password [_ email old-password new-password {:keys [password-reset-link]}]
    (if (= (pr/fetch-by-password-recovery-link password-recovery-store password-reset-link))
      (if (account/correct-password? account-store email old-password (:hash-check-fn hash-fns))
        (account/update-password! account-store email new-password (:hash-fn hash-fns))
        ;; TODO: send email?
        (f/fail (t/locale [:error :core :wrong-pass])))
      (f/fail (t/locale [:error :core :expired-link])))))


(s/defn ^:always-validate new-email-based-authentication
  ;; TODO: do we need some sort of session that expires after a while? And if so should it be handled by this lib or on top of it?
  [stores :- AuthStores
   account-activator :- EmailMessagingSchema
   password-recoverer :- EmailMessagingSchema
   hash-fns :- HashFns]
  (s/validate just_auth.core.Authentication
              (map->EmailBasedAuthentication {:account-store (:account-store stores)
                                              :password-recovery-store (:password-recovery-store stores)
                                              :password-recoverer password-recoverer
                                              :account-activator account-activator
                                              :hash-fns hash-fns})))

(s/defrecord StubEmailBasedAuthentication
    [account-activator :- EmailMessagingSchema
     password-recoverer :- EmailMessagingSchema]

  Authentication
  (sign-up [_ name email password _ other-names]
    "The account is already activated and email is sent"
    (account/new-account! (:account-store account-activator)
                              (cond-> {:name name
                                       :email email
                                       :password password
                                       :activated true}
                                other-names (assoc :other-names other-names))
                              (:hash-fn u/sample-hash-fns)))
  
  (send-activation-message [_ email {:keys [activation-uri]}]
    (let [activation-id (fxc.core/generate 32)
          activation-link (u/construct-link {:uri activation-uri
                                             :action "activate"
                                             :token activation-id
                                             :email email})]
      (m/email-and-update! account-activator email activation-link)))

  (send-password-reset-message [_ email {:keys [reset-uri]}]
    (let [password-reset-id (fxc.core/generate 32)
          password-reset-link (u/construct-link {:uri reset-uri
                                                 :token password-reset-id
                                                 :email email
                                                 :action "reset-password"})]
      (m/email-and-update! password-recoverer email password-reset-link))))

(defn new-stub-email-based-authentication [stores emails]
  (map->StubEmailBasedAuthentication {:account-activator (m/new-stub-account-activator stores emails)
                                      :password-recoverer (m/new-stub-password-recoverer stores emails)}))
