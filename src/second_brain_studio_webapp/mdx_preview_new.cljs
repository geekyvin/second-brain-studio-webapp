(ns second-brain-studio-webapp.mdx-preview-new
  (:require
   [reagent.core :as r]
   [clojure.string :as str]
   [second-brain-studio-webapp.ui-generator :as ui-generator]
   ["markdown-it" :as MarkdownIt]))

;; Initialize markdown-it parser with table support for text parts
(def md-parser
  (-> (MarkdownIt. #js {:html true
                        :linkify true
                        :typographer true
                        :breaks true})
      (.enable "table") 
      (.use (fn [md]
              ;; Add custom table attributes for better styling
              (set! (.. md -renderer -rules -table_open)
                    (fn [tokens idx options env]
                      "<table class=\"mdx-table\">"))
              md))))

(defn extract-visual-code [content]
  (let [pattern #"<VisualComponent code=\"([^\"]+)\" />"]
    (when-let [matches (re-seq pattern content)]
      (map (fn [[_ encoded-code]]
             (try
               (js/decodeURIComponent encoded-code)
               (catch :default _
                 encoded-code)))
           matches))))

(defn process-mdx-content
  "Process MDX content with embedded visualizations"
  [content]
  (if (and content (str/includes? content "<VisualComponent"))
    (let [parts (str/split content #"<VisualComponent code=\"[^\"]+\" />")
          visual-codes (extract-visual-code content)
          combined (interleave parts (concat visual-codes (repeat nil)))]
      [:div.mdx-content
       [:style ".mdx-table { border-collapse: collapse; width: 100%; margin: 1em 0; }
               .mdx-table th, .mdx-table td { border: 1px solid #ddd; padding: 8px; text-align: left; }
               .mdx-table th { background-color: #f2f2f2; font-weight: bold; }
               .mdx-table tr:nth-child(even) { background-color: #f9f9f9; }
               .mdx-table tr:hover { background-color: #f5f5f5; }
               .mdx-text-part table { border-collapse: collapse; width: 100%; margin: 1em 0; }
               .mdx-text-part th, .mdx-text-part td { border: 1px solid #ddd; padding: 8px; text-align: left; }
               .mdx-text-part th { background-color: #f2f2f2; font-weight: bold; }
               .mdx-text-part tr:nth-child(even) { background-color: #f9f9f9; }
               .mdx-text-part tr:hover { background-color: #f5f5f5; }"]
       (for [[i part] (map-indexed vector combined)]
         (if (even? i)
           ;; Regular text content - use markdown-it to render
           [:div.mdx-text-part {:key (str "text-" i)
                                :dangerouslySetInnerHTML 
                                {:__html (.render md-parser (or part ""))}}]
           ;; Visual component
           (when part
             [:div.mdx-visual-part {:key (str "visual-" i)}
              (try
                (let [parsed-code (ui-generator/parse-hiccup part)
                      resolved-comp (ui-generator/resolve-components parsed-code)]
                  resolved-comp)
                (catch :default e
                  [:div.visual-error
                   [:p "Error rendering visual: " (str e)]
                   [:pre part]]))])))
       [:div.mdx-end]])
    
    ;; If no visual components, render markdown directly
    [:div.mdx-content-plain
     [:style ".mdx-content-plain table { border-collapse: collapse; width: 100%; margin: 1em 0; }
             .mdx-content-plain th, .mdx-content-plain td { border: 1px solid #ddd; padding: 8px; text-align: left; }
             .mdx-content-plain th { background-color: #f2f2f2; font-weight: bold; }
             .mdx-content-plain tr:nth-child(even) { background-color: #f9f9f9; }
             .mdx-content-plain tr:hover { background-color: #f5f5f5; }"]
     [:div {:dangerouslySetInnerHTML 
            {:__html (.render md-parser (or content ""))}}]]))

(defn mdx-preview
  "Component to preview MDX content with embedded visualizations"
  [props]
  (let [{:keys [content class-name]} props]
    [:div.mdx-preview {:class class-name}
     [process-mdx-content content]]))