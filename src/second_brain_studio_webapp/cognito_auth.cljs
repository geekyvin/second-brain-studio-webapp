;; filepath: /Users/vinoth/Documents/workspace/second-brain-studio-webapp/src/second_brain_studio_webapp/cognito_auth.cljs
(ns second-brain-studio-webapp.cognito-auth
  (:require
    [reagent.core :as r]
    ["react" :refer [createElement]]
    ["react-oidc-context" :refer [AuthProvider useAuth]]))

(def cognito-config
  #js {:authority "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_zIyIuTvzY",
       :client_id "3hqle652223aot9csu1pqqpr5j"
       :redirect_uri "https://secondbrainstudio.com/callback"
       :response_type "code"
       :scope "openid email"})

(defn auth-provider [child]
    ;; Use createElement to produce the AuthProvider element with props (cognito-config)
  (createElement AuthProvider cognito-config (r/as-element child)))

;; Redirect the browser to the Cognito logout endpoint.
(defn sign-out-redirect []
  (let [clientId "3hqle652223aot9csu1pqqpr5j"
        cognitoDomain "https://us-east-1ziyiutvzy.auth.us-east-1.amazoncognito.com"
        ;; Note: logoutUri should be an allowed URL in your Cognito settings.
        logoutUri (str cognitoDomain "/logout?client_id=" clientId
                       "&logout_uri=https://secondbrainstudio.com/home")]
    (set! (.-location js/window)
          (str cognitoDomain "/logout?client_id=" clientId "&logout_uri="
               (js/encodeURIComponent logoutUri)))))

;; Redirect the browser to the Cognito signup endpoint.
(defn sign-up-redirect []
  (let [clientId "3hqle652223aot9csu1pqqpr5j"
        cognitoDomain "https://us-east-1ziyiutvzy.auth.us-east-1.amazoncognito.com"
        redirectUri "https://secondbrainstudio.com/callback"
        scope "openid+email+phone" ;; Adjust if needed
        final-url (str cognitoDomain
                       "/signup"
                       "?client_id=" clientId
                       "&redirect_uri=" (js/encodeURIComponent redirectUri)
                       "&response_type=code"
                       "&scope=" scope)]
    (println "final-url:" final-url)
    (set! (.-location js/window) final-url)))


;; 1) A pure React sign-in component that uses the OIDC context.
(defn ^:private sign-in-react []
  (let [auth (useAuth)]
    (createElement "button"
      #js {:onClick #(.signinRedirect auth)}
      "Sign in")))

;; 2) Adapt the React component to a Reagent component.
(def sign-in-btn
  (r/adapt-react-class sign-in-react))

;; Reagent component for signing out.
(defn sign-out-btn []
  (let [auth (useAuth)]
    (fn []
      [:button {:on-click #(sign-out-redirect)}
       "Sign out"])))

;; Reagent component for signing up.
(defn sign-up-btn []
  (fn []
    [:button {:on-click #(sign-up-redirect)}
     "Sign up"]))