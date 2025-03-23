(ns second-brain-studio-webapp.db)

(def default-db
  {:name "re-frame"
   :user nil  ;; Add user field to store authentication state
   :active-panel :mdx-panel  ;; Default to the MDX panel
   })
