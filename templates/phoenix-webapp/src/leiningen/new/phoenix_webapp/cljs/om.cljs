(ns {{name}}.ui.app
  (:require [clojure.string :as s]
            [om.core :as om]
            [om.dom :as dom]
            simple-brepl.client))

(enable-console-print!)

(defn hello-world []
  (om/component
   (dom/p nil "Hello world!")))

(set! (.-onload js/window)
      (fn []
        (om/root hello-world {} {:target js/document.body})))

;; ------------------------------------------------------------

;; Below this line is only required for the Phoenix welcome page, feel
;; free to just delete all of it when you want to get cracking on your
;; own project!

(defn code [s]
  (dom/strong #js {:style #js {:font-family "'Courier New', 'monospace'"}}
              s))

(defn demo-component []
  (om/component
   (dom/div #js {:className "container"}

            (dom/h2 #js {:style #js {:margin-top "1em"}}
                    "Hello from Phoenix!")

            (dom/h3 nil "Things to try:")

            (dom/ul nil
                    (dom/li nil (dom/p nil "In your Clojure REPL, run " (code "(phoenix/reload!)") " to completely reload the webapp without restarting the JVM."))
                    (dom/li nil (dom/p nil "Connect to a CLJS bREPL by running " (code "(simple-brepl)")))
                    (dom/li nil
                            (dom/p nil "Once you've opened the bREPL, reload your browser to make the connection, then you can eval some CLJS.")
                            (dom/p nil "I recommend:")

                            (dom/ul nil
                                    (dom/li nil (code "(+ 1 1)"))
                                    (dom/li nil (code "(js/alert \"Hello world!\")"))
                                    (dom/li nil (code "(set! (.-backgroundColor js/document.body.style) \"green\")")))

                            (dom/p nil "Run " (code ":cljs/quit") " to get back to a Clojure REPL."))

                    (dom/li nil
                            (dom/p nil "Start making your webapp!")
                            (dom/ul nil
                                    (dom/li nil (dom/p nil "The CLJS entry point is in " (code "ui-src/{{sanitized}}/ui/app.cljs")))
                                    (dom/li nil (dom/p nil "The Clojure Ring handler is in " (code "src/{{sanitized}}/service/handler.clj")))))

                    (dom/li nil (dom/p nil "Any trouble, let me know - either through GitHub or on Twitter at " (dom/a #js {:href "https://twitter.com/jarohen"} "@jarohen")))

                    (dom/li nil (dom/p nil "Good luck!")))

            (dom/div #js {:style #js {:text-align "right"
                                      :font-weight "bold"}}
                     (dom/p nil
                            (dom/span #js {:style #js {:font-size "1.3em"}} "James Henderson")
                            (dom/br nil)
                            "Twitter: " (dom/a #js {:href "https://twitter.com/jarohen"} "@jarohen")
                            (dom/br nil)
                            "GitHub: " (dom/a #js {:href "https://github.com/james-henderson"} "james-henderson"))))))

(set! (.-onload js/window)
      (fn []
        (om/root demo-component {} {:target js/document.body})))
