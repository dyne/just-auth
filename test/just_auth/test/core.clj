(ns just-auth.test.core
  (:require [midje.sweet :refer :all]
            [just-auth
             [core  :as auth-lib]
             [schema :as schema]
             [messaging :as m]]
            [clj-storage.core :as storage]
            [schema.core :as s]
            [taoensso.timbre :as log]
            [buddy.hashers :as hashers]))

(fact "Create a new email based authentication record and validate schemas"
      (let [stores-m (storage/create-in-memory-stores ["account-store" "password-recovery-store"])
            hash-fns {:hash-fn hashers/derive
                      :hash-check-fn hashers/check}
            email-authentication (auth-lib/new-email-based-authentication stores-m
                                                 (m/new-stub-account-activator stores-m)
                                                 (m/new-stub-password-recoverer stores-m)
                                                 hash-fns)]

        (s/validate schema/AuthStores stores-m) => truthy

        (s/validate schema/HashFns hash-fns) => truthy

        (fact "Sign up a user and check that email has been sent"
              (auth-lib/sign-up email-authentication
                                "Some name"
                                "some@mail.com"
                                "12345678"
                                {:activation-uri "http://test.com"}
                                ["nickname"]))))
