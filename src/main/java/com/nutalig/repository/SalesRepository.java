package com.nutalig.repository;

import com.nutalig.constant.SalesType;
import com.nutalig.entity.SalesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalesRepository extends JpaRepository<SalesEntity, String>, JpaSpecificationExecutor<SalesEntity> {

    Optional<SalesEntity> findBySalesIdAndType(String id, SalesType type);
    List<SalesEntity> findByType(SalesType type);
    Optional<SalesEntity> findFirstByNicknameContainingIgnoreCaseAndType(String nickname, SalesType type);
    Optional<SalesEntity> findFirstByNameContainingIgnoreCaseAndType(String name, SalesType type);

}
