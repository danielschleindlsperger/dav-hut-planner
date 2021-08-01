(ns dav-hut-planner.web
  (:require [clojure.string :as str]
            [org.httpkit.server :refer [run-server]]
            [reitit.ring :as ring]
            [ring.middleware.params :as params]
            [ring.util.response :as response]
            [ring.middleware.defaults :as ring-defaults]
            [taoensso.timbre :as log]
            [hiccup.page :as page]
            [hiccup.core :as hiccup]
            [hiccup.form :as form]
            [dav-hut-planner.hut_tour_planner :as planner]
            [dav-hut-planner.huts :refer [get-huts!]])
  (:import (java.time LocalDate)
           (java.time.format DateTimeFormatter)))

(def date-fmt (DateTimeFormatter/ofPattern "dd.MM."))
(defn- fmt-date [^LocalDate date]
  (.format date date-fmt))
(defn- fmt-hut-name [s]
  (first (str/split s #",")))

(defn page-container [& body]
  (page/html5
    [:head
     [:title "DAV Hut Planner"]
     (page/include-css "https://unpkg.com/sanitize.css"
                       "https://unpkg.com/sanitize.css/typography.css"
                       "https://unpkg.com/sanitize.css/forms.css"
                       "/css/nice-select2.css"
                       ;"/css/style.css"
                       )]
    [:body {:style "max-width: 64rem; margin: 2rem auto; padding: 0 2rem;"} body
     (page/include-js "/js/nice-select2.js")
     (page/include-js "/js/app.js")]))

(defn planner-form [{:keys [first-start-date last-start-date head-count stops huts]}]
  (form/form-to
    {:id "planner-form" :style "display: grid; grid-gap: 2rem;"}
    [:get "/plan-tour"]
    [:fieldset {:style "display: grid; grid-gap: 1rem;"}
     [:legend "Dates"]
     [:label "First start date"
      [:input {:type "date" :name "firstStartDate" :value first-start-date}]]
     [:label "Last start date"
      [:input {:type "date" :name "lastStartDate" :value last-start-date}]]]
    [:fieldset {:style "display: grid; grid-gap: 1rem;"}
     [:legend "Headcount"]
     [:label "Number of beds needed"
      [:input {:type "number" :name "headCount" :value head-count}]]]
    [:fieldset {:id "tour-stops" :style "display: grid; grid-gap: 1rem;"}
     [:legend "Tour stops"]
     (for [stop stops]
       [:div
        (form/drop-down {} "stops" (for [{:keys [dav-hut-id hut-name]} huts]
                                     [hut-name dav-hut-id]) stop)
        [:button {:data-remove-stop true} "X"]])
     [:button {:id "add-stop"} "Add stop"]]

    (form/submit-button "Go")))

(defn plan-tour-handler [req]
  (let [first-start-date (LocalDate/parse (get-in req [:query-params "firstStartDate"]))
        last-start-date (LocalDate/parse (get-in req [:query-params "lastStartDate"]))
        head-count (Integer/parseInt (or (get-in req [:query-params "headCount"]) "3"))
        stops (map #(Integer/parseInt %) (flatten [(get-in req [:query-params "stops"])]))
        huts @(get-huts!)
        tours (planner/tour-possible-dates! stops
                                            first-start-date
                                            last-start-date
                                            head-count)
        html (hiccup/html (page-container
                            [:h1 "Tour Planner"]
                            (planner-form {:first-start-date first-start-date
                                           :last-start-date  last-start-date
                                           :head-count       head-count
                                           :huts             huts
                                           :stops            stops})
                            [:h2 "Possible Tour Dates"]
                            (let [hut-names-by-id (reduce #(assoc %1 (:dav-hut-id %2) (:hut-name %2)) {} huts)
                                  stops (map :dav-hut-id (first tours))]
                              [:table {:style "border-collapse: collapse;"}
                               [:thead
                                [:tr (for [stop stops]
                                       [:th {:style "padding: 8px;"}
                                        (fmt-hut-name (get hut-names-by-id stop "HUT_NAME_NOT_FOUND"))])]]
                               [:tbody (for [tour tours]
                                         [:tr (for [stop tour]
                                                [:td {:style "padding: 8px;"} (fmt-date (:date stop))])])]])))]
    (-> html (response/response) (response/content-type "text/html"))))

(defn configure-tour-planner-handler [_req]
  (let [first-start-date (LocalDate/now)
        last-start-date (.plusDays (LocalDate/now) 14)
        huts @(get-huts!)
        html (hiccup/html (page-container
                            [:h1 "Tour Planner"]
                            (planner-form {:first-start-date first-start-date
                                           :last-start-date  last-start-date
                                           :huts             huts
                                           :stops            [145 144]
                                           :head-count       3})))]
    (-> html (response/response) (response/content-type "text/html"))))

(def default-handler
  (ring/create-default-handler
    {:not-found          (constantly {:status 404 :body "not found"})
     :method-not-allowed (constantly {:status 405 :body "method not allowed"})
     :not-acceptable     (constantly {:status 406 :body "not acceptable"})}))

(def handler
  (ring/ring-handler (ring/router [["/" {:get configure-tour-planner-handler}]
                                   ["/plan-tour" {:get plan-tour-handler}]])
                     default-handler
                     {:middleware [[params/wrap-params]
                                   [ring-defaults/wrap-defaults ring-defaults/site-defaults]]}))

(defonce server (atom nil))

(defn start-server []
  (let [port (Integer/parseInt (or (System/getenv "PORT") "5678"))]
    (reset! server (run-server #'handler {:port port}))
    (log/debug (format "Listening @ http://localhost:%s" port))))

(defn stop-server []
  (when-not (nil? @server)
    (@server)
    (reset! server nil)))

(defn restart-server []
  (stop-server)
  (start-server))

(comment
  (start-server))