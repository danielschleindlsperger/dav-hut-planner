(ns dav-hut-planner.hut_tour_planner
  (:require [clojure.string :as str]
            [jsonista.core :as json]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [promesa.core :as p]
            [dav-hut-planner.util :refer [http-get]])
  (:import (java.time LocalDate)
           (java.time.format DateTimeFormatter)))

(def dav-date-fmt (DateTimeFormatter/ofPattern "dd.MM.uuuu"))
(def ^:private json-kebab-keyword-mapper
  (json/object-mapper {:decode-key-fn ->kebab-case-keyword}))
(def ^:private days-per-request 14)                         ;; arbitrary number of days that the DAV API returns per request. Makes it easier to parallelize.

(defn- parse-cookies [resp]
  (let [pairs (-> resp :headers :set-cookie (str/split #","))
        key-vals (->> pairs
                      (map #(str/split % #"; "))
                      (map first)
                      (mapcat #(str/split % #"=")))]
    (apply hash-map key-vals)))

(defn str->local-date [s] (LocalDate/parse s dav-date-fmt))

(defn set-hut-session!
  "Visit the webpage for a hut. This will create a session on the server.
   Returns the cookies that need to be included in the subsequent requests in order for the hut to be in scope of the request."
  [hut-id]
  (let [cookies (parse-cookies @(http-get "https://www.alpsonline.org/reservation/calendar"
                                          {:query-params {"hut_id" hut-id
                                                          "lang"   "de_DE"}}))
        cookie-header (clojure.string/join ";" (map (fn [[k v]] (str k "=" v)) cookies))]
    cookie-header))

(defn request-dates [^LocalDate initial-date]
  (iterate #(.plusDays % days-per-request) initial-date))

(defn get-hut-data
  [cookie-header ^LocalDate start-date]
  (-> (http-get "https://www.alpsonline.org/reservation/selectDate"
                {:query-params {"date" (.format dav-date-fmt start-date)}
                 :headers      {"cookie" cookie-header}})

      (p/then :body)
      (p/then #(json/read-value % json-kebab-keyword-mapper))
      (p/then (fn [json-body]
                (->> json-body
                     (mapcat (fn [[_ on-date]] on-date))
                     (map #(update % :reservation-date str->local-date))
                     (map #(select-keys % [:reservation-date :free-room :closed :bed-category-id :total-room :booking-enabled])))))))

(defn hut-data [hut-id ^LocalDate start-date ^LocalDate end-date]
  (let [cookie-header (set-hut-session! hut-id)
        dates (doall (take-while #(.isBefore % end-date) (request-dates start-date)))
        results (->> (p/all (map #(get-hut-data cookie-header %) dates))
                     deref
                     flatten
                     (group-by :reservation-date))]
    results))

(defn room-available-on-date? [categories-on-date head-count]
  (let [pred (every-pred (complement :closed)
                         :booking-enabled
                         #(<= head-count (:free-room %)))]
    (some pred categories-on-date)))

(defn tour-possible-dates! [tour-stages ^LocalDate first-start-date ^LocalDate last-start-date head-count]
  (let [hut-reservations (reduce (fn [acc dav-hut-id]
                                   (assoc acc dav-hut-id (hut-data dav-hut-id first-start-date last-start-date)))
                                 {}
                                 (set tour-stages))
        initial-tour (map-indexed (fn [idx stage] {:dav-hut-id stage :date (.plusDays first-start-date idx)}) tour-stages)
        all-tours (iterate (fn [tour] (map (fn [stage] (update stage :date #(.plusDays % 1))) tour)) initial-tour)
        tours-in-time-scope (take-while #(.isBefore (-> % first :date) last-start-date) all-tours)]
    (->> tours-in-time-scope
         (map (fn [tour] (map #(let [beds (get-in hut-reservations [(:dav-hut-id %) (:date %)])]
                                 (merge % {:beds            beds
                                           :room-available? (room-available-on-date? beds head-count)}))
                              tour)))
         (filter (fn [tour] (every? :room-available? tour))))))

(comment
  (def karwendelhaus "87")
  (def lamsenjochhuette "437")
  (def falkenhuette "354")
  (def riemannhaus "145")
  (def ingolstaedterhaus "144")

  (tour-possible-dates! [riemannhaus ingolstaedterhaus]
                        (LocalDate/parse "2021-08-24")
                        (LocalDate/parse "2021-11-01")
                        5)
  )


