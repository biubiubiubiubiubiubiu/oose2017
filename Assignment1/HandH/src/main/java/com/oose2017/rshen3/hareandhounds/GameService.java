//-------------------------------------------------------------------------------------------------------------//
// Code based on a tutorial by Shekhar Gulati of SparkJava at
// https://blog.openshift.com/developing-single-page-web-applications-using-java-8-spark-mongodb-and-angularjs/
//-------------------------------------------------------------------------------------------------------------//

package com.oose2017.rshen3.hareandhounds;

import com.google.gson.Gson;
import com.oose2017.rshen3.model.PlayerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import org.sql2o.Sql2oException;

import javax.sql.DataSource;
import java.util.List;

public class GameService {

    private Sql2o db;

    private final Logger logger = LoggerFactory.getLogger(GameService.class);

    /**
     * Construct the model with a pre-defined datasource. The current implementation
     * also ensures that the DB schema is created if necessary.
     *
     * @param dataSource
     */
    public GameService(DataSource dataSource) throws TodoServiceException {
        db = new Sql2o(dataSource);

        //Create the schema for the database if necessary. This allows this
        //program to mostly self-contained. But this is not always what you want;
        //sometimes you want to create the schema externally via a script.
        try (Connection conn = db.open()) {
            String sql = "CREATE TABLE IF NOT EXISTS `HandHtable` ( `gameID` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                                                     "`playerId` TEXT NOT NULL, " +
                                                     "`pieceType` TEXT NOT NULL )";

            conn.createQuery(sql).executeUpdate();
        } catch(Sql2oException ex) {
            logger.error("Failed to create schema at startup", ex);
            throw new TodoServiceException("Failed to create schema at startup", ex);
        }
    }

    /**
     * Create a new game for the input piece type
     *
     * @return the gameID, playerID, pieceType
     */
    public PlayerInfo createGame(String body) throws TodoServiceException {
        PlayerInfo playerInfo = new Gson().fromJson(body, PlayerInfo.class);
        playerInfo.setPlayerId(playerInfo.getPieceType() + "_player");
        // Insert the new game info into the database
        String sql = "INSERT INTO HandHTable (`playerId`, `pieceType`) " +
                                    "VALUES (:playerId, :pieceType)";
        String fetch_sql = "SELECT last_insert_rowid()";
        try (Connection conn = db.open()) {
            conn.createQuery(sql)
                    .bind(playerInfo)
                    .executeUpdate();
            // get the last inserted record back
            int id = conn.createQuery(fetch_sql).executeAndFetchFirst(Integer.class);
            playerInfo.setGameID(id);
            return playerInfo;
        } catch(Sql2oException ex) {
            logger.error("GameService.createGame: Failed to query database to create the new game", ex);
            throw new TodoServiceException("GameService.createGame: Failed to query database to create the new game", ex);
        }
    }
    /**
     * Fetch all todo entries in the list
     *
     * @return List of all Todo entries
     */
    public List<Todo> findAll() throws TodoServiceException {
        String sql = "SELECT * FROM item" ;
        try (Connection conn = db.open()) {
            List<Todo> todos =  conn.createQuery(sql)
                .addColumnMapping("item_id", "id")
                .addColumnMapping("created_on", "createdOn")
                .executeAndFetch(Todo.class);
            return todos;
        } catch(Sql2oException ex) {
            logger.error("GameService.findAll: Failed to query database", ex);
            throw new TodoServiceException("GameService.findAll: Failed to query database", ex);
        }
    }

    /**
     * Create a new Todo entry.
     */
    public void createNewTodo(String body) throws TodoServiceException {
        Todo todo = new Gson().fromJson(body, Todo.class);

        String sql = "INSERT INTO item (title, done, created_on) " +
                     "             VALUES (:title, :done, :createdOn)" ;

        try (Connection conn = db.open()) {
            conn.createQuery(sql)
                .bind(todo)
                .executeUpdate();
        } catch(Sql2oException ex) {
            logger.error("GameService.createNewTodo: Failed to create new entry", ex);
            throw new TodoServiceException("GameService.createNewTodo: Failed to create new entry", ex);
        }
    }

    /**
     * Find a todo entry given an Id.
     *
     * @param id The id for the Todo entry
     * @return The Todo corresponding to the id if one is found, otherwise null
     */
    public Todo find(String id) throws TodoServiceException {
        String sql = "SELECT * FROM item WHERE item_id = :itemId ";

        try (Connection conn = db.open()) {
            return conn.createQuery(sql)
                .addParameter("itemId", Integer.parseInt(id))
                .addColumnMapping("item_id", "id")
                .addColumnMapping("created_on", "createdOn")
                .executeAndFetchFirst(Todo.class);
        } catch(Sql2oException ex) {
            logger.error(String.format("GameService.find: Failed to query database for id: %s", id), ex);
            throw new TodoServiceException(String.format("GameService.find: Failed to query database for id: %s", id), ex);
        }
    }

    /**
     * Update the specified Todo entry with new information
     */
    public Todo update(String todoId, String body) throws TodoServiceException {
        Todo todo = new Gson().fromJson(body, Todo.class);

        String sql = "UPDATE item SET title = :title, done = :done, created_on = :createdOn WHERE item_id = :itemId ";
        try (Connection conn = db.open()) {
            //Update the item
            conn.createQuery(sql)
                    .bind(todo)  // one-liner to map all Todo object fields to query parameters :title etc
                    .addParameter("itemId", Integer.parseInt(todoId))
                    .executeUpdate();

            //Verify that we did indeed update something
            if (getChangedRows(conn) != 1) {
                logger.error(String.format("GameService.update: Update operation did not update rows. Incorrect id(?): %s", todoId));
                throw new TodoServiceException(String.format("GameService.update: Update operation did not update rows. Incorrect id (?): %s", todoId), null);
            }
        } catch(Sql2oException ex) {
            logger.error(String.format("GameService.update: Failed to update database for id: %s", todoId), ex);
            throw new TodoServiceException(String.format("GameService.update: Failed to update database for id: %s", todoId), ex);
        }

        return find(todoId);
    }

    /**
     * Delete the entry with the specified id
     */
    public void delete(String todoId) throws TodoServiceException {
        String sql = "DELETE FROM item WHERE item_id = :itemId" ;
        try (Connection conn = db.open()) {
            //Delete the item
            conn.createQuery(sql)
                .addParameter("itemId", Integer.parseInt(todoId))
                .executeUpdate();

            //Verify that we did indeed change something
            if (getChangedRows(conn) != 1) {
                logger.error(String.format("GameService.delete: Delete operation did not delete rows. Incorrect id(?): %s", todoId));
                throw new TodoServiceException(String.format("GameService.delete: Delete operation did not delete rows. Incorrect id(?): %s", todoId), null);
            }
        } catch(Sql2oException ex) {
            logger.error(String.format("GameService.update: Failed to delete id: %s", todoId), ex);
            throw new TodoServiceException(String.format("GameService.update: Failed to delete id: %s", todoId), ex);
        }
    }

    //-----------------------------------------------------------------------------//
    // Helper Classes and Methods
    //-----------------------------------------------------------------------------//

    public static class TodoServiceException extends Exception {
        public TodoServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * This Sqlite specific method returns the number of rows changed by the most recent
     * INSERT, UPDATE, DELETE operation. Note that you MUST use the same connection to get
     * this information
     */
    private int getChangedRows(Connection conn) throws Sql2oException {
        return conn.createQuery("SELECT changes()").executeScalar(Integer.class);
    }
}
