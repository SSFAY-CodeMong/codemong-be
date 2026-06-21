package com.codemong.be.mail.repository;

import com.codemong.be.mail.entity.MailSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MailSubscriptionRepository extends JpaRepository<MailSubscription, Long> {

    Optional<MailSubscription> findByUser_Id(Long userId);

    @Query("""
            select subscription
            from MailSubscription subscription
            join fetch subscription.user
            where subscription.enabled = true
            """)
    List<MailSubscription> findEnabledWithUser();
}
