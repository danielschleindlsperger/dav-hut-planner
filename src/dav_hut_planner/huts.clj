(ns dav-hut-planner.huts
  (:require [clojure.set :refer [rename-keys]]
            [promesa.core :as p]
            [jsonista.core :as jsonista]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [dav-hut-planner.util :refer [http-get]]))


(def json-mapper (jsonista/object-mapper {:decode-key-fn ->kebab-case-keyword}))

(defn get-huts! []
  (-> (http-get "https://sedlatschek.github.io/dav-hut-extractor/huts/index.json" {})
      (p/then :body)
      (p/then #(jsonista/read-value % json-mapper))
      (p/then :huts)
      (p/then (fn [huts] (->> huts
                              (remove #(-> % :name empty?))
                              (map #(rename-keys % {:id :dav-hut-id :name :hut-name})))))))

(comment
  @(get-huts!))
