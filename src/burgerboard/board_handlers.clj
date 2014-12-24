(ns burgerboard.board-handlers
  (:use compojure.core
        [clojure.string :only [join]]
        burgerboard.authentication
        burgerboard.database
        burgerboard.boards
        burgerboard.api
        )
  )

(defn board-id [request]
  (Integer. (:board-id (:params request))))

(defn store-id [request]
  (Integer. (:store-id (:params request))))

(defn require-board [request handler]
  (require-membership
   request
   (fn [user group request]
     (if-let [board (find-group-board group (board-id request))]
       (handler user group board request)
       )
     )
   )
  )

(defn require-store [request handler]
  (require-board
   request
   (fn [user group board request]
     (println group board (store-id request))
     (if-let [store (find-board-store board (store-id request))]
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

(defn if-contains-modify [map key modify-fn]
  (if (contains? map key)
    (assoc map
      key
      (modify-fn map))
    map)
  )

(defn render-board [request board]
  (->
   board
   (if-contains-modify
    :stores
    (fn [_]
      (map (partial render-store request (:group board) board)
           (:stores board))
      )
    )
   (if-contains-modify
    :group
    (fn [_]
      (dissoc (:group board) :owner :users)
      )
    )
   (assoc
     :url (resolve-route request
                         "groups" (:id (:group board)) "boards" (:id board))
     :stores_url (resolve-route request
                                "groups" (:id (:group board))
                                "boards" (:id board) "stores"))
   )
  )

(defn get-boards [user group request]
  (->
   {:boards (map (partial render-board request)
                 (find-groups-boards group))}
   (json-response 200))
  )

(defn post-board [user group request]
  (let [name (:name (body-json request))]
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
  (let [name (:name (body-json request))]
    (->
     (create-store name board)
     (insert-store)
     (->> (render-store request group board))
     (json-response 201)
     )
    )
  )

(defn put-rating [user group board store request]
  (let [rating (int (:rating (body-json request)))]
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
