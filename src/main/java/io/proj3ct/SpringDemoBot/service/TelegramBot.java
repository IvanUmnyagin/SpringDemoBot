package io.proj3ct.SpringDemoBot.service;
import com.vdurmont.emoji.EmojiParser;
import io.proj3ct.SpringDemoBot.config.BotConfig;
import io.proj3ct.SpringDemoBot.model.Ads;
import io.proj3ct.SpringDemoBot.model.AdsRepository;
import io.proj3ct.SpringDemoBot.model.User;
import io.proj3ct.SpringDemoBot.model.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AdsRepository adsRepository;
    final BotConfig config;

    static final String HELP_TEXT = "This bot is created to demonstrate Spring capabilities.\n\n" +
            "You can execute commands from the main menu on the left or by typing a command:\n\n" +
            "Type /start to see a welcome message\n\n" +
            "Type /mydata to see data stored about yourself\n\n" +
            "Type /help to see this message again";

    static final String YES_BUTTON = "YES_BUTTON";
    static final String NO_BUTTON = "NO_BUTTON";

    static final String ERROR_TEXT = "Error occurred: ";

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listofCommands = new ArrayList<>();
        listofCommands.add(new BotCommand("/start", "get a welcome message"));
        listofCommands.add(new BotCommand("/mydata", "get your data stored"));
        listofCommands.add(new BotCommand("/deletedata", "delete my data"));
        listofCommands.add(new BotCommand("/help", "info how to use this bot"));
        listofCommands.add(new BotCommand("/settings", "set your preferences"));
        try {
            this.execute(new SetMyCommands(listofCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {       //Update - класс, который содержит сообщение, кот пользователь посылает боту+инф о пользователе

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();      //Чтобы бот мог нам написать. ID, который идентифицирует пользователя

            if(messageText.contains("/send") && config.getOwnerId() == chatId) {        //если сообщение, кот мы получили, если содержит команду send + я отправитель, то
                var textToSend = EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" ")));   //отправляем не всю часть, а то, что после пробела
                var users = userRepository.findAll();   //Нам нужны все пользователя, возвращаем всех пользователей - findAll()
                for (User user: users){
                    prepareAndSendMessage(user.getChatId(), textToSend);
                }
            }

            else {

                switch (messageText) {
                    case "/start":

                        registerUser(update.getMessage());
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                        break;

                    case "/help":

                        prepareAndSendMessage(chatId, HELP_TEXT);
                        break;

                    case "/register":

                        register(chatId);
                        break;

                    default:
                        prepareAndSendMessage(chatId, "Sorry, command was not recognized");
                }
            }
        } else if (update.hasCallbackQuery()) {         //проверяем, может передалось ID кнопки
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();     //у каждого сообщения есть ID, и если получим - сможем редактировать
            long chatId = update.getCallbackQuery().getMessage().getChatId();           //тоже самое

            if(callbackData.equals(YES_BUTTON)){
                String text = "You pressed YES button";
                executeEditMessageText(text, chatId, messageId);
            }
            else if(callbackData.equals(NO_BUTTON)){
                String text = "You pressed NO button";
                executeEditMessageText(text, chatId, messageId);
            }
        }
    }

    private void register(long chatId) {

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Do you really want to register?");

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();        //создадим список списков, в котором будем хранить кнопочки
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();               //создаем список с кнопками для ряда
        var yesButton = new InlineKeyboardButton();

        yesButton.setText("Yes");
        yesButton.setCallbackData(YES_BUTTON);      //идентификатор, кот позволяет боту понять, какая кнопка была нажата

        var noButton = new InlineKeyboardButton();

        noButton.setText("No");
        noButton.setCallbackData(NO_BUTTON);

        rowInLine.add(yesButton);
        rowInLine.add(noButton);

        rowsInLine.add(rowInLine);

        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }

    private void registerUser(Message msg) {

        if(userRepository.findById(msg.getChatId()).isEmpty()){         //не записан ли уже наш пользователь

            var chatId = msg.getChatId();
            var chat = msg.getChat();

            User user = new User();

            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            //user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("user saved: " + user);
        }
    }

    private void startCommandReceived(long chatId, String name) {


        String answer = EmojiParser.parseToUnicode("Hi, " + name + ", nice to meet you!" + " :blush:");
        log.info("Replied to user " + name);

        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();        //создадим сам ряд

        row.add("weather");                         //при нажатии на кнопку соответствующая команда будет отправлена боту
        row.add("get random joke");                 //определили первый ряд

        keyboardRows.add(row);

        row = new KeyboardRow();

        row.add("register");
        row.add("check my data");
        row.add("delete my data");

        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);       //добавили наши ряды

        message.setReplyMarkup(keyboardMarkup);     //к нашему сообщению привязываем только что созданную клавиатуру

        executeMessage(message);
    }


    private void executeEditMessageText(String text, long chatId, long messageId){      //редактирование текста сообщения
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId((int) messageId);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void executeMessage(SendMessage message){
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void prepareAndSendMessage(long chatId, String textToSend){     //подготовить и отправить сообщение. Чтобы скрыть начальную клавиатуру
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessage(message);
    }

                                                //6 параметров: секунды, минуты, часы, дата: день, месяц, день недели
    @Scheduled(cron = "${cron.scheduler}")      //Аннотация для автоматического запуска. Указываем когда мы хотим запустить наш метод
    private void sendAds(){             //метод, который будет запускаться автоматически

        var ads = adsRepository.findAll();      //получить список объявлений. Получаем все записи из таблицы
        var users = userRepository.findAll();       //Получаем всех пользователей

        for(Ads ad: ads) {
            for (User user: users){
                prepareAndSendMessage(user.getChatId(), ad.getAd());
            }
        }

    }
}