package com.sprint.mission.discodeit.storage;

import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class BinaryContentStorageSupport {

  private final BinaryContentStorage binaryContentStorage;

  public void put(UUID binaryContentId, byte[] bytes) {
    binaryContentStorage.put(binaryContentId, bytes);
    registerRollbackCleanup(binaryContentId);
  }

  public void deleteAfterCommit(UUID binaryContentId) {
    if (binaryContentId == null) {
      return;
    }

    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCommit() {
          deleteQuietly(binaryContentId);
        }
      });
      return;
    }

    deleteQuietly(binaryContentId);
  }

  private void registerRollbackCleanup(UUID binaryContentId) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      return;
    }

    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCompletion(int status) {
        if (status != STATUS_COMMITTED) {
          deleteQuietly(binaryContentId);
        }
      }
    });
  }

  private void deleteQuietly(UUID binaryContentId) {
    try {
      binaryContentStorage.delete(binaryContentId);
    } catch (NoSuchElementException exception) {
      log.debug("삭제할 바이너리 파일이 이미 없어 정리를 건너뜁니다: id={}", binaryContentId);
    } catch (RuntimeException exception) {
      log.warn("바이너리 파일 정리에 실패했습니다: id={}", binaryContentId, exception);
      throw exception;
    }
  }
}
