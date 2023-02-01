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
package org.sonatype.nexus.blobstore.api;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;

/**
 * Tests for {@link BlobRef}.
 *
 * @since 3.0
 */
public class BlobRefTest
{
  @Test
  public void testToString() {
    final BlobRef blobRef = new BlobRef("node", "store", "blobId");
    final String spec = blobRef.toString();
    final BlobRef reconstituted = BlobRef.parse(spec);

    assertThat(reconstituted, is(equalTo(blobRef)));
  }

  @Test
  public void testParseBlobRefWithNodeId() {
    String storeName = "test-store";
    String blobId = "test-blob-id";
    String nodeId = "test-node-id";

    String blobRefString = String.format("%s:%s@%s", storeName, blobId, nodeId);
    BlobRef parsed = BlobRef.parse(blobRefString);

    assertThat(parsed.getBlob(), is(equalTo(blobId)));
    assertThat(parsed.getStore(), is(equalTo(storeName)));
    assertThat(parsed.getNode(), isEmptyOrNullString());
  }

  @Test
  public void testParseBlobRefWithoutNodeId() {
    String storeName = "test-store";
    String blobId = "test-blob-id";

    String blobRefString = String.format("%s@%s", storeName, blobId);
    BlobRef parsed = BlobRef.parse(blobRefString);

    assertThat(parsed.getBlob(), is(equalTo(blobId)));
    assertThat(parsed.getStore(), is(equalTo(storeName)));
    assertThat(parsed.getNode(), isEmptyOrNullString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBlobRefIllegalFormat() {
    BlobRef.parse("wrong-blobref-format/string");
  }
}
