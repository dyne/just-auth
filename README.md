# Just Auth - A simple and generic two factor authentication lib

[![Build Status](https://travis-ci.org/Commonfare-net/just-auth.svg?branch=master)](https://travis-ci.org/Commonfare-net/just-auth)
[![Clojars lib][clojars-logo]][clojars-url]
[![license][license-image]][license-url]

[clojars-url]: https://clojars.org/org.clojars.dyne/clj-storage
[clojars-logo]: https://clojars.org/images/clojars-logo.png
[license-url]: https://github.com/Commonfare-net/just-auth/blob/feature/start-without-email-config/LICENSE.txt
[license-image]: https://www.gnu.org/graphics/agplv3-155x51.png

This Clojure software is a simple two factor authentication library. It contains an `email` implementation and aims to be extended to more two factor implementations such as `sms`. It implements the main authentication functionalities like sign-up and sign-in as well as account activation, deactivation and password reset. Passwords and other sensitive information are encrypted and a bruteforce counter-attack mechanism is currently under development.For storage a mongo DB is used internally but an implementation of any other document DB could be added to the generic [storge lib](https://github.com/Commonfare-net/clj-storage) that has been used.

## How to use the library

The library gives you the choice to choose a) what is the second step? Email? Stub-email? b) what storage to use; Mongo? in-memory?

### Use the two factor email based authenticator

```clojure
(require '[just-auth.core :as auth-lib])
```

You can create an email authenticator backed up by mongodb simply by calling 

```clojure

(require '[clj-storage.test.db.test-db :as test-db])
(require '[just-auth.db.just-auth :as auth-db])

(let [_ (test-db/setup-db) ;; you can replace with any other mongodb
      ;; Here we choose to use a mongo db and we create the required stores
      stores-m (auth-db/create-auth-stores (test-db/get-test-db))
      ;; Please replace below the placeholders with the actual email server conf
      email-conf {:email-server "<server>"
                  :email-user "<user>"
                  :email-pass "<pass>"
                  :email-address "<email>"}
      ;; This configuration is needed to adjust the throttling behaviour of the lib. For more info refer to the throttling chapter bellow
      throttling-config {:criteria #{:email :ip-address} 
                         :type :block
                         :time-window-secs 3600
                         :threshold 1000}
      email-authenticator (auth-lib/email-based-authentication stores-m email-conf throttling-config)
      ;; sign-up
      account (auth-lib/sign-up email-authenticator
                                "Some name"
                                "email@mail.com"
                                "password"
                                {:activation-uri "http://host.org"}
                                ["nickname"])]
   ;; activate
   (auth-lib/activate-account email-authenticator
                              "email@mail.com"
                              {:activation-link (:activation-link account)}) 
  

   ;; sign-in
   (auth-lib/sign-in email-authenticator
                     "email@mail.com"
                     "password"))

   ;; The full protocol and calls available can be found at https://github.com/Commonfare-net/just-auth/blob/099be6a4b569bfe8b6f8629ae2383fe380e5c2bd/src/just_auth/core.clj#L42)

```

If you want to have more influence on the authenticator, like for example change the hashing functions the `new-email-based-authentication` constructor can be used like shown in this [test](https://github.com/Commonfare-net/just-auth/blob/8e7144e768e1e8315d6a4f531e426127d8d96c65/test/just_auth/test/core.clj#L42). 

### Just to try out (no email configuration needed, no actual emails will be sent)

The lib can be used without the actual email service for development. To do so please use

```clojure
;; One can use real stores or in memory ones like for example https://github.com/Commonfare-net/clj-storage/blob/b71bd9379a99a85c2c923c2d4b0b45163399a6f6/src/clj_storage/core.clj#L74)
;; Instead of a real mail server an atom is used.

(let [stores (auth-db/create-auth-stores (test-db/get-test-db))
             ;; For an in-memory DB please replace with call below
             #_(storage/create-in-memory-stores ["account-store" "password-recovery-store"])
      ;; here the emails are not sent but instead stored on an atom
      emails (atom [])]
    (auth-lib/new-stub-email-based-authentication stores emails))

```

#### Data encryption
For the data encryption the hash and checking functions can be passed as arguments like

```clojure
{:hash-fn clojure.lang.Fn
 :hash-check-fn clojure.lang.Fn}

```

otherwise it will default to the `derive` and `check` functions from the `buddy/buddy-hashers` [lib](https://funcool.github.io/buddy-hashers/latest/).

#### Error messaging

Instead of throwing exceptions the lib uses the monadic error utilities [failjure](https://github.com/adambard/failjure)

#### Throttling

This chapter explains the lib's mechanism to prevent brutforce attacks and similar attacks to the system. The way this works is that according to config any sign-in attempt will fail with an [Failjure](https://github.com/adambard/failjure) error message if it matches some criteria. To undesrstand the different options lets look into the config:

```clojure
{:criteria #{:email :ip-address}
 :type :block
 :time-window-secs 3600
 :threshold 1000}
```

Criteria takes a set which can contain :email, :ip-address, none or both. If :email is used then the lib will check for failed requests for every email, if :ip-address is used it will check for failed attempts to sign in per ip-address, if both are used it will check for combinations of emails and ip address and count those and if none is used it will just count ALL failed attempts.
The type can be either :block or :delay. The difference is that if block is used, if the criteria are met, a failure will be returned saying that the ip and/or email should be blocked. If :delay is chosen, the error message will contain the seconds for which the email and/or ip should be not let into the system.
The time window in seconds means that this is the time window for which failed attempts will be counted and threshold is the number of failed attempts that are needed to be reached for the sign-ins to be denied.

ATTENTION: the blocking should ideally be done on a higher level of this lib so a DDOs attack will not consume system resources. For this the failjure errors can be used to update blacklists or databases on a higher level.

### Run all tests

For the purpose we use Clojure's `midje` package, to be run with:

```
lein midje
```

### Run only the fast tests

Some of the tests are marked as slow. If you want to avoid running them you cn either

```
lein midje :filter -slow
```

or use the alias

```
lein test-basic
```

The just auth lib is Copyright (C) 2017-2018 by the Dyne.org Foundation, Amsterdam

The development is lead by Aspasia Beneti <aspra@dyne.org>

## Acknowledgements

The Social Wallet API is Free and Open Source research and development
activity funded by the European Commission in the context of
the
[Collective Awareness Platforms for Sustainability and Social Innovation (CAPSSI)](https://ec.europa.eu/digital-single-market/en/collective-awareness) program. Social
Wallet API uses the
underlying [Freecoin-lib](https://github.com/dyne/freecoin-lib)
blockchain implementation library and adopted as a component of the
social wallet toolkit being developed for
the [Commonfare project](https://pieproject.eu) (grant nr. 687922) .


## License

Social Wallet API is Copyright (C) 2017 by the Dyne.org Foundation

This software and its documentation are designed, written and maintained
by Denis Roio <jaromil@dyne.org> and Aspasia Beneti <aspra@dyne.org>

```
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
```
