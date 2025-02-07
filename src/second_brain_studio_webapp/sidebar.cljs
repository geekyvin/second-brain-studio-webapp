(ns second-brain-studio-webapp.sidebar
  (:require [reagent.core :as r]))

(defonce app-state
  (r/atom {:mode :folders
           :folders {:root {:children {"Work" {:children {"Project A" {:notes ["Note 1" "Note 2"]}
                                                      "Project B" {:notes ["Note 3"]}}}
                                     "Personal" {:children {"Hobbies" {:notes ["Note 4"]}}}}}}
           :tags {"#work" ["Note 1" "Note 3"]
                  "#personal" ["Note 4"]
                  "#hobbies" ["Note 4"]}
           :selected-note "Untitled"
           :search-query ""}))

;; Folder Tree
(defn folder-view [folder]
  (let [expanded (r/atom true)]
    (fn [[name content]]
      [:div.folder
       [:div {:style {:cursor "pointer" :font-weight "bold" :padding "5px 10px"}
              :on-click #(swap! expanded not)}
        (if @expanded "▼" "▶") " " name]
       (when @expanded
         (into [:div {:style {:margin-left "15px"}}]
               (concat
                (for [[sub-name sub-folder] (:children content)]
                  [folder-view [sub-name sub-folder]])
                (for [note (:notes content)]
                  [:div.note {:style {:cursor "pointer"
                                      :margin-left "10px"
                                      :padding "5px"}
                              :on-click #(swap! app-state assoc :selected-note note)}
                   note]))))])))

;; Tag View
(defn tag-view []
  [:div.tag-list
   (for [[tag notes] (:tags @app-state)]
     [:div {:style {:margin-bottom "10px" :padding "5px"}}
      [:h4 {:style {:margin-bottom "5px"}} tag]
      (for [note notes]
        [:div.note {:style {:cursor "pointer"
                            :margin-left "10px"
                            :padding "5px"}
                    :on-click #(swap! app-state assoc :selected-note note)}
         note])])])

;; Search Bar
(defn search-bar []
  [:input {:type "text"
           :placeholder "Search anything..."
           :value (:search-query @app-state)
           :on-change #(swap! app-state assoc :search-query (-> % .-target .-value))
           :style {:width "100%" :padding "8px" :border "1px solid #ccc" :border-radius "5px"}}])

;; Sidebar Component
(defn sidebar []
  [:div {:style {:width "260px"
                 :height "100vh"
                 :border-right "1px solid #ccc"
                 :padding "15px"
                 :background "#f9f9f9"}}
   [search-bar]
   [:div {:style {:margin-top "15px"}}
    [:label {:style {:margin-right "10px"}}
     [:input {:type "radio"
              :name "mode"
              :checked (= (:mode @app-state) :folders)
              :on-change #(swap! app-state assoc :mode :folders)}]
     " Folders"]
    [:label {:style {:margin-left "10px"}}
     [:input {:type "radio"
              :name "mode"
              :checked (= (:mode @app-state) :tags)
              :on-change #(swap! app-state assoc :mode :tags)}]
     " Tags"]]
   (case (:mode @app-state)
     :folders [folder-view ["root" (:folders @app-state :root)]]
     :tags [tag-view])])
