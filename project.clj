(defproject org.clojars.dyne/just-auth "0.1.0-SNAPSHOT"
  :description "Simple two factor authentication library"
  :url "https://github.com/PIENews/just-auth"

  :license {:author "Dyne.org Foundation"
            :email "foundation@dyne.org"
            :year 2017
            :key "gpl-3.0"}

  :dependencies [[org.clojure/clojure "1.8.0"]

                 ;; logs
                 [com.taoensso/timbre "4.10.0"]

                 ;; TODO: probably should come from external project using the lib: hashers
                 [buddy/buddy-hashers "1.3.0"] 
                 
                 ;; fxc secret sharing protocol
                 [org.clojars.dyne/fxc "0.5.0"]

                 ;; storage lib
                 [org.clojars.dyne/clj-storage "0.5.0-SNAPSHOT"]

                 ;; config etc.
                 [org.clojars.dyne/auxiliary "0.4.0-SNAPSHOT"]

                 ;; Data validation
                 [prismatic/schema "1.1.7"]

                 ;; email
                 [com.draines/postal "2.0.2"]

                 ;;error handling
                 [failjure "1.2.0"]
                 ]

  :source-paths ["src"]
  :resource-paths ["resources" "test-resources"]
  :jvm-opts ["-Djava.security.egd=file:/dev/random"
             ;; use a proper random source (install haveged)

             "-XX:-OmitStackTraceInFastThrow"
             ;; prevent JVM exceptions without stack trace
             ]

  :env [

        ;; translation is configured here, strings are hard-coded at compile time
        ;; the last one acts as fallback if translated strings are not found
        [:auth-translation-language "lang/auth-en.yml"]
        [:auth-translation-fallback "lang/auth-en.yml"]]
  
  :aliases {"test-basic" ["midje" ":config" "test-resources/fast-tests.config"]}
  
  :profiles {:dev {:dependencies [[midje "1.9.0"]]
                   :repl-options {:init-ns just-auth.core}
                   :plugins [[lein-midje "3.1.3"]]}}

  ;; TODO: do we need this?
  :plugins [[lein-environ "1.0.0"]])
