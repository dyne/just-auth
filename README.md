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

### Use the two factor email based authenticator

```clojure
(require '[just-auth.core :as auth-lib])
```

You can create an email authenticator simply by calling 

```clojure

;; where create-stores is a function that creates two document-like collections with the respective names.
;; An example can be found https://github.com/Commonfare-net/clj-storage/blob/b71bd9379a99a85c2c923c2d4b0b45163399a6f6/src/clj_storage/db/mongo.clj#L88)
(let [stores-m (storage/create-stores ["account-store" "password-recovery-store"])
      email-conf {:email-server "server"
                  :email-user "user"
                  :email-pass "pass"
                  :email-address "email"}
      authenticator (email-based-authentication stores  email-config)
      ;; sign-up
      account (auth-lib/sign-up email-authenticator
                                "Some name"
                                "email@mail.com"
                                "password"
                                {:activation-uri "http://host.org"}
                                ["nickname"])]
   ;; activate
   (auth-lib/activate-account email-authenticator
                              {:activation-link (:activation-link account)})
   ;; sign-in
   (auth-lib/sign-in email-authenticator
                     "email@mail.com"
                     "password"))

   ;; The full protocol can be found at https://github.com/Commonfare-net/just-auth/blob/099be6a4b569bfe8b6f8629ae2383fe380e5c2bd/src/just_auth/core.clj#L42)

```

If you want to have more influence on the authenticator, like for example change the hashing functions the `new-email-based-authentication` constructor can be used like shown in this [test](https://github.com/Commonfare-net/just-auth/blob/8e7144e768e1e8315d6a4f531e426127d8d96c65/test/just_auth/test/core.clj#L42). 

#### Data encryption
For the data encryption the hash and checking functions can be passed as arguments like

```clojure
{:hash-fn clojure.lang.Fn
 :hash-check-fn clojure.lang.Fn}

```

otherwise it will default to the `derive` and `check` functions from the `buddy/buddy-hashers` [lib](https://funcool.github.io/buddy-hashers/latest/).

#### Error messaging

Instead of throwing exceptions the lib uses the monadic error utilities [failjure](https://github.com/adambard/failjure)

### Just to try out (no configuration needed)

The lib can be used without the actual email service for development. To do so please use

```clojure
;; One can use real stores or in memory ones like for example https://github.com/Commonfare-net/clj-storage/blob/b71bd9379a99a85c2c923c2d4b0b45163399a6f6/src/clj_storage/core.clj#L74)
;; Instead of a real mail server an atom is used.

(let [stores (storage/create-in-memory-stores ["account-store" "password-recovery-store"])
      emails (atom [])]
    (new-stub-email-based-authentication stores emails))

```

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