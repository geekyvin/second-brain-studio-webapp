(ns second-brain-studio-webapp.test-runner
  (:require [cljs.test :refer-macros [run-tests]]
            [second-brain-studio-webapp.markdown-editor-test]))

(defn run-all-tests []
  (run-tests 'second-brain-studio-webapp.markdown-editor-test))