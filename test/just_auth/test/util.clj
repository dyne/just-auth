(ns just-auth.test.util
  (:require [midje.sweet :refer :all]
            [just-auth.util :as u]))

(fact "check the link construction"
      (u/construct-link {:uri "locahost:8000"
                         :email "me@mail.com"
                         :token "come-token"}) => "http://locahost:8000/me@mail.com/token"

      (u/construct-link {:uri "https://www.dyne.org/"
                         :email "me@mail.com"
                         :token "come-token"}) => "https://www.dyne.org//me@mail.com/token")
