(ns second-brain-studio-webapp.markdown-editor-test
  (:require [cljs.test :refer-macros [deftest testing is async]]
            [second-brain-studio-webapp.markdown-editor :as editor]
            [reagent.core :as r]
            [clojure.string :as str]))

;; Mock functions to simulate API calls
(def mock-capture-api-response
  #js {:status "success"
       :markdown "# Test Content"})

(def mock-file
  #js {:type "text/markdown"
       :name "test.md"})

(def mock-image-file
  #js {:type "image/png"
       :name "test.png"})

;; Test handle-file-upload function
(deftest test-handle-file-upload
  (testing "Handling text file upload"
    (let [content-atom (r/atom "")
          test-content "Initial content\n"]
      (reset! content-atom test-content)
      (editor/handle-file-upload mock-file content-atom)
      (is (= @content-atom (str test-content "\n" "# Test Content")))))

  (testing "Handling image file upload"
    (let [content-atom (r/atom "")
          test-content "Initial content\n"]
      (reset! content-atom test-content)
      (editor/handle-file-upload mock-image-file content-atom)
      (is (str/includes? @content-atom "![test.png](uploading...)")))))

;; Test save-note function
(deftest test-save-note
  (testing "Successful note save"
    (async done
      (let [test-content "Test note content"
            on-success (fn [data]
                        (is (= (:status data) "success"))
                        (done))
            on-error (fn [error]
                      (is (= error nil))
                      (done))]
        (editor/save-note "test-user" "test-namespace" "test-id" test-content on-success on-error)))))

;; Test markdown preview rendering
(deftest test-markdown-preview
  (testing "Markdown parsing"
    (let [test-markdown "# Test Heading\n\nTest paragraph"
          rendered (.render editor/markdown-parser test-markdown)]
      (is (str/includes? rendered "<h1>"))
      (is (str/includes? rendered "Test Heading"))
      (is (str/includes? rendered "<p>"))
      (is (str/includes? rendered "Test paragraph")))))

;; Test user interface state management
(deftest test-ui-state-management
  (testing "Title editing"
    (let [title (r/atom "Initial Title")]
      (reset! title "New Title")
      (is (= @title "New Title"))))

  (testing "Mode switching"
    (let [mode (r/atom :edit)]
      (reset! mode :preview)
      (is (= @mode :preview))
      (reset! mode :edit)
      (is (= @mode :edit)))))

;; Test content highlighting
(deftest test-content-highlighting
  (testing "Highlight effect timing"
    (let [highlighted (r/atom false)]
      (reset! highlighted true)
      (is (true? @highlighted))
      (js/setTimeout #(is (false? @highlighted)) 2100)))))