package com.avairebot.commands.fun;

import com.avairebot.AvaIre;
import com.avairebot.commands.CommandMessage;
import com.avairebot.contracts.commands.Command;
import com.avairebot.factories.RequestFactory;
import com.avairebot.requests.Response;
import com.avairebot.requests.service.RandomCatService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class RandomCatCommand extends Command {

    public RandomCatCommand(AvaIre avaire) {
        super(avaire);
    }

    @Override
    public String getName() {
        return "Random Cat Command";
    }

    @Override
    public String getDescription() {
        return "I will scour the internet to find a random cat picture for you.";
    }

    @Override
    public List<String> getTriggers() {
        return Arrays.asList("randomcat", "cat");
    }

    @Override
    public List<String> getMiddleware() {
        return Collections.singletonList("throttle:user,2,5");
    }

    @Override
    public boolean onCommand(CommandMessage context, String[] args) {
        RequestFactory.makeGET("http://random.cat/meow")
            .send((Consumer<Response>) response -> {
                RandomCatService service = (RandomCatService) response.toService(RandomCatService.class);

                context.makeEmbeddedMessage().setImage(service.getFile()).queue();
            });
        return true;
    }
}
