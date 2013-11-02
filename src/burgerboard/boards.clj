(ns burgerboard.boards
  )

(defn create-board [name group]
  {:name name
   :group group}
  )

(defn group-boards [boards]
  (reduce
   (fn [groups [group boards]]
     (cons
      (assoc group
        :boards (map
                 (fn [board]
                   (dissoc board :group))
                 boards))
      groups))
   []
   (group-by :group
             boards)
   )
  )

(defn create-store [name board]
  {:name name
   :board board}
  )

(defn tally-store [store]
  (let [provided-ratings (filter
                          (fn [rating]
                            (not (nil? rating)))
                          (map :rating (:ratings store)))
        ratings-count (count provided-ratings)]
    (assoc store
      :rating
      (if (> ratings-count 0)
        (double (/ (reduce + provided-ratings) ratings-count))
        nil
        )
      )
    )
  )

(defn tally-board [stores board]
  (assoc board
    :stores
    (map
     tally-store
     stores)
    )
  )