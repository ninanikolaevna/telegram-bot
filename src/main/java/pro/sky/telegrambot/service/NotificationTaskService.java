package pro.sky.telegrambot.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.entity.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NotificationTaskService {

    private static final Pattern REMINDER_PATTERN =
            Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4}\\s\\d{2}:\\d{2})\\s+(.+)");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private final Logger logger = LoggerFactory.getLogger(NotificationTaskService.class);
    @Autowired
    private NotificationTaskRepository repository;
    @Autowired
    private TelegramBot telegramBot;

    public void processReminderMessage(Long chatId, String text) {
        Matcher matcher = REMINDER_PATTERN.matcher(text);

        if (matcher.matches()) {
            try {
                String dateTimeStr = matcher.group(1);
                String reminderText = matcher.group(2);

                LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);

                // Проверяем, что дата в будущем
                if (dateTime.isBefore(LocalDateTime.now())) {
                    sendMessage(chatId, "Ошибка: дата должна быть в будущем!");
                    return;
                }

                // Создаем и сохраняем напоминание
                NotificationTask task = new NotificationTask();
                task.setChatId(chatId);
                task.setMessage(reminderText);
                task.setDateTime(dateTime);
                task.setSent(false);

                repository.save(task);

                sendMessage(chatId, "Напоминание установлено на " + dateTimeStr);

            } catch (DateTimeParseException e) {
                sendMessage(chatId, "Ошибка в формате даты. Используйте: ДД.ММ.ГГГГ ЧЧ:MM Текст");
            }
        } else {
            sendMessage(chatId, "Неверный формат. Используйте: ДД.ММ.ГГГГ ЧЧ:MM Текст");
        }
    }

    @Scheduled(cron = "0 0/1 * * * *")  // Каждую минуту
    public void sendScheduledNotifications() {
        LocalDateTime currentTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        logger.info("Checking notifications for time: {}", currentTime);

        List<NotificationTask> tasks = repository.findByDateTimeAndNotSent(currentTime);

        for (NotificationTask task : tasks) {
            sendMessage(task.getChatId(), "Напоминание: " + task.getMessage());
            task.setSent(true);
            repository.save(task);
            logger.info("Notification sent to chatId: {}", task.getChatId());
        }
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage(chatId, text);
        SendResponse response = telegramBot.execute(message);

        if (!response.isOk()) {
            logger.error("Failed to send message to chatId {}: {}", chatId, response.errorCode());
        }
    }
}