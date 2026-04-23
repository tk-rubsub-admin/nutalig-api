package com.nutalig.repository;

import com.nutalig.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, String>, JpaSpecificationExecutor<UserEntity> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByLineUserId(String lineUserId);

    @Query("""
       SELECT u 
       FROM User u 
       WHERE u.userRoleEntity.roleCode IN :roleCodes
       """)
    List<UserEntity> findByRoleIn(@Param("roleCodes") List<String> roleCodes);

}
