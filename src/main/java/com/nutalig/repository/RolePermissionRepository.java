package com.nutalig.repository;

import com.nutalig.entity.RolePermissionEntity;
import com.nutalig.entity.id.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, RolePermissionId> {
    @Query("select rp.permissionCode from RolePermission rp where rp.roleCode = :roleCode")
    List<String> findPermissionCodesByRoleCode(String roleCode);
}
