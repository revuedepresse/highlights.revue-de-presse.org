(ns repository.database-schema)

(defn get-column
  [column-name model]
  (keyword (str (:table model) "." column-name)))
