package pro.sky.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pro.sky.telegrambot.entity.NotificationTask;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationTaskRepository extends JpaRepository<NotificationTask, Long> {

    @Query("SELECT n FROM NotificationTask n WHERE n.dateTime = :dateTime AND n.sent = false")
    List<NotificationTask> findByDateTimeAndNotSent(@Param("dateTime") LocalDateTime dateTime);
}