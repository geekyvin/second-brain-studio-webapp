(ns second-brain-studio-webapp.markdown-editor
  (:require
   [reagent.core :as r]
   ["markdown-it" :as MarkdownIt]
   [clojure.string :as str]
   [re-frame.core :as re-frame]
   [second-brain-studio-webapp.events :as events]
   [second-brain-studio-webapp.subs :as subs]
   [second-brain-studio-webapp.sb-backend-client :as sbb-client]
   [second-brain-studio-webapp.note-view :as note-view]))

;; Initialize the Markdown parser
(def markdown-parser (MarkdownIt.))

(defn handle-file-upload [file content-atom]
  (if (str/starts-with? (.-type file) "image/")
    ;; For images, create a temporary placeholder
    (let [temp-tag (str "![" (.-name file) "](uploading...)\n")]
      (swap! content-atom #(str % "\n" temp-tag))
      (sbb-client/call-capture-api 
       file
       (fn [response]
         (js/console.log "Processing response:" response) ;; Debug log
         (if (and (= (.-status response) "success") (.-markdown response))
           (let [;; Create image URL with query param
                 image-name (.-name file)
                 image-url (str "http://localhost:3000/image?name=" 
                              (js/encodeURIComponent image-name))
                 ;; Replace any existing image URLs in the markdown with query param version
                 updated-markdown (-> (.-markdown response)
                                    (str/replace #"\(http://localhost:3000/uploads/[^\)]+\)"
                                               (str "(" image-url ")")))]
             (swap! content-atom 
                    #(str/replace % 
                                temp-tag 
                                (str updated-markdown "\n\n"))))
           ;; If response format is different, handle the error
           (swap! content-atom #(str/replace % temp-tag "Error processing image\n"))))))
    ;; For text files, handle as before
    (sbb-client/call-capture-api 
     file
     (fn [response]
       (if (and (= (.-status response) "success") (.-markdown response))
         (swap! content-atom #(str % "\n" (.-markdown response)))
         (swap! content-atom #(str % "\n" "Error processing file")))))))

(defn save-note [user-id namespace note-id content on-success on-error]
  (let [content-str (if (nil? content) "" content)
        auth-token @(re-frame/subscribe [::subs/auth-token])
        request-body #js {:user-id user-id
                          :namespace namespace
                          :note-id note-id
                          :content content-str}
        json-body (js/JSON.stringify request-body)
        headers (cond-> #js {"Content-Type" "application/json"}
                  auth-token (js/Object.assign #js {"Authorization" (str "Bearer " auth-token)}))]
    (js/console.log "Sending save request with body:" json-body)
    (js/console.log "Content length:" (count content-str) "Content type:" (type content-str))
    
    (js/console.log "=== AUTH TOKEN DEBUG IN SAVE REQUEST ===")
    (if auth-token
      (let [token-preview (if (> (count auth-token) 10)
                          (str (subs auth-token 0 10) "...")
                          auth-token)
            auth-header (str "Bearer " auth-token)
            header-preview (if (> (count auth-header) 20)
                           (str (subs auth-header 0 20) "...")
                           auth-header)]
        (js/console.log "Token found - length: " (count auth-token))
        (js/console.log "Token preview: " token-preview)
        (js/console.log "Authorization header: " header-preview))
      (js/console.warn "No auth token available for save request"))
    
    ;; Log the actual headers being sent
    (let [auth-header (.-Authorization headers)]
      (js/console.log "Final Authorization header: " (if auth-header (str (subs auth-header 0 20) "...") "NONE")))
    (js/console.log "=== END AUTH TOKEN DEBUG ===")
    
    (-> (js/fetch "http://localhost:3000/save-note"
                  #js {:method "POST"
                       :headers headers
                       :body json-body})
        (.then (fn [response]
                 (js/console.log "Save response status:" (.-status response))
                 (if (.-ok response)
                   (.json response)
                   (throw (js/Error. (str "HTTP error! status: " (.-status response)))))))
        (.then (fn [data]
                 (js/console.log "Note saved successfully:" data)
                 (when on-success (on-success data))))
        (.catch (fn [error]
                  (js/console.error "Error saving note:" error)
                  (when on-error (on-error error)))))))

(defn save-current-note! [content saving? save-error last-saved-content user-id namespace note-id]
  (let [current-content @content]
    (js/console.log "Trying to save note, content:" (pr-str current-content))
    (js/console.log "Content length:" (count current-content) "Content type:" (type current-content))
    (js/console.log "Saving? " @saving? ", Content changed? " (not= current-content @last-saved-content))
    (when (and (not @saving?) 
               (not= current-content @last-saved-content)
               (not (nil? current-content))
               (not (empty? current-content)))
      (js/console.log "Starting save with content:" (pr-str current-content))
      (reset! saving? true)
      (reset! save-error nil)
      
      ;; Update re-frame app-db for sidebar status
      (re-frame/dispatch [::events/set-current-content current-content])
      (re-frame/dispatch [::events/set-saving true])
      (re-frame/dispatch [::events/set-save-error nil])
      
      (save-note 
       user-id 
       namespace 
       note-id 
       current-content
       (fn [data]
         (js/console.log "Save succeeded with response:" data)
         (reset! last-saved-content current-content)
         (reset! saving? false)
         
         ;; Update re-frame app-db for sidebar status
         (re-frame/dispatch [::events/set-last-saved-content current-content])
         (re-frame/dispatch [::events/set-saving false]))
       (fn [err]
         (js/console.error "Save failed with error:" err)
         (reset! save-error (str err))
         (reset! saving? false)
         
         ;; Update re-frame app-db for sidebar status
         (re-frame/dispatch [::events/set-save-error (str err)])
         (re-frame/dispatch [::events/set-saving false]))))))

(defn markdown-editor []
  (let [content (r/atom "")
        title (r/atom "Untitled")
        editing-title (r/atom false)
        highlighted (r/atom false)
        file-input-ref (r/atom nil)
        audio-url (r/atom nil)
        audio-player (r/atom nil)
        is-playing (r/atom false)
        audio-progress (r/atom 0)
        audio-duration (r/atom 0)
        last-saved-content (r/atom "")
        saving? (r/atom false)
        save-error (r/atom nil)
        auto-save-interval (r/atom nil)
        user-id "demo-user" ;; In a real app, this would come from auth
        namespace "default" ;; This could be based on selected folder
        note-id (str "note-" (.now js/Date)) ;; Generate a unique ID for this note
        reset-audio-state! (fn []
                             (when @is-playing
                               (when @audio-player (.pause ^js @audio-player)))
                             (reset! audio-url nil)
                             (reset! audio-player nil)
                             (reset! is-playing false)
                             (reset! audio-progress 0)
                             (reset! audio-duration 0))
        save-current-note! (fn []
                             (let [current-content @content]
                               (js/console.log "Trying to save note, content:" (pr-str current-content))
                               (js/console.log "Content length:" (count current-content) "Content type:" (type current-content))
                               (js/console.log "Saving? " @saving? ", Content changed? " (not= current-content @last-saved-content))
                               (when (and (not @saving?) 
                                          (not= current-content @last-saved-content)
                                          (not (nil? current-content))
                                          (not (empty? current-content)))
                                 (js/console.log "Starting save with content:" (pr-str current-content))
                                 (reset! saving? true)
                                 (reset! save-error nil)
                                 
                                 ;; Update re-frame app-db for sidebar status
                                 (re-frame/dispatch [::events/set-current-content current-content])
                                 (re-frame/dispatch [::events/set-saving true])
                                 (re-frame/dispatch [::events/set-save-error nil])
                                 
                                 (save-note 
                                  user-id 
                                  namespace 
                                  note-id 
                                  current-content
                                  (fn [data]
                                    (js/console.log "Save succeeded with response:" data)
                                    (reset! last-saved-content current-content)
                                    (reset! saving? false)
                                    
                                    ;; Update re-frame app-db for sidebar status
                                    (re-frame/dispatch [::events/set-last-saved-content current-content])
                                    (re-frame/dispatch [::events/set-saving false]))
                                  (fn [err]
                                    (js/console.error "Save failed with error:" err)
                                    (reset! save-error (str err))
                                    (reset! saving? false)
                                    
                                    ;; Update re-frame app-db for sidebar status
                                    (re-frame/dispatch [::events/set-save-error (str err)])
                                    (re-frame/dispatch [::events/set-saving false]))))))]
    
    ;; Setup auto-save when component mounts
    (r/create-class
     {:component-did-mount
      (fn [_]
        ;; Set initial test content for debugging
        (let [initial-content "What would you like to do today?"]
          (reset! content initial-content)
          (js/console.log "Initial content set:" initial-content "Length:" (count initial-content))
          
          ;; Update re-frame app-db for sidebar status
          (re-frame/dispatch [::events/set-current-content initial-content])
          
          ;; Trigger initial save immediately (with slight delay to ensure atom is updated)
          (js/setTimeout #(do
                            (js/console.log "Triggering initial save, content:" @content)
                            (save-current-note!)) 
                        500))
        ;; Set up auto-save interval
        (reset! auto-save-interval (js/setInterval save-current-note! 5000)))
      
      :component-will-unmount
      (fn [_]
        (when @auto-save-interval
          (js/clearInterval @auto-save-interval)
          (reset! auto-save-interval nil)))
      
      :reagent-render
      (fn []
        [:div.editor-container
         [:div.action-bar
          [:div.action-bar-left
           (if @editing-title
             [:input.note-title
              {:type "text"
               :value @title
               :auto-focus true
               :on-change #(reset! title (-> % .-target .-value))
               :on-blur #(reset! editing-title false)
               :on-key-down #(when (= (.-key %) "Enter")
                              (reset! editing-title false))}]
             [:div.title-container
              [:h2.note-title
               {:on-click #(reset! editing-title true)}
               @title]])

           [:div.feature-buttons
            [:button.feature-button
             {:on-click #(sbb-client/call-summarize-api 
                         @content
                         (fn [summary]
                           (reset! content (sbb-client/update-summary-section @content summary))
                           (reset! highlighted true)
                           (js/setTimeout (fn [] (reset! highlighted false)) 2000)))}
             [:svg {:xmlns "http://www.w3.org/2000/svg" :viewBox "0 0 24 24" :fill "none" :stroke "currentColor" :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"}
              [:path {:d "M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"}]
              [:polyline {:points "14 2 14 8 20 8"}]]
             "Summarize"]

            [:button.feature-button
             {:class (when @audio-url "has-audio")
              :on-click (fn []
                          (if @audio-url
                            (if @is-playing
                              (do
                                (.pause ^js @audio-player)
                                (reset! is-playing false))
                              (do
                                (.play ^js @audio-player)
                                (reset! is-playing true)))
                            (sbb-client/call-generate-audio-api
                             @content
                             (fn [url]
                               (reset! audio-url url)
                               (let [player (js/Audio. url)]
                                 (reset! audio-player player)
                                 (set! (.-onplay player) #(reset! is-playing true))
                                 (set! (.-onpause player) #(reset! is-playing false))
                                 (set! (.-onended player) #(do (reset! is-playing false)
                                                               (reset! audio-progress 0)))
                                 (set! (.-ondurationchange player) #(reset! audio-duration (.-duration player)))
                                 (set! (.-ontimeupdate player) #(reset! audio-progress (.-currentTime player)))
                                 (.play player)
                                 (reset! is-playing true)))
                             (fn [error]
                               (js/console.error "Error generating audio:" error)))))}
             [:div.audio-controls
              [:svg.audio-icon {:xmlns "http://www.w3.org/2000/svg" :viewBox "0 0 24 24" :fill "none" :stroke "currentColor" :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"}
               [:path {:d "M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z"}]
               [:path {:d "M19 10v2a7 7 0 0 1-14 0v-2"}]
               [:line {:x1 "12" :x2 "12" :y1 "19" :y2 "22"}]]
              
              (when @audio-url
                [:div.audio-player
                 [:div.play-pause-button
                  (if @is-playing
                    [:svg.pause-icon {:xmlns "http://www.w3.org/2000/svg" :viewBox "0 0 24 24" :fill "none" :stroke "currentColor" :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"}
                     [:rect {:x "6" :y "4" :width "4" :height "16"}]
                     [:rect {:x "14" :y "4" :width "4" :height "16"}]]
                    [:svg.play-icon {:xmlns "http://www.w3.org/2000/svg" :viewBox "0 0 24 24" :fill "none" :stroke "currentColor" :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"}
                     [:polygon {:points "5 3 19 12 5 21 5 3"}]])]
                 [:div.progress-bar
                  [:div.progress-fill {:style {:width (str (* 100 (/ @audio-progress (if (pos? @audio-duration) @audio-duration 1))) "%")}}]]])]
             (if @audio-url 
               (if @is-playing "Playing..." "Paused") 
               "Transcribe")]

            [:div
             [:input.hidden
              {:type "file"
               :ref #(reset! file-input-ref %)
               :accept ".md,.txt,image/*"
               :on-change #(when-let [file (-> % .-target .-files (aget 0))]
                            (handle-file-upload file content))}]
             [:button.feature-button
              {:on-click #(when-let [input @file-input-ref]
                           (.click input))}
              [:svg {:xmlns "http://www.w3.org/2000/svg" :viewBox "0 0 24 24" :fill "none" :stroke "currentColor" :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"}
               [:path {:d "M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z"}]
               [:circle {:cx "12" :cy "13" :r "3"}]]
              "Capture"]]

            [:button.feature-button
             [:svg {:xmlns "http://www.w3.org/2000/svg" :viewBox "0 0 24 24" :fill "none" :stroke "currentColor" :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"}
              [:path {:d "M15 14c.2-1 .7-1.7 1.5-2.5 1-.9 1.5-2.2 1.5-3.5A6 6 0 0 0 6 8c0 1 .2 2.2 1.5 3.5.7.7 1.3 1.5 1.5 2.5"}]
              [:path {:d "M9 18h6"}]
              [:path {:d "M10 22h4"}]]
             "CoCreate"]

            [:button.feature-button
             [:svg {:xmlns "http://www.w3.org/2000/svg" :viewBox "0 0 24 24" :fill "none" :stroke "currentColor" :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"}
              [:path {:d "M4 21a2 2 0 0 0 2-2V5a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v14a2 2 0 0 0 2 2"}]
              [:path {:d "m9 7 2 2 4-4"}]]
             "Classify"]]]

          [:div.user-info
           [:div.user-avatar 
            (let [user-name @(re-frame/subscribe [:user])
                  first-initial (when (and user-name (:given_name user-name))
                                  (first (:given_name user-name)))]
              (if first-initial 
                (clojure.string/upper-case (str first-initial))
                "A"))]
           [:span 
            (let [user-name @(re-frame/subscribe [:user])]
              (if (and user-name (:given_name user-name))
                (:given_name user-name)
                "Alex"))]
           [:button.sign-out-button 
            {:on-click (fn [] 
                         (js/console.log "Sign out button clicked")
                         (re-frame/dispatch [:sign-out]))}
            "Sign out"]]]

         [:div.note-editor
          [:textarea.note-content
           {:value @content
            :placeholder "Start writing your thoughts here..."
            :on-change (fn [e]
                         (let [new-value (-> e .-target .-value)]
                           (js/console.log "Textarea content changed, new length:" (count new-value))
                           (js/console.log "New content:" (pr-str new-value))
                           (when (empty? new-value)
                             (js/console.warn "Warning: Empty content detected in textarea onChange"))
                           (reset! content new-value)
                           ;; Reset audio state when content changes
                           (reset-audio-state!)))
            :on-blur (fn [_]
                       (js/console.log "Editor lost focus, triggering save")
                       (save-current-note!))
            :class (when @highlighted "highlight")}]]])})))
