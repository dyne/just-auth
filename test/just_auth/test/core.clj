(ns just-auth.test.core
  (:require [midje.sweet :refer :all]
            [just-auth
             [core  :as auth-lib]
             [schema :as schema]
             [messaging :as m]]
            [just-auth.db
             [storage :as storage]]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [buddy.hashers :as hashers]))

(facts "Create a new email based authentication record and validate schemas"

       (fact "In memory stores"
             (let [stores-m (storage/create-in-memory-stores ["account-store" "password-recovery-store"])
                   hash-fns {:hash-fn hashers/derive
                             :hash-check-fn hashers/check}]

               (s/validate schema/AuthStores stores-m) => truthy

               (s/validate schema/HashFns hash-fns) => truthy

               (auth-lib/new-email-based-authentication stores-m
                                                        (m/new-stub-account-activator stores-m)
                                                        (m/new-stub-password-recoverer stores-m)
                                                        hash-fns)
               => truthy)))
