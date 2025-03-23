(ns second-brain-studio-webapp.mdx-preview-new
  (:require
   [reagent.core :as r]
   [clojure.string :as str]
   [second-brain-studio-webapp.ui-generator :as ui-generator]))

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
       (for [[i part] (map-indexed vector combined)]
         (if (even? i)
           ;; Regular text content
           [:div.mdx-text-part {:key (str "text-" i)
                                :dangerouslySetInnerHTML {:__html part}}]
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
    
    ;; Return nil if no visual components found
    nil))

(defn mdx-preview
  "Component to preview MDX content with embedded visualizations"
  [props]
  (let [{:keys [content class-name]} props]
    [:div.mdx-preview {:class class-name}
     [process-mdx-content content]]))