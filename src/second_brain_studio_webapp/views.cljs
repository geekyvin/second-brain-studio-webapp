(ns second-brain-studio-webapp.views
  (:require
   [re-frame.core :as re-frame]
   [second-brain-studio-webapp.subs :as subs]
   [second-brain-studio-webapp.note-view :as note-view]
   [second-brain-studio-webapp.line-chart :as line-chart]
   [second-brain-studio-webapp.sidebar :as sidebar]
   [second-brain-studio-webapp.editor :as editor]
   [second-brain-studio-webapp.ui-generator :as ui-generator]
   [second-brain-studio-webapp.left-pane :as left-pane]
   [second-brain-studio-webapp.cognito-auth :as auth]
   [second-brain-studio-webapp.markdown-editor :as markdown-editor]
   [second-brain-studio-webapp.events :as events]))

(defn left-panel []
  [:div.left-panel
   {:style {:width "250px"
            :background-color "#ffffff"
            :border-right "1px solid #e5e7eb"
            :height "100vh"
            :padding "16px"}}
   [auth/user-info-section]
   [left-pane/left-pane]])

(defn callback-page []
  (auth/handle-auth-callback)
  [:div.loading
   {:style {:display "flex"
            :justify-content "center"
            :align-items "center"
            :height "100vh"}}
   [:div
    {:style {:text-align "center"}}
    [:h2 "Processing login..."]
    [:p "Please wait while we complete your authentication."]]])

(defn main-panel []
  (let [path (.-pathname js/window.location)]
    (cond
      (= path "/callback")
      [callback-page]
      
      :else
      [:div.app-container
       {:style {:display "flex"}}
       [left-panel]
       [:div.main-content
        {:style {:flex 1
                 :padding "20px"}}
        [markdown-editor/markdown-editor]
        [ui-generator/ui-generator]]])))