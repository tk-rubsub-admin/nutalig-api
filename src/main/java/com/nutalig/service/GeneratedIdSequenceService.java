package com.nutalig.service;

import com.nutalig.entity.GeneratedIdSequenceEntity;
import com.nutalig.repository.GeneratedIdSequenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeneratedIdSequenceService {

    private final GeneratedIdSequenceRepository generatedIdSequenceRepository;

    @Retryable(
            maxAttempts = 5,
            retryFor = ObjectOptimisticLockingFailureException.class
    )
    @Transactional
    public String getNextSequence(String prefix, int digit) {

        GeneratedIdSequenceEntity seq = generatedIdSequenceRepository.findById(prefix)
                .orElseGet(() -> initialPrefixId(prefix));

        Integer id = seq.getNextId();
        log.info("got next sequence id for prefix {} : {}", prefix, id);

        seq.setNextId(id + 1);

        return String.format("%0" + digit + "d", id);
    }

    private GeneratedIdSequenceEntity initialPrefixId(String prefix) {
        GeneratedIdSequenceEntity e = new GeneratedIdSequenceEntity();
        e.setPrefix(prefix);
        e.setNextId(1);
        return generatedIdSequenceRepository.save(e);
    }
}
