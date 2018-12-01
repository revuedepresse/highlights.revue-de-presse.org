(ns repository.status
  (:require [korma.core :as db]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log])
  (:use [korma.db]
        [utils.string]
        [twitter.status-hash]))

(declare status)

(defn get-status-model
  [connection]
  (db/defentity status
                (db/pk :ust_id)
                (db/table :weaving_status)
                (db/database connection)
                (db/entity-fields
                  :ust_id
                  :ust_hash                 ; sha1(str ust_text  ust_status_id)
                  :ust_text
                  :ust_full_name            ; twitter user screen name
                  :ust_name                 ; twitter user full name
                  :ust_access_token
                  :ust_api_document
                  :ust_created_at
                  :ust_status_id))
  status)

(defn select-statuses
  [model]
  (->
    (db/select* model)
    (db/fields [:ust_id :id]
               [:ust_hash :hash]
               [:ust_text :text]
               [:ust_full_name :screen-name]
               [:ust_name :name]
               [:ust_access_token :access-token]
               [:ust_api_document :document]
               [:ust_created_at :created-at]
               [:ust_status_id :twitter-id])))

(defn find-statuses-having-column-matching-values
  "Find statuses which values of a given column
  can be found in collection passed as argument"
  [column values model]
  (let [values (if values values '(0))
        matching-statuses (-> (select-statuses model)
                              (db/where {column [in values]})
                              (db/group :ust_status_id)
                              (db/select))]
    (if matching-statuses
      matching-statuses
      '())))

(defn find-statuses-having-twitter-ids
  "Find statuses by their Twitter ids"
  [twitter-ids model]
  (find-statuses-having-column-matching-values :ust_status_id twitter-ids model))

(defn find-statuses-having-ids
  "Find statuses by their ids"
  [ids model]
  (find-statuses-having-column-matching-values :ust_id ids model))

(defn insert-values-before-selecting-from-ids
  [values twitter-ids model]
  (if (pos? (count twitter-ids))
    (do
      (try
        (db/insert model (db/values values))
        (catch Exception e (log/error (.getMessage e))))
      (find-statuses-having-twitter-ids twitter-ids model))
    '()))

(defn assoc-avatar
  [status]
  (let [raw-document (:ust_api_document status)
        decoded-document (json/read-str raw-document)
        user (get decoded-document "user")
        avatar (get user "profile_image_url_https")]
  (assoc status :ust_avatar avatar)))

(defn is-subset-of
  [statuses-set]
  (fn [status]
    (let [status-id (:twitter-id status)]
     (clojure.set/subset? #{status-id} statuses-set))))

(defn bulk-unarchive-statuses
 [statuses model]
 (let [statuses-twitter-ids (map #(:twitter-id %) statuses)
       existing-statuses (find-statuses-having-twitter-ids statuses-twitter-ids model)
       existing-statuses-twitter-ids (map #(:twitter-id %) existing-statuses)
       filtered-statuses (doall (remove (is-subset-of (set existing-statuses-twitter-ids)) statuses))
       statuses-props (map #(dissoc %
                                   :id
                                   :ust_id
                                   :screen-name
                                   :access-token
                                   :document
                                   :name
                                   :text
                                   :hash
                                   :created-at
                                   :twitter-id) filtered-statuses)
       statuses-props (map assoc-avatar statuses-props)
       deduped-statuses (dedupe (sort-by #(:status_id %) statuses-props))
       twitter-ids (map #(:ust_status_id %) deduped-statuses)
       new-statuses (insert-values-before-selecting-from-ids deduped-statuses twitter-ids model)]
   (if (pos? (count new-statuses))
     new-statuses
     existing-statuses)))

(defn bulk-insert-new-statuses
  [statuses model]
  (let [snake-cased-values (map snake-case-keys statuses)
        statuses-values (map assoc-hash snake-cased-values)
        deduped-statuses (dedupe (sort-by #(:status_id %) statuses-values))
        prefixed-keys-values (map prefixed-keys deduped-statuses)
        twitter-ids (map #(:ust_status_id %) prefixed-keys-values)]
    (insert-values-before-selecting-from-ids prefixed-keys-values twitter-ids model)))
