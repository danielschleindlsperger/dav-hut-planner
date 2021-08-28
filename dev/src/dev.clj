(ns dev
  (:require [dav-hut-planner.web :refer [restart-server]]))

(defn restart []
  (restart-server))

;; run once initially when jacking in
(restart)