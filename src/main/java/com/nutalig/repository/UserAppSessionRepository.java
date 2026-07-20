package com.nutalig.repository;

import com.nutalig.constant.AppSessionDeviceType;
import com.nutalig.entity.UserAppSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAppSessionRepository extends JpaRepository<UserAppSessionEntity, Long> {

    Optional<UserAppSessionEntity> findByUser_IdAndDeviceType(String userId, AppSessionDeviceType deviceType);

    Optional<UserAppSessionEntity> findByUser_IdAndSessionIdAndDeviceType(
            String userId,
            String sessionId,
            AppSessionDeviceType deviceType
    );

    void deleteByUser_Id(String userId);

    void deleteByUser_IdAndSessionIdAndDeviceType(String userId, String sessionId, AppSessionDeviceType deviceType);
}
