(ns burgerboard.board-handlers
  (:use compojure.core
        [clojure.string :only [join]]
        [clojure.data.json :as json :only [read-str write-str]]
        burgerboard.authentication
        burgerboard.database
        burgerboard.boards
        burgerboard.api
        )
  )

(defn require-board [request handler]
  (require-membership
   request
   (fn [user group request]
     (if-let [board (find-group-board group (:board-id (:params request)))]
       (handler user group board request)
       )
     )
   )
  )

(defn require-store [request handler]
  (require-board
   request
   (fn [user group board request]
     (if-let [store (find-board-store board (:store-id (:params request)))]
       (handler user group board store request)
       )
     )
   )
  )

(defn render-store [request group board store]
  (assoc store
    :rating_url (resolve-route request
                               "groups" (:id group)
                               "boards" (:id board)
                               "stores" (:id store) "rating"))
  )

(defn render-board [request board]
  (->
   (if (contains? board :stores)
     (assoc board
       :stores
       (map (partial render-store request (:group board) board)
            (:stores board)))
     board)
   (assoc
     :url (resolve-route request
                         "groups" (:id (:group board)) "boards" (:id board))
     :stores_url (resolve-route request
                                "groups" (:id (:group board))
                                "boards" (:id board) "stores"))
   )
  )

(defn get-boards [user group request]
  (json-response {} 200)
  )

(defn post-board [user group request]
  (let [name (:name (json/read-str (slurp (:body request))
                                   :key-fn keyword))]
    (->
     (create-board name group)
     (insert-board)
     (merge {:group (select-keys group [:id :name])})
     (->> (render-board request))
     (json-response 201)
     )
    )
  )

(defn get-board [user group board request]
  (->
   (find-board-ratings board)
   (tally-board board)
   (->> (render-board request))
   (json-response 200)
   )
  )

(defn post-store [user group board request]
  (let [name (:name (json/read-str (slurp (:body request))
                                   :key-fn keyword))]
    (->
     (create-store name board)
     (insert-store)
     (->> (render-store request group board))
     (json-response 201)
     )
    )
  )

(defn put-rating [user group board store request]
  (let [rating (int (:rating (json/read-str (slurp (:body request))
                                            :key-fn keyword)))]
    (->
     (set-rating store user rating)
     (tally-store)
     (->> (render-store request group board))
     (json-response 200)
     )
    )
  )

(defroutes board-routes
  (GET "/groups/:group-id/boards/:board-id" request
       (require-board request get-board))
  
  (GET "/groups/:group-id/boards" request
       (require-membership request get-boards))

  (POST "/groups/:group-id/boards" request
        (require-ownership request post-board))

  (POST "/groups/:group-id/boards/:board-id/stores" request
        (require-board request post-store))

  (PUT "/groups/:group-id/boards/:board-id/stores/:store-id/rating" request
       (require-store request put-rating))
  )