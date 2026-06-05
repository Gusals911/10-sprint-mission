package com.sprint.mission.discodeit.listener;

import com.sprint.mission.discodeit.entity.Notification;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.event.MessageCreatedEvent;
import com.sprint.mission.discodeit.event.RoleUpdatedEvent;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.repository.NotificationRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationRequiredEventListener {

    private final NotificationRepository notificationRepository;
    private final ReadStatusRepository readStatusRepository;
    private final UserRepository userRepository;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(MessageCreatedEvent event) {
        String channelName = event.channelName() == null ? "private" : event.channelName();
        String title = event.authorUsername() + " (#" + channelName + ")";

        List<Notification> notifications =
                readStatusRepository.findAllByChannelIdAndNotificationEnabledIsTrueWithUser(event.channelId())
                        .stream()
                        .map(ReadStatus::getUser)
                        .filter(user -> !user.getId().equals(event.authorId()))
                        .map(user -> new Notification(user, title, event.content()))
                        .toList();

        notificationRepository.saveAll(notifications);
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(RoleUpdatedEvent event) {
        User receiver = userRepository.findById(event.userId())
                .orElseThrow(() -> UserNotFoundException.withId(event.userId()));

        Notification notification = new Notification(
                receiver,
                "권한이 변경되었습니다.",
                event.oldRole() + " -> " + event.newRole()
        );

        notificationRepository.save(notification);
    }
}
