(ns dav-hut-planner.util
  (:require [org.httpkit.client :as http]
            [promesa.core :as p]))

(defn http-get
  "Wraps http-kit.client/get in a promesa promise."
  [url opts]
  (p/create (fn [resolve reject]
              (http/get url opts
                        (fn [{:keys [error] :as resp}]
                          (if error (reject error) (resolve resp)))))))
