(ns second-brain-studio-webapp.left-pane
  (:require
   [reagent.core :as r]))

(defonce app-state
  (r/atom {:mode :folders
           :folders {:root {:children {"Work" {:children {"Project A" {:notes ["Note 1" "Note 2"]}
                                                          "Project B" {:notes ["Note 3"]}}}
                                       "Personal" {:children {"Hobbies" {:notes ["Note 4"]}}}}}}
           :tags {"#work" ["Note 1" "Note 3"]
                  "#personal" ["Note 4"]
                  "#hobbies" ["Note 4"]}
           :selected-note nil
           :search-query ""}))

;; 🔹 Search Bar Component
(defn search-bar []
  [:div {:style {:display "flex"
                 :align-items "center"
                 :background "#f0f0f0"
                 :border-radius "8px"
                 :padding "8px"
                 :margin-bottom "10px"}}
   [:input {:type "text"
            :placeholder "Search anything..."
            :value (:search-query @app-state)
            :on-change #(swap! app-state assoc :search-query (-> % .-target .-value))
            :style {:flex "1"
                    :padding "8px"
                    :border "none"
                    :outline "none"
                    :background "transparent"
                    :font-size "14px"}}]])

;; 🔹 Sidebar Item Component
(defn sidebar-item [{:keys [label icon active?]}]
  [:div {:style {:display "flex"
                 :align-items "center"
                 :gap "10px"
                 :padding "10px 15px"
                 :cursor "pointer"
                 :border-radius "8px"
                 :font-size "14px"
                 :font-weight "500"
                 :color (if active? "#000" "#444")
                 :background (if active? "#e8e8e8" "transparent")
                 :transition "background 0.2s ease"
                 :hover {:background "#f2f2f2"}}}
   [:span {:style {:font-size "18px"}} icon]
   [:span label]])

;; 🔹 Sidebar Component
(defn left-pane []
  [:div {:style {:width "280px"
                 :height "100vh"
                 :background "#f9f9f9"
                 :border-right "1px solid #ddd"
                 :padding "15px"
                 :display "flex"
                 :flex-direction "column"}}

   ;; Search Bar
   [search-bar]

   ;; Navigation Links
   [:div
    [sidebar-item {:label "Daily Tasks" :icon "📝" :active? true}]
    [sidebar-item {:label "All Notes" :icon "📂"}]
    [sidebar-item {:label "Tasks" :icon "✅"}]
    ;;[sidebar-item {:label "Map" :icon "🌍"}]
    ]

   ;; Pinned Notes Section
   [:h4 {:style {:margin "15px 0 5px 10px"
                 :font-size "12px"
                 :font-weight "600"
                 :color "#666"
                 :text-transform "uppercase"}} "Pinned Notes"]
   [:div
    [:div {:style {:padding "5px 10px"
                   :cursor "pointer"
                   :color "#444"
                   :font-size "13px"
                   :border-radius "5px"
                   :hover {:background "#f2f2f2"}}} "How to use Second Brain Studio"]
    ;;[:div {:style {:padding "5px 10px"
    ;;              :cursor "pointer"
    ;;               :color "#444"
    ;;               :font-size "13px"
    ;;               :border-radius "5px"
    ;;               :hover {:background "#f2f2f2"}}} "The power of Backlinks"]
    [:div {:style {:padding "5px 10px"
                   :cursor "pointer"
                   :color "#444"
                   :font-size "13px"
                   :border-radius "5px"
                   :hover {:background "#f2f2f2"}}} "Favorites"]
    [:div {:style {:padding "5px 10px"
                   :cursor "pointer"
                   :color "#444"
                   :font-size "13px"
                   :border-radius "5px"
                   :hover {:background "#f2f2f2"}}} "Tips"]]])
