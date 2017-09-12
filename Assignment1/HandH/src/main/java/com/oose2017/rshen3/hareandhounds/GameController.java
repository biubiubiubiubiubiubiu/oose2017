//-------------------------------------------------------------------------------------------------------------//
// Code based on a tutorial by Shekhar Gulati of SparkJava at
// https://blog.openshift.com/developing-single-page-web-applications-using-java-8-spark-mongodb-and-angularjs/
//-------------------------------------------------------------------------------------------------------------//

package com.oose2017.rshen3.hareandhounds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

import static spark.Spark.*;

public class GameController {

    private static final String API_CONTEXT = "/api/v1";

    private static final String API_PREFIX = "/hareandhounds/api/games";

    private final GameService gameService;

    private final Logger logger = LoggerFactory.getLogger(GameController.class);

    public GameController(GameService gameService) {
        this.gameService = gameService;
        setupEndpoints();
    }

    private void setupEndpoints() {
        post(API_PREFIX, "application/json", (request, response) -> {
            try {
                logger.info("Creating a new game for" + request.body());
                gameService.createGame(request.body());
            } catch (GameService.TodoServiceException ex) {
                logger.error("Failed to create a new game!");
            }
            return Collections.EMPTY_MAP;
        }, new JsonTransformer());

        post(API_CONTEXT + "/todos", "application/json", (request, response) -> {
            try {
                gameService.createNewTodo(request.body());
                response.status(201);
            } catch (GameService.TodoServiceException ex) {
                logger.error("Failed to create new entry");
                response.status(500);
            }
            return Collections.EMPTY_MAP;
        }, new JsonTransformer());

        get(API_CONTEXT + "/todos/:id", "application/json", (request, response) -> {
            try {
                return gameService.find(request.params(":id"));
            } catch (GameService.TodoServiceException ex) {
                logger.error(String.format("Failed to find object with id: %s", request.params(":id")));
                response.status(500);
                return Collections.EMPTY_MAP;
            }
        }, new JsonTransformer());

        get(API_CONTEXT + "/todos", "application/json", (request, response)-> {
            try {
                return gameService.findAll() ;
            } catch  (GameService.TodoServiceException ex) {
                logger.error("Failed to fetch the list of todos");
                response.status(500);
                return Collections.EMPTY_MAP;
            }
        }, new JsonTransformer());

        put(API_CONTEXT + "/todos/:id", "application/json", (request, response) -> {
            try {
                return gameService.update(request.params(":id"), request.body());
            } catch (GameService.TodoServiceException ex) {
                logger.error(String.format("Failed to update todo with id: %s", request.params(":id")));
                response.status(500);
                return Collections.EMPTY_MAP;
            }
        }, new JsonTransformer());

        delete(API_CONTEXT + "/todos/:id", "application/json", (request, response) -> {
            try {
                gameService.delete(request.params(":id"));
                response.status(200);
            } catch (GameService.TodoServiceException ex) {
                logger.error(String.format("Failed to delete todo with id: %s", request.params(":id")));
                response.status(500);
            }
            return Collections.EMPTY_MAP;
        }, new JsonTransformer());
    }
}
