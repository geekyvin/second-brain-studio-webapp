(ns second-brain-studio-webapp.mdx-editor-viewer
  (:require
   [reagent.core :as r]
   [re-frame.core :as re-frame]
   [clojure.string :as str]
   [second-brain-studio-webapp.events :as events]
   [second-brain-studio-webapp.subs :as subs]
   ["markdown-it" :as MarkdownIt]))

;; Initialize the Markdown parser
(def markdown-parser 
  (MarkdownIt. #js {:html true
                    :linkify true
                    :typographer true}))

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

(defn save-note [content on-success on-error]
  (js/console.log "Saving note with content:" content)
  ;; For now, just simulate success
  (js/setTimeout
   (fn []
     (js/console.log "Note saved successfully")
     (when on-success (on-success {:status "success"})))
   500))

(defn mdx-editor-viewer []
  (let [content (r/atom "# Welcome to the MDX Editor\n\nThis is a simple editor for Markdown with support for data visualization.\n\n## Example Data\n\n| Area | 2019 | 2020 | 2021 | 2022 | 2023 |\n|------|------|------|------|------|------|\n| Barton and Sandhills | 54 | 61 | 72 | 64 | 59 |\n| Blackbird Leys | 82 | 79 | 85 | 91 | 88 |\n| Churchill | 35 | 37 | 34 | 39 | 42 |\n| Cowley | 67 | 72 | 75 | 78 | 71 |\n| Headington | 44 | 47 | 53 | 58 | 61 |\n\nCreate a line chart for 5 years excluding Oxford.")
        title (r/atom "Example MDX Document")
        editing-title (r/atom false)
        file-input-ref (r/atom nil)
        is-preview-mode (r/atom true)
        last-saved-content (r/atom "")
        saving? (r/atom false)
        save-error (r/atom nil)]
    
    (r/create-class
     {:component-did-mount
      (fn [_]
        ;; Set initial content for debugging
        (js/console.log "MDX Editor/Viewer component mounted")
        ;; Store for auto-save comparison
        (reset! last-saved-content @content))
      
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
              :else [:span.saved "All changes saved"])]]]
         
         [:div.editor-preview-container
          (if @is-preview-mode
            [:div.mdx-viewer
             {:dangerouslySetInnerHTML 
              {:__html (.render markdown-parser @content)}}]
            
            [:div.editor-container
             [:textarea.mdx-editor-textarea
              {:value @content
               :on-change #(reset! content (-> % .-target .-value))
               :placeholder "Write your markdown content here..."}]
             
             [:div.editor-tools
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
                "📎 Insert"]]
              
              [:button.tool-btn
               {:on-click #(swap! content str "\n```\n\n```\n")}
               "💻 Code Block"]
              
              [:button.tool-btn
               {:on-click #(swap! content str "\n| Header 1 | Header 2 |\n| -------- | -------- |\n| Cell 1   | Cell 2   |\n")}
               "📊 Table"]]])]]))})))