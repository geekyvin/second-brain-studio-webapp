(ns second-brain-studio-webapp.cognito-auth
  (:require
    [reagent.core :as r]
    [re-frame.core :as re-frame]
    ["amazon-cognito-identity-js" :as cognito]
    ["jwt-decode" :as jwt-decode]))

;; Constants for Cognito configuration
(def cognito-domain "https://us-east-1km00lsjbx.auth.us-east-1.amazoncognito.com")
(def client-id "75nerovj6gl86nrlm1iuoa5aui")
(def redirect-uri "http://localhost:8280/callback")
(def logout-uri "http://localhost:8280")

;; Forward declarations
(declare sign-in-redirect)

;; Re-frame events and effects
(re-frame/reg-event-db
 :set-user
 (fn [db [_ user]]
   (assoc db :user user)))

(re-frame/reg-event-fx
 :check-auth
 (fn [{:keys [db]} _]
   {:db db
    :fx [[:check-auth-fx]]}))

(defn parse-jwt [token]
  (try
    (when token
      (js/console.log "Parsing JWT token...")
      (let [payload (second (clojure.string/split token #"\."))
            decoded (js->clj (js/JSON.parse (js/atob payload)) :keywordize-keys true)]
        (js/console.log "Decoded token payload:" (js/JSON.stringify (clj->js decoded)))
        decoded))
    (catch :default e
      (js/console.error "Error parsing JWT:" e)
      nil)))

(defn check-token-validity [token]
  (when token
    (try
      (let [decoded (parse-jwt token)
            now (/ (.now js/Date) 1000)  ; Convert to seconds
            exp (:exp decoded)]
        
        (js/console.log "Token validation:")
        (js/console.log "- Current time (seconds):" now)
        (js/console.log "- Token expiration (seconds):" exp)
        (js/console.log "- Token issuer:" (:iss decoded))
        (js/console.log "- Token audience:" (:aud decoded))
        (js/console.log "- Token type:" (:token_use decoded))
        
        (and decoded
             exp
             (> exp now)
             (= (:iss decoded) "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_kM00lsJBx")
             (or (= (:aud decoded) client-id)
                 (= (:client_id decoded) client-id))))
      (catch :default e
        (js/console.error "Error validating token:" e)
        false))))

(re-frame/reg-fx
 :check-auth-fx
 (fn []
   (let [token (.getItem js/localStorage "access_token")]
     (if token
       (let [decoded (parse-jwt token)]
         (if (check-token-validity token)
           (re-frame/dispatch [:set-user {:email (or (:email decoded) (:username decoded))
                                        :name (or (:name decoded) (:username decoded))
                                        :sub (:sub decoded)}])
           (do
             (.removeItem js/localStorage "access_token")
             (.removeItem js/localStorage "refresh_token")
             (sign-in-redirect))))
       (sign-in-redirect)))))

(re-frame/reg-sub
 :user
 (fn [db]
   (:user db)))

(defn sign-in-redirect []
  (let [encoded-redirect (js/encodeURIComponent redirect-uri)
        auth-url (str cognito-domain "/oauth2/authorize"
                     "?client_id=" client-id
                     "&response_type=code"
                     "&scope=email"
                     "&redirect_uri=" encoded-redirect)]
    (js/console.log "Redirecting to login:" auth-url)
    (set! (.-location js/window) auth-url)))

(defn sign-out []
  (js/console.log "Signing out...")
  (.removeItem js/localStorage "access_token")
  (.removeItem js/localStorage "refresh_token")
  (re-frame/dispatch [:set-user nil])
  (let [encoded-logout-uri (js/encodeURIComponent logout-uri)
        logout-url (str cognito-domain "/logout"
                       "?client_id=" client-id
                       "&logout_uri=" encoded-logout-uri
                       "&response_type=code"
                       "&redirect_uri=" (js/encodeURIComponent redirect-uri))]
    (js/console.log "Redirecting to logout:" logout-url)
    (set! (.-location js/window) logout-url)))

(defn handle-auth-callback []
  (let [url (.-href js/window.location)
        url-obj (js/URL. url)
        code (.get (.-searchParams url-obj) "code")
        error (.get (.-searchParams url-obj) "error")
        error-description (.get (.-searchParams url-obj) "error_description")]
    
    (js/console.log "Checking auth state - Current path:" (.-pathname url-obj))
    
    (cond
      code
      (do
        (js/console.log "Processing auth code...")
        (let [params (js/URLSearchParams.)]
          (.append params "grant_type" "authorization_code")
          (.append params "client_id" client-id)
          (.append params "code" code)
          (.append params "redirect_uri" redirect-uri)
          
          (-> (js/fetch (str cognito-domain "/oauth2/token")
                       #js {:method "POST"
                            :headers #js {"Content-Type" "application/x-www-form-urlencoded"}
                            :mode "cors"
                            :body (.toString params)})
              (.then (fn [^js response]
                      (js/console.log "Token exchange response status:" (.-status response))
                      (if (.-ok response)
                        (.json response)
                        (-> (.text response)
                            (.then (fn [error-text]
                                   (js/console.error "Token exchange failed:" error-text)
                                   (throw (js/Error. (str "Token exchange failed: " error-text)))))))))
              (.then (fn [^js data]
                      (js/console.log "Token exchange successful")
                      (let [access-token (.-access_token data)
                            refresh-token (.-refresh_token data)]
                        
                        (js/console.log "Validating access token...")
                        (let [decoded (parse-jwt access-token)]
                          (if (check-token-validity access-token)
                            (do
                              (js/console.log "Token validation successful, storing tokens...")
                              (.setItem js/localStorage "access_token" access-token)
                              (when refresh-token
                                (.setItem js/localStorage "refresh_token" refresh-token))
                              
                              (js/console.log "Dispatching user data...")
                              (re-frame/dispatch [:set-user {:email (or (:email decoded) (:username decoded))
                                                           :name (or (:name decoded) (:username decoded))
                                                           :sub (:sub decoded)
                                                           :tokens {:access_token access-token
                                                                  :refresh_token refresh-token}}])
                              
                              (js/console.log "Navigating to home...")
                              (js/history.replaceState nil "" "/")
                              (js/setTimeout #(set! (.-href js/window.location) "/") 100))
                            
                            (do
                              (js/console.error "Received invalid token from exchange")
                              (js/alert "Authentication failed: Received invalid token")
                              (js/history.replaceState nil "" "/")))))))
              (.catch (fn [error]
                       (js/console.error "Network or parsing error during token exchange:" error)
                       (js/alert (str "Authentication failed: " (.-message error)))
                       (js/history.replaceState nil "" "/"))))))

      error
      (do
        (js/console.error "Auth error:" error)
        (js/console.error "Error description:" (js/decodeURIComponent error-description))
        (js/alert (str "Authentication error: " (js/decodeURIComponent error-description)))
        (js/history.replaceState nil "" "/"))

      (= (.-pathname url-obj) "/callback")
      (do
        (js/console.log "On callback path but no code/error - redirecting to login")
        (sign-in-redirect))

      :else
      (let [token (.getItem js/localStorage "access_token")]
        (when (or (not token)
                  (not (check-token-validity token)))
          (js/console.log "No valid token - redirecting to login")
          (sign-in-redirect))))))

(defn check-auth []
  (let [token (.getItem js/localStorage "access_token")]
    (js/console.log "Checking auth status...")
    (js/console.log "Raw token from localStorage:" (if token "[PRESENT]" "[MISSING]"))
    (if token
      (let [is-valid (check-token-validity token)]
        (js/console.log "Token validity check result:" is-valid)
        (if is-valid
          (let [decoded (parse-jwt token)]
            (js/console.log "Token is valid, setting user...")
            (re-frame/dispatch [:set-user {:email (or (:email decoded) (:username decoded))
                                         :name (or (:name decoded) (:username decoded))
                                         :sub (:sub decoded)}]))
          (do
            (js/console.log "Token is invalid or expired, clearing...")
            (.removeItem js/localStorage "access_token")
            (.removeItem js/localStorage "refresh_token")
            (when-not (= (.-pathname js/window.location) "/callback")
              (sign-in-redirect)))))
      (do
        (js/console.log "No token found, redirecting to login...")
        (when-not (= (.-pathname js/window.location) "/callback")
          (sign-in-redirect))))))

(defn init-auth []
  (js/console.log "Initializing auth...")
  (check-auth))

(defn sign-out-button []
  [:button.signout-button
   {:on-click sign-out
    :style {:padding "8px 16px"
            :border "none"
            :background "#cc6633"
            :color "white"
            :cursor "pointer"
            :font-family "'atkinson-hyper', 'dm-sans'"
            :font-size "14px"
            :border-radius "4px"
            :margin "8px"
            :transition "background-color 0.2s"
            :hover {:background-color "#b55522"}}}
   "Sign out"])

(defn user-info-section []
  (let [user @(re-frame/subscribe [:user])]
    [:div.user-info
     {:style {:margin-bottom "16px"}}
     (if user
       [:div.user-email
        {:style {:padding "12px"
                 :border-bottom "1px solid #e5e7eb"
                 :font-family "'atkinson-hyper', 'dm-sans'"
                 :display "flex"
                 :align-items "center"
                 :gap "8px"
                 :background-color "#f9fafb"}}
        [:span.user-icon "👤"]
        [:div.user-details
         {:style {:display "flex"
                  :flex-direction "column"
                  :gap "2px"}}
         [:span.name
          {:style {:font-weight "500"
                   :color "#111827"}}
          (:name user)]
         [:span.given-name
          {:style {:font-size "0.875rem"
                   :color "#6b7280"}}
          (:given_name user)]
         [:span.email
          {:style {:font-size "0.875rem"
                   :color "#6b7280"}}
          (:email user)]]]
       [:div.not-signed-in
        {:style {:padding "12px"
                 :color "#6b7280"
                 :font-family "'atkinson-hyper', 'dm-sans'"}}
        "Not signed in"])
     [sign-out-button]]))