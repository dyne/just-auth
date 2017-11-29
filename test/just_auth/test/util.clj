(ns just-auth.test.util
  (:require [midje.sweet :refer :all]
            [just-auth.util :as u]))

(fact "check the link construction"
      (u/construct-link {:uri "locahost:8000"
                         :email "me@mail.com"
                         :token "some-token"
                         :action "activate"}) => "http://locahost:8000/activate/me@mail.com/some-token"

      (u/construct-link {:uri "https://www.dyne.org/"
                         :email "me@mail.com"
                         :token "some-token"
                         :action "reset-password"}) => "https://www.dyne.org/reset-password/me@mail.com/some-token")
