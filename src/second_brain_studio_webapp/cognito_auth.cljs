(ns second-brain-studio-webapp.cognito-auth
  (:require
    [reagent.core :as r]
    [re-frame.core :as re-frame]
    ["amazon-cognito-identity-js" :as cognito]
    ["jwt-decode" :as jwt-decode]
    [second-brain-studio-webapp.events :as events]
    [second-brain-studio-webapp.subs :as subs]))

;; Constants for Cognito configuration
(def cognito-domain "https://us-east-1km00lsjbx.auth.us-east-1.amazoncognito.com")
(def client-id "75nerovj6gl86nrlm1iuoa5aui")
(def redirect-uri "http://localhost:8280/callback")
(def logout-uri "http://localhost:8280")

;; Forward declarations
(declare sign-in-redirect)

(defn fetch-user-info [access-token]
  (js/console.log "Fetching user info with token..." (if access-token "[PRESENT]" "[MISSING]"))
  (-> (js/fetch (str cognito-domain "/oauth2/userInfo")
                #js {:method "GET"
                     :headers #js {"Authorization" (str "Bearer " access-token)}})
      (.then (fn [^js response]
              (js/console.log "User info response status:" (.-status response))
              (if (.-ok response)
                (.json response)
                (throw (js/Error. "Failed to fetch user info")))))
      (.then (fn [^js data]
              (js/console.log "Complete user info response:" (js/JSON.stringify data))
              (js/console.log "Available fields:" (js/Object.keys data))
              (js/console.log "Name field:" (.-name data))
              (js/console.log "Given name field:" (.-given_name data))
              (js/console.log "Family name field:" (.-family_name data))
              (clj->js data)))))

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
           (-> (fetch-user-info token)
               (.then (fn [user-info]
                      (re-frame/dispatch 
                       [:set-user 
                        {:email (or (.-email user-info) (:email decoded) (:username decoded))
                         :name (or (.-name user-info) (:name decoded) (:username decoded))
                         :given_name (.-given_name user-info)
                         :family_name (.-family_name user-info)
                         :sub (:sub decoded)}]))))
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
  (let [scopes ["openid" "email" "profile"]
        encoded-scopes (js/encodeURIComponent (clojure.string/join " " scopes))
        encoded-redirect (js/encodeURIComponent redirect-uri)
        auth-url (str cognito-domain "/oauth2/authorize"
                     "?client_id=" client-id
                     "&response_type=code"
                     "&scope=" encoded-scopes
                     "&redirect_uri=" encoded-redirect)]
    (js/console.log "Auth scopes being requested:" scopes)
    (js/console.log "Encoded scopes:" encoded-scopes)
    (js/console.log "Complete auth URL:" auth-url)
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
                              
                              ;; Fetch user info before dispatching
                              (-> (fetch-user-info access-token)
                                  (.then (fn [user-info]
                                         (js/console.log "Dispatching user data with profile info...")
                                         (re-frame/dispatch 
                                          [:set-user 
                                           {:email (or (.-email user-info) 
                                                     (.-username decoded))
                                            :name (or (.-name user-info)
                                                     (.-given_name user-info)
                                                     (.-username decoded))
                                            :given_name (.-given_name user-info)
                                            :family_name (.-family_name user-info)
                                            :sub (:sub decoded)
                                            :tokens {:access_token access-token
                                                   :refresh_token refresh-token}}])
                                         
                                         (js/console.log "Navigating to home...")
                                         (js/history.replaceState nil "" "/")
                                         (js/setTimeout #(set! (.-href js/window.location) "/") 100)))
                                  (.catch (fn [error]
                                          (js/console.error "Error fetching user info:" error)
                                          ;; Fall back to token data if user info fetch fails
                                          (re-frame/dispatch 
                                           [:set-user 
                                            {:email (or (:email decoded) 
                                                      (:username decoded))
                                             :name (or (:name decoded) 
                                                     (:username decoded))
                                             :sub (:sub decoded)
                                             :tokens {:access_token access-token
                                                    :refresh_token refresh-token}}])
                                          (js/history.replaceState nil "" "/")
                                          (js/setTimeout #(set! (.-href js/window.location) "/") 100)))))
                            
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
          (-> (fetch-user-info token)
              (.then (fn [user-info]
                     (js/console.log "Fetched user info during check-auth:" (js/JSON.stringify user-info))
                     (let [decoded (parse-jwt token)]
                       (re-frame/dispatch 
                        [:set-user 
                         {:email (or (.-email user-info) (:email decoded) (:username decoded))
                          :name (or (.-name user-info) (:name decoded) (:username decoded))
                          :given_name (.-given_name user-info)
                          :family_name (.-family_name user-info)
                          :sub (:sub decoded)}])))))
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

;; Copy localStorage token to our app state and session storage if available
(defn sync-token-from-local-storage []
  (let [access-token (.getItem js/localStorage "access_token")]
    (when access-token
      (js/console.log "Found token in localStorage - copying to session storage and app-db")
      (try
        (let [token-preview (subs access-token 0 (min 15 (count access-token)))]
          (js/console.log "Token preview from localStorage: " token-preview "...")
          (.setItem js/sessionStorage "auth-token" access-token)
          (re-frame/dispatch [::events/set-auth-token access-token])
          (js/console.log "Token successfully synced from localStorage"))
        (catch js/Error e
          (js/console.error "Error syncing token from localStorage:" e))))))

(defn init-auth []
  (js/console.log "Initializing auth...")
  ;; First try to get token from localStorage
  (sync-token-from-local-storage)
  ;; Then check auth state
  (check-auth)
  ;; Debug token after initialization
  (js/setTimeout debug-log-token 200))

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
    (js/console.log "Current user data:" (clj->js user))
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
                  :justify-content "center"}}
         [:span.name
          {:style {:font-weight "600"
                   :color "#111827"
                   :font-size "1.1rem"}}
          (or (:given_name user) (:name user) "Guest")]]]
       [:div.not-signed-in
        {:style {:padding "12px"
                 :color "#6b7280"
                 :font-family "'atkinson-hyper', 'dm-sans'"}}
        "Not signed in"])
     [sign-out-button]]))

;; Function to check and log token availability everywhere
(defn debug-log-token []
  (js/console.log "========================")
  (js/console.log "TOKEN DEBUG INFORMATION")
  (js/console.log "========================")
  
  ;; Check localStorage
  (let [local-token (.getItem js/localStorage "access_token")]
    (js/console.log "localStorage access_token:" (if local-token "[PRESENT]" "[MISSING]"))
    (when local-token
      (js/console.log "localStorage token preview:" (subs local-token 0 (min 15 (count local-token))) "...")))
  
  ;; Check sessionStorage
  (let [session-token (.getItem js/sessionStorage "auth-token")]
    (js/console.log "sessionStorage auth-token:" (if session-token "[PRESENT]" "[MISSING]"))
    (when session-token
      (js/console.log "sessionStorage token preview:" (subs session-token 0 (min 15 (count session-token))) "...")))
  
  ;; Check token in app-db
  (let [auth-token @(re-frame/subscribe [::subs/auth-token])]
    (js/console.log "app-db auth token:" (if auth-token "[PRESENT]" "[MISSING]"))
    (when auth-token
      (js/console.log "app-db token preview:" (subs auth-token 0 (min 15 (count auth-token))) "...")))
  
  (js/console.log "========================"))

;; Call this function on initialization and after token storage
(defn store-token-in-storage [token]
  (when token
    (try
      (let [token-preview (if (> (count token) 10)
                           (str (subs token 0 10) "...")
                           token)]
        (.setItem js/sessionStorage "auth-token" token)
        (js/console.log "Auth token stored in session storage:" token-preview)
        (re-frame/dispatch [::events/set-auth-token token])
        (js/console.log "Token dispatched to app-db")
        (js/setTimeout debug-log-token 100)) ;; Log token info after storage
      (catch js/Error e
        (js/console.error "Failed to store token in session storage:" e)))))

;; Get token from storage on app load
(defn get-token-from-storage []
  (try
    (let [token (.getItem js/sessionStorage "auth-token")]
      (when token
        (js/console.log "Retrieved token from session storage")
        (re-frame/dispatch [::events/set-auth-token token])
        token))
    (catch js/Error e
      (js/console.error "Failed to get token from session storage:" e)
      nil)))

;; Remove token from storage on logout
(defn remove-token-from-storage []
  (try
    (.removeItem js/sessionStorage "auth-token")
    (js/console.log "Token removed from session storage")
    (catch js/Error e
      (js/console.error "Failed to remove token from session storage:" e))))

;; Extract token from Cognito user session - separate function
(defn extract-token-from-user [cognito-user]
  (try
    (let [session (.-signInUserSession cognito-user)
          tokens (when session (.-accessToken session))
          jwt-token (when tokens (.-jwtToken tokens))]
      (when jwt-token
        (js/console.log "Successfully extracted token from Cognito user session")
        jwt-token))
    (catch js/Error e
      (js/console.error "Error extracting token from user session:" e)
      nil)))

;; Authenticate user with Cognito
(defn authenticate-user [username password]
  (reset! sign-in-error nil)
  (let [auth-data (cognito/AuthenticationDetails. #js {:Username username :Password password})
        user-data #js {:Username username :Pool user-pool}
        cognito-user (cognito/CognitoUser. user-data)]
    (.authenticateUser cognito-user
                       auth-data
                       #js {:onSuccess (fn [result]
                                         (js/console.log "Sign in success" result)
                                         (let [token (extract-token-from-user cognito-user)]
                                           (when token
                                             (store-token-in-storage token)))
                                         (re-frame/dispatch [::events/set-user-signed-in true])
                                         (if (and redirect-url (not= redirect-url "/"))
                                           (set! (.-location js/window) redirect-url)
                                           (set! (.-location js/window) "/")))
                            :onFailure (fn [err]
                                         (js/console.error "Sign in failure" err)
                                         (reset! sign-in-error (.-message err)))})))

;; Re-frame event handler for sign out
(re-frame/reg-fx
 :sign-out-fx
 (fn []
   (js/console.log "Signing out via Cognito...")
   ;; Clear tokens from storage
   (remove-token-from-storage)
   ;; Redirect to Cognito logout URL
   (let [encoded-logout-uri (js/encodeURIComponent logout-uri)
         logout-url (str cognito-domain "/logout"
                        "?client_id=" client-id
                        "&logout_uri=" encoded-logout-uri
                        "&response_type=code"
                        "&redirect_uri=" (js/encodeURIComponent redirect-uri))]
     (js/console.log "Redirecting to logout:" logout-url)
     (set! (.-location js/window) logout-url))))