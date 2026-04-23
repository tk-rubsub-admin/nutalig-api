package com.nutalig.repository;

import com.nutalig.entity.UserPermissionOverrideEntity;
import com.nutalig.entity.id.UserPermissionOverrideId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPermissionOverrideRepository extends JpaRepository<UserPermissionOverrideEntity, UserPermissionOverrideId> {
    @Query("select u from UserPermissionOverride u where u.userId = :userId")
    List<UserPermissionOverrideEntity> findByUserId(String userId);
}
