(ns phoenix.modules.mailgun
  (:require [clj-http.client :as http]
            [clojure.string :as s]
            [cheshire.core :as json]
            [camel-snake-kebab.core :as csk]
            [com.stuartsierra.component :as c]))

(defprotocol IMailgunComponent
  (send-email! [_ {:keys [from to cc bcc subject headers text-body html-body]}]))

(defprotocol IMockMailgunComponent
  (get-messages [_]))

(defn format-account [{:keys [name email]}]
  (cond
    (and name email) (format "%s <%s>" name email)
    name name
    email email))

(defn format-accounts [accounts]
  (when (seq accounts)
    (->> accounts
         (map format-account)
         (s/join ","))))

(defn make-mailgun-api-req! [{:keys [api-key domain]} {:keys [form-params]}]
  (let [{:keys [status body]} (http/post (format "https://api.mailgun.net/v3/%s/messages" domain)
                                         {:basic-auth ["api" api-key]
                                          :form-params form-params
                                          :as :json
                                          :accept :json})]
    (if (= 200 status)
      body
      (throw (ex-info "Error sending e-mail" {:status status
                                              :resp body})))))

(defn format-headers [headers]
  (->> (for [[k v] headers]
         [(str "h:" (csk/->HTTP-Header-Case-String k)) v])
       (into {})))

(defn mail->form-params [{:keys [domain api-key default-from default-to test-mode?]}
                         {:keys [from to cc bcc subject headers text-body html-body]}]
  (-> {:from (format-account (or from default-from))
       :to (format-accounts (or to [default-to]))
       :subject subject
       :text text-body
       :html html-body}

      (merge (format-headers headers))

      (cond-> test-mode? (assoc :o:testmode "yes")
              (seq cc) (assoc :cc (format-accounts cc))
              (seq bcc) (assoc :bcc (format-accounts bcc)))))

(defrecord MailgunComponent []
  IMailgunComponent
  (send-email! [mailgun-opts mail]
    (make-mailgun-api-req! mailgun-opts {:form-params (mail->form-params mailgun-opts mail)})))

(defn make-mailgun-component [mailgun-opts]
  (map->MailgunComponent mailgun-opts))


(defrecord MockMailgunComponent []
  IMailgunComponent
  (send-email! [{:keys [::!messages]} mail]
    (swap! !messages conj mail))

  IMockMailgunComponent
  (get-messages [{:keys [::!messages]}]
    @!messages))

(defn make-mock-mailgun-component [mailgun-opts]
  (map->MockMailgunComponent (assoc mailgun-opts ::!messages (atom []))))
