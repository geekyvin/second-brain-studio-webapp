(ns second-brain-studio-webapp.markdown-editor
  (:require
   [reagent.core :as r]
   ["markdown-it" :as MarkdownIt]
   [clojure.string :as str]
   [second-brain-studio-webapp.note-view :as note-view]))

;; Initialize the Markdown parser
(def markdown-parser (MarkdownIt.))

(defn call-summarize-api [content on-success]
  (-> (js/fetch "http://localhost:3000/get-summary"
                #js {:method "POST"
                     :headers #js {"Content-Type" "application/json"}
                     :body (js/JSON.stringify #js {:content content})})
      (.then (fn [response]
               (if (.-ok response)
                 (.json response)
                 (throw (js/Error (str "HTTP error! status: " (.-status response)))))))
      (.then (fn [data]
               (js/console.log "API Response:" data)
               (on-success (.-message data))))
      (.catch (fn [error]
                (js/console.error "Error fetching summary:" error)))))

(defn update-summary-section [content summary]
  ;; Find or create the `#### Summary` section
  (let [lines (clojure.string/split content #"\n")
        summary-index (.indexOf lines "#### Summary")
        updated-lines (if (>= summary-index 0)
                        ;; Replace existing summary section
                        (concat (take (inc summary-index) lines) [summary])
                        ;; Append new summary section
                        (concat lines ["#### Summary" summary]))]
    ;; Join the updated lines back into Markdown
    (clojure.string/join "\n" updated-lines)))

(defn call-generate-audio-api [content on-success on-error]
  (-> (js/fetch "http://localhost:3000/generate-audio"
                #js {:method "POST"
                     :headers #js {"Content-Type" "application/json"}
                     :body (js/JSON.stringify #js {:content content})})
      (.then (fn [response]
               (if (.-ok response)
                 (.blob response) ;; Get the audio file as a Blob
                 (throw (js/Error (str "HTTP error! status: " (.-status response)))))))
      (.then (fn [blob]
               (let [audio-url (js/URL.createObjectURL blob)]
                 (on-success audio-url))))
      (.catch (fn [error]
                (js/console.error "Error generating audio:" error)
                (when on-error (on-error error))))))

(defn markdown-editor []
  (let [content (r/atom "### Hello World!\n\nWrite some **Markdown** here!")
        mode (r/atom :edit)
        title (r/atom "Untitled") ;; Default title
        audio-url (r/atom nil) ;; Stores the generated audio URL
        audio-player (r/atom nil) ;; Audio element reference]
        is-playing (r/atom false) ;; Tracks if the audio is playing
        editing-title (r/atom false) ;; Track if the title is being edited
        highlighted (r/atom false)] ;; Track if the summary is highlighted
    (fn []
       ;; Left Pane: Note view
       ;;[:div {:style {:width "250px"
       ;;               :padding "10px"
       ;;               :padding-top "20px"
       ;;              :background-color "#f5f5f5"}}
       ;; [note-view/left-pane]]

       ;; Right Pane: Markdown Editor
       [:div {:style {:flex "1"
                      :padding "20px"}}
        ;; Title Section
        [:div {:style {:margin-bottom "10px"}}
         (if @editing-title
           ;; Title in edit mode
           [:input {:type "text"
                    :value @title
                    :auto-focus true
                    :on-change #(reset! title (-> % .-target .-value))
                    :on-blur #(reset! editing-title false)
                    :on-key-down #(when (= (.-key %) "Enter")
                                    (reset! editing-title false))
                    :style {:font-size "20px"
                            :font-weight "bold"
                            :margin-bottom "10px"
                            :border "1px solid #ccc"
                            :border-radius "5px"
                            :padding "5px"}}]
           ;; Title in label mode
           [:h2 {:on-click #(reset! editing-title true)
                 :style {:font-size "20px"
                         :font-weight "bold"
                         :margin-bottom "10px"
                         :cursor "pointer"}}
            @title])]

        
;; Button Container
      [:div {:style {:display "flex"
                       :align-items "center"
                       :gap "10px"    ;; Adds spacing between buttons
                       :margin-bottom "10px"}}
        ;; Summarize Button
        [:button {:class "text-btn"
                  :style {:padding "10px"
                          :color "#fff"
                          :border "none"
                          :border-radius "5px"
                          :cursor "pointer"}
                  :on-click #(call-summarize-api @content
                                                 (fn [summary]
                                                   ;; Replace the Summary section
                                                   (reset! content (update-summary-section @content summary))
                                                   ;; Trigger highlight effect
                                                   (reset! highlighted true)
                                                   (js/setTimeout (fn [] (reset! highlighted false)) 2000)))}
         "Summarize"]

       ;; Generate Audio Button
        [:button {:class "text-btn"
                  :style {:padding "10px"
                          :color "#fff"
                          :border "none"
                          :border-radius "5px"
                          :cursor "pointer"}
                  :on-click #(call-generate-audio-api
                              @content
                              (fn [url]
                                (reset! audio-url url)
                                (reset! audio-player (js/Audio. url)))
                              (fn [error]
                                (js/console.error "Error generating audio:" error)))}
         "Orate"]

        ;; Toggle Play/Pause Button
        (when @audio-url
          [:button {:class "text-btn-img"
                    :style {:padding-top "8px"
                            :cursor "pointer"
                            :border "none"
                            :border-radius "5px"}
                    :on-click #(when-let [player @audio-player]
                                 (if @is-playing
                                   (do (.pause player) (reset! is-playing false)) ;; Pause action
                                   (do (.play player) (reset! is-playing true))))} ;; Play action
           (if @is-playing
             [:img {:src "../images/pause.png" :alt "Pause"}]
             [:img {:src "../images/play.png" :alt "Play"}])])
        
                
        [:div 
         [:button {:class "text-btn"
                   :style {:padding "10px"
                           :color "#fff"
                           :border "none"
                           :border-radius "5px"
                           :cursor "pointer"
                           :margin-right "2px"}
                  }
          "CoCreate"]]
        [:div
         [:button {:class "text-btn"
                   :style {:padding "10px"
                           :color "#fff"
                           :border "none"
                           :border-radius "5px"
                           :cursor "pointer"
                           :margin-right "2px"}}
          "Castify"]]
        
       ;; **🔹 Edit / Preview Mode Toggle**
       [:div {:style {:margin-bottom "10px"
                      :margin-top "20px"
                      :display "flex"
                      :font-size "18px"
                      :font-family "'atkinson-hyper', 'dm-sans'"
                      :color "#cc6633"
                      :align-items "center"
                      :gap "4px"}}
        [:label
         [:input {:type "radio"
                  :name "mode"
                  :checked (= @mode :edit)
                  :on-change #(reset! mode :edit)}]
         " Edit"]
        [:label
         [:input {:type "radio"
                  :name "mode"
                  :checked (= @mode :preview)
                  :on-change #(reset! mode :preview)}]
         " Preview"]]]
       
        ;; Conditionally render Edit or Preview mode
        (case @mode
          :edit [:textarea {:value @content
                            :on-change #(reset! content (-> % .-target .-value))
                            :class (when @highlighted "highlight") ;; Apply highlight class
                            :style {:width "100%"
                                    :height "70vh"
                                    :padding "10px"
                                    :border "none"
                                    :border-radius "5px"}}]
          :preview [:div {:style {:width "100%"
                                  :height "70vh"
                                  :padding "10px"
                                  :overflow-y "auto"
                                  :border-radius "5px"
                                  :background-color "#f9f9f9"}}
                    [:div {:dangerouslySetInnerHTML
                           #js {:__html (.render markdown-parser @content)}}]])])))
