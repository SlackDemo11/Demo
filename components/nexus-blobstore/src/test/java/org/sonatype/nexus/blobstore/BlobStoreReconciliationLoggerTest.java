/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

package org.sonatype.nexus.blobstore;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobId;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.common.app.ApplicationDirectories;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class BlobStoreReconciliationLoggerTest
    extends TestSupport
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock
  private ApplicationDirectories applicationDirectories;

  @Mock
  private BlobStore blobStore;

  @Mock
  private Logger logger;

  @Mock
  private MockedStatic<LoggerFactory> mockedStatic;

  private BlobStoreReconciliationLogger underTest;

  @Before
  public void setUp() throws IOException {
    // mock blob store and its configuration
    BlobStoreConfiguration blobStoreConfiguration = mock(BlobStoreConfiguration.class);
    when(blobStoreConfiguration.getName()).thenReturn("blob-store-name");
    when(blobStore.getBlobStoreConfiguration()).thenReturn(blobStoreConfiguration);

    // mock logger used to actually log blob ids
    mockedStatic.when(() -> LoggerFactory.getLogger("blobstore-reconciliation-log")).thenReturn(logger);
    mockedStatic.when(() -> LoggerFactory.getLogger(BlobStoreReconciliationLogger.class)).thenReturn(mock(Logger.class));

    underTest = new BlobStoreReconciliationLogger(applicationDirectories);
  }

  @After
  public void teardown() {
    if (mockedStatic != null) {
      mockedStatic.close();
    }
  }

  @Test
  public void shouldNotLogTemporaryBlobs() {
    underTest.logBlobCreated(blobStore, new BlobId("tmp$00000000-0000-0000-0000-000000000000"));
    verifyNoInteractions(logger);
  }

  @Test
  public void shouldLogBlobId() {
    underTest.logBlobCreated(blobStore, new BlobId("00000000-0000-0000-0000-000000000000"));

    verify(logger).info("00000000-0000-0000-0000-000000000000");

    mockedStatic.verify(() -> LoggerFactory.getLogger("blobstore-reconciliation-log"));
  }

  @Test
  public void shouldReadBlobIdsLoggedOnAndAfterRequestedDate() throws IOException {
    when(applicationDirectories
        .getWorkDirectory("log" + File.separator + "blobstore" + File.separator + "blob-store-name"))
        .thenReturn(temporaryFolder.getRoot());
    Files.write(temporaryFolder.newFile("2021-04-13").toPath(),
        "2021-04-13 00:00:00,00000000-0000-0000-0000-000000000001".getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE);
    Files.write(temporaryFolder.newFile("2021-04-14").toPath(),
        ("2021-04-14 00:00:00,00000000-0000-0000-0000-000000000002\n" +
            "00000000-0000-0000-0000-000000000003\n" + // corrupted log line
            "2021-04-14 00:00:00,00000000-0000-0000-0000-000000000004\n").getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE);
    Files.write(temporaryFolder.newFile("2021-04-15").toPath(),
        "2021-04-15 00:00:00,00000000-0000-0000-0000-000000000005".getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE);
    // also put some unrelated file to verify it can skip over unrelated files without failing the reconcile process
    Files.write(temporaryFolder.newFile("2021-04-15-rubbish.bak").toPath(),
        "2021-04-14 00:00:00,00000000-0000-0000-0000-000000000006".getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE);

    List<String> result = underTest.getBlobsCreatedSince(blobStore.getBlobStoreConfiguration().getName(), LocalDate.parse("2021-04-14"))
        .map(BlobId::asUniqueString)
        .collect(toList());

    assertThat(result, hasSize(3));
    assertThat(result, containsInAnyOrder(
        "00000000-0000-0000-0000-000000000002",
        "00000000-0000-0000-0000-000000000004",
        "00000000-0000-0000-0000-000000000005"));
  }
}
