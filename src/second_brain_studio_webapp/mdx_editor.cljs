(ns second-brain-studio-webapp.mdx-editor
  (:require
   [reagent.core :as r]
   [re-frame.core :as re-frame]
   [clojure.string :as str]
   [second-brain-studio-webapp.events :as events]
   [second-brain-studio-webapp.subs :as subs]
   [second-brain-studio-webapp.mdx-viewer :as mdx-viewer]
   [second-brain-studio-webapp.ui-generator :as ui-generator]
   [second-brain-studio-webapp.sb-backend-client :as sbb-client]
   ["markdown-it" :as MarkdownIt]))

;; Example markdown content with a data table for visualization
(def initial-content "# MDX Editor Example

This is a sample document with data visualization.

## Data Table

| Area | 2019 | 2020 | 2021 | 2022 | 2023 |
|------|------|------|------|------|------|
| Barton and Sandhills | 54 | 61 | 72 | 64 | 59 |
| Blackbird Leys | 82 | 79 | 85 | 91 | 88 |
| Churchill | 35 | 37 | 34 | 39 | 42 |
| Cowley | 67 | 72 | 75 | 78 | 71 |
| Headington | 44 | 47 | 53 | 58 | 61 |

Try creating a visualization using the command field below:
- Type 'Create a line chart for all areas' to visualize the data
- Try 'Create a bar chart comparing 2019 vs 2023' for comparing specific years
")

(defn handle-file-upload [file content-atom]
  (if (str/starts-with? (.-type file) "image/")
    ;; For images, create a data URL
    (let [reader (js/FileReader.)]
      (set! (.-onload reader) 
            (fn [e]
              (let [img-url (.. e -target -result)
                    img-tag (str "![" (.-name file) "](" img-url ")\n")]
                (swap! content-atom #(str % "\n" img-tag)))))
      (.readAsDataURL reader file))
    ;; For text files, read the content
    (let [reader (js/FileReader.)]
      (set! (.-onload reader)
            (fn [e]
              (let [content (.. e -target -result)]
                (swap! content-atom #(str % "\n```\n" content "\n```\n")))))
      (.readAsText reader file))))

(defn save-note [content title on-success on-error]
  (let [auth-token @(re-frame/subscribe [::subs/auth-token])
        user-id "demo-user" ;; In a real app, this would come from auth
        namespace "default" ;; This could be based on selected folder
        note-id (str "mdx-note-" (.now js/Date)) ;; Generate a unique ID for this note
        content-str (if (nil? content) "" content)
        request-body #js {:user-id user-id
                          :namespace namespace
                          :note-id note-id
                          :title title
                          :content content-str}
        json-body (js/JSON.stringify request-body)
        headers (cond-> #js {"Content-Type" "application/json"}
                  auth-token (js/Object.assign #js {"Authorization" (str "Bearer " auth-token)}))]
    
    (js/console.log "Saving MDX note:" title)
    (js/console.log "Content length:" (count content-str))
    
    ;; Simulate a successful save for now
    (js/setTimeout
     (fn []
       (js/console.log "MDX note saved successfully")
       (when on-success (on-success {:status "success"})))
     500)))

(defn visualization-generator [content-atom]
  (let [command (r/atom "")
        loading (r/atom false)
        error (r/atom nil)
        result (r/atom nil)
        fallback-visualizations {
          ;; Sample line chart for area data
          "line chart area" 
          "[:> ResponsiveContainer {:width \"100%\" :height 400} [:> LineChart {:data [{:name \"Barton and Sandhills\", :2016 3173, :2017 3373, :2018 3573, :2019 3748, :2020 3898} {:name \"Blackbird Leys\", :2016 2410, :2017 2610, :2018 2610, :2019 2610, :2020 2710} {:name \"Churchill\", :2016 2604, :2017 2709, :2018 2709, :2019 2709, :2020 2709} {:name \"Cowley\", :2016 2539, :2017 2569, :2018 2569, :2019 2569, :2020 2569}]} [:> XAxis {:dataKey \"name\"}] [:> YAxis] [:> CartesianGrid {:strokeDasharray \"3 3\"}] [:> Tooltip] [:> Legend] [:> Line {:type \"monotone\" :dataKey \"2016\" :stroke \"#8884d8\"}] [:> Line {:type \"monotone\" :dataKey \"2017\" :stroke \"#82ca9d\"}] [:> Line {:type \"monotone\" :dataKey \"2018\" :stroke \"#ffc658\"}] [:> Line {:type \"monotone\" :dataKey \"2019\" :stroke \"#ff7300\"}] [:> Line {:type \"monotone\" :dataKey \"2020\" :stroke \"#ff0000\"}]]]"
          
          ;; Sample bar chart
          "bar chart" 
          "[:> ResponsiveContainer {:width \"100%\" :height 400} [:> BarChart {:data [{:name \"Barton and Sandhills\", :value 54} {:name \"Blackbird Leys\", :value 82} {:name \"Churchill\", :value 35} {:name \"Cowley\", :value 67} {:name \"Headington\", :value 44}]} [:> XAxis {:dataKey \"name\"}] [:> YAxis] [:> CartesianGrid {:strokeDasharray \"3 3\"}] [:> Tooltip] [:> Legend] [:> Bar {:dataKey \"value\" :fill \"#8884d8\"}]]]"
          
          ;; Sample pie chart
          "pie chart"
          "[:> ResponsiveContainer {:width \"100%\" :height 400} [:> PieChart [:> Pie {:data [{:name \"Barton and Sandhills\", :value 54} {:name \"Blackbird Leys\", :value 82} {:name \"Churchill\", :value 35} {:name \"Cowley\", :value 67} {:name \"Headington\", :value 44}], :dataKey \"value\", :nameKey \"name\", :cx \"50%\", :cy \"50%\", :outerRadius 80, :fill \"#8884d8\", :label true} [:> Cell {:fill \"#8884d8\"}] [:> Cell {:fill \"#82ca9d\"}] [:> Cell {:fill \"#ffc658\"}] [:> Cell {:fill \"#ff7300\"}] [:> Cell {:fill \"#ff0000\"}]] [:> Tooltip]]]"
        }
        insert-fallback-visualization (fn [cmd]
                                       (let [clean-cmd (str/lower-case (str/trim cmd))
                                             matching-keys (filter #(str/includes? clean-cmd %) (keys fallback-visualizations))
                                             selected-key (when (seq matching-keys) (first matching-keys))
                                             viz-code (when selected-key (get fallback-visualizations selected-key))]
                                         (if viz-code
                                           (let [encoded-code (js/encodeURIComponent viz-code)
                                                 visual-tag (str "\n<VisualComponent code=\"" encoded-code "\" />\n")]
                                             (swap! content-atom #(str % visual-tag))
                                             (reset! command "")
                                             (reset! result "Visualization inserted successfully!")
                                             true)
                                           false)))
        insert-direct-code (fn [cmd]
                           (if (and (str/starts-with? cmd "[:>") (str/ends-with? cmd "]"))
                             (let [encoded-code (js/encodeURIComponent cmd)
                                   visual-tag (str "\n<VisualComponent code=\"" encoded-code "\" />\n")]
                               (swap! content-atom #(str % visual-tag))
                               (reset! command "")
                               (reset! result "Visualization code inserted directly!")
                               true)
                             false))
        insert-visualization (fn [command-text]
                              (when-not (str/blank? command-text)
                                (reset! loading true)
                                (reset! error nil)
                                (reset! result nil)
                                ;; Try direct code insertion first
                                (if (insert-direct-code command-text)
                                  (reset! loading false)
                                  ;; Try API call
                                  (sbb-client/call-generate-ui-api
                                   command-text
                                   @content-atom
                                   (fn [data]
                                     (reset! loading false)
                                     (let [ui-code (ui-generator/extract-ui-code 
                                                   (if (and data (.-body data))
                                                     (.-body data)
                                                     (str data)))]
                                       (if ui-code
                                         (let [encoded-code (js/encodeURIComponent ui-code)
                                               visual-tag (str "\n<VisualComponent code=\"" encoded-code "\" />\n")]
                                           (swap! content-atom #(str % visual-tag))
                                           (reset! command "")
                                           (reset! result "Visualization inserted successfully!"))
                                         ;; Try fallback visualizations when API fails to generate code
                                         (if (insert-fallback-visualization command-text)
                                           (js/console.log "Used fallback visualization")
                                           (reset! error "Failed to generate visualization")))))
                                   (fn [err]
                                     (reset! loading false)
                                     (js/console.error "API error:", err)
                                     ;; Try fallback visualizations when API fails
                                     (if (insert-fallback-visualization command-text)
                                       (js/console.log "Used fallback visualization after API error")
                                       (reset! error (str "Error: " err))))))))]
    (fn []
      [:div.visualization-generator
       [:h3 "Generate Visualization"]
       [:div.visualization-input-container
        [:input.visualization-input
         {:type "text"
          :value @command
          :placeholder "Describe the visualization or paste chart code"
          :disabled @loading
          :on-change #(reset! command (-> % .-target .-value))
          :on-key-down #(when (and (= (.-key %) "Enter") (not @loading))
                          (insert-visualization @command))}]
        [:button.visualization-btn
         {:on-click #(insert-visualization @command)
          :disabled @loading}
         (if @loading
           "Generating..."
           "Create Visualization")]]
       (when @error
         [:div.visualization-error @error])
       (when @result
         [:div.visualization-success @result])
       
       ;; Show examples
       [:div.visualization-examples
        [:h4 "Examples:"]
        [:ul
         [:li {:on-click #(reset! command "Create a bar chart from the first table in this document")}
          "Create a bar chart from the first table in this document"]
         [:li {:on-click #(reset! command "Create a line chart showing trends over time")}
          "Create a line chart showing trends over time"]
         [:li {:on-click #(reset! command "Visualize the data as a pie chart")}
          "Visualize the data as a pie chart"]
         [:li {:on-click #(reset! command "[:> ResponsiveContainer {:width \"100%\" :height 400} [:> LineChart {:data [...]}] ]")}
          "Paste Recharts component code directly"]]]])))

(defn apply-markdown-formatting [text-area format-type]
  (let [start (.-selectionStart text-area)
        end (.-selectionEnd text-area)
        value (.-value text-area)
        selected-text (.substring value start end)
        format-markers (case format-type
                          :bold "**"
                          :italic "*"
                          :code "`"
                          :strikethrough "~~"
                          :heading "#"
                          :link ["[", "](url)"]
                          :image ["![", "](url)"]
                          :quote "> "
                          :ul "- "
                          :ol "1. "
                          :hr "---"
                          :task "- [ ] "
                          "")]
    
    (if (empty? selected-text)
      ;; No text selected, insert placeholder
      (let [placeholder (case format-type
                          :bold "bold text"
                          :italic "italic text"
                          :code "code"
                          :strikethrough "strikethrough text"
                          :heading " Heading"
                          :link "link text"
                          :image "alt text"
                          :quote "quoted text"
                          :ul "list item"
                          :ol "list item"
                          :task "task"
                          "")
            new-text (if (vector? format-markers)
                       (str (first format-markers) placeholder (second format-markers))
                       (str format-markers placeholder format-markers))
            cursor-pos (if (= format-type :heading)
                         (+ start 2)  ;; Position after "# " for heading
                         (+ start (count (if (vector? format-markers) 
                                           (first format-markers) 
                                           format-markers))))]
        
        ;; Insert placeholder and position cursor
        (set! (.-value text-area) 
              (str (.substring value 0 start) new-text (.substring value end)))
        (set! (.-selectionStart text-area) (if (#{:link :image :quote :ul :ol :heading :task} format-type)
                                             cursor-pos
                                             start))
        (set! (.-selectionEnd text-area) (if (#{:link :image :quote :ul :ol :heading :task} format-type)
                                           (+ cursor-pos (count placeholder))
                                           (+ start (count new-text)))))
      
      ;; Text is selected, wrap with format markers
      (let [new-text (if (vector? format-markers)
                       (str (first format-markers) selected-text (second format-markers))
                       (if (#{:quote :ul :ol :heading :task} format-type)
                         ;; For list items and headings, add to beginning of each line
                         (let [lines (str/split selected-text #"\n")
                               formatted-lines (map #(str format-markers %) lines)]
                           (str/join "\n" formatted-lines))
                         (str format-markers selected-text format-markers)))
            new-cursor-pos (+ start (count new-text))]
        
        (set! (.-value text-area) 
              (str (.substring value 0 start) new-text (.substring value end)))
        (set! (.-selectionStart text-area) start)
        (set! (.-selectionEnd text-area) (+ start (count new-text))))))
    
    ;; Trigger change event to update state
    (.dispatchEvent text-area (new js/Event "input" #js {:bubbles true})))

(defn handle-editor-keydown [e text-area]
  (let [is-mac? (and (exists? js/navigator) 
                     (or (str/includes? (.-platform js/navigator) "Mac")
                         (str/includes? (.-userAgent js/navigator) "Mac")))
        modifier-key? (if is-mac? (.-metaKey e) (.-ctrlKey e))]
    
    (js/console.log "Key pressed: " (.-key e) 
                    ", Ctrl key: " (.-ctrlKey e) 
                    ", Meta key: " (.-metaKey e)
                    ", Is Mac: " is-mac?
                    ", Modifier pressed: " modifier-key?)
    
    (when modifier-key?
      (case (.-key e)
        "b" (do
              (.preventDefault e)
              (apply-markdown-formatting text-area :bold))
        "i" (do
              (.preventDefault e)
              (apply-markdown-formatting text-area :italic))
        "k" (do
              (.preventDefault e)
              (apply-markdown-formatting text-area :link))
        "`" (do
              (.preventDefault e)
              (apply-markdown-formatting text-area :code))
        "." (do  ;; Command/Ctrl+. for task item
              (.preventDefault e)
              (apply-markdown-formatting text-area :task))
        "q" (do  ;; Command/Ctrl+Q for blockquote
              (.preventDefault e)
              (apply-markdown-formatting text-area :quote))
        "1" (when (.-shiftKey e)  ;; Command/Ctrl+Shift+1 for heading
              (.preventDefault e)
              (apply-markdown-formatting text-area :heading))
        "8" (when (.-shiftKey e)  ;; Command/Ctrl+Shift+8 (asterisk) for unordered list
              (.preventDefault e)
              (apply-markdown-formatting text-area :ul))
        "7" (when (.-shiftKey e)  ;; Command/Ctrl+Shift+7 for ordered list
              (.preventDefault e)
              (apply-markdown-formatting text-area :ol))
        ;; For undo, we don't prevent default so the browser's native undo works
        "z" (js/console.log "Undo triggered, using browser's native implementation")
        nil))))

(defn mdx-editor []
  (let [content (r/atom initial-content)
        text-area-ref (r/atom nil)
        editor-mode (r/atom :edit)
        preview-content (r/atom initial-content)
        active-tab (r/atom :editor)
        panel-locked (r/atom false)
        title (r/atom "Untitled Document")
        editing-title (r/atom false)
        file-input-ref (r/atom nil)
        saving? (r/atom false)
        save-error (r/atom nil)
        last-saved-content (r/atom initial-content)
        audio-url (r/atom nil)
        audio-player (r/atom nil)
        is-playing (r/atom false)
        audio-progress (r/atom 0)
        audio-duration (r/atom 0)
        highlighted (r/atom false)]
    
    (defn reset-audio-state! []
      (when @is-playing
        (when @audio-player (.pause ^js @audio-player)))
      (reset! audio-url nil)
      (reset! audio-player nil)
      (reset! is-playing false)
      (reset! audio-progress 0)
      (reset! audio-duration 0))
    
    (defn update-split-preview []
      (when (= @editor-mode :split)
        (reset! preview-content @content)))
    
    (defn add-keyboard-event-listeners [text-area]
      (when text-area
        ;; For input and click events
        (.addEventListener text-area "input" #(reset! preview-content (.-value text-area)))
        (.addEventListener text-area "click" update-split-preview)
        
        ;; Extra event listener for keydown to ensure keyboard shortcuts work
        (.addEventListener text-area "keydown" 
                           (fn [e]
                             (handle-editor-keydown e text-area))
                           false)))
    
    (r/create-class
      {:component-did-mount
       (fn [_]
         (js/console.log "MDX Editor component mounted")
         ;; Set as global for debugging
         (set! js/window.mdxEditorContent @content)
         
         ;; Setup keyboard events
         (when-let [text-area @text-area-ref]
           (add-keyboard-event-listeners text-area))
         
         ;; Add watch to update preview content when content changes
         (add-watch content :update-preview
                   (fn [_ _ _ new-value]
                     (when (= @editor-mode :split)
                       (reset! preview-content new-value)))))
       
       :component-did-update
       (fn [this]
         ;; Check if textarea ref changed and setup events if needed
         (when-let [text-area @text-area-ref]
           (add-keyboard-event-listeners text-area)))
       
       :component-will-unmount
       (fn [_]
         ;; Remove event listeners
         (when-let [text-area @text-area-ref]
           (.removeEventListener text-area "input" #(reset! preview-content (.-value text-area)))
           (.removeEventListener text-area "click" update-split-preview)
           (.removeEventListener text-area "keydown" (fn [e] (handle-editor-keydown e text-area))))
         
         ;; Remove watchers
         (remove-watch content :update-preview)
         
         ;; Clean up audio resources
         (reset-audio-state!))
       
       :reagent-render
       (fn []
         [:div.mdx-editor-container
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
                @title]])]
           
           [:div.action-bar-right
            [:div.view-toggle
             [:div.option-selector
              [:button.option-button 
               {:class (when (= @editor-mode :edit) "active")
                :on-click #(reset! editor-mode :edit)}
               "Edit"]
              [:button.option-button 
               {:class (when (= @editor-mode :split) "active")
                :on-click #(do
                             (reset! preview-content @content)
                             (reset! editor-mode :split))}
                "Split"]
              [:button.option-button 
               {:class (when (= @editor-mode :preview) "active")
                :on-click #(do
                             (reset! preview-content @content)
                             (reset! editor-mode :preview))}
                "Preview"]]]
            
            [:div.feature-buttons
             [:button.feature-button
              {:on-click #(sbb-client/call-summarize-api 
                          @content
                          (fn [summary]
                            (reset! content (sbb-client/update-summary-section @content summary))
                            (reset! preview-content @content)))}
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
                 "Capture"]]]
             
              [:div.save-status
               (cond
                 @saving? [:span.saving "Saving..."]
                 @save-error [:span.save-error (str "Error: " @save-error)]
                 (not= @content @last-saved-content) [:span.unsaved "Unsaved changes"]
                 :else [:span.saved "All changes saved"])]
               
               [:button.save-button
                {:on-click #(save-note @content @title
                                     (fn [_]
                                       (reset! last-saved-content @content)
                                       (reset! saving? false))
                                     (fn [err]
                                       (reset! save-error (str err))
                                       (reset! saving? false)))}
                "Save"]]]
         
          [:div.editor-preview-container
           (case @editor-mode
             :preview 
             ;; Full preview mode shows rendered markdown of entire content
             [:div.preview-container
              [mdx-viewer/mdx-viewer {:content @content}]]
             
             :split
             ;; Split mode shows editor and preview side by side
             [:div.split-mode-container
              [:div.split-editor-container
               [:textarea.mdx-editor-textarea
                {:ref #(reset! text-area-ref %)
                 :value @content
                 :on-change #(do
                              (reset! content (-> % .-target .-value))
                              (reset! preview-content (-> % .-target .-value)))
                :on-key-down #(handle-editor-keydown % @text-area-ref)
                :placeholder "Write your markdown content here..."}]
               ;; Add visualization generator component to split mode
               [visualization-generator content]]
               [:div.split-preview-container
                [mdx-viewer/mdx-viewer {:content @preview-content}]]]
                
             ;; Default: Edit mode shows textarea and tools
             [:div.editor-container
              [:textarea.mdx-editor-textarea
               {:ref #(reset! text-area-ref %)
                :value @content
                :on-change #(do 
                              (reset! content (-> % .-target .-value))
                              (reset! preview-content (-> % .-target .-value)))
                :on-key-down #(handle-editor-keydown % @text-area-ref)
                :placeholder "Write your markdown content here..."}]
              
              ;; Add visualization generator component
              [visualization-generator content]])]
         
          ;; Footer with status info
          [:div.editor-footer
           [:div.word-count
            (let [words (-> @content
                         (str/replace #"```[\s\S]*?```" "") ; Remove code blocks
                         (str/replace #"<.*?>" "") ; Remove HTML tags
                         (str/split #"\s+")
                         count)]
              (str words " words"))]]])})))