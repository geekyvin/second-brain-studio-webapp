(ns second-brain-studio-webapp.login
  (:require
   [reagent.core :as r]
   [clojure.string :as str]
   [re-frame.core :as re-frame]
   [second-brain-studio-webapp.events :as events]))

(defn brain-logo []
  [:div.logo-container
   [:img {:src "../images/brain-logo.png"
          :alt "Brain Logo"
          :style {:width "64px"
                 :height "64px"}}]])

(defn input-field [{:keys [id type placeholder value on-change icon]}]
  [:div.input-container
   [:div.input-wrapper
    [:span.input-icon icon]
    [:input {:id id
             :type type
             :placeholder placeholder
             :value @value
             :on-change #(reset! value (.. % -target -value))
             :class "input-field"}]]])

(defn login-page []
  (let [email (r/atom "")
        password (r/atom "")
        error (r/atom nil)
        loading? (r/atom false)]
    (fn []
      [:div.login-container
       [brain-logo]
       
       [:div.card
        ;; Card Header
        [:div.card-header
         [:h1.card-title "Create an account"]
         [:p.card-description "Enter your information to create an account"]]
        
        ;; Card Content
        [:div.card-content
         ;; Error Alert
         (when @error
           [:div.error-alert
            [:p @error]])
         
         ;; Login Form
         [:form.login-form
          {:on-submit (fn [e]
                       (.preventDefault e)
                       (when (and (not (str/blank? @email))
                                (not (str/blank? @password)))
                         (reset! loading? true)
                         ;; Add your login logic here
                         (-> (js/fetch "http://localhost:3000/login"
                                     #js {:method "POST"
                                          :headers #js {"Content-Type" "application/json"}
                                          :body (js/JSON.stringify 
                                                #js {:email @email
                                                     :password @password})})
                             (.then (fn [response]
                                     (if (.-ok response)
                                       (.json response)
                                       (throw (js/Error. "Login failed")))))
                             (.then (fn [data]
                                     (reset! loading? false)
                                     (re-frame/dispatch [::events/login-success data])))
                             (.catch (fn [error]
                                     (reset! loading? false)
                                     (reset! error "Invalid email or password"))))))}
          
          ;; Full Name Field
          [:div.form-group
           [:label.form-label "Full Name"]
           [input-field
            {:id "fullname"
             :type "text"
             :placeholder "John Doe"
             :value email
             :icon "👤"}]]
          
          ;; Email Field
          [:div.form-group
           [:label.form-label "Email"]
           [input-field
            {:id "email"
             :type "email"
             :placeholder "name@example.com"
             :value email
             :icon "✉️"}]]
          
          ;; Password Field
          [:div.form-group
           [:label.form-label "Password"]
           [input-field
            {:id "password"
             :type "password"
             :placeholder "••••••••"
             :value password
             :icon "🔒"}]]
          
          ;; Submit Button
          [:button.submit-button
           {:type "submit"
            :disabled @loading?}
           (if @loading?
             "Signing in..."
             "Sign in")]
          
          [:div.sign-in-link
           "Don't have an account? "
           [:a {:href "/signup"} "Sign up"]]]]]])))