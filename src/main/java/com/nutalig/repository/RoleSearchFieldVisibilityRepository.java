package com.nutalig.repository;

import com.nutalig.entity.RoleSearchFieldVisibilityEntity;
import com.nutalig.entity.id.RoleSearchFieldVisibilityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface RoleSearchFieldVisibilityRepository
        extends JpaRepository<RoleSearchFieldVisibilityEntity, RoleSearchFieldVisibilityId> {

    List<RoleSearchFieldVisibilityEntity> findByRoleCodeAndScreenCodeAndVisibleTrue(String roleCode, String screenCode);

    List<RoleSearchFieldVisibilityEntity> findByRoleCodeInAndScreenCodeIn(
            Collection<String> roleCodes,
            Collection<String> screenCodes
    );

    void deleteByRoleCodeAndScreenCode(String roleCode, String screenCode);
}
