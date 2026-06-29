package com.ns.nap_backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class TokenDenylistServiceTest {

  private final TokenDenylistService service = new TokenDenylistService();

  @Test
  void denylistedTokenIsReportedAsDenylisted() {
    service.denylist("jti-1", Instant.now().plus(10, ChronoUnit.MINUTES));

    assertThat(service.isDenylisted("jti-1")).isTrue();
  }

  @Test
  void unknownTokenIsNotDenylisted() {
    assertThat(service.isDenylisted("nope")).isFalse();
  }

  @Test
  void nullJtiIsHandledGracefully() {
    service.denylist(null, Instant.now().plus(10, ChronoUnit.MINUTES));

    assertThat(service.isDenylisted(null)).isFalse();
  }

  @Test
  void alreadyExpiredEntryIsNotStored() {
    service.denylist("old", Instant.now().minus(1, ChronoUnit.MINUTES));

    assertThat(service.isDenylisted("old")).isFalse();
  }

  @Test
  void isDenylistedLazilyEvictsExpiredEntry() {
    service.denylist("soon", Instant.now().plusMillis(20));

    await(40);

    assertThat(service.isDenylisted("soon")).isFalse();
  }

  @Test
  void purgeExpiredRemovesOnlyExpiredEntries() {
    service.denylist("live", Instant.now().plus(10, ChronoUnit.MINUTES));
    service.denylist("dying", Instant.now().plusMillis(20));

    await(40);
    service.purgeExpired();

    assertThat(service.isDenylisted("live")).isTrue();
    assertThat(service.isDenylisted("dying")).isFalse();
  }

  private static void await(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
