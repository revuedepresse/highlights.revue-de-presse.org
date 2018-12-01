(ns command.unarchive-statuses
  (:require [clojure.tools.logging :as log]
            [clojure.edn :as edn]
            [environ.core :refer [env]])
  (:use [repository.entity-manager]
        [repository.aggregate]
        [repository.status]
        [repository.timely-status]
        [twitter.status]
        [command.generate-timely-statuses]))

(defn unarchive-statuses
  [week year]
  (let [press-aggregate-name (:press (edn/read-string (:aggregate env)))
        db-read-params {:models (get-entity-manager (:database-archive env))}
        db-write-params {:models (get-entity-manager (:database env))}
        archived-status-model (:archived-status (:models db-read-params))
        read-aggregate-model (:aggregate (:models db-read-params))
        write-status-aggregate-model (:status-aggregate (:models db-write-params))
        write-status-model (:status (:models db-write-params))
        aggregate (first (find-aggregate-by-name press-aggregate-name read-aggregate-model))
        {:keys [statuses-ids total-timely-statuses]} (get-timely-statuses-for-aggregate
                                                       press-aggregate-name
                                                       week
                                                       year
                                                       :are-archived)
        matching-archived-statuses (find-statuses-having-ids statuses-ids archived-status-model)
        new-statuses (bulk-unarchive-statuses matching-archived-statuses write-status-model)
        {total-new-relationships :total-new-relationships} (new-relationships
                                                             aggregate
                                                             new-statuses
                                                             write-status-aggregate-model
                                                             write-status-model)
        total-new-statuses (count new-statuses)]
    (log-new-relationships-between-aggregate-and-statuses
      total-new-relationships
      total-new-statuses
      press-aggregate-name)
    (log/info (str total-timely-statuses " archived statuses have been found."))))