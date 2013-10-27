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