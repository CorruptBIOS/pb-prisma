package com.avairebot.servlet.routes.v1.get;

import com.avairebot.AvaIre;
import com.avairebot.contracts.metrics.SparkRoute;
import com.avairebot.database.controllers.GuildController;
import com.avairebot.database.transformers.GuildTransformer;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.Random;

public class GetEvaluationQuestions extends SparkRoute {
    private static final Logger log = LoggerFactory.getLogger(GetEvaluationQuestions.class);

    @Override
    public Object handle(Request request, Response response) throws Exception {
        if (!hasValidEvaluationsAuthorizationHeader(request)) {
            log.warn("Unauthorized request, missing or invalid `Authorization` header give.");
            return buildResponse(response, 401, "Unauthorized request, missing or invalid `Authorization` header give.");
        }

        String guildId = request.params("guildId");
        JSONObject root = new JSONObject();
        ArrayList<String> questions = new ArrayList<>();
        Guild guild = AvaIre.getInstance().getShardManager().getGuildById(guildId);
        if (guild == null) {
            response.status(400);
            root.put("error", "XEUS_GUILD_DOES_NOT_EXIST");
            root.put("message", "Guild doesn't exist. :(");
            return root;
        }

        GuildTransformer transformer = GuildController.fetchGuild(AvaIre.getInstance(), guild);
        if (transformer == null) {
            response.status(500);
            root.put("error", "XEUS_GUILD_MISSING_TRANSFORMER");
            root.put("message", "Guild doesn't have a transformer, please check with the guild admins and api developer (Stefano#7366).");
            return root;
        }

        if (transformer.getEvalQuestions() == null || transformer.getEvalQuestions().size() == 0) {
            response.status(500);
            root.put("error", "XEUS_MISSING_QUESTIONS");
            root.put("message", "Guild doesn't have any questions.");
            return root;
        }

        if (transformer.getEvalQuestions().size() < 8) {
            response.status(500);
            root.put("error", "XEUS_NOT_ENOUGH_QUESTIONS");
            root.put("message", "Xeus only has less then 8 questions in the guild database, please contact the guild admin.");
            return root;
        }

        Random random_method = new Random();
        for (int i = 0; i < 8; i++) {
            int index = random_method.nextInt(transformer.getEvalQuestions().size());
            String question = transformer.getEvalQuestions().get(index);
            if (questions.contains(question)) {
                i--;
                continue;
            }

            questions.add(question);
        }
        root.put("guildId", guild.getId());
        root.put("guildName", guild.getName());
        root.put("questions", questions);
        return root;
    }
}