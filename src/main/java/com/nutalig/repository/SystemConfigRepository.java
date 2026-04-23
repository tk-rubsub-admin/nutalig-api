package com.nutalig.repository;

import com.nutalig.constant.SystemConstant;
import com.nutalig.entity.SystemConfigEntity;
import com.nutalig.entity.id.SystemConfigId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfigEntity, SystemConfigId>, JpaSpecificationExecutor<SystemConfigEntity> {

    List<SystemConfigEntity> findByIdGroupCodeOrderBySortAsc(SystemConstant groupCode);
    Optional<SystemConfigEntity> findByIdGroupCodeAndIdCode(SystemConstant groupCode, String code);
    Optional<SystemConfigEntity> findByIdGroupCodeAndNameTh(SystemConstant groupCode, String value);

}
