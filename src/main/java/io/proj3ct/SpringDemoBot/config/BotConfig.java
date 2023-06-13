package io.proj3ct.SpringDemoBot.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;


@Configuration      //Чтобы работала аннотация Value
@EnableScheduling       //будут соотвествующие методы, котороые подлежат автомаческому запуску
@Data           //Находится в библиотеке Lombok. Автоматически создает конструкторы
@PropertySource("application.properties")       //Указать, где находится свойства, которые считываются через Value
public class BotConfig {

    @Value("${bot.name}")
    String botName;

    @Value("${bot.token}")
    String Token;

    @Value("${bot.owner}")
    Long ownerId;
}