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
             [password-recovery :as pr]
             [storage :refer [AuthStore]]]
            [just-auth
             [messaging :as m :refer [Email]]
             [schema :refer [HashFns]]]
            [taoensso.timbre :as log]
            [schema.core :as s]
            [fxc.core :as fxc]))

(defprotocol Authentication
  ;; About names http://www.kalzumeus.com/2010/06/17/falsehoods-programmers-believe-about-name
  (sign-up [this name email password activation-uri & other-names])

  (sign-in [this email password])

  ;; TODO: maybe add password?
  (activate-account [this email activation-link])

  ;; TODO - shouldnt be email but...
  (send-activation-message [this email activation-uri])

  (de-activate-account [this email password])

  ;; TODO:; not URI as it can be an sms
  (reset-password [this email old-password new-password]))

;; TODO: We could use something like https://github.com/adambard/failjure for error handling
(s/defrecord EmailBasedAuthentication
    [account-store :-  AuthStore
     account-activator :- Email
     hash-fns :- HashFns]

  Authentication
  (send-activation-message [_ email activation-uri]
    (let [account (account/fetch account-store email)]
      (if account
        (if (:activated account)
          {:error :already-active}
          (let [activation-id (fxc.core/generate 32)
                activation-link (str activation-uri "/" activation-id)]
            (if-not (m/email-and-update! account-activator email activation-link)
              {:error :email}
              account)))
        ;; TODO: send an email to that email
        {:error :not-found})))
  
  (sign-up [this name email password activation-uri & other-names] 
    (if (account/fetch account-store email)
      {:error :already-exists}
      (do (account/new-account! account-store
                                (cond-> {:name name
                                         :email email
                                         :password password}
                                  other-names (assoc :other-names other-names))
                                hash-fns)
          (send-activation-message this email activation-uri))))
  
  (sign-in [_ email password]
    (if-let [account (account/fetch account-store email)]
      (if (:activated account)
        (if (account/correct-password? account-store email password (:hash-check-fn hash-fns))
          {:email email
           :name (:name account)
           :other-names (:other-names account)}
          ;; TODO: send email?
          {:error :wrong-password})
        {:error :account-inactive})
      {:error :not-found}))

  (activate-account [_ email activation-link]
    (let [account (account/fetch-by-activation-link account-store activation-link)]
      (if account
        (if (= (:email account) email)
          (account/activate! account-store email)
          {:error :no-matching-email})
        {:error :not-found})))

  (de-activate-account [_ email de-activation-link]
    ;; TODO
    )

  (reset-password [_ email old-password new-password]
    (if (account/correct-password? account-store email old-password (:hash-check-fn hash-fns))
      (account/update-password! account-store email new-password (:hash-fn hash-fns))
      {:error :wrong-password})))

#_(lc/defresource send-password-recovery-email [account-store password-recovery-store password-recoverer]
  :allowed-methods [:post]
  :available-media-types content-types
  :known-content-type? #(check-content-type % content-types)
  :processable? (fn [ctx]
                  (let [{:keys [status data problems]}
                        (fh/validate-form sign-in-page/password-recovery-form
                                          (ch/context->params ctx))]
                    (if (= :ok status)
                      (let [email (-> ctx :request :params :email-address)]
                        (if-not (account/fetch account-store email)
                          ;; TODO: It would be safer if we do not notify that the email doesnt exist but send an email anyway saying that someone attempted to change the password.
                          [false (fh/form-problem (conj problems
                                                        {:keys [:email] :msg (str "The email " email " is not registered yet. Please sign up first")}))]
                          (if (pr/fetch password-recovery-store email)
                            [false (fh/form-problem (conj problems
                                                          {:keys [:email] :msg (str "A recovery email for " email " has already been sent.")}))]
                            ctx)))
                      [false (fh/form-problem problems)])))

  :handle-unprocessable-entity (fn [ctx] 
                                 (lr/ring-response (fh/flash-form-problem
                                                    (r/redirect (routes/absolute-path :sign-in))
                                                    ctx)))
  :post! (fn [ctx]
           (let [email (get-in ctx [:request :params :email-address])]
             (when-not (email-activation/email-and-update! password-recoverer email)
               (error-redirect ctx "The password recovery email failed to send")
               (log/error "The password recovery email failed to send"))))

  :post-redirect? (fn [ctx] 
                    (assoc ctx
                           ;; TODO check text - check 502 getwaway
                           :location (routes/absolute-path :email-confirmation))))

#_(lc/defresource reset-password [account-store password-recovery-store] 
  :allowed-methods [:post]
  :available-media-types content-types
  :processable? (fn [ctx]
                  (let [{:keys [password-recovery-id email]} (get-in ctx [:request :params])
                        {:keys [status data problems]}
                        (fh/validate-form (reset-password-page/reset-password-form email password-recovery-id)
                                          (ch/context->params ctx))]
                    (if-not (= :ok status)
                      [false (fh/form-problem problems)]
                      ctx)))

  :handle-unprocessable-entity (fn [ctx] 
                                 (let [{:keys [password-recovery-id email]} (get-in ctx [:request :params])]
                                   (lr/ring-response (fh/flash-form-problem
                                                      (r/redirect (routes/absolute-path :reset-password :email email :password-recovery-id password-recovery-id))
                                                      ctx))))
  :post! (fn [ctx]
           (let [data (-> ctx :request :params)
                 {:keys [new-password email]} data]
             ;; upadte with a new hashed password
             (account/update-password! account-store email new-password)
             ;; remove the password recovery data
             (pr/remove! password-recovery-store email)))

  :post-redirect? (fn [ctx] 
                    (assoc ctx
                           :location (routes/absolute-path :password-changed))))


