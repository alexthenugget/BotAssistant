package com.github.ALEX_THE_NUGGET.bot_assistant.telegram;

import com.github.ALEX_THE_NUGGET.bot_assistant.database.DataBaseWork;
import com.github.ALEX_THE_NUGGET.bot_assistant.services.WeatherHandler;
import com.github.ALEX_THE_NUGGET.bot_assistant.services.YandexTranslate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TelegramBot extends TelegramLongPollingBot {
    private HashMap<String, Runnable> commandHandlers = new HashMap<>();
    private HashMap<Long, String> chatStates = new HashMap<>();
    private DataBaseWork dataBase = new DataBaseWork();
    private WeatherHandler weatherHandler = new WeatherHandler();
    private String usersCity;
    private String usersTimeZone;
    private YandexTranslate yandexTranslate = new YandexTranslate();

    @Override
    public String getBotUsername() {
        return "MorningSN_BOT";
    }

    @Override
    public String getBotToken() {
        return System.getenv("BOT_TOKEN");
    }

    public void onUpdateReceived(Update update) {
        long chatId = update.getMessage().getChatId();
        if (update.hasMessage() && update.getMessage().hasText()) {
            String inputText = update.getMessage().getText();
            SendMessage message = new SendMessage();
            message.setChatId(chatId);

            if (chatStates.containsKey(chatId) && chatStates.get(chatId).equals("awaitingNotification")) {
                chatStates.remove(chatId);
                handleNotificationCommand(chatId, message, inputText);
            } else if (chatStates.containsKey(chatId) && chatStates.get(chatId).equals("awaitingTask")) {
                chatStates.remove(chatId);
                handleAddTaskCommand(chatId, message, inputText);
            } else if (chatStates.containsKey(chatId) && chatStates.get(chatId).equals("awaitingWeather")) {
                chatStates.remove(chatId);
                handleWeatherCommand(message);
            } else if (chatStates.containsKey(chatId) && chatStates.get(chatId).equals("awaitingText")) {
                chatStates.remove(chatId);
                handleTranslationCommand(message, inputText);
            } else if (chatStates.containsKey(chatId) && chatStates.get(chatId).equals("awaitingCity")) {
                chatStates.remove(chatId);
                handleCityCommand(message, inputText);
            } else {
                processCommand(inputText, message, update);
            }
        }
    }

    private void processCommand(String inputText, SendMessage message, Update update) {
        if (inputText.startsWith("/")) {
            if (update != null && update.hasMessage() && update.getMessage().hasText()) {
                String commandText = update.getMessage().getText();
                SendMessage responseMessage = new SendMessage();
                long chatId = update.getMessage().getChatId();
                responseMessage.setChatId(chatId);
                commandHandlers.put("/setcity", () -> {
                    chatStates.put(chatId, "awaitingCity");
                    responseMessage.setText("Введите название вашего города в таком формате: Asia/Yekaterinburg");
                    executeMessage(responseMessage);
                });
                commandHandlers.put("/notification", () -> {
                    chatStates.put(chatId, "awaitingNotification");
                    responseMessage.setText("Введите время, в которое хотите получать рассылку в " +
                            "формате HH:mm:ss.");
                    executeMessage(responseMessage);
                });
                commandHandlers.put("/addtask", () -> {
                    chatStates.put(chatId, "awaitingTask");
                    responseMessage.setText("Введите задачу в формате: dd.mm.yy HH:mm:ss Название");
                    executeMessage(responseMessage);
                });
                commandHandlers.put("/translate", () -> {
                    chatStates.put(chatId, "awaitingText");
                    responseMessage.setText("Введите ваш текст для перевода");
                    executeMessage(responseMessage);
                });
                commandHandlers.put("/start", () -> {
                    handleStartCommand(responseMessage);
                    executeMessage(responseMessage);
                });
                commandHandlers.put("/help", () -> {
                    handleHelpCommand(responseMessage);
                    executeMessage(responseMessage);
                });
                commandHandlers.put("/showtasks", () -> {
                    handleTaskCommand(chatId, responseMessage);
                    executeMessage(responseMessage);
                });
                commandHandlers.put("/showweather", () -> {
                    handleWeatherCommand(responseMessage);
                    executeMessage(responseMessage);
                });
                Runnable commandHandler = commandHandlers.getOrDefault(commandText, () -> {
                    handleUnknownCommand(responseMessage);
                    executeMessage(responseMessage);
                });
                commandHandler.run();
            }
        } else {
            handleTextMessage(message);
            executeMessage(message);
        }
    }

    private void handleTextMessage(SendMessage message) {
        message.setText("Не существует такой команды");
    }

    private void handleUnknownCommand(SendMessage message) {
        message.setText("Такой команды нет. Нажмите /help");
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.out.println("Error occurred: " + e.getMessage());
        }
    }

    public void handleStartCommand(SendMessage message) {
        message.setText("Привет! Я - Бот Рабочий Ассисент. Я помогу вам планировать и продуктивно проводить свой день." +
                " Нажмите /help, чтобы узнать о том, что я умею.");
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        KeyboardRow row1 = new KeyboardRow();
        row1.add("/start");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("/help");
        row2.add("/setcity");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("/addtask");
        row3.add("/notification");

        KeyboardRow row4 = new KeyboardRow();
        row4.add("/translate");
        row4.add("/showtasks");
        row4.add("/showweather");

        keyboardMarkup.setKeyboard(List.of(row1, row2, row3, row4));
        message.setReplyMarkup(keyboardMarkup);
    }

    public void handleHelpCommand(SendMessage message) {
        message.setText("Вот, что я умею: \n" +
                "/start - вывести стартовое сообщение \n" +
                "/help - вывести справочную информацию \n" +
                "/setcity - установить город \n" +
                "/notification - установить время для рассылки погоды и списка задач \n" +
                "/addtask - добавить задачу \n" +
                "/showtasks - показать задачи на сегодня \n" +
                "/showweather - показать погоду \n" +
                "/translate - перевести текст с английского на русский");
    }

    public void handleTaskCommand(long Id, SendMessage message) {
        if (usersCity != null && !usersCity.isEmpty()) {
            ZoneId userZoneId = ZoneId.of(usersTimeZone);
            ZonedDateTime now = ZonedDateTime.now(userZoneId);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy");
            String formattedDate = now.format(formatter);
            message.setText(dataBase.getDataByDate(Id, formattedDate, message));
        } else {
            message.setText("Сначала установите город при помощи /setcity.");
        }
    }

    private void handleNotificationCommand(long Id, SendMessage message, String time) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        timeFormat.setLenient(false);
        try {
            timeFormat.parse(time);
            message.setText("Успешно. Уведомление будет отправлено в назначенное вами время");
        } catch (ParseException e) {
            message.setText("Неверный формат времени. Ожидается HH:mm:ss.");
            executeMessage(message);
            return;
        }
        String weather = null;
        try {
            if (usersCity != null && !usersCity.isEmpty()) {
                weather = weatherHandler.returnWeather(usersCity);
                message.setText("Успешно. Уведомление будет отправлено в назначенное вами время");
                executeMessage(message);
            } else {
                message.setText("Сначала установите город при помощи /setcity.");
                executeMessage(message);
                return;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ZoneId userZoneId = ZoneId.of(usersTimeZone);
        ZonedDateTime now = ZonedDateTime.now(userZoneId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy");
        String formattedDate = now.format(formatter);
        String plan = dataBase.getDataByDate(Id, formattedDate, message);
        scheduleDailyNotification(usersTimeZone, time, weather, plan, message);
    }

    private void handleAddTaskCommand(long Id, SendMessage message, String task) {
        if (usersCity == null || usersCity.isEmpty()) {
            message.setText("Сначала установите город при помощи /setcity.");
            executeMessage(message);
            return;
        }
        ZoneId userZoneId = ZoneId.of(usersTimeZone);
        ZonedDateTime now = ZonedDateTime.now(userZoneId);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy");
        String formattedDate = now.format(formatter);
        String[] parts = task.split(" ", 3);
        if (parts.length != 3) {
            message.setText("Неверное количество частей в строке. Строка должна содержать Дату, Время и Название задачи.");
            return;
        }
        String date = parts[0];
        String time = parts[1];
        String taskName = parts[2];
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy");
        dateFormat.setLenient(false);
        try {
            dateFormat.parse(date);
        } catch (ParseException e) {
            message.setText("Неверный формат даты. Ожидается dd.MM.yy.");
            executeMessage(message);
            return;
        }
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        timeFormat.setLenient(false);
        try {
            timeFormat.parse(time);
        } catch (ParseException e) {
            message.setText("Неверный формат времени. Ожидается HH:mm:ss.");
            executeMessage(message);
            return;
        }
        dataBase.connectTable(message);
        executeMessage(message);
        dataBase.insertData(Id, date, time, taskName, message);
        executeMessage(message);
    }

    private void handleWeatherCommand(SendMessage message) {
        String weather = null;
        try {
            if (usersCity != null && !usersCity.isEmpty()) {
                weather = weatherHandler.returnWeather(usersCity);
                message.setText(weather);
            } else {
                message.setText("Сначала установите город при помощи /setcity.");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleTranslationCommand(SendMessage message, String text) {
        try {
            String targetLanguage = "ru";
            String translatedText = yandexTranslate.translate(text, targetLanguage);
            message.setText("Ваш текст на английском: " + text);
            executeMessage(message);
            message.setText("Перевод на русский: " + translatedText);
            executeMessage(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleCityCommand(SendMessage message, String city) {
        String[] parts = city.split("/");
        usersCity = parts[1];
        try {
            ZoneId zoneId = ZoneId.of(city);
            usersTimeZone = zoneId.getId();
            message.setText("Часовой пояс для вашего города успешно найден.");
            executeMessage(message);
        } catch (Exception e) {
            message.setText("Не удалось найти часовой пояс для города: " + usersCity);
            executeMessage(message);
        }
    }

    public void scheduleDailyNotification(String timeZone, String time, String weatherString, String tasks, SendMessage message) {
        if (!isValidTimeFormat(time)) {
            message.setText("Ошибка: некорректное время. Ожидается формат HH:mm:ss.");
            return;
        }
        ZoneId userZoneId = ZoneId.of(timeZone);
        LocalDateTime now = LocalDateTime.now(userZoneId);
        LocalTime parsedTime = LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm:ss"));
        LocalDateTime notificationTime = LocalDateTime.of(now.toLocalDate(), parsedTime);

        if (now.isAfter(notificationTime)) {
            notificationTime = notificationTime.plusDays(1);
        }

        ZonedDateTime zonedNow = ZonedDateTime.now(userZoneId);
        ZonedDateTime zonedTargetTime = notificationTime.atZone(userZoneId);

        long delay = Duration.between(zonedNow, zonedTargetTime).toMillis();
        long period = Duration.ofDays(1).toMillis();

        Timer timer = new Timer();
        String response = "Погода в вашем городе: " + '\n' + weatherString + '\n' + '\n' + tasks;
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                message.setText(response);
                executeMessage(message);
            }
        };
        timer.scheduleAtFixedRate(timerTask, delay, period);
    }

    private static boolean isValidTimeFormat(String time) {
        try {
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            LocalTime.parse(time, timeFormatter);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

}


