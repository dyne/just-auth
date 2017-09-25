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
            [schema.core :as s]))

(defprotocol Authentication
  ;; About names http://www.kalzumeus.com/2010/06/17/falsehoods-programmers-believe-about-name
  (sign-up [this name email password & other-names])

  (sign-in [this email password])

  (activate-account [this email activation-id password])

  (de-activate-account [this email password])

  (reset-password [this email old-password new-password]))

;; TODO: We could use something like https://github.com/adambard/failjure for error handling
(s/defrecord EmailBasedAuthentication
    [account-store :-  AuthStore
     account-activator :- Email
     hash-fns :- HashFns
     activation-link :- s/Str ;; TODO: URI?
     ]
  Authentication
  (sign-up [this name email password & other-names]
    (if (account/fetch account-store email)
      {:error :already-exists}
      (do (account/new-account! account-store
                                (cond-> {:name name
                                         :email email
                                         :password password}
                                  other-names (assoc :other-names other-names))
                                hash-fns)
          (when-not (m/email-and-update! account-activator email activation-link)
            {:error :email}))))
  
  (sign-in [this email password]
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

  )




#_(lc/defresource create-account [account-store email-activator]
  :allowed-methods [:post]
  :available-media-types content-types

  :known-content-type? #(check-content-type % content-types)

  :processable? (fn [ctx]
                  (let [{:keys [status data problems]}
                        (fh/validate-form sign-in-page/sign-up-form
                                          (ch/context->params ctx))]
                    (if (= :ok status)
                      (let [email (-> ctx :request :params :email)]
                        (if (account/fetch account-store email)
                          [false (fh/form-problem (conj problems
                                                        {:keys [:email] :msg (str "An account with email " email
                                                                                     " already exists.")}))]
                          ctx))
                      [false (fh/form-problem problems)])))

  :handle-unprocessable-entity (fn [ctx] 
                                 (lr/ring-response (fh/flash-form-problem
                                                    (r/redirect (routes/absolute-path :sign-in))
                                                    ctx)))
  :post! (fn [ctx]
           (let [data (-> ctx :request :params)
                 email (get data :email)]
             (if (account/new-account! account-store (select-keys data [:first-name :last-name :email :password]))
               (when-not (email-activation/email-and-update! email-activator email) 
                 (error-redirect ctx "The activation email failed to send "))
               (log/error "Something went wrong when creating a user in the DB"))))

  :post-redirect? (fn [ctx] 
                    (assoc ctx
                           :location (routes/absolute-path :email-confirmation))))



#_(lc/defresource activate-account [account-store]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok (fn [ctx]
               (let [activation-code (get-in ctx [:request :params :activation-id])
                     email (get-in ctx [:request :params :email])]
                 (if-let [account (account/fetch-by-activation-id account-store activation-code)]
                   (if (= (:email account) email)
                     (do (account/activate! account-store (:email account))
                         (-> (routes/absolute-path :account-activated)
                             r/redirect 
                             lr/ring-response))
                     (error-redirect ctx "The email and activation id do not match"))
                   (error-redirect ctx "The activation id could not be found")))))

#_(lc/defresource resend-activation-email [account-store email-activator]
  :allowed-methods [:post]
  :available-media-types content-types
  :known-content-type? #(check-content-type % content-types)
  :processable? (fn [ctx]
                  (let [{:keys [status data problems]}
                        (fh/validate-form sign-in-page/resend-activation-form
                                          (ch/context->params ctx))]
                    (if (= :ok status)
                      (let [email (-> ctx :request :params :activation-email)]
                        (if-not (account/fetch account-store email)
                          [false (fh/form-problem (conj problems
                                                        {:keys [:email] :msg (str "The email " email " is not registered yet. Please sign up first")}))]
                          ctx))
                      [false (fh/form-problem problems)])))

  :handle-unprocessable-entity (fn [ctx] 
                                 (lr/ring-response (fh/flash-form-problem
                                                    (r/redirect (routes/absolute-path :sign-in))
                                                    ctx)))
  :post! (fn [ctx]
           (let [email (get-in ctx [:request :params :activation-email])
                 account (account/fetch account-store email)]
             (when-not (email-activation/email-and-update! email-activator email)
               (error-redirect ctx "The activation email failed to send")
               (log/error "The activation email failed to send"))))

  :post-redirect? (fn [ctx] 
                    (assoc ctx
                           :location (routes/absolute-path :email-confirmation))))

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


