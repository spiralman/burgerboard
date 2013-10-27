(ns burgerboard.boards
  )

(defn create-board [name group]
  {:name name
   :group group}
  )

(defn create-store [name board]
  {:name name
   :board board}
  )

(defn tally-board [stores board]
  (assoc board
    :stores
    (map
     (fn [store]
       (let [provided-ratings (filter
                               (fn [rating]
                                 (not (nil? rating)))
                               (map :rating (:ratings store)))
             ratings-count (count provided-ratings)]
         (if (> ratings-count 0)
           (assoc store
             :rating (double (/ (reduce + provided-ratings) ratings-count)))
           )
         )
       )
     stores)
    )
  )