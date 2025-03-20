(ns second-brain-studio-webapp.views
  (:require
   [re-frame.core :as re-frame]
   [reagent.core :as r]
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
  (let [collapsed? (r/atom false)
        active-item (r/atom "Work")]
    (fn []
      [:div.sidebar {:class (when @collapsed? "collapsed")}
       [:div.sidebar-header
        [:div.logo-container
         (if @collapsed?
           [:img.logo {:src "/images/sbs_logo.png" :alt "Second Brain Studio Logo"}]
           [:img.logo-banner {:src "/images/logo_banner.png" :alt "Second Brain Studio"}])]
        
        [:button.collapse-toggle
         {:on-click #(swap! collapsed? not)}
         [:svg.icon {:xmlns "http://www.w3.org/2000/svg" :width "16" :height "16" :viewBox "0 0 24 24" :fill "none" :stroke "currentColor" :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"}
          (if @collapsed?
            [:path {:d "M9 18l6-6-6-6"}]
            [:path {:d "M15 18l-6-6 6-6"}])]]]

       [:div.search-bar
        (when-not @collapsed?
          [:input.search-input {:type "text"
                              :placeholder "Ask your second brain..."}])
        (when @collapsed?
          [:svg.icon {:xmlns "http://www.w3.org/2000/svg" :width "20" :height "20" :viewBox "0 0 24 24" :fill "none" :stroke "currentColor" :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"}
           [:circle {:cx "11" :cy "11" :r "8"}]
           [:path {:d "m21 21-4.3-4.3"}]])]
       
       [:div.sidebar-nav
        [:div.nav-section
         [:div.nav-section-header
          [:div.icon-text
           [:svg.icon {:xmlns "http://www.w3.org/2000/svg" :width "20" :height "20" :viewBox "0 0 24 24" :fill "none" :stroke "currentColor" :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"}
            [:path {:d "M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1 0-5H20"}]]
           (when-not @collapsed? [:span "Notes"])]
          (when-not @collapsed?
            [:div.actions
             [:button.icon {:on-click #(js/console.log "Add note")}
              [:svg {:xmlns "http://www.w3.org/2000/svg" :viewBox "0 0 24 24" :fill "none" :stroke "currentColor" :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"}
               [:line {:x1 "12" :y1 "5" :x2 "12" :y2 "19"}]
               [:line {:x1 "5" :y1 "12" :x2 "19" :y2 "12"}]]]])]
         
         [:div.nav-section-content
          [:div.nav-item {:class (when (= @active-item "Work") "active")
                         :on-click #(reset! active-item "Work")}
           [:svg.icon {:xmlns "http://www.w3.org/2000/svg" :width "20" :height "20" :viewBox "0 0 24 24" :fill "none" :stroke "currentColor" :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"}
            [:rect {:x "2" :y "7" :width "20" :height "14" :rx "2" :ry "2"}]
            [:path {:d "M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"}]]
           [:span "Work"]]
          
          [:div.nav-item {:class (when (= @active-item "Personal") "active")
                         :on-click #(reset! active-item "Personal")}
           [:svg.icon {:xmlns "http://www.w3.org/2000/svg" :width "20" :height "20" :viewBox "0 0 24 24" :fill "none" :stroke "currentColor" :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"}
            [:path {:d "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"}]]
           [:span "Personal"]]]]
        
        [:div.nav-section
         [:div.nav-section-header
          [:div.icon-text
           [:svg.icon {:xmlns "http://www.w3.org/2000/svg" :width "20" :height "20" :viewBox "0 0 24 24" :fill "none" :stroke "currentColor" :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"}
            [:rect {:width "18" :height "18" :x "3" :y "3" :rx "2"}]
            [:path {:d "m9 12 2 2 4-4"}]]
           (when-not @collapsed? [:span "Tasks"])]
          (when-not @collapsed?
            [:div.actions
             [:button.icon {:on-click #(js/console.log "Add task")}
              [:svg {:xmlns "http://www.w3.org/2000/svg" :viewBox "0 0 24 24" :fill "none" :stroke "currentColor" :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"}
               [:line {:x1 "12" :y1 "5" :x2 "12" :y2 "19"}]
               [:line {:x1 "5" :y1 "12" :x2 "19" :y2 "12"}]]]])]]
        
        [:div.nav-section
         [:div.nav-section-header
          [:div.icon-text
           [:svg.icon {:xmlns "http://www.w3.org/2000/svg" :width "20" :height "20" :viewBox "0 0 24 24" :fill "none" :stroke "currentColor" :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"}
            [:polyline {:points "21 8 21 21 3 21 3 8"}]
            [:rect {:x "1" :y "3" :width "22" :height "5"}]
            [:line {:x1 "10" :y1 "12" :x2 "14" :y2 "12"}]]
           (when-not @collapsed? [:span "Archive"])]]]
                
        [:div.nav-section
         [:div.nav-section-header
          [:div.icon-text
           [:svg.icon {:xmlns "http://www.w3.org/2000/svg" :width "20" :height "20" :viewBox "0 0 24 24" :fill "none" :stroke "currentColor" :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"}
            [:path {:d "M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"}]]
           (when-not @collapsed? [:span "Favorites"])]]]]
        
       ;; Status bar at the bottom of sidebar
       [:div.editor-status-bar
        [:div.save-status
         (let [content @(re-frame/subscribe [::subs/current-content])
               last-saved-content @(re-frame/subscribe [::subs/last-saved-content])
               saving? @(re-frame/subscribe [::subs/saving?])
               save-error @(re-frame/subscribe [::subs/save-error])]
           (cond
             saving? [:span.saving "Saving..."]
             save-error [:span.save-error (str "Error: " save-error)]
             (and content last-saved-content (not= content last-saved-content)) [:span.unsaved "Unsaved changes"]
             :else [:span.saved "All changes saved"]))]]])))

(defn callback-page []
  (auth/handle-auth-callback)
  [:div.loading
   [:div.loading-content
    [:h2 "Processing login..."]
    [:p "Please wait while we complete your authentication."]]])

(defn main-panel []
  (let [path (.-pathname js/window.location)]
    (cond
      (= path "/callback")
      [callback-page]
      
      :else
      [:div.app-container
       [left-panel]
       [:div.editor-container
        [markdown-editor/markdown-editor]]])))