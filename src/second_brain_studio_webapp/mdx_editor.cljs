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

(defn mdx-editor []
  (let [content (r/atom initial-content)
        title (r/atom "Untitled MDX Document")
        editing-title (r/atom false)
        file-input-ref (r/atom nil)
        is-preview-mode (r/atom true)
        last-saved-content (r/atom initial-content)
        saving? (r/atom false)
        save-error (r/atom nil)]
    
    (r/create-class
      {:component-did-mount
       (fn [_]
         (js/console.log "MDX Editor component mounted")
         ;; Set as global for debugging
         (set! js/window.mdxEditorContent @content))
       
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
             [:button.view-toggle-btn 
              {:class (when-not @is-preview-mode "active")
               :on-click #(reset! is-preview-mode false)}
              "Edit"]
             [:button.view-toggle-btn 
              {:class (when @is-preview-mode "active")
               :on-click #(reset! is-preview-mode true)}
              "Preview"]]
            
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
           (if @is-preview-mode
             ;; Preview mode shows rendered markdown
             [:div.preview-container
              [mdx-viewer/mdx-viewer {:content @content}]]
             
             ;; Edit mode shows textarea and tools
             [:div.editor-container
              [:textarea.mdx-editor-textarea
               {:value @content
                :on-change #(reset! content (-> % .-target .-value))
                :placeholder "Write your markdown content here..."}]
              
              [:div.editor-tools
               [:div.tool-section
                [:h3 "Insert"]
                [:div.tool-buttons
                 [:div
                  [:input.hidden
                   {:type "file"
                    :ref #(reset! file-input-ref %)
                    :accept ".md,.txt,image/*"
                    :on-change #(when-let [file (-> % .-target .-files (aget 0))]
                                 (handle-file-upload file content))}]
                  [:button.tool-btn
                   {:on-click #(when-let [input @file-input-ref]
                               (.click input))}
                   "📎 File/Image"]]
                 
                 [:button.tool-btn
                  {:on-click #(swap! content str "\n```\n\n```\n")}
                  "💻 Code Block"]
                 
                 [:button.tool-btn
                  {:on-click #(swap! content str "\n| Header 1 | Header 2 |\n| -------- | -------- |\n| Cell 1   | Cell 2   |\n")}
                  "📊 Table"]]]
               
               ;; Replace the simple visualization input with our enhanced visualization generator
               [visualization-generator content]]])]
         
          ;; Footer with status info
          [:div.editor-footer
           [:div.word-count
            (let [words (-> @content
                         (str/replace #"```[\s\S]*?```" "") ; Remove code blocks
                         (str/replace #"<.*?>" "") ; Remove HTML tags
                         (str/split #"\s+")
                         count)]
              (str words " words"))]]])})))