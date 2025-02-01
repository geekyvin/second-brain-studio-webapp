(ns second-brain-studio-webapp.note-view
  (:require
   [reagent.core :as r]))

(defonce app-state
  (r/atom
   {:mode :folders ;; Can be :folders or :tags
    :folders {:root {:children {"Work" {:children {"Project A" {:notes ["Note 1" "Note 2"]}
                                               "Project B" {:notes ["Note 3"]}}}
                                "Personal" {:children {"Hobbies" {:notes ["Note 4"]}}}}}}
    :tags {"#work" ["Note 1" "Note 3"]
           "#personal" ["Note 4"]
           "#hobbies" ["Note 4"]}
    :selected-note nil}))

(defn folder-view [folder]
  (let [expanded (r/atom true)]
    (fn [folder]
      [:div.folder
       [:div {:style {:cursor "pointer"
                      :font-weight "bold"
                      :margin-bottom "5px"}
              :on-click #(swap! expanded not)}
        (if @expanded "▼" "▶") " " (first folder)]
       (when @expanded
         (let [content (second folder)]
           (into [:div {:style {:margin-left "10px"}}]
                 (concat
                  (for [[name sub-folder] (:children content)]
                    [folder-view [name sub-folder]])
                  (for [note (:notes content)]
                    [:div.note {:style {:cursor "pointer"
                                        :margin-left "10px"}
                                :on-click #(swap! app-state assoc :selected-note note)}
                     note])))))])))

(defn tag-view []
  [:div.tag-list
   (for [[tag notes] (:tags @app-state)]
     [:div {:style {:margin-bottom "10px"}}
      [:h4 {:style {:margin-bottom "5px"}} tag]
      (for [note notes]
        [:div.note {:style {:cursor "pointer"
                            :margin-left "10px"}
                    :on-click #(swap! app-state assoc :selected-note note)}
         note])])])

(defn left-pane []
  (let [mode (:mode @app-state)]
    [:div {:style {:width "250px"
                   :height "100%"
                   :border-right "1px solid #ccc"
                   :padding "10px"
                   :overflow-y "auto"}}
     ;; Mode Toggle
     [:div {:style {:margin-bottom "10px"}}
      [:label {:style {:margin-right "10px"}}
       [:input {:type "radio"
                :name "mode"
                :checked (= mode :folders)
                :on-change #(swap! app-state assoc :mode :folders)}]
       "Folders"]
      [:label
       [:input {:type "radio"
                :name "mode"
                :checked (= mode :tags)
                :on-change #(swap! app-state assoc :mode :tags)}]
       "Tags"]]
     ;; Render Mode
     (case mode
       :folders [folder-view ["root" (:folders @app-state :root)]]
       :tags [tag-view])]))

(defn main-view []
  [:div {:style {:display "flex"
                 :height "100vh"}}
   ;; Left Pane
   [left-pane]
   ;; Main Content
   [:div {:style {:flex "1"
                  :padding "20px"}}
    [:h1 "Selected Note: " (or (:selected-note @app-state) "None")]]])
