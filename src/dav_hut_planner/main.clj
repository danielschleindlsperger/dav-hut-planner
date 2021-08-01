(ns dav-hut-planner.main
  (:gen-class)
  (:require [dav-hut-planner.web :refer [start-server]]))

(defn -main [& _args]
  (start-server))